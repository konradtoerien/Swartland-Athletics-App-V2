package com.example.swartlandathletics.model

object AppConstants {
    val AGE_GROUPS = listOf("6", "7", "8", "9", "10", "11", "12", "13")
    val GENDERS = listOf("Seuns", "Dogters")

    val ALL_DISCIPLINES = listOf(
        "50m", "60m", "80m", "100m", "150m", "200m",
        "1200m", "1500m", "1500m Walk", "4x100m Relay",
        "70m Hurdles", "75m Hurdles", "80m Hurdles", "150m Hurdles", "200m Hurdles",
        "Long Jump", "High Jump", "Shot Put", "Discus", "Javelin"
    )

    val TRACK_EVENTS = listOf("50m", "60m", "80m", "100m", "150m", "200m", "800m", "1200m", "1500m", "4x100m Relay")
    val FIELD_EVENTS = listOf("Long Jump", "High Jump", "Shot Put", "Discus", "Javelin")
    val HURDLE_EVENTS = listOf("70m Hurdles", "75m Hurdles", "80m Hurdles", "150m Hurdles", "200m Hurdles")
    val WALK_EVENTS = listOf("1500m Walk")

    val VENUES = listOf("Hoofbaan", "Baan 1", "Baan 2", "Hoogspring Mat A", "Verspring Put 1", "Gewigstoot Sirkel", "Diskus / Spiesgooi Veld")

    fun normalizeAgeGroup(input: String): String {
        val s = input.trim().lowercase().replace("u/", "").replace("u", "").replace("o/", "").replace("o", "")
        return if (s in AGE_GROUPS) s else "13"
    }

    fun getEventsForAgeAndGender(ageGroup: String, gender: String): List<String> {
        val age = normalizeAgeGroup(ageGroup)
        val isGirls = gender.trim().lowercase().startsWith("d") ||
                gender.trim().lowercase().startsWith("g") ||
                gender.trim().lowercase().startsWith("f")

        val events = mutableListOf<String>()
        when (age) {
            "6" -> events.add("50m")
            "7", "8" -> {
                events.add("60m")
                events.add("80m")
            }
            "9" -> {
                events.addAll(listOf("60m", "80m", "100m", "1200m", "Long Jump", "High Jump", "Shot Put"))
            }
            "10", "11" -> {
                events.addAll(listOf("60m", "80m", "100m", "1200m", "70m Hurdles", "Long Jump", "High Jump", "Shot Put"))
            }
            "12" -> {
                events.addAll(
                    listOf(
                        "60m", "80m", "100m", "150m", "1200m",
                        "75m Hurdles", "150m Hurdles",
                        "Long Jump", "High Jump", "Shot Put", "Discus", "Javelin", "1500m Walk"
                    )
                )
            }
            "13" -> {
                val hurdle = if (isGirls) "75m Hurdles" else "80m Hurdles"
                events.addAll(
                    listOf(
                        "80m", "100m", "200m", "1500m",
                        hurdle, "200m Hurdles",
                        "Long Jump", "High Jump", "Shot Put", "Discus", "Javelin", "1500m Walk"
                    )
                )
            }
            else -> events.addAll(listOf("80m", "100m", "200m", "Long Jump", "Shot Put"))
        }

        events.add("4x100m Relay")
        return events
    }

    fun isTrackEvent(eventName: String): Boolean {
        val lower = eventName.lowercase()
        val isField = lower.contains("jump") || lower.contains("put") || lower.contains("discus") || lower.contains("javelin")
        val isTrackKeyword = lower.contains("m") || lower.contains("hurdle") || lower.contains("walk") || lower.contains("relay")
        return isTrackKeyword && !isField
    }

    fun isHighJump(eventName: String): Boolean = eventName.lowercase().contains("high jump")

    fun isFieldEvent(eventName: String): Boolean = !isTrackEvent(eventName)
}
