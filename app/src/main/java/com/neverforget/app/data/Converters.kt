package com.neverforget.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromRecurrenceType(value: RecurrenceType?): String {
        return value?.name ?: RecurrenceType.NONE.name
    }

    @TypeConverter
    fun toRecurrenceType(value: String?): RecurrenceType {
        return try {
            if (value != null) RecurrenceType.valueOf(value) else RecurrenceType.NONE
        } catch (e: IllegalArgumentException) {
            RecurrenceType.NONE
        }
    }

    @TypeConverter
    fun fromMealContext(value: MealContext?): String {
        return value?.name ?: MealContext.ANYTIME.name
    }

    @TypeConverter
    fun toMealContext(value: String?): MealContext {
        return try {
            if (value != null) MealContext.valueOf(value) else MealContext.ANYTIME
        } catch (e: IllegalArgumentException) {
            MealContext.ANYTIME
        }
    }
}
