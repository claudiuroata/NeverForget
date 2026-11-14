package com.neverforget.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun testRecurrenceTypeConverters_AllValuesNullAndInvalidFallback() {
        RecurrenceType.entries.forEach { type ->
            val encoded = converters.fromRecurrenceType(type)
            val decoded = converters.toRecurrenceType(encoded)
            assertEquals(type, decoded)
        }

        assertEquals(RecurrenceType.NONE.name, converters.fromRecurrenceType(null))
        assertEquals(RecurrenceType.NONE, converters.toRecurrenceType(null))
        assertEquals(RecurrenceType.NONE, converters.toRecurrenceType("UNKNOWN_TYPE"))
    }

    @Test
    fun testMealContextConverters_AllValuesNullAndInvalidFallback() {
        MealContext.entries.forEach { context ->
            val encoded = converters.fromMealContext(context)
            val decoded = converters.toMealContext(encoded)
            assertEquals(context, decoded)
        }

        assertEquals(MealContext.ANYTIME.name, converters.fromMealContext(null))
        assertEquals(MealContext.ANYTIME, converters.toMealContext(null))
        assertEquals(MealContext.ANYTIME, converters.toMealContext("INVALID_MEAL_CONTEXT"))
    }
}
