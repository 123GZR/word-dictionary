package com.galaxy.worddictionary.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * 词库整体结构：与 assets/words.json 顶层对象对应（由 tools/parse_vocabulary.py 生成）。
 */
@Immutable
@Serializable
data class WordBook(
    val total: Int = 0,
    val generatedAt: String = "",
    val source: String = "",
    val words: List<WordEntry> = emptyList()
)

/**
 * 词条模型（@Immutable：Compose 编译器视为稳定类型，
 * 列表刷新时内容未变则跳过子树重组，避免手表上全列表无谓重组）。
 *
 * @param word 英文（单词或短语）
 * @param pos 词性标注（v./n./a./ad./prep./conj./pron./num./art. 及组合，可能为空）
 * @param meaning 中文释义（多义词 ①②③ 标注原样保留）
 * @param wordForms 词性变化/派生词链（源文件无 → 链时为空列表）
 * @param raw 源文件原始行，容错保留，便于追溯
 */
@Immutable
@Serializable
data class WordEntry(
    val word: String = "",
    val pos: String = "",
    val meaning: String = "",
    val wordForms: List<String> = emptyList(),
    val raw: String = ""
) {
    /** 列表/详情展示用：词性 + 释义 合并行 */
    val displayLine: String
        get() = if (pos.isBlank()) meaning else "$pos$meaning"

    /** 词条在词库中的稳定标识：以英文词条文本为键 */
    val id: String get() = word
}
