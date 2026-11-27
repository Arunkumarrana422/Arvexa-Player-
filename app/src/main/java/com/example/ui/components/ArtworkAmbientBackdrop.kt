package com.example.ui.components

import android.graphics.Bitmap
import android.os.Build
import android.util.LruCache
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.domain.model.Song
import com.example.ui.screens.getSongFallbackGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

data class ArtworkAmbientStyle(
    val blurredBitmap: Bitmap?,
    val dominantColor: Color,
    val vibrantColor: Color,
    val averageLuminance: Float
)

data class PlayerAdaptiveColors(
    val primaryText: Color,
    val secondaryText: Color,
    val inactiveIcon: Color,
    val sliderInactiveTrack: Color,
    val pillBackground: Color,
    val pillBorder: Color,
    val activePillBackground: Color
)

object ArtworkAmbientCache {
    private val cache = LruCache<String, ArtworkAmbientStyle>(32)

    fun get(songId: String): ArtworkAmbientStyle? = cache.get(songId)

    fun extractOrGet(songId: String, source: Bitmap): ArtworkAmbientStyle {
        cache.get(songId)?.let { return it }
        val style = buildStyleFromBitmap(source)
        cache.put(songId, style)
        return style
    }

    private fun buildStyleFromBitmap(source: Bitmap): ArtworkAmbientStyle {
        val sampleSize = 28
        val tiny = try {
            Bitmap.createScaledBitmap(source, sampleSize, sampleSize, true)
        } catch (_: Exception) {
            source
        }

        val w = tiny.width.coerceAtLeast(1)
        val h = tiny.height.coerceAtLeast(1)
        val pixels = IntArray(w * h)
        tiny.getPixels(pixels, 0, w, 0, 0, w, h)

        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var sumLum = 0.0

        var vibR = 0.0
        var vibG = 0.0
        var vibB = 0.0
        var vibWeightSum = 0.0

        val hsv = FloatArray(3)
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            sumR += r
            sumG += g
            sumB += b
            sumLum += (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0

            android.graphics.Color.RGBToHSV(r, g, b, hsv)
            val sat = hsv[1]
            val value = hsv[2]
            if (sat > 0.22f && value in 0.18f..0.95f) {
                val weight = (sat * sat * (1f - kotlin.math.abs(value - 0.58f))).toDouble().coerceAtLeast(0.05)
                vibR += r * weight
                vibG += g * weight
                vibB += b * weight
                vibWeightSum += weight
            }
        }

        val count = pixels.size.coerceAtLeast(1)
        val domR = (sumR / count).toInt().coerceIn(0, 255)
        val domG = (sumG / count).toInt().coerceIn(0, 255)
        val domB = (sumB / count).toInt().coerceIn(0, 255)
        val dominant = Color(domR, domG, domB)

        val vibrant = if (vibWeightSum > 0.01) {
            Color(
                (vibR / vibWeightSum).toInt().coerceIn(0, 255),
                (vibG / vibWeightSum).toInt().coerceIn(0, 255),
                (vibB / vibWeightSum).toInt().coerceIn(0, 255)
            )
        } else {
            dominant
        }

        val avgLum = (sumLum / count).toFloat().coerceIn(0f, 1f)

        // Apply 2 passes of separable box blur on the tiny bitmap so even API 24-30 gets
        // a silky multi-tone ambient color cloud matching the album art.
        val blurredPixels = boxBlur(pixels, w, h, radius = 2)
        val blurredPixels2 = boxBlur(blurredPixels, w, h, radius = 2)
        val blurredBitmap = try {
            Bitmap.createBitmap(blurredPixels2, w, h, Bitmap.Config.ARGB_8888)
        } catch (_: Exception) {
            tiny
        }

        if (tiny !== source && tiny !== blurredBitmap) {
            try {
                tiny.recycle()
            } catch (_: Exception) {
            }
        }

        return ArtworkAmbientStyle(
            blurredBitmap = blurredBitmap,
            dominantColor = dominant,
            vibrantColor = vibrant,
            averageLuminance = avgLum
        )
    }

    private fun boxBlur(src: IntArray, width: Int, height: Int, radius: Int): IntArray {
        val temp = IntArray(src.size)
        val out = IntArray(src.size)

        // Horizontal pass
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                val startX = max(0, x - radius)
                val endX = min(width - 1, x + radius)
                for (kx in startX..endX) {
                    val c = src[rowOffset + kx]
                    r += (c shr 16) and 0xFF
                    g += (c shr 8) and 0xFF
                    b += c and 0xFF
                    count++
                }
                temp[rowOffset + x] = (0xFF shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
            }
        }

        // Vertical pass
        for (x in 0 until width) {
            for (y in 0 until height) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                val startY = max(0, y - radius)
                val endY = min(height - 1, y + radius)
                for (ky in startY..endY) {
                    val c = temp[ky * width + x]
                    r += (c shr 16) and 0xFF
                    g += (c shr 8) and 0xFF
                    b += c and 0xFF
                    count++
                }
                out[y * width + x] = (0xFF shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
            }
        }

        return out
    }
}

