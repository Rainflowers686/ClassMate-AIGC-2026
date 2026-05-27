package com.classmate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.classmate.app.ui.AppRoot
import com.classmate.app.ui.theme.ClassMateTheme

/**
 * Single-activity host. Hands off to [AppRoot], which owns navigation via
 * the ViewModel's [com.classmate.app.state.ClassMateScreen] state.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClassMateTheme {
                AppRoot()
            }
        }
    }
}
