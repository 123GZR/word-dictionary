package com.galaxy.worddictionary.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import com.galaxy.worddictionary.data.model.WordEntry
import com.galaxy.worddictionary.ui.StudyState
import com.galaxy.worddictionary.ui.components.BackIcon
import com.galaxy.worddictionary.ui.components.CheckIcon
import com.galaxy.worddictionary.ui.components.CrossIcon
import com.galaxy.worddictionary.ui.components.FlipIcon
import com.galaxy.worddictionary.ui.components.SummaryIcon

/**
 * 背诵模式（交互原型来自 HTML demo：背单词 MVP）：
 * - 正面显示单词，点击/点按卡片翻面显示释义
 * - 背面「认识 / 不认识」标记并自动前进；标记「不认识」同步记入错题集
 * - 转表圈（或上滑）= 第一次翻面显示释义，再转一次跳过（不记录状态）
 * - 「梗概」展示已标记的记得/没记得清单（表圈可滚动）；可清空更改重新开始
 * - 全部答完显示结束统计（认识/不认识 + 再来一组）
 */
@Composable
fun StudyScreen(
    state: StudyState,
    onFlip: () -> Unit,
    onMark: (Boolean) -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSummary by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    // 表圈输入：映射到可滚动状态，每个格值变化触发一次动作（未翻面→翻面显示释义，已翻面→跳过）
    val bezelScroll = remember { ScrollState(0) }
    // FlingBehavior 非 SAM 接口，用对象表达式实现（禁用惯性甩动：表圈一格 = 一次动作）
    val flingBehavior = remember {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .rotaryScrollable(
                behavior = RotaryScrollableDefaults.behavior(bezelScroll, flingBehavior),
                focusRequester = focusRequester
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏：返回 / 标题 / 梗概
            StudyTopBar(
                reviewed = state.reviewed,
                total = state.words.size,
                onBack = onBack,
                onSummary = { showSummary = true }
            )

            // 卡片区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.words.isEmpty() -> Text(
                        text = "准备中…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    state.isComplete -> CompletionContent(
                        known = state.known.size,
                        unknown = state.unknown.size,
                        onRestart = {
                            onReset()
                            showSummary = false
                        },
                        onBack = onBack
                    )

                    else -> state.current?.let { entry ->
                        // key(word)：换词时重建卡片，动画从正面（英文）开始，
                        // 避免旧词翻面动画回落到新词时闪现新词中文释义
                        key(entry.word) {
                            FlipCard(
                                entry = entry,
                                flipped = state.flipped,
                                currentReviewed = state.currentReviewed,
                                onFlip = onFlip,
                                onMark = onMark
                            )
                        }
                    }
                }
            }
        }

        // 梗概覆盖层
        if (showSummary) {
            SummaryOverlay(
                state = state,
                onClose = { showSummary = false },
                onReset = {
                    onReset()
                    showSummary = false
                }
            )
        }
    }

    // 焦点：梗概打开时由梗概层接管表圈焦点，关闭后交还背诵卡片
    LaunchedEffect(showSummary) {
        if (!showSummary) focusRequester.requestFocus()
    }

    // 表圈转动 → 未翻面先翻面显示释义，已翻面则跳过下一词，并把滚动状态归零等待下一次
    LaunchedEffect(bezelScroll.value) {
        if (bezelScroll.value != 0) {
            if (!state.flipped) onFlip() else onSkip()
            bezelScroll.scrollTo(0)
        }
    }
}