@Composable
fun rememberSongAmbientStyle(song: Song): ArtworkAmbientStyle {
    val context = LocalContext.current
    val fallbackColors = remember(song.id, song.title) { getSongFallbackGradient(song.id, song.title) }
    val defaultStyle = remember(song.id, song.title) {
        val primary = fallbackColors.firstOrNull() ?: Color(0xFF1E3A8A)
        val secondary = fallbackColors.lastOrNull() ?: Color(0xFF0F172A)
        ArtworkAmbientStyle(
            blurredBitmap = null,
            dominantColor = primary,
            vibrantColor = secondary,
            averageLuminance = ((primary.luminance() + secondary.luminance()) / 2f).coerceIn(0f, 1f)
        )
    }

    var ambientStyle by remember(song.id) {
        mutableStateOf(
            ArtworkAmbientCache.get(song.id) ?: SongArtworkLoader.getCached(song.id)?.let {
                ArtworkAmbientCache.extractOrGet(song.id, it)
            } ?: defaultStyle
        )
    }

    LaunchedEffect(song.id, song.uri, song.dataPath) {
        val cachedStyle = ArtworkAmbientCache.get(song.id)
        if (cachedStyle != null) {
            ambientStyle = cachedStyle
            return@LaunchedEffect
        }
        val bitmap = SongArtworkLoader.getCached(song.id) ?: if (!SongArtworkLoader.hasNoArtwork(song.id)) {
            withContext(Dispatchers.IO) {
                SongArtworkLoader.loadSync(context, song)
            }
        } else null

        if (bitmap != null) {
            val extracted = withContext(Dispatchers.Default) {
                ArtworkAmbientCache.extractOrGet(song.id, bitmap)
            }
            ambientStyle = extracted
        } else {
            ambientStyle = defaultStyle
        }
    }

    return ambientStyle
}

fun resolvePlayerAdaptiveColors(useLightText: Boolean): PlayerAdaptiveColors {
    return if (useLightText) {
        PlayerAdaptiveColors(
            primaryText = Color.White,
            secondaryText = Color.White.copy(alpha = 0.78f),
            inactiveIcon = Color.White.copy(alpha = 0.74f),
            sliderInactiveTrack = Color.White.copy(alpha = 0.28f),
            pillBackground = Color.White.copy(alpha = 0.14f),
            pillBorder = Color.White.copy(alpha = 0.24f),
            activePillBackground = Color.White.copy(alpha = 0.24f)
        )
    } else {
        PlayerAdaptiveColors(
            primaryText = Color(0xFF111827),
            secondaryText = Color(0xFF1F2937).copy(alpha = 0.80f),
            inactiveIcon = Color(0xFF1F2937).copy(alpha = 0.76f),
            sliderInactiveTrack = Color(0xFF111827).copy(alpha = 0.20f),
            pillBackground = Color.White.copy(alpha = 0.42f),
            pillBorder = Color.White.copy(alpha = 0.58f),
            activePillBackground = Color.White.copy(alpha = 0.62f)
        )
    }
}

@Composable
fun MusicPlayerAmbientBackground(
    style: ArtworkAmbientStyle,
    useDeepBackdrop: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedDominant by animateColorAsState(
        targetValue = style.dominantColor,
        animationSpec = tween(durationMillis = 450),
        label = "ambient_dominant"
    )
    val animatedVibrant by animateColorAsState(
        targetValue = style.vibrantColor,
        animationSpec = tween(durationMillis = 450),
        label = "ambient_vibrant"
    )

    val baseTopColor = if (useDeepBackdrop) {
        lerp(animatedDominant, Color(0xFF0B1020), 0.55f)
    } else {
        lerp(animatedVibrant, Color.White, 0.58f)
    }
    val baseBottomColor = if (useDeepBackdrop) {
        lerp(animatedVibrant, Color(0xFF070B14), 0.72f)
    } else {
        lerp(animatedDominant, Color.White, 0.64f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(baseTopColor, baseBottomColor)))
    ) {
        Crossfade(
            targetState = style.blurredBitmap,
            animationSpec = tween(durationMillis = 450),
            label = "ambient_artwork_crossfade"
        ) { bitmap ->
            if (bitmap != null) {
                val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier
                        .fillMaxSize()
                        .blur(26.dp)
                } else {
                    Modifier.fillMaxSize()
                }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                    modifier = blurModifier
                )
            }
        }

        // Contrast-guaranteed adaptive frosted veil so text & icons stay razor-sharp
        // on EVERY thumbnail (pitch-black, bright-white, or vivid multicolor)
        val lum = style.averageLuminance
        val scrimBrush = if (useDeepBackdrop) {
            val topAlpha = (0.42f + lum * 0.30f).coerceIn(0.42f, 0.74f)
            val midAlpha = (0.34f + lum * 0.30f).coerceIn(0.34f, 0.66f)
            val botAlpha = (0.58f + lum * 0.24f).coerceIn(0.58f, 0.82f)
            val deepTint = lerp(animatedDominant, Color(0xFF080C16), 0.78f)
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to deepTint.copy(alpha = topAlpha),
                    0.42f to deepTint.copy(alpha = midAlpha),
                    1.0f to deepTint.copy(alpha = botAlpha)
                )
            )
        } else {
            val darkBoost = (1f - lum).coerceIn(0f, 1f)
            val topAlpha = (0.28f + darkBoost * 0.32f).coerceIn(0.28f, 0.62f)
            val midAlpha = (0.18f + darkBoost * 0.24f).coerceIn(0.18f, 0.44f)
            val botAlpha = (0.36f + darkBoost * 0.28f).coerceIn(0.36f, 0.66f)
            val softVeil = lerp(animatedVibrant, Color.White, 0.76f)
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to softVeil.copy(alpha = topAlpha),
                    0.42f to softVeil.copy(alpha = midAlpha),
                    1.0f to softVeil.copy(alpha = botAlpha)
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimBrush)
        )
    }
}
