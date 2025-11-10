package com.beatwell.app.models

import java.util.*

data class CalendarDay(
    val dayNumber: Int,
    val date: Date,
    val isCurrentMonth: Boolean,
    val isToday: Boolean = false,
    val hasMeals: Boolean = false,
    val meals: List<MealLog> = emptyList()
)

data class MealLog(
    val id: Int,
    val mealType: String,
    val mealName: String,
    val calories: Int,
    val portionSize: Float,
    val loggedAt: Date
)

data class WaterIntake(
    val id: Int,
    val glasses: Int,
    val date: Date
)
