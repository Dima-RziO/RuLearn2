package ru.dimarzio.rulearn2.viewmodels

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.dimarzio.rulearn2.application.Database
import ru.dimarzio.rulearn2.models.Level
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.groupBy
import ru.dimarzio.rulearn2.utils.normalized
import ru.dimarzio.rulearn2.utils.replaceKeys
import ru.dimarzio.rulearn2.utils.replaceValues
import ru.dimarzio.rulearn2.utils.replaceValuesCompat
import ru.dimarzio.rulearn2.utils.whether
import java.io.File
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.filter

typealias Filter = (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit

abstract class CourseViewModel(
    private val database: Database,
    val course: String,
    lifecycle: Lifecycle
) : ViewModel(), (Throwable) -> Unit {
    private val _words = MutableStateFlow(emptyMap<Int, Word>())
    private val _levels = MutableStateFlow(emptyMap<String, Level>())

    private val _showLoadingIndicator = MutableStateFlow(false)

    private var searchResults by mutableStateOf(emptyMap<Int, Word>())

    val showLoadingIndicator = _showLoadingIndicator.asStateFlow()

    val words = _words.asStateFlow()
    val levels = _levels.asStateFlow()

    var showSearchingIndicator by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
        private set
    var filterRepeat by mutableStateOf(true)
        private set
    var filterNotRepeat by mutableStateOf(true)
        private set
    var filterDifficult by mutableStateOf(true)
        private set
    var filterNotDifficult by mutableStateOf(true)
        private set
    var filterSkip by mutableStateOf(true)
        private set
    var filterNotSkip by mutableStateOf(true)
        private set
    var filterLearned by mutableStateOf(true)
        private set
    var filterNotLearned by mutableStateOf(true)
        private set

    val filteredSearchResults by derivedStateOf {
        val repeatLambda: Map<Int, Word>.() -> Map<Int, Word> = {
            filterNot { (_, word) -> word.isRepeat }
        }
        val notRepeatLambda: Map<Int, Word>.() -> Map<Int, Word> = {
            filter { (_, word) -> word.isRepeat }
        }
        val difficultLambda: Map<Int, Word>.() -> Map<Int, Word> = {
            filterNot { (_, word) -> word.difficult }
        }
        val notDifficultLambda: Map<Int, Word>.() -> Map<Int, Word> = {
            filter { (_, word) -> word.difficult }
        }
        val skipLambda: Map<Int, Word>.() -> Map<Int, Word> = {
            filterNot { (_, word) -> word.skip }
        }
        val notSkipLambda: Map<Int, Word>.() -> Map<Int, Word> = {
            filter { (_, word) -> word.skip }
        }
        val learnedLambda: Map<Int, Word>.() -> Map<Int, Word> = {
            filterNot { (_, word) -> word.learned }
        }
        val notLearnedLambda: Map<Int, Word>.() -> Map<Int, Word> = {
            filter { (_, word) -> word.learned }
        }

        searchResults
            .whether(!filterRepeat, repeatLambda)
            .whether(!filterNotRepeat, notRepeatLambda)
            .whether(!filterDifficult, difficultLambda)
            .whether(!filterNotDifficult, notDifficultLambda)
            .whether(!filterSkip, skipLambda)
            .whether(!filterNotSkip, notSkipLambda)
            .whether(!filterLearned, learnedLambda)
            .whether(!filterNotLearned, notLearnedLambda)
            .groupBy { (_, word) -> word.level }
    }

    val locale = Locale(course.substring(0..1))

    init {
        viewModelScope.launch(Dispatchers.Default) {
            _showLoadingIndicator.value = true

            runCatching { database.getWords(course) }
                .onFailure(::invoke)
                .onSuccess { words ->
                    _words.value = words
                    words.values.map(Word::level).distinct().forEach { level ->
                        _levels.value += level to getLevel(level)
                    }
                }

            _showLoadingIndicator.value = false
        }

        lifecycle.addObserver(
            LifecycleEventObserver { _, e ->
                if (e == Lifecycle.Event.ON_RESUME) {
                    _levels.value = _levels.value.replaceValuesCompat(
                        with = { (name, level) ->
                            val count = _words.value.count { (_, word) ->
                                word.level == name && word.isRepeat
                            }
                            level.copy(toRepeat = count)
                        },
                        predicate = { (name, level) ->
                            level.toRepeat < _words.value.count { (_, word) ->
                                word.level == name && word.isRepeat
                            }
                        }
                    )
                }
            }
        )
    }

    private fun getLevel(name: String) = with(_words.value.values.filter { it.level == name }) {
        Level(
            total = count { word -> !word.skip },
            learned = count { word -> word.learned && !word.skip },
            toRepeat = count(Word::isRepeat),
            difficult = count(Word::isDifficult)
        )
    }

    private fun Word.delete() {
        runCatching { audios?.forEach(File::delete) }.onFailure(::invoke)
    }

    fun getWord(id: Int) = _words.value[id]

    fun updateWord(id: Int, word: Word) {
        runCatching { database.updateWord(course, id, word) }.onFailure(::invoke)

        val changedLevel = _words.value
            .entries
            .find { id == it.key && word.level != it.value.level }
            ?.value
            ?.level

        if (_words.value.any { entry -> entry.key == id }) {
            _words.value = _words.value.replaceValues(word) { it.key == id }
        } else {
            _words.value += id to word
        }

        if (_levels.value.any { (name, _) -> name == word.level }) {
            // getLevel iterates words, so words flow should be first updated
            _levels.value = _levels.value.replaceValues(getLevel(word.level)) { (name, _) ->
                name == word.level
            }
        } else {
            _levels.value += word.level to getLevel(word.level)
        }

        if (changedLevel != null) {
            _levels.value = _levels.value.replaceValues(getLevel(changedLevel)) {
                it.key == changedLevel
            }
        }

        searchResults = searchResults.replaceValues(word) { it.key == id }
    }

    fun removeWord(id: Int) {
        runCatching { database.deleteWord(course, id) }.onFailure(::invoke)

        _words.value[id]?.let { word ->
            word.delete()
            _levels.value = _levels.value.replaceValues(getLevel(word.level)) { (name, _) ->
                name == word.level
            }
        }

        _words.value -= id
        searchResults -= id
    }

    fun deleteLevel(name: String) {
        database.deleteLevel(course, name)

        _words.value = _words.value.minus(
            _words.value
                .filter { (_, word) -> word.level == name }
                .onEach { (_, word) -> runCatching { word.delete() }.onFailure(::invoke) }
                .keys
        )

        _levels.value -= name

        searchResults.forEach { (id, word) ->
            if (word.level == name) {
                searchResults -= id
            }
        }
    }

    fun confirmRenameLevel(to: String) = _levels.value.none { (name, _) -> name == to }

    fun renameLevel(from: String, to: String) {
        runCatching { database.renameLevel(course, from, to) }.onFailure(::invoke)

        val fromModel = _levels.value[from]

        if (fromModel != null) {
            val toModel = _levels.value[to]

            _levels.value = if (toModel != null) {
                _levels.value.replaceValues(toModel + fromModel) { (name, _) -> name == to }
            } else {
                _levels.value.replaceKeys(to) { (name, _) -> name == from }
            }
            _levels.value -= from
        }

        _words.value = _words.value.replaceValuesCompat(
            with = { (_, word) -> word.copy(level = to) },
            predicate = { (_, word) -> word.level == from }
        )
    }

    fun filter(
        repeat: Boolean,
        notRepeat: Boolean,
        difficult: Boolean,
        notDifficult: Boolean,
        skip: Boolean,
        notSkip: Boolean,
        learned: Boolean,
        notLearned: Boolean
    ) {
        filterRepeat = repeat
        filterNotRepeat = notRepeat
        filterDifficult = difficult
        filterNotDifficult = notDifficult
        filterSkip = skip
        filterNotSkip = notSkip
        filterLearned = learned
        filterNotLearned = notLearned
    }

    fun updateQuery(with: String) {
        query = with
    }

    fun search(scope: CoroutineScope) = scope.async(Dispatchers.Default) {
        showSearchingIndicator = true

        val normalized = query.normalized()

        searchResults = _words.value.filter { (id, word) ->
            normalized in word.normalizedName
                    || normalized in word.normalizedTranslation
                    || normalized.toIntOrNull() == id
        }

        showSearchingIndicator = false

        filteredSearchResults.isNotEmpty()
    }
}