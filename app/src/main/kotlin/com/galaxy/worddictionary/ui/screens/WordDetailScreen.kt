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
import androidx.compose.ui.text.style.TextOverflow
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

/**
 * 单词详情：英文、词性、释义、词性变化链，以及「开始背诵」入口。
 * 表圈滚动浏览（TransformingLazyColumn）。
 */
@Composable
fun WordDetailScreen(
    entry: WordEntry,
    onBack: () -> Unit,
    onStartStudy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = listState, modifier = modifier) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state = listState)
        ) {
            // 顶部：返回 + 标题
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onBack) {
                        BackIcon(sizeDp = 16.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("返回", style = MaterialTheme.typography.labelMedium)
                    }
                    Text(
                        text = "单词详情",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            // 英文
            item(key = "word") {
                Text(
                    text = entry.word,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            // 词性
            if (entry.pos.isNotBlank()) {
                item(key = "pos") {
                    Text(
                        text = entry.pos,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 释义
            item(key = "meaning") {
                Text(
                    text = entry.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }

            // 词性变化/派生链
            entry.wordForms.forEachIndexed { index, form ->
                item(key = "form-$index") {
                    Text(
                        text = "→ $form",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                }
            }

            // 开始背诵入口（顺序）
            item(key = "study") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = onStartStudy) {
                        Text("开始背诵", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
