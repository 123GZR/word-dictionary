package com.galaxy.worddictionary.data

import com.galaxy.worddictionary.data.model.WordEntry

/**
 * 词典仓库接口：UI/ViewModel 层只依赖本接口，不感知数据来源。
 */
interface DictionaryRepository {

    /**
     * 按关键字搜索词条：
     * - 英文前缀/包含匹配（不区分大小写）
     * - 中文释义包含匹配
     * - 空查询返回全部词条
     */
    fun searchWords(query: String): List<WordEntry>

    /** 返回全部词条（词库浏览与背诵会话数据源） */
    fun getAllWords(): List<WordEntry>

    /** 按词条 id（英文词条文本）查询单个词条 */
    fun getWord(id: String): WordEntry?
}
