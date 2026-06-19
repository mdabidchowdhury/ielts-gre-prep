package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.WordEntity
import com.example.data.WordRepository
import com.example.data.UserStatsEntity
import com.example.utils.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VocabViewModel(private val repository: WordRepository) : ViewModel() {

    private val _selectedType = MutableStateFlow("IELTS")
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()

    // Observe word list based on selected segment
    val currentWords: StateFlow<List<WordEntity>> = _selectedType
        .flatMapLatest { type ->
            repository.getWordsByType(type)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userStats: StateFlow<UserStatsEntity?> = repository.getUserStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _currentWordIndex = MutableStateFlow(0)
    val currentWordIndex: StateFlow<Int> = _currentWordIndex.asStateFlow()

    private val _isCardFlipped = MutableStateFlow(false)
    val isCardFlipped: StateFlow<Boolean> = _isCardFlipped.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed base words if DB is empty
            repository.checkAndSeedDatabase()
            // Sync/freeze streaks based on dates
            repository.syncStreakAndStats()
        }
    }

    fun buyStreakFreeze(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.buyStreakFreeze()
            onResult(success)
        }
    }

    fun submitMatchScore(score: Int) {
        viewModelScope.launch {
            repository.updateMaxMatchGameScore(score)
        }
    }

    fun triggerAchievement(badgeName: String, gemsAward: Int) {
        viewModelScope.launch {
            repository.awardClassicAchievements(badgeName, gemsAward)
        }
    }

    fun changeSegment(type: String) {
        _selectedType.value = type
        _currentWordIndex.value = 0
        _isCardFlipped.value = false
    }

    fun setWordIndex(index: Int) {
        _currentWordIndex.value = index
        _isCardFlipped.value = false
    }

    fun flipCard() {
        _isCardFlipped.value = !_isCardFlipped.value
    }

    fun recordReview(word: WordEntity, isCorrect: Boolean) {
        viewModelScope.launch {
            repository.handleWordReview(word, isCorrect)
            // Advance/rotate index if there are more words
            val listSize = currentWords.value.size
            if (listSize > 1) {
                // Keep the card visual simple or increment to next word
                _isCardFlipped.value = false
                val nextIndex = (_currentWordIndex.value + 1) % listSize
                _currentWordIndex.value = nextIndex
            } else {
                _isCardFlipped.value = false
            }
        }
    }

    fun addNewWord(word: String, phonetic: String, definition: String, example: String, type: String, importance: Int) {
        viewModelScope.launch {
            if (word.isNotBlank() && definition.isNotBlank()) {
                repository.insertCustomWord(
                    word = word,
                    phonetic = phonetic,
                    definition = definition,
                    example = example,
                    type = type,
                    importance = importance
                )
            }
        }
    }

    fun deleteWord(word: WordEntity) {
        viewModelScope.launch {
            val list = currentWords.value
            if (list.size <= 1) {
                _currentWordIndex.value = 0
            } else if (_currentWordIndex.value >= list.size - 1) {
                _currentWordIndex.value = list.size - 2
            }
            _isCardFlipped.value = false
            repository.deleteWord(word)
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateDarkModePreferred(enabled)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateNotificationsEnabled(enabled)
        }
    }

    fun changeDailyGoal(goal: Int) {
        viewModelScope.launch {
            if (goal in 1..100) {
                repository.updateDailyGoalCount(goal)
            }
        }
    }

    fun triggerReminderNotification(context: Context) {
        val stats = userStats.value ?: UserStatsEntity()
        NotificationHelper.showStudyReminderNotification(
            context = context,
            streakCount = stats.streakCount,
            dailyGoal = stats.dailyGoalCount,
            reviewedCount = stats.reviewedTodayCount
        )
    }

    fun syncManhattanWords(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val insertedCount = repository.seedManhattanWords()
            onComplete(insertedCount)
        }
    }

    private val _aiSyncState = MutableStateFlow<String?>(null)
    val aiSyncState: StateFlow<String?> = _aiSyncState.asStateFlow()

    private val _aiSyncProgress = MutableStateFlow(0)
    val aiSyncProgress: StateFlow<Int> = _aiSyncProgress.asStateFlow()

    fun triggerGeminiIeltsExpansion(onComplete: (Int) -> Unit) {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            _aiSyncState.value = "ERROR_KEY"
            onComplete(0)
            return
        }

        viewModelScope.launch {
            _aiSyncState.value = "LOADING"
            _aiSyncProgress.value = 0
            try {
                val added = repository.generateAndAddGeminiIeltsWords(apiKey) { progress ->
                    _aiSyncProgress.value = progress
                }
                if (added > 0) {
                    _aiSyncState.value = "SUCCESS"
                } else {
                    _aiSyncState.value = "ERROR_ZERO"
                }
                onComplete(added)
            } catch (e: java.lang.Exception) {
                _aiSyncState.value = "ERROR"
                onComplete(0)
            }
        }
    }

    fun resetAiSyncState() {
        _aiSyncState.value = null
        _aiSyncProgress.value = 0
    }
}

class VocabViewModelFactory(private val repository: WordRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VocabViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VocabViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