/** 顶部栏：返回 / 标题（含进度）/ 梗概 */
@Composable
private fun StudyTopBar(
    reviewed: Int,
    total: Int,
    onBack: () -> Unit,
    onSummary: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                BackIcon(sizeDp = 16.dp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("返回", style = MaterialTheme.typography.labelMedium)
            }
            Text(
                text = "背诵",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onSummary) {
                SummaryIcon(sizeDp = 16.dp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("梗概", style = MaterialTheme.typography.labelMedium)
            }
        }
        Text(
            text = "已标记 $reviewed / $total",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** 翻卡：正/背面 3D 翻转（rotationY + alpha 过渡） */
@Composable
private fun FlipCard(
    entry: WordEntry,
    flipped: Boolean,
    currentReviewed: Boolean,
    onFlip: () -> Unit,
    onMark: (Boolean) -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "flip"
    )
    val density = LocalDensity.current.density

    Box(modifier = Modifier.fillMaxSize()) {
        // 正面（单词）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .alpha(if (rotation <= 90f) 1f else 0f)
                .clickable(enabled = !flipped, onClick = onFlip)
        ) {
            FrontFace(entry = entry, currentReviewed = currentReviewed)
        }

        // 背面（释义 + 认识/不认识）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = rotation - 180f
                    cameraDistance = 12f * density
                }
                .alpha(if (rotation > 90f) 1f else 0f)
        ) {
            BackFace(entry = entry, onMark = onMark)
        }
    }
}

/** 卡片正面：单词 + 词性 + 操作提示 */
@Composable
private fun FrontFace(entry: WordEntry, currentReviewed: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = entry.word,
            style = MaterialTheme.typography.displaySmall.copy(fontSize = wordFontSize(entry.word.length)),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        if (entry.pos.isNotBlank()) {
            Text(
                text = entry.pos,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlipIcon(sizeDp = 14.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (currentReviewed) "已标记 · 转表圈看释义" else "点击翻面 · 转表圈看释义",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 卡片背面：词性/释义 + 认识/不认识 */
@Composable
private fun BackFace(entry: WordEntry, onMark: (Boolean) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = entry.word,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = entry.displayLine,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlipIcon(sizeDp = 12.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "转表圈跳过",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { onMark(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                CheckIcon(sizeDp = 16.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("认识", style = MaterialTheme.typography.labelMedium)
            }
            Button(
                onClick = { onMark(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                CrossIcon(sizeDp = 16.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("不认识", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** 结束统计 */
@Composable
private fun CompletionContent(
    known: Int,
    unknown: Int,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "本轮完成",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCell(label = "认识", value = known, color = MaterialTheme.colorScheme.secondary)
            StatCell(label = "没记住", value = unknown, color = MaterialTheme.colorScheme.tertiary)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRestart) {
            Text("再来一组", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onBack) {
            Text("返回列表", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun StatCell(label: String, value: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleLarge,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 梗概覆盖层：记得/没记得清单 + 清空更改；打开时接管表圈焦点，转表圈滚动清单 */
@Composable
private fun SummaryOverlay(
    state: StudyState,
    onClose: () -> Unit,
    onReset: () -> Unit
) {
    val knownWords = state.words.filter { it.id in state.known }
    val unknownWords = state.words.filter { it.id in state.unknown }
    val scrollState = rememberScrollState()
    val summaryFocus = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(summaryFocus)
                .rotaryScrollable(
                    behavior = RotaryScrollableDefaults.behavior(scrollState),
                    focusRequester = summaryFocus
                )
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "我的梗概",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            SummaryGroup(
                title = "记得 (${knownWords.size})",
                color = MaterialTheme.colorScheme.secondary,
                words = knownWords
            )
            SummaryGroup(
                title = "没记得 (${unknownWords.size})",
                color = MaterialTheme.colorScheme.tertiary,
                words = unknownWords
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = onClose) {
                    Text("返回学习", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onReset) {
                    Text("清空更改", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    // 梗概打开时接管表圈焦点（转表圈滚动清单）
    LaunchedEffect(Unit) {
        summaryFocus.requestFocus()
    }
}

@Composable
private fun SummaryGroup(
    title: String,
    color: androidx.compose.ui.graphics.Color,
    words: List<WordEntry>
) {
    Column(modifier = Modifier.padding(top = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(color, shape = MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
        if (words.isEmpty()) {
            Text(
                text = "暂无",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            words.forEach { entry ->
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = entry.word,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = entry.displayLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** 长单词自动缩小字号（仿 demo 的 fitWord） */
private fun wordFontSize(length: Int): androidx.compose.ui.unit.TextUnit = when {
    length >= 14 -> 20.sp
    length >= 11 -> 24.sp
    length >= 9 -> 28.sp
    else -> 32.sp
}
