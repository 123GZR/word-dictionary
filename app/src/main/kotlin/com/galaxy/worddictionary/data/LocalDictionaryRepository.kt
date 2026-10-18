package com.galaxy.worddictionary.data

import android.content.Context
import com.galaxy.worddictionary.data.model.WordBook
import com.galaxy.worddictionary.data.model.WordEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 内置词库实现：从 assets/words.json 加载并解析（单例缓存，首次访问后常驻内存）。
 *
 * 词库由 tools/parse_vocabulary.py 从《高频词汇总.txt》生成：
 * 1269 条结构化词条，kotlinx-serialization 可解析。
 * 解析失败抛 [WordDataLoadException]，由 ViewModel 映射为错误态。
 */
@Singleton
class LocalDictionaryRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : DictionaryRepository {

    /** 共享 Json 反序列化器：忽略未知键，词库结构升级时保持兼容 */
    private val json = Json { ignoreUnknownKeys = true }

    /** 单例缓存：词库加载后常驻内存，避免重复读文件/反序列化 */
    private val book: WordBook by lazy {
        try {
            val jsonText = context.assets
                .open(WORDS_ASSET)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            json.decodeFromString(WordBook.serializer(), jsonText)
        } catch (e: Exception) {
            throw WordDataLoadException("词库加载失败（$WORDS_ASSET）", e)
        }
    }

    override fun getAllWords(): List<WordEntry> = book.words

    override fun searchWords(query: String): List<WordEntry> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return book.words

        val lowerQuery = trimmed.lowercase()
        return book.words.filter { entry ->
            entry.word.lowercase().contains(lowerQuery) ||
                entry.meaning.contains(trimmed)
        }
    }

    override fun getWord(id: String): WordEntry? =
        book.words.firstOrNull { it.word == id }

    companion object {
        private const val WORDS_ASSET = "words.json"
    }
}
