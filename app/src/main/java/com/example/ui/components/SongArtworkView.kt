package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections

object SongArtworkLoader {
    private const val MAX_CACHE_KB = 16 * 1024

    private val bitmapCache = object : LruCache<String, Bitmap>(MAX_CACHE_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.allocationByteCount / 1024).coerceAtLeast(1)
        }
    }

    private val noArtworkIds: MutableSet<String> = Collections.synchronizedSet(
        Collections.newSetFromMap(object : LinkedHashMap<String, Boolean>(256, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
                return size > 1000
            }
        })
    )

    fun getCached(songId: String): Bitmap? = bitmapCache.get(songId)

    fun hasNoArtwork(songId: String): Boolean = noArtworkIds.contains(songId)

    fun loadSync(context: Context, song: Song, maxDimension: Int = 512): Bitmap? {
        bitmapCache.get(song.id)?.let { return it }
        if (noArtworkIds.contains(song.id)) return null

        if (song.uri.startsWith("http://", ignoreCase = true) ||
            song.uri.startsWith("https://", ignoreCase = true)
        ) {
            noArtworkIds.add(song.id)
            return null
        }

        val artBytes = extractEmbeddedPicture(context, song)
        if (artBytes == null || artBytes.isEmpty()) {
            noArtworkIds.add(song.id)
            return null
        }

        val decoded = decodeSampledBitmap(artBytes, maxDimension)
        if (decoded != null) {
            bitmapCache.put(song.id, decoded)
            noArtworkIds.remove(song.id)
        } else {
            noArtworkIds.add(song.id)
        }
        return decoded
    }

    private fun extractEmbeddedPicture(context: Context, song: Song): ByteArray? {
        if (song.dataPath.isNotBlank()) {
            try {
                val file = File(song.dataPath)
                if (file.exists() && file.canRead()) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(song.dataPath)
                        val bytes = retriever.embeddedPicture
                        if (bytes != null && bytes.isNotEmpty()) {
                            return bytes
                        }
                    } finally {
                        try {
                            retriever.release()
                        } catch (_: Exception) {
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        if (song.uri.isNotBlank()) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(song.uri))
                val bytes = retriever.embeddedPicture
                if (bytes != null && bytes.isNotEmpty()) {
                    return bytes
                }
            } catch (_: Exception) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }

        return null
    }

    private fun decodeSampledBitmap(bytes: ByteArray, maxDimension: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val width = bounds.outWidth
            val height = bounds.outHeight
            if (width <= 0 || height <= 0) return null

            var inSampleSize = 1
            while ((width / inSampleSize) > maxDimension || (height / inSampleSize) > maxDimension) {
                inSampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                this.inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
        } catch (_: Exception) {
            null
        }
    }
}

@Composable
fun SongArtworkView(
    song: Song,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    placeholder: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    var bitmap by remember(song.id) {
        mutableStateOf(SongArtworkLoader.getCached(song.id))
    }

    LaunchedEffect(song.id, song.uri, song.dataPath) {
        if (bitmap == null && !SongArtworkLoader.hasNoArtwork(song.id)) {
            val loaded = withContext(Dispatchers.IO) {
                SongArtworkLoader.loadSync(context, song)
            }
            if (loaded != null) {
                bitmap = loaded
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            placeholder()
        }
    }
}
