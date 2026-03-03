package com.pianoacademy

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pianoacademy.ui.PianoScreen
import com.pianoacademy.ui.theme.PianoAcademyTheme
import com.pianoacademy.viewmodel.PianoViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 화면 항상 켜짐 (연주 중 화면 꺼짐 방지)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()

        setContent {
            PianoAcademyTheme {
                val vm: PianoViewModel = viewModel()

                // 화면 회전 감지
                val config = resources.configuration
                LaunchedEffect(config.orientation) {
                    val isLandscape = config.orientation ==
                        android.content.res.Configuration.ORIENTATION_LANDSCAPE
                    vm.setLandscape(isLandscape)
                }

                PianoScreen(
                    vm = vm,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // 앱이 백그라운드로 가면 소리 정지
    }
}
