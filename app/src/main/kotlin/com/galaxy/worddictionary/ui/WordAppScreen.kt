package com.galaxy.worddictionary.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.material3.AppScaffold
import com.galaxy.worddictionary.data.model.WordEntry
import com.galaxy.worddictionary.ui.screens.ErrorScreen
import com.galaxy.worddictionary.ui.screens.LoadingScreen
import com.galaxy.worddictionary.ui.screens.StudyScreen
import com.galaxy.worddictionary.ui.screens.WordDetailScreen
import com.galaxy.worddictionary.ui.screens.WordListScreen

/**
 * 顶层导航根：使用 navigation-compose 的 [NavHost] 管理三个目的地
 * （list / detail/{word} / study）。
 *
 * 说明：不使用 Wear Compose 的 SwipeDismissableNavHost——其内部在 Wear OS 6
 * （API 36）上走 PredictiveBackNavHost（预测性返回路径），1.6.2 版本存在首屏内容
 * 不渲染的缺陷（真机仅显示 TimeText、内容区黑屏；官方修复在 1.7.x，而 1.7.x 要求
 * API 37+）。改用普通 NavHost 后真机验证正常；右滑返回手势随之放弃，
 * 系统返回键仍由 NavController 处理。
 */
@Composable
fun WordAppScreen(
    viewModel: WordViewModel,
    modifier: Modifier = Modifier
) {
    val nav = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val studyState by viewModel.studyState.collectAsState()

    // 稳定 lambda：避免每次重组产生新实例导致下游无谓重组
    val onBack = remember(nav) {
        {
            nav.popBackStack()
            Unit
        }
    }
    val onStartStudy = remember(viewModel, nav) {
        { order: StudyOrder ->
            viewModel.startStudy(order)
            nav.navigate("study") { launchSingleTop = true }
            Unit
        }
    }
    val onWordClick = remember(viewModel, nav) {
        { entry: WordEntry ->
            nav.navigate("detail/${Uri.encode(entry.word)}")
            Unit
        }
    }

    AppScaffold(modifier = modifier) {
        NavHost(navController = nav, startDestination = "list") {
            composable("list") {
                when (val s = uiState) {
                    is WordUiState.Loading -> LoadingScreen()
                    is WordUiState.Success -> WordListScreen(
                        words = s.displayWords,
                        total = s.allWords.size,
                        onQueryChange = viewModel::updateSearchQuery,
                        onClearSearch = viewModel::clearSearch,
                        onWordClick = onWordClick,
                        onStartStudy = onStartStudy
                    )
                    is WordUiState.Error -> ErrorScreen(onRetry = viewModel::loadWords)
                }
            }
            composable("detail/{word}") { entry ->
                val word = Uri.decode(entry.arguments?.getString("word").orEmpty())
                val found = (uiState as? WordUiState.Success)
                    ?.allWords
                    ?.firstOrNull { it.word == word }
                if (found != null) {
                    WordDetailScreen(
                        entry = found,
                        onBack = onBack,
                        onStartStudy = { onStartStudy(StudyOrder.SEQUENTIAL) }
                    )
                } else {
                    // 词条不存在（理论不可达）：直接返回
                    LaunchedEffect(Unit) { nav.popBackStack() }
                }
            }
            composable("study") {
                StudyScreen(
                    state = studyState,
                    onFlip = viewModel::flipCard,
                    onMark = viewModel::markCurrent,
                    onSkip = viewModel::skipNext,
                    onReset = viewModel::resetStudy,
                    onBack = onBack
                )
            }
        }
    }
}
