package com.pianoacademy

import android.media.audiofx.BassBoost
import android.media.audiofx.PresetReverb
import android.os.Bundle
import android.util.Log
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

    // 오디오 이펙트: 앱 실행 중에만 적용 (포그라운드 한정)
    private var reverb: PresetReverb? = null
    private var bassBoost: BassBoost? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        // 피아노 음질 향상: 소방 리버브 + 저음 강조
        // session 0 = 출력 믹스 전체에 적용 (앱 포그라운드 중에만)
        try {
            reverb = PresetReverb(0, 0).apply {
                preset = PresetReverb.PRESET_SMALLROOM
                enabled = true
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "PresetReverb not supported: ${e.message}")
        }
        try {
            bassBoost = BassBoost(0, 0).apply {
                setStrength(350)   // 0~1000 (HTML: +5dB @ 250Hz 에 해당하는 수준)
                enabled = true
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "BassBoost not supported: ${e.message}")
        }

        setContent {
            PianoAcademyTheme {
                val vm: PianoViewModel = viewModel()

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

    override fun onDestroy() {
        super.onDestroy()
        try { reverb?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
    }
}
