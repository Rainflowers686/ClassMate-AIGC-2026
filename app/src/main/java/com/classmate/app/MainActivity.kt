package com.classmate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.classmate.app.ui.HomeScreen
import com.classmate.app.ui.theme.ClassMateTheme

/**
 * Single-activity host for the v0.2.5 probe.
 *
 * We intentionally avoid a ViewModel here — the probe has one button and one
 * list; introducing MVVM scaffolding would obscure the wiring that's actually
 * being demonstrated (provider → validator → render). v0.3 will graduate to
 * a ViewModel per screen.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClassMateTheme {
                HomeScreen(context = applicationContext)
            }
        }
    }
}
