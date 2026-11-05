package com.example.swartlandathletics.model

data class Athlete(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val school: String,
    val gender: String, // "Seuns" or "Dogters"
    val ageGroup: String, // "6".."13"
    val events: List<String>
) {
    val fullName: String get() = "$firstName $lastName"
}

data class EventRecord(
    val id: Long,
    val eventName: String,
    val gender: String,
    val ageGroup: String,
    val recordValue: String,
    val holder: String,
    val year: Int
)

data class EventResult(
    val id: Long = 0,
    val eventName: String,
    val gender: String,
    val ageGroup: String,
    val place: Int,
    val athleteId: Long,
    val athleteName: String,
    val school: String,
    val resultValue: String,
    val asaPoints: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class ProgramItem(
    val id: Long,
    val itemNo: Int,
    val startTime: String,
    val ageGroup: String,
    val gender: String,
    val eventName: String,
    val venue: String,
    val status: String = "Wagtend" // "Wagtend", "Besig", "Voltooi"
)

data class HeatEntry(
    val place: Int,
    val time: String,
    val athlete: Athlete?
)

data class HighJumpAttempt(
    val athleteId: Long,
    val athleteName: String,
    val school: String,
    val status: String, // "O (Slaag)", "X (Misluk)", "P (Pass)"
    val height: Double
)
