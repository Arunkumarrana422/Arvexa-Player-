package com.example.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.example.BuildConfig

object FeedbackUtils {
    const val FEEDBACK_EMAIL = "ranaarunrana7079@gmail.com"

    fun sendFeedbackEmail(context: Context) {
        try {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$FEEDBACK_EMAIL")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Arvexa Player - Feedback & Support (v${BuildConfig.VERSION_NAME})"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    """
                    Hi Arun,

                    App Version: ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})
                    Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})

                    Feedback / Bug Report / Feature Suggestion:
                    
                    """.trimIndent()
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooser = Intent.createChooser(emailIntent, "Send Feedback via Email").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            // Fallback: Copy email address to clipboard and inform user
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText("Feedback Email", FEEDBACK_EMAIL)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(
                    context,
                    "Email copied to clipboard: $FEEDBACK_EMAIL",
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "Please email your feedback to: $FEEDBACK_EMAIL",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
