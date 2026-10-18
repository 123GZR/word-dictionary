package com.galaxy.worddictionary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ChildButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import com.galaxy.worddictionary.data.model.WordEntry
import com.galaxy.worddictionary.ui.StudyOrder
import com.galaxy.worddictionary.ui.components.BookIcon
import com.galaxy.worddictionary.ui.components.SearchIcon

/**
 * 词库浏览列表：TransformingLazyColumn + 表圈 snap 滚动。
 * 顶部：标题/总数进度、顺序/随机背诵入口、搜索框；下方为词条列表。
 */
@Composable
fun WordListScreen(
    words: List<WordEntry>,
    total: Int,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onWordClick: (WordEntry) -> Unit,
    onStartStudy: (StudyOrder) -> Unit,
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
            // 标题 + 总数进度
            item(key = "header") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BookIcon(
                            sizeDp = 18.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "背单词",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "共 $total 词 · 点击查看释义",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 背诵入口：顺序 / 随机
            item(key = "study-entries") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { onStartStudy(StudyOrder.SEQUENTIAL) }) {
                        Text("顺序背", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(onClick = { onStartStudy(StudyOrder.SHUFFLED) }) {
                        Text("随机背", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // 搜索框
            item(key = "search-input") {
                SearchInputRow(
                    onQueryChange = onQueryChange,
                    onClearSearch = onClearSearch
                )
            }

            // 无结果提示
            if (words.isEmpty()) {
                item(key = "no-results") {
                    Text(
                        text = "未找到匹配的词条",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    )
                }
            } else {
                // 词条列表（稳定 key：word 文本）
                words.forEach { entry ->
                    item(key = "word-${entry.word}") {
                        WordRow(entry = entry, onClick = { onWordClick(entry) })
                    }
                }
            }
        }
    }
}

/** 词条行：英文 + 词性/释义（单行省略） */
@Composable
private fun WordRow(
    entry: WordEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ChildButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(
                text = entry.word,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = entry.displayLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 搜索输入行：放大镜图标 + 输入框 + 清空按钮 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchInputRow(
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SearchIcon(
            sizeDp = 16.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = query,
            onValueChange = { newValue ->
                query = newValue
                onQueryChange(newValue)
            },
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            placeholder = {
                Text(
                    text = "搜索单词 / 释义",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            query = ""
                            onClearSearch()
                        },
                        modifier = Modifier
                            .height(28.dp)
                            .padding(end = 4.dp)
                    ) {
                        Text(
                            text = "清空",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = MaterialTheme.shapes.small,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )
    }
}
