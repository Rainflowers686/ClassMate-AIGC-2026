package com.classmate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.classmate.app.ui.AppRoot

/**
 * Single-activity host. AppRoot owns theme + navigation; this class only
 * forwards onCreate to Compose so the Activity stays free of UI concerns.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppRoot()
        }
    }
}
