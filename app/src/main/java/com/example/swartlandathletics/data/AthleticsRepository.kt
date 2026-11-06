package com.example.swartlandathletics.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.swartlandathletics.model.Athlete
import com.example.swartlandathletics.model.EventRecord
import com.example.swartlandathletics.model.EventResult
import com.example.swartlandathletics.model.ProgramItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object AthleticsRepository {
    private const val TAG = "AthleticsRepository"

    private var db: FirebaseFirestore? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _athletes = MutableStateFlow<List<Athlete>>(emptyList())
    val athletes: StateFlow<List<Athlete>> = _athletes.asStateFlow()

    private val _records = MutableStateFlow<List<EventRecord>>(emptyList())
    val records: StateFlow<List<EventRecord>> = _records.asStateFlow()

    private val _results = MutableStateFlow<List<EventResult>>(emptyList())
    val results: StateFlow<List<EventResult>> = _results.asStateFlow()

    private val _program = MutableStateFlow<List<ProgramItem>>(emptyList())
    val program: StateFlow<List<ProgramItem>> = _program.asStateFlow()

    private val _completedCeremonies = MutableStateFlow<Set<String>>(emptySet())
    val completedCeremonies: StateFlow<Set<String>> = _completedCeremonies.asStateFlow()

    private val _meetName = MutableStateFlow<String>("Laerskool Swartland Atletiekbijeenkoms")
    val meetName: StateFlow<String> = _meetName.asStateFlow()

    private val _meetDate = MutableStateFlow<String>("2026-02-15")
    val meetDate: StateFlow<String> = _meetDate.asStateFlow()

    private var athletesListener: ListenerRegistration? = null
    private var recordsListener: ListenerRegistration? = null
    private var resultsListener: ListenerRegistration? = null
    private var programListener: ListenerRegistration? = null
    private var meetInfoListener: ListenerRegistration? = null

    fun initialize(context: Context) {
        if (db != null) return
        try {
            val dbId = context.getString(R.string.firestore_database_id)
            db = FirebaseFirestore.getInstance(dbId)
            startRealtimeSync()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firestore with custom database ID", e)
        }
    }

    fun startRealtimeSync() {
        val firestore = db ?: return

        // 0. Meet info listener
        meetInfoListener?.remove()
        meetInfoListener = firestore.collection("settings").document("meet_info")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null && snapshot.exists()) {
                    val name = snapshot.getString("name")
                    val date = snapshot.getString("date")
                    if (!name.isNullOrBlank()) _meetName.value = name
                    if (!date.isNullOrBlank()) _meetDate.value = date
                }
            }

        // 1. Athletes real-time snapshot listener
        athletesListener?.remove()
        athletesListener = firestore.collection("athletes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Athletes sync error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(Athlete::class.java) }
                    _athletes.value = list
                }
            }

        // 2. Records real-time snapshot listener
        recordsListener?.remove()
        recordsListener = firestore.collection("records")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Records sync error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(EventRecord::class.java) }
                    _records.value = list
                }
            }

        // 3. Results real-time snapshot listener
        resultsListener?.remove()
        resultsListener = firestore.collection("results")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Results sync error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(EventResult::class.java) }
                        .sortedWith(compareBy({ it.eventName }, { it.place }))
                    _results.value = list
                }
            }

        // 4. Program real-time snapshot listener
        programListener?.remove()
        programListener = firestore.collection("program")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Program sync error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(ProgramItem::class.java) }
                        .sortedBy { it.itemNo }
                    _program.value = list
                }
            }
    }

    private fun currentUid(): String = Firebase.auth.currentUser?.uid ?: "anonymous"

    fun seedInitialData() {
        val firestore = db ?: return
        val uid = currentUid()

        val sampleAthletes = listOf(
            mapOf("firstName" to "Liam", "lastName" to "van der Merwe", "school" to "Laerskool Swartland", "gender" to "Seuns", "ageGroup" to "13", "events" to listOf("100m", "200m", "Long Jump"), "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("firstName" to "Jean", "lastName" to "du Plessis", "school" to "Laerskool Swartland", "gender" to "Seuns", "ageGroup" to "13", "events" to listOf("80m", "100m", "Shot Put"), "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("firstName" to "Mieke", "lastName" to "Smit", "school" to "Laerskool Swartland", "gender" to "Dogters", "ageGroup" to "13", "events" to listOf("100m", "75m Hurdles", "High Jump"), "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("firstName" to "Anika", "lastName" to "Kotze", "school" to "Laerskool Swartland", "gender" to "Dogters", "ageGroup" to "12", "events" to listOf("100m", "150m", "Long Jump"), "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("firstName" to "Ruan", "lastName" to "Botha", "school" to "Laerskool Vredenburg", "gender" to "Seuns", "ageGroup" to "13", "events" to listOf("100m", "200m", "80m Hurdles"), "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("firstName" to "Kobus", "lastName" to "Visser", "school" to "Laerskool Vredenburg", "gender" to "Seuns", "ageGroup" to "12", "events" to listOf("100m", "150m", "Shot Put"), "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("firstName" to "Carla", "lastName" to "Louw", "school" to "Laerskool Vredenburg", "gender" to "Dogters", "ageGroup" to "13", "events" to listOf("100m", "200m", "Long Jump"), "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("firstName" to "Zack", "lastName" to "Marais", "school" to "Laerskool Malmesbury", "gender" to "Seuns", "ageGroup" to "13", "events" to listOf("100m", "80m", "High Jump"), "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("firstName" to "Danie", "lastName" to "Theron", "school" to "Laerskool Malmesbury", "gender" to "Seuns", "ageGroup" to "11", "events" to listOf("80m", "100m", "70m Hurdles"), "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("firstName" to "Lize", "lastName" to "Bester", "school" to "Laerskool Malmesbury", "gender" to "Dogters", "ageGroup" to "13", "events" to listOf("100m", "1500m", "Long Jump"), "userId" to uid, "createdAt" to FieldValue.serverTimestamp())
        )

        val sampleRecords = listOf(
            mapOf("eventName" to "100m", "gender" to "Seuns", "ageGroup" to "13", "recordValue" to "11.62s", "holder" to "L. van der Merwe (LSS)", "year" to 2024, "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("eventName" to "100m", "gender" to "Dogters", "ageGroup" to "13", "recordValue" to "12.35s", "holder" to "M. Smit (LSS)", "year" to 2023, "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("eventName" to "High Jump", "gender" to "Seuns", "ageGroup" to "13", "recordValue" to "1.65m", "holder" to "C. Joubert (Vredenburg)", "year" to 2022, "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("eventName" to "Shot Put", "gender" to "Seuns", "ageGroup" to "13", "recordValue" to "12.80m", "holder" to "J. du Plessis (LSS)", "year" to 2023, "userId" to uid, "createdAt" to FieldValue.serverTimestamp())
        )

        val sampleProgram = listOf(
            mapOf("itemNo" to 1, "startTime" to "08:00", "ageGroup" to "13", "gender" to "Seuns", "eventName" to "100m", "venue" to "Hoofbaan", "status" to "Voltooi", "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("itemNo" to 2, "startTime" to "08:15", "ageGroup" to "13", "gender" to "Dogters", "eventName" to "100m", "venue" to "Hoofbaan", "status" to "Voltooi", "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("itemNo" to 3, "startTime" to "08:30", "ageGroup" to "13", "gender" to "Seuns", "eventName" to "High Jump", "venue" to "Hoogspring Mat A", "status" to "Voltooi", "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("itemNo" to 4, "startTime" to "08:45", "ageGroup" to "12", "gender" to "Dogters", "eventName" to "Shot Put", "venue" to "Gewigstoot Sirkel", "status" to "Voltooi", "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("itemNo" to 5, "startTime" to "09:00", "ageGroup" to "13", "gender" to "Seuns", "eventName" to "200m", "venue" to "Hoofbaan", "status" to "Voltooi", "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("itemNo" to 6, "startTime" to "09:15", "ageGroup" to "13", "gender" to "Dogters", "eventName" to "High Jump", "venue" to "Hoogspring Mat A", "status" to "Voltooi", "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("itemNo" to 7, "startTime" to "09:30", "ageGroup" to "13", "gender" to "Seuns", "eventName" to "Shot Put", "venue" to "Gewigstoot Sirkel", "status" to "Voltooi", "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("itemNo" to 8, "startTime" to "09:45", "ageGroup" to "13", "gender" to "Dogters", "eventName" to "75m Hurdles", "venue" to "Hoofbaan", "status" to "Voltooi", "userId" to uid, "createdAt" to FieldValue.serverTimestamp())
        )

        val sampleResults = listOf(
            // 100m Seuns o/13
            mapOf("eventName" to "100m", "gender" to "Seuns", "ageGroup" to "13", "place" to 1, "athleteId" to "ath_1", "athleteName" to "Liam van der Merwe", "school" to "Laerskool Swartland", "resultValue" to "11.45s", "asaPoints" to 820, "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("eventName" to "100m", "gender" to "Seuns", "ageGroup" to "13", "place" to 2, "athleteId" to "ath_2", "athleteName" to "Jean du Plessis", "school" to "Laerskool Swartland", "resultValue" to "11.78s", "asaPoints" to 780, "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("eventName" to "100m", "gender" to "Seuns", "ageGroup" to "13", "place" to 3, "athleteId" to "ath_5", "athleteName" to "Ruan Botha", "school" to "Laerskool Vredenburg", "resultValue" to "12.02s", "asaPoints" to 730, "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            // 200m Seuns o/13
            mapOf("eventName" to "200m", "gender" to "Seuns", "ageGroup" to "13", "place" to 1, "athleteId" to "ath_1", "athleteName" to "Liam van der Merwe", "school" to "Laerskool Swartland", "resultValue" to "24.12s", "asaPoints" to 840, "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("eventName" to "200m", "gender" to "Seuns", "ageGroup" to "13", "place" to 2, "athleteId" to "ath_5", "athleteName" to "Ruan Botha", "school" to "Laerskool Vredenburg", "resultValue" to "24.80s", "asaPoints" to 790, "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            // Long Jump Seuns o/13
            mapOf("eventName" to "Long Jump", "gender" to "Seuns", "ageGroup" to "13", "place" to 1, "athleteId" to "ath_1", "athleteName" to "Liam van der Merwe", "school" to "Laerskool Swartland", "resultValue" to "5.68m", "asaPoints" to 860, "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("eventName" to "Long Jump", "gender" to "Seuns", "ageGroup" to "13", "place" to 2, "athleteId" to "ath_8", "athleteName" to "Zack Marais", "school" to "Laerskool Malmesbury", "resultValue" to "5.32m", "asaPoints" to 790, "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            // Shot Put Seuns o/13
            mapOf("eventName" to "Shot Put", "gender" to "Seuns", "ageGroup" to "13", "place" to 1, "athleteId" to "ath_6", "athleteName" to "Kobus Visser", "school" to "Laerskool Vredenburg", "resultValue" to "13.45m", "asaPoints" to 860, "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("eventName" to "Shot Put", "gender" to "Seuns", "ageGroup" to "13", "place" to 2, "athleteId" to "ath_2", "athleteName" to "Jean du Plessis", "school" to "Laerskool Swartland", "resultValue" to "12.90m", "asaPoints" to 810, "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            // 100m Dogters o/13
            mapOf("eventName" to "100m", "gender" to "Dogters", "ageGroup" to "13", "place" to 1, "athleteId" to "ath_3", "athleteName" to "Mieke Smit", "school" to "Laerskool Swartland", "resultValue" to "12.30s", "asaPoints" to 850, "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("eventName" to "100m", "gender" to "Dogters", "ageGroup" to "13", "place" to 2, "athleteId" to "ath_7", "athleteName" to "Carla Louw", "school" to "Laerskool Vredenburg", "resultValue" to "12.75s", "asaPoints" to 790, "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            // High Jump Dogters o/13
            mapOf("eventName" to "High Jump", "gender" to "Dogters", "ageGroup" to "13", "place" to 1, "athleteId" to "ath_3", "athleteName" to "Mieke Smit", "school" to "Laerskool Swartland", "resultValue" to "1.52m", "asaPoints" to 880, "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            // 75m Hurdles Dogters o/13
            mapOf("eventName" to "75m Hurdles", "gender" to "Dogters", "ageGroup" to "13", "place" to 1, "athleteId" to "ath_3", "athleteName" to "Mieke Smit", "school" to "Laerskool Swartland", "resultValue" to "11.82s", "asaPoints" to 870, "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            // 150m Dogters o/12 (Junior)
            mapOf("eventName" to "150m", "gender" to "Dogters", "ageGroup" to "12", "place" to 1, "athleteId" to "ath_4", "athleteName" to "Anika Kotze", "school" to "Laerskool Swartland", "resultValue" to "20.15s", "asaPoints" to 810, "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            mapOf("eventName" to "Long Jump", "gender" to "Dogters", "ageGroup" to "12", "place" to 1, "athleteId" to "ath_4", "athleteName" to "Anika Kotze", "school" to "Laerskool Swartland", "resultValue" to "4.82m", "asaPoints" to 830, "userId" to uid, "createdAt" to FieldValue.serverTimestamp()),
            // 80m Seuns o/11 (Junior)
            mapOf("eventName" to "80m", "gender" to "Seuns", "ageGroup" to "11", "place" to 1, "athleteId" to "ath_9", "athleteName" to "Danie Theron", "school" to "Laerskool Malmesbury", "resultValue" to "10.88s", "asaPoints" to 770, "userId" to uid, "createdAt" to FieldValue.serverTimestamp())
        )

        scope.launch {
            sampleAthletes.forEach { firestore.collection("athletes").add(it) }
            sampleRecords.forEach { firestore.collection("records").add(it) }
            sampleProgram.forEach { firestore.collection("program").add(it) }
            sampleResults.forEach { firestore.collection("results").add(it) }
        }
    }

    // Athletes
    fun addAthlete(firstName: String, lastName: String, school: String, gender: String, ageGroup: String, events: List<String>) {
        val firestore = db ?: return
        val uid = currentUid()
        val payload = mapOf(
            "firstName" to firstName.trim(),
            "lastName" to lastName.trim(),
            "school" to school.trim(),
            "gender" to gender,
            "ageGroup" to ageGroup,
            "events" to events,
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("athletes").add(payload)
    }

    fun deleteAthlete(id: String) {
        val firestore = db ?: return
        if (id.isNotBlank()) {
            firestore.collection("athletes").document(id).delete()
        }
    }

    fun bulkAddAthletes(athletesToAdd: List<Athlete>, onComplete: ((Int) -> Unit)? = null) {
        val firestore = db ?: return
        val uid = currentUid()
        scope.launch {
            var count = 0
            athletesToAdd.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { ath ->
                    val docRef = firestore.collection("athletes").document()
                    val payload = mapOf(
                        "firstName" to ath.firstName.trim(),
                        "lastName" to ath.lastName.trim(),
                        "school" to ath.school.trim(),
                        "gender" to ath.gender.trim(),
                        "ageGroup" to ath.ageGroup.trim(),
                        "events" to ath.events,
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(docRef, payload)
                    count++
                }
                try {
                    batch.commit()
                } catch (e: Exception) {
                    Log.e(TAG, "Bulk athletes batch commit error", e)
                }
            }
            onComplete?.invoke(count)
        }
    }

    // Records
    fun addRecord(eventName: String, gender: String, ageGroup: String, recordValue: String, holder: String, year: Int) {
        val firestore = db ?: return
        val uid = currentUid()
        val payload = mapOf(
            "eventName" to eventName.trim(),
            "gender" to gender,
            "ageGroup" to ageGroup,
            "recordValue" to recordValue.trim(),
            "holder" to holder.trim(),
            "year" to year,
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("records").add(payload)
    }

    fun deleteRecord(recordId: String) {
        val firestore = db ?: return
        if (recordId.isNotBlank()) {
            firestore.collection("records").document(recordId).delete()
        }
    }

    fun bulkAddRecords(recordsToAdd: List<EventRecord>, onComplete: ((Int) -> Unit)? = null) {
        val firestore = db ?: return
        val uid = currentUid()
        scope.launch {
            var count = 0
            recordsToAdd.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { rec ->
                    val docRef = firestore.collection("records").document()
                    val payload = mapOf(
                        "eventName" to rec.eventName.trim(),
                        "gender" to rec.gender.trim(),
                        "ageGroup" to rec.ageGroup.trim(),
                        "recordValue" to rec.recordValue.trim(),
                        "holder" to rec.holder.trim(),
                        "year" to rec.year,
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(docRef, payload)
                    count++
                }
                try {
                    batch.commit()
                } catch (e: Exception) {
                    Log.e(TAG, "Bulk records commit error", e)
                }
            }
            onComplete?.invoke(count)
        }
    }

    fun clearAllRecords(onComplete: (() -> Unit)? = null) {
        val firestore = db ?: return
        _records.value = emptyList()
        scope.launch {
            firestore.collection("records").get().addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().addOnCompleteListener {
                    onComplete?.invoke()
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error clearing records collection", e)
            }
        }
    }

    fun getRecord(eventName: String, gender: String, ageGroup: String): EventRecord? {
        return _records.value.find {
            it.eventName.equals(eventName, ignoreCase = true) &&
                    it.gender.equals(gender, ignoreCase = true) &&
                    it.ageGroup == ageGroup
        }
    }

    // Results
    fun saveEventResults(eventName: String, gender: String, ageGroup: String, results: List<EventResult>) {
        val firestore = db ?: return
        val uid = currentUid()

        scope.launch {
            results.forEachIndexed { idx, item ->
                val payload = mapOf(
                    "eventName" to eventName,
                    "gender" to gender,
                    "ageGroup" to ageGroup,
                    "place" to (idx + 1),
                    "athleteId" to item.athleteId,
                    "athleteName" to item.athleteName,
                    "school" to item.school,
                    "resultValue" to item.resultValue,
                    "asaPoints" to item.asaPoints,
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("results").add(payload)
            }

            // Update program item status to "Voltooi"
            val progItem = _program.value.find {
                it.eventName.equals(eventName, ignoreCase = true) &&
                        it.gender.equals(gender, ignoreCase = true) &&
                        it.ageGroup == ageGroup
            }
            if (progItem != null && progItem.id.isNotBlank()) {
                firestore.collection("program").document(progItem.id).update(
                    "status", "Voltooi",
                    "updatedAt", FieldValue.serverTimestamp()
                )
            }
        }
    }

    fun getEventResults(eventName: String, gender: String, ageGroup: String): List<EventResult> {
        return _results.value.filter {
            it.eventName.equals(eventName, ignoreCase = true) &&
                    it.gender.equals(gender, ignoreCase = true) &&
                    it.ageGroup == ageGroup
        }.sortedBy { it.place }
    }

    fun markCeremonyCompleted(key: String) {
        _completedCeremonies.value = _completedCeremonies.value + key
    }

    // Program
    fun addProgramItem(itemNo: Int, startTime: String, ageGroup: String, gender: String, eventName: String, venue: String, status: String = "Wagtend") {
        val firestore = db ?: return
        val uid = currentUid()
        val payload = mapOf(
            "itemNo" to itemNo,
            "startTime" to startTime,
            "ageGroup" to ageGroup,
            "gender" to gender,
            "eventName" to eventName,
            "venue" to venue,
            "status" to status,
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("program").add(payload)
    }

    fun updateProgramStatus(id: String, newStatus: String) {
        val firestore = db ?: return
        if (id.isNotBlank()) {
            firestore.collection("program").document(id).update(
                "status", newStatus,
                "updatedAt", FieldValue.serverTimestamp()
            )
        }
    }

    fun bulkAddProgram(itemsToAdd: List<ProgramItem>, onComplete: ((Int) -> Unit)? = null) {
        val firestore = db ?: return
        val uid = currentUid()
        scope.launch {
            var count = 0
            itemsToAdd.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { prog ->
                    val docRef = firestore.collection("program").document()
                    val payload = mapOf(
                        "itemNo" to prog.itemNo,
                        "startTime" to prog.startTime.trim(),
                        "ageGroup" to prog.ageGroup.trim(),
                        "gender" to prog.gender.trim(),
                        "eventName" to prog.eventName.trim(),
                        "venue" to prog.venue.trim(),
                        "status" to if (prog.status.isNotBlank()) prog.status else "Wagtend",
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(docRef, payload)
                    count++
                }
                try {
                    batch.commit()
                } catch (e: Exception) {
                    Log.e(TAG, "Bulk program commit error", e)
                }
            }
            onComplete?.invoke(count)
        }
    }

    fun clearAllProgram(onComplete: (() -> Unit)? = null) {
        val firestore = db ?: return
        _program.value = emptyList()
        scope.launch {
            firestore.collection("program").get().addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().addOnCompleteListener {
                    onComplete?.invoke()
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error clearing program collection", e)
            }
        }
    }

    fun clearAllAthletes() {
        val firestore = db ?: return
        _athletes.value = emptyList()
        scope.launch {
            firestore.collection("athletes").get().addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit()
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error clearing athletes collection", e)
            }
        }
    }

    fun resetAll() {
        val firestore = db ?: return
        _athletes.value = emptyList()
        _records.value = emptyList()
        _results.value = emptyList()
        _program.value = emptyList()
        _completedCeremonies.value = emptySet()

        val collections = listOf("athletes", "records", "results", "program")
        scope.launch {
            collections.forEach { colName ->
                firestore.collection(colName).get().addOnSuccessListener { snapshot ->
                    val batch = firestore.batch()
                    snapshot.documents.forEach { doc ->
                        batch.delete(doc.reference)
                    }
                    batch.commit()
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Error clearing $colName collection", e)
                }
            }
        }
    }

    fun updateMeetInfo(name: String, date: String, onComplete: ((Boolean, String) -> Unit)? = null) {
        val cleanName = name.ifBlank { "Laerskool Swartland Atletiek" }
        val cleanDate = date.ifBlank { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()) }
        _meetName.value = cleanName
        _meetDate.value = cleanDate
        val firestore = db ?: run {
            onComplete?.invoke(true, "Byeenkoms inligting plaaslik opgedateer.")
            return
        }
        val doc = mapOf(
            "name" to cleanName,
            "date" to cleanDate,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("settings").document("meet_info").set(doc)
            .addOnSuccessListener {
                onComplete?.invoke(true, "Byeenkoms inligting suksesvol gestoor!")
            }
            .addOnFailureListener { e ->
                onComplete?.invoke(false, "Fout met stoor: ${e.localizedMessage}")
            }
    }

    // Meet Template Backup & Restore (Program & Records for New Meets / Next Year)
    fun exportMeetTemplateJson(meetName: String = _meetName.value, meetDate: String = _meetDate.value): String {
        val root = org.json.JSONObject()
        val nameToUse = meetName.ifBlank { _meetName.value.ifBlank { "Laerskool Swartland Atletiek" } }
        val dateToUse = meetDate.ifBlank { _meetDate.value.ifBlank { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()) } }
        root.put("meetName", nameToUse)
        root.put("meetDate", dateToUse)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("version", 1)

        val progArray = org.json.JSONArray()
        _program.value.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("itemNo", item.itemNo)
            obj.put("startTime", item.startTime)
            obj.put("ageGroup", item.ageGroup)
            obj.put("gender", item.gender)
            obj.put("eventName", item.eventName)
            obj.put("venue", item.venue)
            obj.put("status", "Wagtend")
            progArray.put(obj)
        }
        root.put("program", progArray)

        val recArray = org.json.JSONArray()
        _records.value.forEach { rec ->
            val obj = org.json.JSONObject()
            obj.put("eventName", rec.eventName)
            obj.put("ageGroup", rec.ageGroup)
            obj.put("gender", rec.gender)
            obj.put("recordValue", rec.recordValue)
            obj.put("holder", rec.holder)
            obj.put("year", rec.year)
            recArray.put(obj)
        }
        root.put("records", recArray)

        return root.toString(2)
    }

    fun importMeetTemplateJson(
        jsonString: String,
        clearExistingProgram: Boolean = true,
        clearExistingRecords: Boolean = true,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        val firestore = db ?: run {
            onComplete?.invoke(false, "Firestore nie gereed nie")
            return
        }
        val uid = currentUid()

        try {
            val root = org.json.JSONObject(jsonString)
            val meetName = root.optString("meetName", "")
            val meetDate = root.optString("meetDate", "")
            if (meetName.isNotBlank()) {
                updateMeetInfo(meetName, meetDate)
            }
            val progArray = root.optJSONArray("program") ?: org.json.JSONArray()
            val recArray = root.optJSONArray("records") ?: org.json.JSONArray()

            val progItems = mutableListOf<ProgramItem>()
            for (i in 0 until progArray.length()) {
                val obj = progArray.getJSONObject(i)
                progItems.add(
                    ProgramItem(
                        itemNo = obj.optInt("itemNo", i + 1),
                        startTime = obj.optString("startTime", "08:00"),
                        ageGroup = obj.optString("ageGroup", "13"),
                        gender = obj.optString("gender", "Seuns"),
                        eventName = obj.optString("eventName", "100m"),
                        venue = obj.optString("venue", "Hoofbaan"),
                        status = "Wagtend"
                    )
                )
            }

            val recordItems = mutableListOf<EventRecord>()
            for (i in 0 until recArray.length()) {
                val obj = recArray.getJSONObject(i)
                recordItems.add(
                    EventRecord(
                        eventName = obj.optString("eventName", "100m"),
                        ageGroup = obj.optString("ageGroup", "13"),
                        gender = obj.optString("gender", "Seuns"),
                        recordValue = obj.optString("recordValue", "-"),
                        holder = obj.optString("holder", "-"),
                        year = obj.optInt("year", 2024)
                    )
                )
            }

            scope.launch {
                try {
                    if (clearExistingProgram) {
                        val pSnap = com.google.android.gms.tasks.Tasks.await(firestore.collection("program").get())
                        val batchP = firestore.batch()
                        pSnap.documents.forEach { batchP.delete(it.reference) }
                        com.google.android.gms.tasks.Tasks.await(batchP.commit())
                    }
                    if (clearExistingRecords) {
                        val rSnap = com.google.android.gms.tasks.Tasks.await(firestore.collection("records").get())
                        val batchR = firestore.batch()
                        rSnap.documents.forEach { batchR.delete(it.reference) }
                        com.google.android.gms.tasks.Tasks.await(batchR.commit())
                    }

                    progItems.chunked(400).forEach { chunk ->
                        val batch = firestore.batch()
                        chunk.forEach { p ->
                            val docRef = firestore.collection("program").document()
                            batch.set(
                                docRef, mapOf(
                                    "itemNo" to p.itemNo,
                                    "startTime" to p.startTime,
                                    "ageGroup" to p.ageGroup,
                                    "gender" to p.gender,
                                    "eventName" to p.eventName,
                                    "venue" to p.venue,
                                    "status" to "Wagtend",
                                    "userId" to uid,
                                    "createdAt" to FieldValue.serverTimestamp(),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                        com.google.android.gms.tasks.Tasks.await(batch.commit())
                    }

                    recordItems.chunked(400).forEach { chunk ->
                        val batch = firestore.batch()
                        chunk.forEach { r ->
                            val docRef = firestore.collection("records").document()
                            batch.set(
                                docRef, mapOf(
                                    "eventName" to r.eventName,
                                    "ageGroup" to r.ageGroup,
                                    "gender" to r.gender,
                                    "recordValue" to r.recordValue,
                                    "holder" to r.holder,
                                    "year" to r.year,
                                    "userId" to uid,
                                    "createdAt" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                        com.google.android.gms.tasks.Tasks.await(batch.commit())
                    }

                    onComplete?.invoke(true, "Byeenkoms \"$meetName\" ($meetDate) herstel met ${progItems.size} program items en ${recordItems.size} rekords!")
                } catch (e: Exception) {
                    onComplete?.invoke(false, "Fout met stoor na Firestore: ${e.localizedMessage}")
                }
            }
        } catch (e: Exception) {
            onComplete?.invoke(false, "Fout met ontleding van lêer: ${e.localizedMessage}")
        }
    }

    fun resetMeetForNewYear(
        clearAthletes: Boolean = true,
        resetProgramStatus: Boolean = true,
        clearResults: Boolean = true,
        onComplete: (() -> Unit)? = null
    ) {
        val firestore = db ?: return
        scope.launch {
            try {
                if (clearAthletes) {
                    val snap = com.google.android.gms.tasks.Tasks.await(firestore.collection("athletes").get())
                    val b = firestore.batch()
                    snap.documents.forEach { b.delete(it.reference) }
                    com.google.android.gms.tasks.Tasks.await(b.commit())
                }
                if (clearResults) {
                    val snap = com.google.android.gms.tasks.Tasks.await(firestore.collection("results").get())
                    val b = firestore.batch()
                    snap.documents.forEach { b.delete(it.reference) }
                    com.google.android.gms.tasks.Tasks.await(b.commit())
                }
                if (resetProgramStatus) {
                    val snap = com.google.android.gms.tasks.Tasks.await(firestore.collection("program").get())
                    snap.documents.chunked(400).forEach { chunk ->
                        val b = firestore.batch()
                        chunk.forEach { doc ->
                            b.update(doc.reference, "status", "Wagtend", "updatedAt", FieldValue.serverTimestamp())
                        }
                        com.google.android.gms.tasks.Tasks.await(b.commit())
                    }
                }
                onComplete?.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "Error in resetMeetForNewYear", e)
                onComplete?.invoke()
            }
        }
    }
}
