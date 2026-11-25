package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ThemeMode
import com.example.ui.IFintechRoot
import com.example.ui.theme.IFintechTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SessionUiState

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.Factory((application as IFintechApplication).container)
    }

    private val financeViewModel: FinanceViewModel by viewModels {
        FinanceViewModel.Factory((application as IFintechApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val session by mainViewModel.sessionState.collectAsStateWithLifecycle()
            val themeMode = when (val s = session) {
                is SessionUiState.Authenticated -> ThemeMode.fromKey(s.setting.theme)
                else -> ThemeMode.SYSTEM
            }
            IFintechTheme(themeMode = themeMode) {
                IFintechRoot(
                    mainViewModel = mainViewModel,
                    financeViewModel = financeViewModel
                )
            }
        }
    }
}
