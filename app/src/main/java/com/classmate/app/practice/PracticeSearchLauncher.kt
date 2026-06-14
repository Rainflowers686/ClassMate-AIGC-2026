package com.classmate.app.practice

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.classmate.core.practice.PracticeSearchLink

/**
 * Opens a practice search in the SYSTEM browser via ACTION_VIEW. ClassMate never fetches or crawls the
 * results itself — it only hands a public results URL to the OS. Returns false when no browser/activity
 * can handle it, so the caller can fall back to showing the keywords for manual search.
 */
object PracticeSearchLauncher {

    fun viewIntent(link: PracticeSearchLink): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(link.url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

    fun clipboardText(link: PracticeSearchLink): String = link.query

    fun open(context: Context, link: PracticeSearchLink): Boolean =
        try {
            context.startActivity(viewIntent(link))
            true
        } catch (_: Exception) {
            false
        }
}
