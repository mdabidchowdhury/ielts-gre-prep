package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 1,
    val streakCount: Int = 0,
    val lastActiveDate: String = "", // "YYYY-MM-DD" style
    val dailyGoalCount: Int = 10,
    val reviewedTodayCount: Int = 0,
    val notificationsEnabled: Boolean = true,
    val darkModePreferred: Boolean = true, // default to elegant dark mode requested
    val gemsCount: Int = 12, // Start with some initial gems
    val streakFreezesCount: Int = 1, // Start with 1 free streak freeze
    val streakGoalMetToday: Boolean = false, // Whether the user completed their goal today
    val badgesEarned: String = "[\"FIRST_STEPS\"]", // JSON/comma format: "[\"FIRST_STEPS\", \"7_DAY\"]"
    val maxMatchGameScore: Int = 0
)

