package com.thewolf1724.cinetosis

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.thewolf1724.cinetosis.data.Settings
import com.thewolf1724.cinetosis.data.SettingsRepository
import com.thewolf1724.cinetosis.ui.MainScreen
import com.thewolf1724.cinetosis.ui.OnboardingScreen
import com.thewolf1724.cinetosis.ui.theme.CinetosisTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CinetosisTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    val repository = remember { SettingsRepository(context.applicationContext) }
                    val scope = rememberCoroutineScope()

                    // null mientras DataStore carga; luego decide qué pantalla mostrar.
                    val settings by produceState<Settings?>(initialValue = null, repository) {
                        repository.settings.collect { value = it }
                    }

                    when (val current = settings) {
                        null -> Unit // carga breve: superficie vacía
                        else -> if (current.onboardingDone) {
                            MainScreen()
                        } else {
                            OnboardingScreen(
                                onFinish = {
                                    scope.launch { repository.update { it.copy(onboardingDone = true) } }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
