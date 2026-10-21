package com.galaxy.worddictionary.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 错题集实现：SharedPreferences 持久化错题词条 id 集合（StringSet）。
 *
 * 注意：getStringSet 返回的 Set 是底层引用，读取时一律复制副本，
 * 写入前重新构造新 Set，避免直接修改底层导致不触发保存。
 */
@Singleton
class LocalWrongBookRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : WrongBookRepository {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun loadWrongIds(): Set<String> =
        prefs.getStringSet(KEY_WRONG_IDS, emptySet())
            ?.toHashSet()
            ?: emptySet()

    override fun addWrong(id: String) {
        val current = loadWrongIds()
        if (id in current) return
        prefs.edit()
            .putStringSet(KEY_WRONG_IDS, current + id)
            .apply()
    }

    override fun removeWrong(id: String) {
        val current = loadWrongIds()
        if (id !in current) return
        prefs.edit()
            .putStringSet(KEY_WRONG_IDS, current - id)
            .apply()
    }

    override fun clearWrong() {
        prefs.edit()
            .remove(KEY_WRONG_IDS)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "word_dictionary_prefs"
        const val KEY_WRONG_IDS = "wrong_book_ids"
    }
}
