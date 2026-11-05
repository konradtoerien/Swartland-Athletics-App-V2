package com.example.swartlandathletics.data

import com.example.swartlandathletics.model.Athlete
import com.example.swartlandathletics.model.EventRecord
import com.example.swartlandathletics.model.EventResult
import com.example.swartlandathletics.model.ProgramItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

object AthleticsRepository {
    private val athleteIdGen = AtomicLong(100)
    private val recordIdGen = AtomicLong(100)
    private val resultIdGen = AtomicLong(100)
    private val programIdGen = AtomicLong(100)

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

    init {
        seedInitialData()
    }

    fun seedInitialData() {
        val initialAthletes = listOf(
            Athlete(1, "Liam", "van der Merwe", "Laerskool Swartland", "Seuns", "13", listOf("100m", "200m", "Long Jump")),
            Athlete(2, "Jean", "du Plessis", "Laerskool Swartland", "Seuns", "13", listOf("80m", "100m", "Shot Put")),
            Athlete(3, "Mieke", "Smit", "Laerskool Swartland", "Dogters", "13", listOf("100m", "75m Hurdles", "High Jump")),
            Athlete(4, "Anika", "Kotze", "Laerskool Swartland", "Dogters", "12", listOf("100m", "150m", "Long Jump")),
            Athlete(5, "Ruan", "Botha", "Laerskool Vredenburg", "Seuns", "13", listOf("100m", "200m", "80m Hurdles")),
            Athlete(6, "Kobus", "Visser", "Laerskool Vredenburg", "Seuns", "12", listOf("100m", "150m", "Shot Put")),
            Athlete(7, "Carla", "Louw", "Laerskool Vredenburg", "Dogters", "13", listOf("100m", "200m", "Long Jump")),
            Athlete(8, "Zack", "Marais", "Laerskool Malmesbury", "Seuns", "13", listOf("100m", "80m", "High Jump")),
            Athlete(9, "Danie", "Theron", "Laerskool Malmesbury", "Seuns", "11", listOf("80m", "100m", "70m Hurdles")),
            Athlete(10, "Lize", "Bester", "Laerskool Malmesbury", "Dogters", "13", listOf("100m", "1500m", "Long Jump")),
            Athlete(11, "Jaco", "Pretorius", "Weskus Skool", "Seuns", "13", listOf("100m", "200m", "Discus")),
            Athlete(12, "Mia", "Nel", "Weskus Skool", "Dogters", "12", listOf("100m", "Shot Put", "Discus")),
            Athlete(13, "Pieter", "Steyn", "Dirkie Uys", "Seuns", "10", listOf("80m", "100m", "Long Jump")),
            Athlete(14, "Nadine", "Fourie", "Dirkie Uys", "Dogters", "11", listOf("60m", "80m", "High Jump")),
            Athlete(15, "Stefan", "van Zyl", "Laerskool Swartland", "Seuns", "12", listOf("100m", "High Jump", "Javelin")),
            Athlete(16, "Emma", "de Villiers", "Laerskool Swartland", "Dogters", "10", listOf("60m", "80m", "Long Jump")),
            Athlete(17, "Christiaan", "Joubert", "Laerskool Vredenburg", "Seuns", "13", listOf("High Jump", "Long Jump", "100m")),
            Athlete(18, "Marli", "Kruger", "Dirkie Uys", "Dogters", "13", listOf("Shot Put", "Discus", "Javelin"))
        )
        _athletes.value = initialAthletes

        val initialRecords = listOf(
            EventRecord(1, "100m", "Seuns", "13", "11.62s", "L. van der Merwe (LSS)", 2024),
            EventRecord(2, "100m", "Dogters", "13", "12.35s", "M. Smit (LSS)", 2023),
            EventRecord(3, "200m", "Seuns", "13", "24.18s", "R. Botha (Vredenburg)", 2023),
            EventRecord(4, "High Jump", "Seuns", "13", "1.65m", "C. Joubert (Vredenburg)", 2022),
            EventRecord(5, "High Jump", "Dogters", "13", "1.52m", "M. Smit (LSS)", 2024),
            EventRecord(6, "Long Jump", "Seuns", "13", "5.45m", "L. van der Merwe (LSS)", 2024),
            EventRecord(7, "Shot Put", "Seuns", "13", "12.80m", "J. du Plessis (LSS)", 2023),
            EventRecord(8, "Shot Put", "Dogters", "12", "10.42m", "M. Nel (Weskus)", 2023),
            EventRecord(9, "80m", "Seuns", "11", "10.40s", "D. Theron (Malmesbury)", 2024),
            EventRecord(10, "1500m", "Dogters", "13", "5:08.12", "L. Bester (Malmesbury)", 2024)
        )
        _records.value = initialRecords

        val initialProgram = listOf(
            ProgramItem(1, 1, "08:00", "13", "Seuns", "100m", "Hoofbaan", "Voltooi"),
            ProgramItem(2, 2, "08:15", "13", "Dogters", "100m", "Hoofbaan", "Voltooi"),
            ProgramItem(3, 3, "08:30", "13", "Seuns", "High Jump", "Hoogspring Mat A", "Besig"),
            ProgramItem(4, 4, "08:45", "12", "Dogters", "Shot Put", "Gewigstoot Sirkel", "Wagtend"),
            ProgramItem(5, 5, "09:00", "13", "Seuns", "200m", "Hoofbaan", "Wagtend"),
            ProgramItem(6, 6, "09:20", "11", "Seuns", "80m", "Baan 1", "Wagtend"),
            ProgramItem(7, 7, "09:40", "13", "Dogters", "Long Jump", "Verspring Put 1", "Wagtend"),
            ProgramItem(8, 8, "10:00", "13", "Seuns", "Shot Put", "Gewigstoot Sirkel", "Wagtend"),
            ProgramItem(9, 9, "10:30", "13", "Dogters", "75m Hurdles", "Hoofbaan", "Wagtend"),
            ProgramItem(10, 10, "11:00", "13", "Seuns", "4x100m Relay", "Hoofbaan", "Wagtend")
        )
        _program.value = initialProgram

        val initialResults = listOf(
            EventResult(1, "100m", "Seuns", "13", 1, 1, "Liam van der Merwe", "Laerskool Swartland", "11.60s", 944),
            EventResult(2, "100m", "Seuns", "13", 2, 5, "Ruan Botha", "Laerskool Vredenburg", "11.78s", 937),
            EventResult(3, "100m", "Seuns", "13", 3, 2, "Jean du Plessis", "Laerskool Swartland", "11.95s", 931),
            EventResult(4, "100m", "Seuns", "13", 4, 8, "Zack Marais", "Laerskool Malmesbury", "12.10s", 926),
            EventResult(5, "100m", "Dogters", "13", 1, 3, "Mieke Smit", "Laerskool Swartland", "12.30s", 919),
            EventResult(6, "100m", "Dogters", "13", 2, 7, "Carla Louw", "Laerskool Vredenburg", "12.55s", 910),
            EventResult(7, "100m", "Dogters", "13", 3, 10, "Lize Bester", "Laerskool Malmesbury", "12.80s", 902),
            EventResult(8, "100m", "Dogters", "13", 4, 4, "Anika Kotze", "Laerskool Swartland", "13.05s", 893)
        )
        _results.value = initialResults
    }

