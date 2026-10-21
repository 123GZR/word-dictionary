package com.galaxy.worddictionary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import com.galaxy.worddictionary.data.model.WordEntry
import com.galaxy.worddictionary.ui.components.BackIcon
import com.galaxy.worddictionary.ui.components.CrossIcon

/**
 * 错题集：背诵时标记「不认识」的单词回顾列表。
 * TransformingLazyColumn + snap 表圈滚动（表圈驱动，与词库列表一致），
 * 点击词条进入单词详情；可「开始复习」（标记认识自动移出）或「清空错题」。
 */
@Composable
fun WrongBookScreen(
    words: List<WordEntry>,
    onWordClick: (WordEntry) -> Unit,
    onStartReview: () -> Unit,
    onClearWrong: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = listState, modifier = modifier) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state = listState)
        ) {
            // 顶部：返回 + 标题 + 数量
            item(key = "header") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onBack) {
                            BackIcon(sizeDp = 16.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("返回", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            text = "错题集",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CrossIcon(
                            sizeDp = 14.dp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "共 ${words.size} 词 · 转表圈浏览",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (words.isEmpty()) {
                // 空状态
                item(key = "empty") {
                    Text(
                        text = "暂无错题\n背诵时标记「不认识」会记录到这里",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                    )
                }
            } else {
                // 操作区：开始复习 / 清空错题
                item(key = "actions") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(onClick = onStartReview) {
                            Text("开始复习", style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(onClick = onClearWrong) {
                            Text("清空错题", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            text = "复习时标记「认识」自动移出错题",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // 错题列表（稳定 key：word 文本）
                words.forEach { entry ->
                    item(key = "wrong-${entry.word}") {
                        WordRow(entry = entry, onClick = { onWordClick(entry) })
                    }
                }
            }
        }
    }
}
