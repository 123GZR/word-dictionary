package com.galaxy.worddictionary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.galaxy.worddictionary.ui.WordAppScreen
import com.galaxy.worddictionary.ui.WordViewModel
import com.galaxy.worddictionary.ui.theme.WordDictionaryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** 依赖由 Hilt 注入（见 di/DataModule.kt 与 LocalDictionaryRepository 的 @Inject 构造） */
    private val viewModel: WordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 主题已在 manifest 中声明（Theme.WordDictionary），无需重复 setTheme

        setContent {
            WordDictionaryTheme {
                WordAppScreen(viewModel = viewModel)
            }
        }
    }
}
