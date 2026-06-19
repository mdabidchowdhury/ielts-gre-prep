package com.example.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class WordRepository(
    private val wordDao: WordDao,
    private val context: android.content.Context
) {

    val allWords: Flow<List<WordEntity>> = wordDao.getAllWordsFlow()

    fun getWordsByType(type: String): Flow<List<WordEntity>> = wordDao.getWordsByTypeFlow(type)

    fun getUserStats(): Flow<UserStatsEntity?> = wordDao.getUserStatsFlow()

    suspend fun insertCustomWord(word: String, phonetic: String, definition: String, example: String, type: String, importance: Int) {
        val wordEntity = WordEntity(
            word = word.trim(),
            phonetic = phonetic.trim(),
            definition = definition.trim(),
            example = example.trim(),
            type = type,
            importance = importance,
            isCustom = true
        )
        wordDao.insertWord(wordEntity)
    }

    suspend fun deleteWord(word: WordEntity) {
        wordDao.deleteWord(word)
    }

    suspend fun updateWord(word: WordEntity) {
        wordDao.updateWord(word)
    }

    suspend fun syncStreakAndStats() {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val currentStats = wordDao.getUserStats() ?: return
        
        if (currentStats.lastActiveDate.isEmpty()) {
            return
        }
        
        if (currentStats.lastActiveDate == todayStr) {
            return // opened today, stats match
        }
        
        // Let's determine how many calendar days passed
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val lastActive = LocalDate.parse(currentStats.lastActiveDate, formatter)
        val today = LocalDate.parse(todayStr, formatter)
        val daysBetween = ChronoUnit.DAYS.between(lastActive, today)
        
        if (daysBetween <= 1L) {
            if (daysBetween == 1L) {
                // New day opened, reset day reviews & met goal flag
                val updatedStats = currentStats.copy(
                    reviewedTodayCount = 0,
                    streakGoalMetToday = false,
                    lastActiveDate = todayStr
                )
                wordDao.insertOrUpdateUserStats(updatedStats)
            }
            return
        }
        
        // Gap of > 1 day. Check if we can freeze streak
        var freezes = currentStats.streakFreezesCount
        var streak = currentStats.streakCount
        if (freezes > 0 && streak > 0) {
            freezes -= 1
            // Streak is preserved!
            val updatedStats = currentStats.copy(
                streakFreezesCount = freezes,
                reviewedTodayCount = 0,
                streakGoalMetToday = false,
                lastActiveDate = todayStr
            )
            wordDao.insertOrUpdateUserStats(updatedStats)
        } else {
            // Reset streak
            val updatedStats = currentStats.copy(
                streakCount = 0,
                reviewedTodayCount = 0,
                streakGoalMetToday = false,
                lastActiveDate = todayStr
            )
            wordDao.insertOrUpdateUserStats(updatedStats)
        }
    }

    suspend fun handleWordReview(word: WordEntity, isCorrect: Boolean) {
        val updatedWord = word.copy(
            appearedCount = word.appearedCount + 1,
            correctCount = word.correctCount + if (isCorrect) 1 else 0,
            wrongCount = word.wrongCount + if (isCorrect) 0 else 1,
            lastReviewedMs = System.currentTimeMillis(),
            isMistakenLastTime = !isCorrect,
            wasMistakenEver = word.wasMistakenEver || !isCorrect
        )
        wordDao.updateWord(updatedWord)

        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val currentStats = wordDao.getUserStats() ?: UserStatsEntity()

        val isNewDay = currentStats.lastActiveDate != todayStr
        val updatedReviewedToday = if (isNewDay) 1 else (currentStats.reviewedTodayCount + 1)
        
        var updatedStreak = currentStats.streakCount
        var updatedGems = currentStats.gemsCount
        var updatedGoalMet = currentStats.streakGoalMetToday
        var badgesList = mutableListOf<String>()
        try {
            if (currentStats.badgesEarned.isNotEmpty()) {
                badgesList = currentStats.badgesEarned
                    .removeSurrounding("[", "]")
                    .split(",")
                    .map { it.replace("\"", "").trim() }
                    .filter { it.isNotEmpty() }
                    .toMutableList()
            }
        } catch (e: Exception) {
            // ignore
        }

        if (isNewDay) {
            if (currentStats.lastActiveDate.isNotEmpty()) {
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                val lastActive = LocalDate.parse(currentStats.lastActiveDate, formatter)
                val today = LocalDate.parse(todayStr, formatter)
                val daysBetween = ChronoUnit.DAYS.between(lastActive, today)
                if (daysBetween > 1L) {
                    var freezes = currentStats.streakFreezesCount
                    if (freezes > 0 && updatedStreak > 0) {
                        freezes -= 1
                    } else {
                        updatedStreak = 0
                    }
                }
            }
            updatedGoalMet = false
        }

        // Streak increments once Daily Goal is achieved today
        if (!updatedGoalMet && updatedReviewedToday >= currentStats.dailyGoalCount) {
            updatedGoalMet = true
            updatedStreak += 1
            updatedGems += 10 // Award 10 gems
            
            // Check milestones
            if (updatedStreak >= 7 && !badgesList.contains("STREAK_7")) {
                badgesList.add("STREAK_7")
                updatedGems += 50
            }
            if (updatedStreak >= 30 && !badgesList.contains("STREAK_30")) {
                badgesList.add("STREAK_30")
                updatedGems += 200
            }
        }

        // Double daily goal gives extra 15 gems!
        val doubleThreshold = currentStats.dailyGoalCount * 2
        val bonusTag = "DOUBLE_GOAL_$todayStr"
        if (updatedReviewedToday >= doubleThreshold && !badgesList.contains(bonusTag)) {
            badgesList.add(bonusTag)
            updatedGems += 15
        }

        val updatedStats = currentStats.copy(
            streakCount = updatedStreak,
            lastActiveDate = todayStr,
            reviewedTodayCount = updatedReviewedToday,
            streakGoalMetToday = updatedGoalMet,
            gemsCount = updatedGems,
            badgesEarned = badgesList.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        )
        wordDao.insertOrUpdateUserStats(updatedStats)
    }

    suspend fun buyStreakFreeze(): Boolean {
        val currentStats = wordDao.getUserStats() ?: return false
        if (currentStats.gemsCount >= 50) {
            val updatedStats = currentStats.copy(
                gemsCount = currentStats.gemsCount - 50,
                streakFreezesCount = currentStats.streakFreezesCount + 1
            )
            wordDao.insertOrUpdateUserStats(updatedStats)
            return true
        }
        return false
    }

    suspend fun updateMaxMatchGameScore(score: Int) {
        val currentStats = wordDao.getUserStats() ?: return
        if (score > currentStats.maxMatchGameScore) {
            var badgesList = mutableListOf<String>()
            try {
                if (currentStats.badgesEarned.isNotEmpty()) {
                    badgesList = currentStats.badgesEarned
                        .removeSurrounding("[", "]")
                        .split(",")
                        .map { it.replace("\"", "").trim() }
                        .filter { it.isNotEmpty() }
                        .toMutableList()
                }
            } catch (e: Exception) {}

            var updatedGems = currentStats.gemsCount
            if (score >= 10 && !badgesList.contains("SPEED_MERCHANT")) {
                badgesList.add("SPEED_MERCHANT")
                updatedGems += 30 // reward 30 gems for game achievement
            }

            wordDao.insertOrUpdateUserStats(currentStats.copy(
                maxMatchGameScore = score,
                gemsCount = updatedGems,
                badgesEarned = badgesList.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
            ))
        }
    }

    suspend fun awardClassicAchievements(badgeName: String, gemsAward: Int) {
        val currentStats = wordDao.getUserStats() ?: return
        var badgesList = mutableListOf<String>()
        try {
            if (currentStats.badgesEarned.isNotEmpty()) {
                badgesList = currentStats.badgesEarned
                    .removeSurrounding("[", "]")
                    .split(",")
                    .map { it.replace("\"", "").trim() }
                    .filter { it.isNotEmpty() }
                    .toMutableList()
            }
        } catch (e: Exception) {}

        if (!badgesList.contains(badgeName)) {
            badgesList.add(badgeName)
            wordDao.insertOrUpdateUserStats(currentStats.copy(
                gemsCount = currentStats.gemsCount + gemsAward,
                badgesEarned = badgesList.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
            ))
        }
    }

    suspend fun updateDarkModePreferred(enabled: Boolean) {
        val currentStats = wordDao.getUserStats() ?: UserStatsEntity()
        wordDao.insertOrUpdateUserStats(currentStats.copy(darkModePreferred = enabled))
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        val currentStats = wordDao.getUserStats() ?: UserStatsEntity()
        wordDao.insertOrUpdateUserStats(currentStats.copy(notificationsEnabled = enabled))
    }

    suspend fun updateDailyGoalCount(goal: Int) {
        val currentStats = wordDao.getUserStats() ?: UserStatsEntity()
        wordDao.insertOrUpdateUserStats(currentStats.copy(dailyGoalCount = goal))
    }

    suspend fun checkAndSeedDatabase() {
        val ieltsSeeds = loadWordsFromRawResource(com.example.R.raw.ielts_words, "IELTS")
        val greSeeds = loadWordsFromRawResource(com.example.R.raw.gre_words, "GRE")
        wordDao.insertWords(ieltsSeeds)
        wordDao.insertWords(greSeeds)

        val currentStats = wordDao.getUserStats()
        if (currentStats == null) {
            wordDao.insertOrUpdateUserStats(
                UserStatsEntity(
                    id = 1,
                    streakCount = 0,
                    lastActiveDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    dailyGoalCount = 10,
                    reviewedTodayCount = 0,
                    notificationsEnabled = true,
                    darkModePreferred = true
                )
            )
        }
    }

    suspend fun generateAndAddGeminiIeltsWords(
        apiKey: String,
        onProgress: (Int) -> Unit
    ): Int {
        var totalAdded = 0
        for (batch in 1..10) {
            val offset = (batch - 1) * 50
            val prompt = """
                Generate exactly 50 high-frequency academic vocabulary words suitable for the IELTS exam.
                These must be words with high utility on the IELTS test.
                Ensure each word has a real, detailed definition, phonetic transcription (in standard IPA slash notation format), and high-quality, immersive example sentences to make flashcards look complete and informative rather than flat.
                Do not overlap or repeat any words from previous batches if possible.
                Provide the output strictly in a JSON object format inside a valid JSON container where the root is an object containing a list called 'words'.
                Format:
                {
                  "words": [
                    {
                      "word": "word",
                      "phonetic": "phonetic e.g. /æstˈθetɪk/",
                      "definition": "accurate academic dictionary definition",
                      "example": "An engaging, deep sentence showing exactly how to use the word in an academic writing context.",
                      "importance": 3,
                      "category": "Academic"
                    }
                  ]
                }
                Current offset identifier to ensure variation: Batch #$batch (Offset: $offset).
            """.trimIndent()

            try {
                val request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.7f)
                )
                val response = GeminiApiClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!responseText.isNullOrBlank()) {
                    val jsonOnly = responseText.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                    
                    val parsed = GeminiApiClient.parseGeneratedWords(jsonOnly)
                    if (!parsed.isNullOrEmpty()) {
                        val wordEntities = parsed.map {
                            WordEntity(
                                word = it.word.trim(),
                                phonetic = it.phonetic.trim(),
                                definition = it.definition.trim(),
                                example = it.example.trim(),
                                type = "IELTS",
                                importance = it.importance.coerceIn(1, 3),
                                category = it.category.trim(),
                                isCustom = true
                            )
                        }
                        wordDao.insertWords(wordEntities)
                        totalAdded += wordEntities.size
                        onProgress(totalAdded)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return totalAdded
    }

    suspend fun seedManhattanWords(): Int {
        val existingWords = wordDao.getAllWordsDirect()
        val existingSet = existingWords.map { it.word.lowercase().trim() }.toSet()
        val extraSeeds = getManhattanSeedsList()
        val newSeeds = extraSeeds.filter { !existingSet.contains(it.word.lowercase().trim()) }
        if (newSeeds.isNotEmpty()) {
            wordDao.insertWords(newSeeds)
        }
        return newSeeds.size
    }

    fun getManhattanSeedsList(): List<WordEntity> {
        return loadWordsFromRawResource(com.example.R.raw.gre_words, "GRE")
    }

    private fun loadWordsFromRawResource(resourceId: Int, type: String): List<WordEntity> {
        val list = mutableListOf<WordEntity>()
        try {
            val inputStream = context.resources.openRawResource(resourceId)
            val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream))
            var line: String? = reader.readLine()
            while (line != null) {
                if (line.isNotBlank() && line.contains("|")) {
                    val parts = line.split("|")
                    if (parts.size >= 2) {
                        val wordVal = parts[0].trim()
                        val defVal = parts[1].trim()
                        if (wordVal.isNotEmpty() && defVal.isNotEmpty()) {
                            val phoneticVal = "/${wordVal.lowercase()}/"
                            val sentenceVal = "The student learned the meaning of the word '${wordVal}' during their prep."
                            list.add(
                                WordEntity(
                                    word = wordVal,
                                    phonetic = phoneticVal,
                                    definition = defVal,
                                    example = sentenceVal,
                                    type = type,
                                    importance = (2..3).random(),
                                    category = "Academic"
                                )
                            )
                        }
                    }
                }
                line = reader.readLine()
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
