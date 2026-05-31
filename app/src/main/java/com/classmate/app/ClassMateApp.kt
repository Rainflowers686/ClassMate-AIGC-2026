package com.classmate.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classmate.app.navigation.ClassMateNavHost
import com.classmate.app.state.AppViewModel
import com.classmate.app.ui.theme.ClassMateTheme
import kotlinx.coroutines.delay

@Composable
fun ClassMateApp(viewModel: AppViewModel = viewModel()) {
    val ui = viewModel.ui
    val darkTheme = ui.darkMode ?: isSystemInDarkTheme()

    ClassMateTheme(themeOption = ui.theme, darkTheme = darkTheme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                ClassMateNavHost(viewModel)

                val toast = ui.toast
                if (toast != null) {
                    LaunchedEffect(toast) {
                        delay(2200)
                        viewModel.consumeToast()
                    }
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                    ) { androidx.compose.material3.Text(toast) }
                }
            }
        }
    }
}
