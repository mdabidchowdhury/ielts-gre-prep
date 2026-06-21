package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words ORDER BY importance DESC, isMistakenLastTime DESC, appearedCount ASC")
    fun getAllWordsFlow(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words")
    suspend fun getAllWordsDirect(): List<WordEntity>

    @Query("SELECT * FROM words WHERE type = :type ORDER BY importance DESC, isMistakenLastTime DESC, appearedCount ASC")
    fun getWordsByTypeFlow(type: String): Flow<List<WordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWords(words: List<WordEntity>)

    @Update
    suspend fun updateWord(word: WordEntity)

    @Delete
    suspend fun deleteWord(word: WordEntity)

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordCount(): Int

    @Query("DELETE FROM words WHERE isCustom = 0")
    suspend fun deleteAllNonCustomWords()

    @Query("SELECT COUNT(*) FROM words WHERE example LIKE 'The student learned%'")
    suspend fun getDummyExampleCount(): Int

    // User Stats queries
    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStatsFlow(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStats(): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserStats(stats: UserStatsEntity)
}
