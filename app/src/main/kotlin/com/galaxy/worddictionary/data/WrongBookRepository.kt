package com.galaxy.worddictionary.data

/**
 * 错题集仓库接口：记录背诵时标记「不认识」的词条 id（持久化，跨会话保留）。
 * UI/ViewModel 层只依赖本接口，不感知存储实现。
 */
interface WrongBookRepository {

    /** 读取全部错题词条 id 集合 */
    fun loadWrongIds(): Set<String>

    /** 将词条 id 加入错题集（幂等） */
    fun addWrong(id: String)

    /** 将词条 id 移出错题集（复习时标记「认识」） */
    fun removeWrong(id: String)

    /** 清空错题集 */
    fun clearWrong()
}
