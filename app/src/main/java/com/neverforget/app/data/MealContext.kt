package com.neverforget.app.data

enum class MealContext(val displayName: String, val badgeText: String) {
    ANYTIME("Anytime", "Anytime"),
    BEFORE_MEAL("Before meal", "Before meal"),
    WITH_MEAL("With food", "With food"),
    AFTER_MEAL("After meal", "After meal"),
    EMPTY_STOMACH("Empty stomach", "Empty stomach")
}
