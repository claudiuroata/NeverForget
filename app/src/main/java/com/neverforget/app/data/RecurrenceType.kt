package com.neverforget.app.data

enum class RecurrenceType(val displayName: String, val badgeText: String) {
    NONE("One-time task", "Once"),
    DAILY("Every day", "Daily"),
    WEEKDAYS("Weekdays (Mon-Fri)", "Weekdays"),
    WEEKLY("Every week", "Weekly"),
    MONTHLY("Every month", "Monthly"),
    QUARTERLY("Every 3 months", "Quarterly"),
    YEARLY("Every year", "Yearly");

    val isRecurring: Boolean
        get() = this != NONE
}
