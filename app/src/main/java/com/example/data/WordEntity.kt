package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val phonetic: String,
    val definition: String,
    val example: String,
    val type: String, // "IELTS" or "GRE"
    val importance: Int, // 1 = Low, 2 = Medium, 3 = High
    val synonyms: String = "",
    val appearedCount: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val lastReviewedMs: Long = 0L,
    val isMistakenLastTime: Boolean = false,
    val wasMistakenEver: Boolean = false,
    val isCustom: Boolean = false, // to differentiate pre-populated vs user-added words
    val category: String = "Academic"
)
