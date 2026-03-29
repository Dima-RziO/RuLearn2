package ru.dimarzio.rulearn2.viewmodels

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dimarzio.rulearn2.application.Database
import ru.dimarzio.rulearn2.models.Level
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.tflite.Loss
import ru.dimarzio.rulearn2.tflite.ModelFactory
import ru.dimarzio.rulearn2.utils.normalized
import ru.dimarzio.rulearn2.utils.replaceKeys
import ru.dimarzio.rulearn2.utils.replaceValues
import ru.dimarzio.rulearn2.utils.replaceValuesCompat
import ru.dimarzio.rulearn2.utils.whether
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

typealias Filter = (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit

class CourseViewModel(
    private val database: Database,
    private val handler: ErrorHandler,
    val course: String,
    appFolder: File,
    lifecycle: Lifecycle
) : ViewModel() {
    // SnapshotStateMap is impossible to use because it does not save the order.
    private val _words = MutableStateFlow(emptyMap<Int, Word>())
    private val _levels = MutableStateFlow(emptyMap<String, Level>())

    val words = _words.asStateFlow()
    val levels = _levels.asStateFlow()

    var showLoadingIndicator by mutableStateOf(false)

    private var searchResults = mutableStateMapOf<Int, Word>()

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
            .entries.groupBy { (_, word) -> word.level }
            .mapValues { (_, list) -> list.associate(Map.Entry<Int, Word>::toPair) }
            .toMap()
    }

    var loss by mutableStateOf(Loss(0.0, 0.0, 0.0))

    val locale = Locale(course.take(2))

    val model = ModelFactory.getModel(course, File(appFolder, "tflite/"))

    init {
        viewModelScope.launch {
            showLoadingIndicator = true

            val result = withContext(Dispatchers.Default) {
                runCatching {
                    _words.value = database.getWords(course)
                    _levels.value = _words.value.values
                        .groupingBy(Word::level)
                        .fold(Level()) { current, word ->
                            current.copy(
                                total = current.total + if (!word.skip) 1 else 0,
                                learned = current.learned + if (word.learned && !word.skip) 1 else 0,
                                toRepeat = current.toRepeat + if (word.isRepeat) 1 else 0,
                                difficult = current.difficult + if (word.learned) 1 else 0
                            )
                        }
                }
            }

            result.onFailure(handler::onErrorHandled)

            showLoadingIndicator = false
        }

        lifecycle.addObserver(
            LifecycleEventObserver { _, e ->
                if (e == Lifecycle.Event.ON_RESUME) {
                    val toRepeat = _words.value
                        .filter { (_, word) -> word.isRepeat }
                        .toList()
                        .groupingBy { (_, word) -> word.level }
                        .eachCount()

                    _levels.value = _levels.value.replaceValuesCompat(
                        with = { (name, level) -> level.copy(toRepeat = toRepeat[name] ?: 0) },
                        predicate = { (name, level) -> (toRepeat[name] ?: 0) > level.toRepeat }
                    )
                }
            }
        )

        viewModelScope.launch {
            _words.collect {
                if (_words.value.isEmpty()) {
                    loss = Loss(0.0, 0.0, 0.0)
                }

                val mae = _words.value
                    .map { (_, word) -> abs(word.ratio - word.successRate) }
                    .average()

                val ssRes = _words.value.values.sumOf { word ->
                    (word.ratio - word.successRate).pow(2)
                }
                val rmse = sqrt(ssRes / _words.value.size)

                val avg = _words.value.map { (_, word) -> word.ratio }.average()
                val ssTot = _words.value.values.sumOf { word -> (word.ratio - avg).pow(2) }
                val r2 = if (ssTot != 0.0) 1.0 - (ssRes / ssTot) else 1.0

                loss = Loss(mae, rmse, r2)
            }
        }
    }

    private fun Word.delete() {
        runCatching { audios?.forEach(File::delete) }.onFailure(handler::onErrorHandled)
    }

    fun getWord(id: Int) = _words.value[id]

    fun updateWord(id: Int, word: Word) {
        runCatching { database.updateWord(course, id, word) }.onFailure(handler::onErrorHandled)

        val old = words.value[id]

        _words.value = _words.value.replaceValues(
            with = word,
            predicate = { (i, _) -> id == i }
        )

        if (old?.level == word.level) { // Level was not changed.
            _levels.value = _levels.value.replaceValuesCompat(
                with = { (_, level) ->
                    level.copy(
                        total = if (old.skip && !word.skip) {
                            level.total + 1
                        } else if (!old.skip && word.skip) {
                            level.total - 1
                        } else {
                            level.total
                        },
                        toRepeat = if (old.isRepeat && !word.isRepeat) {
                            level.toRepeat - 1
                        } else if (!old.isRepeat && word.isRepeat) {
                            level.toRepeat + 1
                        } else {
                            level.toRepeat
                        },
                        learned = if (old.learned && !word.learned) {
                            level.learned - 1
                        } else if (!old.learned && word.learned) {
                            level.learned + 1
                        } else {
                            level.learned
                        }
                    )
                },
                predicate = { (name, _) -> name == word.level }
            )
        } else {
            _levels.value = _levels.value.replaceValuesCompat(
                with = { (_, level) ->
                    level.copy(
                        total = if (!word.skip) level.total + 1 else level.total,
                        toRepeat = if (word.isRepeat) level.toRepeat + 1 else level.toRepeat,
                        learned = if (word.learned) level.learned + 1 else level.learned,
                    )
                },
                predicate = { (name, _) -> name == word.level }
            )

            if (old != null) { // Update previous level
                _levels.value = _levels.value.replaceValuesCompat(
                    with = { (_, level) ->
                        level.copy(
                            total = if (!old.skip) level.total - 1 else level.total,
                            toRepeat = if (old.isRepeat) level.toRepeat - 1 else level.toRepeat,
                            learned = if (old.learned) level.learned - 1 else level.learned,
                        )
                    },
                    predicate = { (name, _) -> name == old.level }
                )
            }
        }

        searchResults[id] = word
    }

    fun removeWord(id: Int) {
        runCatching { database.deleteWord(course, id) }.onFailure(handler::onErrorHandled)

        val word = words.value[id]
        if (word != null) {
            runCatching { word.delete() }.onFailure(handler::onErrorHandled) // Remove audio

            _words.value -= id
            _levels.value = _levels.value.replaceValuesCompat(
                with = { (_, level) ->
                    level.copy(
                        total = if (!word.skip) level.total - 1 else level.total,
                        toRepeat = if (word.isRepeat) level.toRepeat - 1 else level.toRepeat,
                        learned = if (word.learned) level.learned - 1 else level.learned,
                    )
                },
                predicate = { (name, _) -> name == word.level }
            )
            searchResults.remove(id)
        }
    }

    fun deleteLevel(name: String) {
        runCatching { database.deleteLevel(course, name) }.onFailure(handler::onErrorHandled)

        _words.value = _words.value
            .onEach { (_, word) -> if (word.level == name) word.delete() }
            .filter { (_, word) -> word.level != name }
        _levels.value -= name

        searchResults.forEach { (id, word) ->
            if (word.level == name) {
                searchResults.remove(id)
            }
        }
    }

    fun confirmRenameLevel(to: String) = _levels.value.none { (name, _) -> name == to }

    fun renameLevel(from: String, to: String) {
        runCatching { database.renameLevel(course, from, to) }.onFailure(handler::onErrorHandled)

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

    fun search() {
        viewModelScope.launch {
            showSearchingIndicator = true

            val normalized = query.normalized()

            val result = withContext(Dispatchers.Default) {
                _words.value.filter { (id, word) ->
                    normalized in word.normalizedName
                            || normalized in word.translation
                            || normalized.toIntOrNull() == id
                }
            }

            searchResults.clear()
            searchResults.putAll(result)

            showSearchingIndicator = false

            if (result.isEmpty()) {
                handler.onMessageReceived("Nothing found.")
            }
        }
    }

    override fun onCleared() {
        ModelFactory.removeModel(course)
    }
}