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

    // Exact order of events as on Laerskool Swartland Inskrywingsdokument (Image 1)
    val ENTRY_SHEET_DISCIPLINES = listOf(
        // Track
        "50m", "60m", "80m", "100m", "150m", "200m", "1200m", "1500m", "4x100m Relay",
        // Field
        "Long Jump", "High Jump", "Shot Put", "Discus", "Javelin",
        // Hurdles
        "70m Hurdles", "75m Hurdles", "80m Hurdles", "150m Hurdles", "200m Hurdles",
        // Walk
        "1500m Walk"
    )

    fun normalizeDiscipline(raw: String): String {
        val clean = raw.trim()
        val lower = clean.lowercase().replace(" ", "")
        return when {
            lower == "50m" -> "50m"
            lower == "60m" -> "60m"
            lower == "80m" -> "80m"
            lower == "100m" -> "100m"
            lower == "150m" -> "150m"
            lower == "200m" -> "200m"
            lower == "800m" -> "800m"
            lower == "1200m" -> "1200m"
            lower == "1500m" -> "1500m"
            lower.contains("relay") || lower.contains("aflos") || lower.contains("4x100") -> "4x100m Relay"
            lower.contains("verspring") || lower.contains("longjump") -> "Long Jump"
            lower.contains("hoogspring") || lower.contains("highjump") -> "High Jump"
            lower.contains("gewigstoot") || lower.contains("shotput") || lower.contains("shot") -> "Shot Put"
            lower.contains("diskus") || lower.contains("discus") -> "Discus"
            lower.contains("spiesgooi") || lower.contains("javelin") || lower.contains("spies") -> "Javelin"
            lower.contains("70") && (lower.contains("hekkie") || lower.contains("hurdle")) -> "70m Hurdles"
            lower.contains("75") && (lower.contains("hekkie") || lower.contains("hurdle")) -> "75m Hurdles"
            lower.contains("80") && (lower.contains("hekkie") || lower.contains("hurdle")) -> "80m Hurdles"
            lower.contains("150") && (lower.contains("hekkie") || lower.contains("hurdle")) -> "150m Hurdles"
            lower.contains("200") && (lower.contains("hekkie") || lower.contains("hurdle")) -> "200m Hurdles"
            lower.contains("stap") || lower.contains("walk") -> "1500m Walk"
            else -> clean
        }
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
