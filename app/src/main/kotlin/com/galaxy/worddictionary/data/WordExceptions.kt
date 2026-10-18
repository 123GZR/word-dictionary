package com.galaxy.worddictionary.data

/**
 * 词库数据源业务异常。
 */

/** 词库数据加载/解析失败（assets/words.json 缺失或格式错误） */
class WordDataLoadException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
