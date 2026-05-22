package com.petergabriel.budgetcalendar.features.notifications.domain.model

enum class NotificationType(
    val dbValue: String,
) {
    OVERDUE_TRANSACTIONS("overdue_transactions"),
    ROLLOVER_REMINDER_7("rollover_reminder_7"),
    ROLLOVER_REMINDER_5("rollover_reminder_5"),
    ROLLOVER_REMINDER_3("rollover_reminder_3"),
    STS_LOW("sts_low"),
    STS_NEGATIVE("sts_negative"),
    ;

    companion object {
        fun fromDbValue(value: String): NotificationType {
            return entries.firstOrNull { type -> type.dbValue == value }
                ?: throw IllegalArgumentException("Unknown NotificationType dbValue: $value")
        }
    }
}