    // Athletes
    fun addAthlete(firstName: String, lastName: String, school: String, gender: String, ageGroup: String, events: List<String>): Athlete {
        val athlete = Athlete(
            id = athleteIdGen.incrementAndGet(),
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            school = school.trim(),
            gender = gender,
            ageGroup = ageGroup,
            events = events
        )
        _athletes.value = _athletes.value + athlete
        return athlete
    }

    fun deleteAthlete(id: Long) {
        _athletes.value = _athletes.value.filter { it.id != id }
    }

    // Records
    fun addRecord(eventName: String, gender: String, ageGroup: String, recordValue: String, holder: String, year: Int) {
        val record = EventRecord(
            id = recordIdGen.incrementAndGet(),
            eventName = eventName.trim(),
            gender = gender,
            ageGroup = ageGroup,
            recordValue = recordValue.trim(),
            holder = holder.trim(),
            year = year
        )
        _records.value = _records.value + record
    }

    fun deleteRecord(recordId: Long) {
        _records.value = _records.value.filter { it.id != recordId }
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
        // Remove existing results for this exact item if replacing
        val filtered = _results.value.filterNot {
            it.eventName.equals(eventName, ignoreCase = true) &&
                    it.gender.equals(gender, ignoreCase = true) &&
                    it.ageGroup == ageGroup
        }
        val numbered = results.mapIndexed { idx, item ->
            item.copy(
                id = resultIdGen.incrementAndGet(),
                place = idx + 1,
                eventName = eventName,
                gender = gender,
                ageGroup = ageGroup
            )
        }
        _results.value = numbered + filtered

        // Update corresponding program item status to "Voltooi"
        val prog = _program.value.map { item ->
            if (item.eventName.equals(eventName, ignoreCase = true) &&
                item.gender.equals(gender, ignoreCase = true) &&
                item.ageGroup == ageGroup
            ) {
                item.copy(status = "Voltooi")
            } else {
                item
            }
        }
        _program.value = prog
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
        val item = ProgramItem(
            id = programIdGen.incrementAndGet(),
            itemNo = itemNo,
            startTime = startTime,
            ageGroup = ageGroup,
            gender = gender,
            eventName = eventName,
            venue = venue,
            status = status
        )
        _program.value = (_program.value + item).sortedBy { it.itemNo }
    }

    fun updateProgramStatus(id: Long, newStatus: String) {
        _program.value = _program.value.map {
            if (it.id == id) it.copy(status = newStatus) else it
        }
    }

    fun resetAll() {
        _athletes.value = emptyList()
        _records.value = emptyList()
        _results.value = emptyList()
        _program.value = emptyList()
        _completedCeremonies.value = emptySet()
    }
}
