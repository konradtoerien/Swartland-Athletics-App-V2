package com.example.swartlandathletics.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Athlete(
    @DocumentId val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val school: String = "",
    val gender: String = "Seuns", // "Seuns" or "Dogters"
    val ageGroup: String = "13", // "6".."13"
    val events: List<String> = emptyList(),
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

@IgnoreExtraProperties
data class EventRecord(
    @DocumentId val id: String = "",
    val eventName: String = "",
    val gender: String = "Seuns",
    val ageGroup: String = "13",
    val recordValue: String = "",
    val holder: String = "",
    val year: Int = 2024,
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class EventResult(
    @DocumentId val id: String = "",
    val eventName: String = "",
    val gender: String = "Seuns",
    val ageGroup: String = "13",
    val place: Int = 1,
    val athleteId: String = "",
    val athleteName: String = "",
    val school: String = "",
    val resultValue: String = "",
    val asaPoints: Int = 0,
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ProgramItem(
    @DocumentId val id: String = "",
    val itemNo: Int = 1,
    val startTime: String = "",
    val ageGroup: String = "13",
    val gender: String = "Seuns",
    val eventName: String = "",
    val venue: String = "",
    val status: String = "Wagtend", // "Wagtend", "Besig", "Voltooi"
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
