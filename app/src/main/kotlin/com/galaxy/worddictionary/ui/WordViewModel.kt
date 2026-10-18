package com.galaxy.worddictionary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxy.worddictionary.data.DictionaryRepository
import com.galaxy.worddictionary.data.WordDataLoadException
import com.galaxy.worddictionary.data.model.WordEntry
import com.galaxy.worddictionary.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import javax.inject.Inject

/** 词库主界面状态：单一 StateFlow 合并加载/数据/错误，一次刷新一轮重组 */
sealed interface WordUiState {
    data object Loading : WordUiState

    data class Success(
        /** 完整词库（背诵会话数据源，不受搜索过滤影响） */
        val allWords: List<WordEntry>,
        /** 当前列表展示词条（搜索过滤后；空查询时为全部） */
        val displayWords: List<WordEntry>,
        val query: String = ""
    ) : WordUiState

    data class Error(val type: ErrorType) : WordUiState

    enum class ErrorType { DATA_LOAD_FAILED }
}

/** 背诵顺序 */
enum class StudyOrder { SEQUENTIAL, SHUFFLED }

/**
 * 背诵会话状态。
 *
 * @param words 会话词序（顺序或随机打乱后的完整词库）
 * @param index 当前词索引
 * @param flipped 是否已翻面（正/背面）
 * @param known 已标记「认识」的词条 id 集合
 * @param unknown 已标记「不认识」的词条 id 集合
 * @param order 本次会话顺序
 */
data class StudyState(
    val words: List<WordEntry> = emptyList(),
    val index: Int = 0,
    val flipped: Boolean = false,
    val known: Set<String> = emptySet(),
    val unknown: Set<String> = emptySet(),
    val order: StudyOrder = StudyOrder.SEQUENTIAL
) {
    val current: WordEntry? get() = words.getOrNull(index)
    val reviewed: Int get() = known.size + unknown.size
    val isComplete: Boolean get() = words.isNotEmpty() && reviewed >= words.size
    val currentReviewed: Boolean
        get() = current?.let { it.id in known || it.id in unknown } ?: false
}

/**
 * 词库 ViewModel：词库加载、搜索防抖、背诵会话状态管理。
 * 数据流：UI 事件 → ViewModel → StateFlow<WordUiState>/StateFlow<StudyState> → UI 重组。
 */
@HiltViewModel
class WordViewModel @Inject constructor(
    private val dictionaryRepository: DictionaryRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineContext
) : ViewModel() {

    private val _uiState = MutableStateFlow<WordUiState>(WordUiState.Loading)
    val uiState: StateFlow<WordUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _studyState = MutableStateFlow(StudyState())
    val studyState: StateFlow<StudyState> = _studyState.asStateFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null

    init {
        loadWords()
    }

    /** 首次加载词库（assets 解析在 IO 调度器执行，避免主线程卡顿） */
    fun loadWords() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch(ioDispatcher) {
            _uiState.value = WordUiState.Loading
            _uiState.value = try {
                val words = dictionaryRepository.getAllWords()
                WordUiState.Success(
                    allWords = words,
                    displayWords = words
                )
            } catch (e: WordDataLoadException) {
                WordUiState.Error(WordUiState.ErrorType.DATA_LOAD_FAILED)
            } catch (e: Exception) {
                WordUiState.Error(WordUiState.ErrorType.DATA_LOAD_FAILED)
            }
        }
    }

    // ---- 搜索 ----

    /** 更新搜索关键字并触发防抖刷新（200ms），避免每键触发全量过滤 */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            applySearch(query)
        }
    }

    /** 清空搜索 */
    fun clearSearch() {
        _searchQuery.value = ""
        searchJob?.cancel()
        applySearch("")
    }

    private fun applySearch(query: String) {
        val result = dictionaryRepository.searchWords(query)
        _uiState.update { state ->
            if (state is WordUiState.Success) {
                state.copy(displayWords = result, query = query)
            } else {
                state
            }
        }
    }

    // ---- 背诵会话 ----

    /** 开始背诵会话：顺序或随机（随机使用 shuffled 一次成型，会话内顺序固定） */
    fun startStudy(order: StudyOrder) {
        val allWords = (_uiState.value as? WordUiState.Success)?.allWords
            ?: dictionaryRepository.getAllWords()
        val words = when (order) {
            StudyOrder.SEQUENTIAL -> allWords
            StudyOrder.SHUFFLED -> allWords.shuffled()
        }
        _studyState.value = StudyState(words = words, order = order)
    }

    /** 翻面：显示释义（正→背） */
    fun flipCard() {
        val s = _studyState.value
        if (s.current == null || s.isComplete) return
        _studyState.value = s.copy(flipped = !s.flipped)
    }

    /** 标记当前词「认识/不认识」并前进到下一词 */
    fun markCurrent(known: Boolean) {
        val s = _studyState.value
        val current = s.current ?: return
        if (s.isComplete) return

        val newKnown = if (known) s.known + current.id else s.known
        val newUnknown = if (known) s.unknown else s.unknown + current.id
        _studyState.value = s.copy(
            known = newKnown,
            unknown = newUnknown,
            flipped = false,
            index = advanceIndex(s, newKnown.size + newUnknown.size)
        )
    }

    /** 跳过（转表圈/上滑）：不记录状态，前进到下一词 */
    fun skipNext() {
        val s = _studyState.value
        if (s.words.isEmpty() || s.isComplete) return
        _studyState.value = s.copy(
            index = (s.index + 1) % s.words.size,
            flipped = false
        )
    }

    /** 清空更改并重新开始（保持会话词序；随机模式重新打乱） */
    fun resetStudy() {
        val s = _studyState.value
        if (s.words.isEmpty()) return
        _studyState.value = StudyState(
            words = if (s.order == StudyOrder.SHUFFLED) s.words.shuffled() else s.words,
            order = s.order
        )
    }

    /** 前移索引：全部答完时停留在完成态，否则循环 */
    private fun advanceIndex(s: StudyState, newReviewed: Int): Int {
        if (s.words.isEmpty()) return 0
        val next = s.index + 1
        if (next >= s.words.size) {
            return if (newReviewed >= s.words.size) s.index else 0
        }
        return next
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 200L
    }
}
