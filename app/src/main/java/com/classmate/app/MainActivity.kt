package com.classmate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * Single-activity host. All UI is Compose; navigation is an in-memory back stack driven by
 * [com.classmate.app.state.AppViewModel] (see ClassMateApp / ClassMateNavHost).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ClassMateApp() }
    }
}
