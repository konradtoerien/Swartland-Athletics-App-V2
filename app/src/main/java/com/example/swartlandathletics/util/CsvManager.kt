package com.example.swartlandathletics.util

import android.content.Context
import android.content.Intent
import com.example.swartlandathletics.model.AppConstants
import com.example.swartlandathletics.model.Athlete
import com.example.swartlandathletics.model.EventRecord
import com.example.swartlandathletics.model.EventResult
import com.example.swartlandathletics.model.ProgramItem

object CsvManager {

    // =========================================================================
    // 1. INSKRYWINGSDOKUMENT (Image 1: Athlete Entry Spreadsheet)
    // =========================================================================

    /**
     * Generates the official Laerskool Swartland athlete entry spreadsheet matching Image 1.
     * Header columns: Ouderdom, Voornaam, Van, Skool followed by the 20 event columns.
     * Schools tick with 'x' under each event.
     */
    fun generateAthleteMatrixTemplateCsv(gender: String = "Seuns"): String {
        val isDogters = gender.trim().lowercase().startsWith("d") ||
                gender.trim().lowercase().startsWith("g") ||
                gender.trim().lowercase().startsWith("f")
        val genderLabel = if (isDogters) "DOGTERS (D)" else "SEUNS (M)"
        val defaultSchool = "Laerskool Swartland"

        val sb = StringBuilder()
        // Row 1: Document title (exact wording from Image 1)
        sb.append("LAERSKOOL SWARTLAND - INSKRYWINGSDOKUMENT ($genderLabel)\n")

        // Row 2: Category grouping hints (Track, Field, Hurdles, Walk)
        sb.append(",,,,Track,Track,Track,Track,Track,Track,Track,Track,Track,Field,Field,Field,Field,Field,Hurdles,Hurdles,Hurdles,Hurdles,Hurdles,Walk\n")

        // Row 3: Header columns
        sb.append("Ouderdom,Voornaam,Van,Skool,")
        sb.append(AppConstants.ENTRY_SHEET_DISCIPLINES.joinToString(","))
        sb.append("\n")

        // Sample rows showing how schools fill in with 'x'
        if (!isDogters) {
            sb.append("13,Liam,van der Merwe,$defaultSchool,,,,x,,,,,,x,,,,,,,,\n")
            sb.append("13,Jean,du Plessis,$defaultSchool,,x,,x,,,,,,,,x,,,,,,,\n")
            sb.append("12,Ruan,Botha,Laerskool Vredenburg,,,,x,,x,,,,,,,,x,,,,,\n")
            sb.append("11,Kobus,Visser,Laerskool Malmesbury,,,x,x,,,,,,,x,,,,,,,,\n")
            sb.append("10,Danie,Theron,Dirkie Uys,,,,,,,,,x,,,,,,,,,\n")
        } else {
            sb.append("13,Mieke,Smit,$defaultSchool,,,,x,,,,,,x,,,,,x,,,\n")
            sb.append("12,Anika,Kotze,$defaultSchool,,,,x,,x,,,,x,,,,,,,,,\n")
            sb.append("13,Carla,Louw,Laerskool Vredenburg,,,,x,x,,,,,,,,,,,,,,x\n")
            sb.append("11,Lize,Bester,Laerskool Malmesbury,,,x,,,,,,,,,,,,x,,,,\n")
        }

        return sb.toString()
    }

    /**
     * Generates a combined athlete entry spreadsheet containing an optional 'Geslag' column.
     */
    fun generateCombinedAthleteMatrixTemplateCsv(): String {
        val sb = StringBuilder()
        sb.append("LAERSKOOL SWARTLAND - INSKRYWINGSDOKUMENT (SEUNS & DOGTERS)\n")
        sb.append(",,,,,Track,Track,Track,Track,Track,Track,Track,Track,Track,Field,Field,Field,Field,Field,Hurdles,Hurdles,Hurdles,Hurdles,Hurdles,Walk\n")
        sb.append("Ouderdom,Voornaam,Van,Skool,Geslag,")
        sb.append(AppConstants.ENTRY_SHEET_DISCIPLINES.joinToString(","))
        sb.append("\n")
        sb.append("13,Liam,van der Merwe,Laerskool Swartland,Seuns,,,,x,,,,,,x,,,,,,,,\n")
        sb.append("13,Mieke,Smit,Laerskool Swartland,Dogters,,,,x,,,,,,x,,,,,x,,,\n")
        sb.append("12,Ruan,Botha,Laerskool Vredenburg,Seuns,,,,x,,x,,,,,,,,x,,,,,\n")
        sb.append("12,Anika,Kotze,Laerskool Swartland,Dogters,,,,x,,x,,,,x,,,,,,,,,\n")
        return sb.toString()
    }

    /**
     * Exports registered athletes to the Image 1 wide spreadsheet format with 'x' marks.
     */
    fun exportAthletesToMatrixCsv(athletes: List<Athlete>, genderFilter: String? = null): String {
        val filtered = if (!genderFilter.isNullOrBlank()) {
            athletes.filter { it.gender.equals(genderFilter, ignoreCase = true) }
        } else {
            athletes
        }

        val sb = StringBuilder()
        val titleGender = genderFilter?.let {
            if (it.lowercase().startsWith("d")) " (DOGTERS (D))" else " (SEUNS (M))"
        } ?: " (ALLE ATLETE)"
        sb.append("LAERSKOOL SWARTLAND - INSKRYWINGSDOKUMENT$titleGender\n")
        sb.append("Ouderdom,Voornaam,Van,Skool,Geslag,")
        sb.append(AppConstants.ENTRY_SHEET_DISCIPLINES.joinToString(","))
        sb.append("\n")

        val disciplines = AppConstants.ENTRY_SHEET_DISCIPLINES

        filtered.sortedWith(compareBy({ it.school }, { it.gender }, { it.ageGroup }, { it.lastName })).forEach { a ->
            val row = mutableListOf<String>()
            row.add(a.ageGroup)
            row.add(escapeCsv(a.firstName))
            row.add(escapeCsv(a.lastName))
            row.add(escapeCsv(a.school))
            row.add(a.gender)

            val athleteNormalizedEvents = a.events.map { AppConstants.normalizeDiscipline(it).lowercase() }

            disciplines.forEach { disc ->
                val discNorm = disc.lowercase()
                val isEntered = athleteNormalizedEvents.any { it == discNorm }
                row.add(if (isEntered) "x" else "")
            }
            sb.append(row.joinToString(",") + "\n")
        }
        return sb.toString()
    }

    /**
     * Legacy simple export format: Voornaam,Van,Skool,Geslag,Ouderdom,Items
     */
    fun exportAthletesToCsv(athletes: List<Athlete>): String {
        val sb = StringBuilder()
        sb.append("Voornaam,Van,Skool,Geslag,Ouderdom,Items\n")
        athletes.forEach { a ->
            val escapedEvents = a.events.joinToString(";").replace("\"", "\"\"")
            sb.append("${escapeCsv(a.firstName)},${escapeCsv(a.lastName)},${escapeCsv(a.school)},${a.gender},${a.ageGroup},\"$escapedEvents\"\n")
        }
        return sb.toString()
    }

    /**
     * Parses uploaded CSV or copied text for athletes.
     * Automatically handles both:
     * 1) Image 1 Matrix Format (columns: Ouderdom, Voornaam, Van, Skool, [50m, 60m, ... with 'x'])
     * 2) Standard List Format (columns: Voornaam, Van, Skool, Geslag, Ouderdom, Items)
     */
    fun parseAthletesCsv(rawContent: String, fallbackGender: String = "Seuns"): List<Athlete> {
        val lines = rawContent.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        // 1. Detect if sheet has a title specifying sheet-wide gender
        var sheetGender = fallbackGender
        val firstTwoLinesText = lines.take(3).joinToString(" ").lowercase()
        when {
            firstTwoLinesText.contains("seuns") || firstTwoLinesText.contains("(m)") || firstTwoLinesText.contains("boys") -> {
                sheetGender = "Seuns"
            }
            firstTwoLinesText.contains("dogters") || firstTwoLinesText.contains("(d)") || firstTwoLinesText.contains("girls") -> {
                sheetGender = "Dogters"
            }
        }

        // 2. Locate the header row
        var headerIndex = -1
        var headerCols: List<String> = emptyList()

        for (i in 0 until minOf(5, lines.size)) {
            val cols = splitCsvLine(lines[i])
            val colsLower = cols.map { it.lowercase().trim() }
            val hasName = colsLower.any { it.contains("voornaam") || it.contains("firstname") || it == "name" || it == "naam" }
            val hasSurname = colsLower.any { it.contains("van") || it.contains("lastname") || it.contains("surname") }
            val hasSchool = colsLower.any { it.contains("skool") || it.contains("school") }
            if ((hasName && hasSurname) || (hasName && hasSchool)) {
                headerIndex = i
                headerCols = cols
                break
            }
        }

        if (headerIndex == -1) {
            // Fallback: assume first line is header
            headerIndex = 0
            headerCols = splitCsvLine(lines[0])
        }

        val headerLower = headerCols.map { it.trim().lowercase() }

        // Find standard column indexes
        val idxAge = headerLower.indexOfFirst { it.contains("ouderdom") || it.contains("age") }
        val idxFirst = headerLower.indexOfFirst { it.contains("voornaam") || it.contains("first") || it == "naam" }
        val idxLast = headerLower.indexOfFirst { it.contains("van") || it.contains("last") || it.contains("surname") }
        val idxSchool = headerLower.indexOfFirst { it.contains("skool") || it.contains("school") }
        val idxGender = headerLower.indexOfFirst { it.contains("geslag") || it.contains("gender") }
        val idxItems = headerLower.indexOfFirst { it.contains("items") || it.contains("events") || it.contains("dissiplines") }

        // Determine if this is Matrix Format (Image 1) by checking if any column headers match known event names
        val eventColumnMap = mutableMapOf<Int, String>()
        for (colIdx in headerCols.indices) {
            if (colIdx in listOf(idxAge, idxFirst, idxLast, idxSchool, idxGender, idxItems)) continue
            val colName = headerCols[colIdx].trim()
            if (colName.isBlank()) continue
            val normalizedEvent = AppConstants.normalizeDiscipline(colName)
            if (normalizedEvent in AppConstants.ALL_DISCIPLINES ||
                colName.lowercase().contains("m") ||
                colName.lowercase().contains("jump") ||
                colName.lowercase().contains("put") ||
                colName.lowercase().contains("discus") ||
                colName.lowercase().contains("javelin") ||
                colName.lowercase().contains("hurdle") ||
                colName.lowercase().contains("walk") ||
                colName.lowercase().contains("relay")
            ) {
                eventColumnMap[colIdx] = normalizedEvent
            }
        }

        val isMatrixFormat = eventColumnMap.isNotEmpty()
        val parsedAthletes = mutableListOf<Athlete>()

        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i]
            val cols = splitCsvLine(line)
            if (cols.size < 2) continue

            val firstName = if (idxFirst >= 0 && idxFirst < cols.size) cols[idxFirst].trim() else ""
            val lastName = if (idxLast >= 0 && idxLast < cols.size) cols[idxLast].trim() else ""
            val school = if (idxSchool >= 0 && idxSchool < cols.size) cols[idxSchool].trim() else ""

            if (firstName.isBlank() && lastName.isBlank()) continue

            // Determine Gender
            val rawGender = if (idxGender >= 0 && idxGender < cols.size && cols[idxGender].isNotBlank()) {
                cols[idxGender].trim()
            } else {
                sheetGender
            }
            val normalizedGender = when {
                rawGender.lowercase().startsWith("d") ||
                        rawGender.lowercase().startsWith("g") ||
                        rawGender.lowercase().startsWith("f") -> "Dogters"
                else -> "Seuns"
            }

            // Determine Age Group
            val rawAge = if (idxAge >= 0 && idxAge < cols.size && cols[idxAge].isNotBlank()) {
                cols[idxAge].trim()
            } else {
                "13"
            }
            val normalizedAge = AppConstants.normalizeAgeGroup(rawAge)

            // Extract Events
            val eventsList = mutableListOf<String>()

            if (isMatrixFormat) {
                // Check each event column for an 'x', 'X', '1', or check indicator
                for ((colIdx, eventName) in eventColumnMap) {
                    if (colIdx < cols.size) {
                        val cellVal = cols[colIdx].trim().lowercase()
                        if (cellVal.isNotBlank() &&
                            cellVal != "-" &&
                            cellVal != "0" &&
                            cellVal != "nee" &&
                            cellVal != "no" &&
                            cellVal != "false"
                        ) {
                            eventsList.add(eventName)
                        }
                    }
                }
            } else if (idxItems >= 0 && idxItems < cols.size) {
                // Semicolon/comma separated list in single column
                val rawEvents = cols[idxItems]
                eventsList.addAll(
                    rawEvents.split(';', ',', '|', '/')
                        .map { AppConstants.normalizeDiscipline(it.trim()) }
                        .filter { it.isNotBlank() }
                )
            }

            parsedAthletes.add(
                Athlete(
                    id = "",
                    firstName = firstName,
                    lastName = lastName,
                    school = school,
                    gender = normalizedGender,
                    ageGroup = normalizedAge,
                    events = eventsList.distinct()
                )
            )
        }

        return parsedAthletes
    }

    // =========================================================================
    // 2. PROGRAMDOKUMENT (Image 2: Meet Program Spreadsheet)
    // =========================================================================

    /**
     * Generates empty program spreadsheet matching Image 2.
     * Header: LAERSKOOL SWARTLAND - AMPTELIKE PROGRAMDOKUMENT
     * Columns: Item No,Begin Tyd,Ouderdom,Geslag,Item,Plek,Status
     */
    fun generateProgramTemplateCsv(): String {
        val sb = StringBuilder()
        sb.append("LAERSKOOL SWARTLAND - AMPTELIKE PROGRAMDOKUMENT\n")
        sb.append("Item No,Begin Tyd,Ouderdom,Geslag,Item,Plek,Status\n")
        sb.append("1,08:00,13,Seuns,100m,Hoofbaan,Wagtend\n")
        sb.append("2,08:15,13,Dogters,100m,Hoofbaan,Wagtend\n")
        sb.append("3,08:30,12,Seuns,Verspring,Verspring Put 1,Wagtend\n")
        sb.append("4,08:45,12,Dogters,Hoogspring,Hoogspring Mat A,Wagtend\n")
        sb.append("5,09:00,11,Seuns,Gewigstoot,Gewigstoot Sirkel,Wagtend\n")
        sb.append("6,09:15,11,Dogters,70m Hekkies,Hoofbaan,Wagtend\n")
        sb.append("7,09:30,13,Seuns,1500m,Hoofbaan,Wagtend\n")
        sb.append("8,09:50,13,Dogters,1500m,Hoofbaan,Wagtend\n")
        return sb.toString()
    }

    /**
     * Exports current program to Image 2 CSV format.
     */
    fun exportProgramToCsv(program: List<ProgramItem>): String {
        val sb = StringBuilder()
        sb.append("LAERSKOOL SWARTLAND - AMPTELIKE PROGRAMDOKUMENT\n")
        sb.append("Item No,Begin Tyd,Ouderdom,Geslag,Item,Plek,Status\n")
        program.sortedBy { it.itemNo }.forEach { p ->
            sb.append("${p.itemNo},${escapeCsv(p.startTime)},${p.ageGroup},${p.gender},${escapeCsv(p.eventName)},${escapeCsv(p.venue)},${escapeCsv(p.status)}\n")
        }
        return sb.toString()
    }

    /**
     * Parses uploaded program CSV or text.
     * Columns expected: Item No, Begin Tyd, Ouderdom, Geslag, Item, Plek, Status
     */
    fun parseProgramCsv(rawContent: String): List<ProgramItem> {
        val lines = rawContent.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        var headerIndex = -1
        var headerCols: List<String> = emptyList()

        for (i in 0 until minOf(5, lines.size)) {
            val cols = splitCsvLine(lines[i])
            val colsLower = cols.map { it.lowercase().trim() }
            if (colsLower.any { it.contains("item") } && (colsLower.any { it.contains("tyd") || it.contains("time") } || colsLower.any { it.contains("ouderdom") || it.contains("geslag") })) {
                headerIndex = i
                headerCols = cols
                break
            }
        }

        if (headerIndex == -1) {
            headerIndex = 0
            headerCols = splitCsvLine(lines[0])
        }

        val headerLower = headerCols.map { it.lowercase().trim() }

        val idxItemNo = headerLower.indexOfFirst { it.contains("item no") || it.contains("nommer") || it == "no" || it == "nr" }
        val idxTime = headerLower.indexOfFirst { it.contains("tyd") || it.contains("time") || it.contains("begin") }
        val idxAge = headerLower.indexOfFirst { it.contains("ouderdom") || it.contains("age") }
        val idxGender = headerLower.indexOfFirst { it.contains("geslag") || it.contains("gender") }
        val idxEvent = headerLower.indexOfFirst { (it == "item" || it.contains("event") || it.contains("dissipline")) && it != "item no" }
        val idxVenue = headerLower.indexOfFirst { it.contains("plek") || it.contains("venue") || it.contains("baan") }
        val idxStatus = headerLower.indexOfFirst { it.contains("status") }

        val parsedItems = mutableListOf<ProgramItem>()

        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i]
            val cols = splitCsvLine(line)
            if (cols.size < 3) continue

            val rawNo = if (idxItemNo in cols.indices) cols[idxItemNo].trim() else ""
            val itemNo = rawNo.filter { it.isDigit() }.toIntOrNull() ?: (parsedItems.size + 1)

            val startTime = if (idxTime in cols.indices) cols[idxTime].trim() else "08:00"
            val rawAge = if (idxAge in cols.indices) cols[idxAge].trim() else "13"
            val ageGroup = AppConstants.normalizeAgeGroup(rawAge)

            val rawGender = if (idxGender in cols.indices) cols[idxGender].trim() else "Seuns"
            val gender = when {
                rawGender.lowercase().startsWith("d") ||
                        rawGender.lowercase().startsWith("g") ||
                        rawGender.lowercase().startsWith("f") -> "Dogters"
                else -> "Seuns"
            }

            val rawEvent = if (idxEvent in cols.indices) cols[idxEvent].trim() else "100m"
            val eventName = AppConstants.normalizeDiscipline(rawEvent)

            val venue = if (idxVenue in cols.indices && cols[idxVenue].isNotBlank()) cols[idxVenue].trim() else "Hoofbaan"
            val status = if (idxStatus in cols.indices && cols[idxStatus].isNotBlank()) cols[idxStatus].trim() else "Wagtend"

            if (eventName.isNotBlank()) {
                parsedItems.add(
                    ProgramItem(
                        id = "",
                        itemNo = itemNo,
                        startTime = startTime,
                        ageGroup = ageGroup,
                        gender = gender,
                        eventName = eventName,
                        venue = venue,
                        status = status
                    )
                )
            }
        }

        return parsedItems
    }

    // =========================================================================
    // 3. REKORDDOKUMENT (Image 3: Meet Records Spreadsheet)
    // =========================================================================

    /**
     * Generates empty records spreadsheet matching Image 3.
     * Header: LAERSKOOL SWARTLAND - AMPTELIKE REKORDDOKUMENT
     * Columns: Item,Ouderdom,Geslag,Tyd of Afstand,Skool of Atleet,Jaar
     */
    fun generateRecordsTemplateCsv(): String {
        val sb = StringBuilder()
        sb.append("LAERSKOOL SWARTLAND - AMPTELIKE REKORDDOKUMENT\n")
        sb.append("Item,Ouderdom,Geslag,Tyd of Afstand,Skool of Atleet,Jaar\n")

        // Pre-fill standard template rows for schools to easily type values into
        sb.append("100m,13,Seuns,11.42s,Liam van der Merwe (Laerskool Swartland),2023\n")
        sb.append("100m,13,Dogters,12.18s,Mieke Smit (Laerskool Swartland),2022\n")
        sb.append("200m,13,Seuns,24.10s,Jean du Plessis (Laerskool Swartland),2021\n")
        sb.append("Long Jump,13,Seuns,5.62m,Ruan Botha (Laerskool Vredenburg),2022\n")
        sb.append("High Jump,12,Dogters,1.45m,Anika Kotze (Laerskool Swartland),2020\n")
        sb.append("Shot Put,13,Seuns,13.20m,Kobus Visser (Laerskool Vredenburg),2023\n")
        sb.append("Discus,13,Seuns,38.45m,Danie Theron (Laerskool Malmesbury),2019\n")
        sb.append("Javelin,13,Seuns,36.12m,Zack Marais (Dirkie Uys),2021\n")
        sb.append("80m Hurdles,13,Seuns,12.30s,Liam van der Merwe (Laerskool Swartland),2023\n")
        sb.append("75m Hurdles,13,Dogters,11.95s,Mieke Smit (Laerskool Swartland),2022\n")
        sb.append("1500m,13,Dogters,5:12.4,Lize Bester (Laerskool Malmesbury),2022\n")
        sb.append("1500m Walk,13,Dogters,7:45.0,Carla Louw (Laerskool Vredenburg),2023\n")
        return sb.toString()
    }

    /**
     * Exports current records to Image 3 CSV format.
     */
    fun exportRecordsToCsv(records: List<EventRecord>): String {
        val sb = StringBuilder()
        sb.append("LAERSKOOL SWARTLAND - AMPTELIKE REKORDDOKUMENT\n")
        sb.append("Item,Ouderdom,Geslag,Tyd of Afstand,Skool of Atleet,Jaar\n")
        records.sortedWith(compareBy({ it.eventName }, { it.ageGroup }, { it.gender })).forEach { r ->
            sb.append("${escapeCsv(r.eventName)},${r.ageGroup},${r.gender},${escapeCsv(r.recordValue)},${escapeCsv(r.holder)},${r.year}\n")
        }
        return sb.toString()
    }

    /**
     * Parses uploaded records CSV or text.
     * Columns expected: Item, Ouderdom, Geslag, Tyd of Afstand, Skool of Atleet, Jaar
     */
    fun parseRecordsCsv(rawContent: String): List<EventRecord> {
        val lines = rawContent.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        var headerIndex = -1
        var headerCols: List<String> = emptyList()

        for (i in 0 until minOf(5, lines.size)) {
            val cols = splitCsvLine(lines[i])
            val colsLower = cols.map { it.lowercase().trim() }
            if (colsLower.any { it.contains("item") || it.contains("event") } && (colsLower.any { it.contains("tyd") || it.contains("afstand") || it.contains("record") || it.contains("rekord") })) {
                headerIndex = i
                headerCols = cols
                break
            }
        }

        if (headerIndex == -1) {
            headerIndex = 0
            headerCols = splitCsvLine(lines[0])
        }

        val headerLower = headerCols.map { it.lowercase().trim() }

        val idxItem = headerLower.indexOfFirst { it.contains("item") || it.contains("event") || it.contains("dissipline") }
        val idxAge = headerLower.indexOfFirst { it.contains("ouderdom") || it.contains("age") }
        val idxGender = headerLower.indexOfFirst { it.contains("geslag") || it.contains("gender") }
        val idxValue = headerLower.indexOfFirst { it.contains("tyd") || it.contains("afstand") || it.contains("record") || it.contains("rekord") || it.contains("prestasie") }
        val idxHolder = headerLower.indexOfFirst { it.contains("skool") || it.contains("atleet") || it.contains("holder") || it.contains("naam") }
        val idxYear = headerLower.indexOfFirst { it.contains("jaar") || it.contains("year") }

        val parsedRecords = mutableListOf<EventRecord>()

        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i]
            val cols = splitCsvLine(line)
            if (cols.size < 3) continue

            val rawItem = if (idxItem in cols.indices) cols[idxItem].trim() else ""
            if (rawItem.isBlank()) continue
            val eventName = AppConstants.normalizeDiscipline(rawItem)

            val rawAge = if (idxAge in cols.indices) cols[idxAge].trim() else "13"
            val ageGroup = AppConstants.normalizeAgeGroup(rawAge)

            val rawGender = if (idxGender in cols.indices) cols[idxGender].trim() else "Seuns"
            val gender = when {
                rawGender.lowercase().startsWith("d") ||
                        rawGender.lowercase().startsWith("g") ||
                        rawGender.lowercase().startsWith("f") -> "Dogters"
                else -> "Seuns"
            }

            val recordValue = if (idxValue in cols.indices) cols[idxValue].trim() else ""
            val holder = if (idxHolder in cols.indices) cols[idxHolder].trim() else ""
            val rawYear = if (idxYear in cols.indices) cols[idxYear].trim() else "2024"
            val year = rawYear.filter { it.isDigit() }.toIntOrNull() ?: 2024

            if (eventName.isNotBlank() && recordValue.isNotBlank()) {
                parsedRecords.add(
                    EventRecord(
                        id = "",
                        eventName = eventName,
                        gender = gender,
                        ageGroup = ageGroup,
                        recordValue = recordValue,
                        holder = holder,
                        year = year
                    )
                )
            }
        }

        return parsedRecords
    }

    // =========================================================================
    // 4. DRAW SHEETS & RESULTS EXPORTS
    // =========================================================================

    /**
     * Exports randomized or ordered draw sheets to a clean shareable CSV.
     */
    fun generateDrawSheetCsv(
        eventName: String,
        ageGroup: String,
        gender: String,
        athletes: List<Athlete>,
        isRandomized: Boolean
    ): String {
        val sb = StringBuilder()
        sb.append("BYEENKOMS LOTINGSLYS / DRAW SHEET\n")
        sb.append("Item: $eventName, Ouderdom: o/$ageGroup, Geslag: $gender\n")
        sb.append("Status: ${if (isRandomized) "Ewekansig Geloot (Randomized)" else "Standaard Volgorde"}\n")
        sb.append("Totaal Atlete: ${athletes.size}\n\n")
        sb.append("Baan/Posisie,Atleet Naam,Skool,Tyd/Afstand,Plek\n")

        athletes.forEachIndexed { index, ath ->
            val pos = index + 1
            sb.append("$pos,${escapeCsv(ath.fullName)},${escapeCsv(ath.school)},,\n")
        }
        return sb.toString()
    }

    /**
     * Exports full structured meet results report in CSV format after meet concludes.
     * Includes:
     * 1. Highest team winners (Team Standings & Awards)
     * 2. Victor and Victrix Ludorum (Overall, Senior & Junior) + Top 10 Athletes
     * 3. Best performances per discipline per age group
     * 4. Complete event-by-event individual results list
     */
    fun exportComprehensiveMeetReportCsv(
        results: List<EventResult>,
        records: List<EventRecord> = emptyList()
    ): String {
        val sb = StringBuilder()

        // =========================================================================
        // HEADER BANNER
        // =========================================================================
        sb.append("LAERSKOOL SWARTLAND INTER-SKOLE ATLETIEKBYEENKOMS\n")
        sb.append("AMPTELIKE FINALE BYEENKOMSVERSLAG (OFFICIAL FINAL MEET REPORT)\n")
        val uniqueDisciplines = results.map { "${it.eventName} o/${it.ageGroup} ${it.gender}" }.distinct().size
        sb.append("Totaal Items Afgehandel: $uniqueDisciplines,Totaal Resultate Geregistreer: ${results.size}\n")
        sb.append("========================================================================================\n\n")

        // =========================================================================
        // SECTION 1: HIGHEST TEAM WINNERS & SCHOOL STANDINGS
        // =========================================================================
        sb.append("AFDELING 1: SPAN WENNERS EN SKOLE RANGSLYS (TEAM STANDINGS & HIGHEST WINNERS)\n")
        sb.append("Plek,Skool,Toekenning / Status,Totale ASA Punte,Goud (1ste),Silwer (2de),Brons (3de),Totaal Inskrywings,Gemiddeld per Inskrywing\n")

        val schoolStandings = results.groupBy { it.school.trim() }
            .filter { it.key.isNotBlank() }
            .map { (school, resList) ->
                val points = resList.sumOf { it.asaPoints }
                val g = resList.count { it.place == 1 }
                val s = resList.count { it.place == 2 }
                val b = resList.count { it.place == 3 }
                val total = resList.size
                val avg = if (total > 0) points / total else 0
                SchoolStandingsItem(school, points, g, s, b, total, avg)
            }.sortedByDescending { it.points }

        schoolStandings.forEachIndexed { index, item ->
            val rank = index + 1
            val award = when (rank) {
                1 -> "WENNER: KAMPIOENSKOOL WISSELTROFEE"
                2 -> "NAASWENNER (2de Plek)"
                3 -> "3de Plek Beker"
                else -> "Deelnemende Skool"
            }
            sb.append("$rank,${escapeCsv(item.school)},\"$award\",${item.points},${item.gold},${item.silver},${item.bronze},${item.total},${item.avg}\n")
        }

        sb.append("\n========================================================================================\n\n")

        // =========================================================================
        // SECTION 2: VICTOR AND VICTRIX LUDORUM
        // =========================================================================
        sb.append("AFDELING 2: VICTOR EN VICTRIX LUDORUM (TOP INDIVIDUELE ATLETE)\n")
        sb.append("Titel,Kategorie,Atleet Naam,Skool,Ouderdom,Totale ASA Punte,Beste Enkel Item,Beste Prestasie,Punte vir Beste Item\n")

        val athleteAggregates = results.groupBy {
            val key = "${it.athleteName.trim().lowercase()}|${it.school.trim().lowercase()}"
            if (it.athleteId.isNotBlank()) it.athleteId else key
        }.map { (_, list) ->
            val first = list.first()
            val totalPts = list.sumOf { it.asaPoints }
            val topPerf = list.maxByOrNull { it.asaPoints }
            AthleteAggregate(
                name = first.athleteName.trim(),
                school = first.school.trim(),
                gender = first.gender.trim(),
                ageGroup = first.ageGroup.trim(),
                totalPoints = totalPts,
                eventsCount = list.size,
                topEvent = topPerf?.eventName ?: "",
                topResult = topPerf?.resultValue ?: "",
                topPoints = topPerf?.asaPoints ?: 0,
                allEvents = list.map { "${it.eventName} (${it.place}de, ${it.resultValue}, ${it.asaPoints}pt)" }
            )
        }

        val boys = athleteAggregates.filter {
            it.gender.equals("Seuns", ignoreCase = true) ||
                    it.gender.lowercase().startsWith("s") ||
                    it.gender.lowercase().startsWith("m")
        }.sortedByDescending { it.totalPoints }

        val girls = athleteAggregates.filter {
            it.gender.equals("Dogters", ignoreCase = true) ||
                    it.gender.lowercase().startsWith("d") ||
                    it.gender.lowercase().startsWith("g") ||
                    it.gender.lowercase().startsWith("f")
        }.sortedByDescending { it.totalPoints }

        val overallVictor = boys.firstOrNull()
        val seniorVictor = boys.firstOrNull { it.ageGroup == "12" || it.ageGroup == "13" }
        val juniorVictor = boys.firstOrNull { it.ageGroup in listOf("6", "7", "8", "9", "10", "11") }

        val overallVictrix = girls.firstOrNull()
        val seniorVictrix = girls.firstOrNull { it.ageGroup == "12" || it.ageGroup == "13" }
        val juniorVictrix = girls.firstOrNull { it.ageGroup in listOf("6", "7", "8", "9", "10", "11") }

        overallVictor?.let {
            sb.append("\"VICTOR LUDORUM\",\"Algeheel Seuns\",${escapeCsv(it.name)},${escapeCsv(it.school)},o/${it.ageGroup},${it.totalPoints},${escapeCsv(it.topEvent)},${escapeCsv(it.topResult)},${it.topPoints}\n")
        }
        seniorVictor?.let {
            if (it != overallVictor) {
                sb.append("\"SENIOR VICTOR LUDORUM\",\"Senior Seuns (o/12-13)\",${escapeCsv(it.name)},${escapeCsv(it.school)},o/${it.ageGroup},${it.totalPoints},${escapeCsv(it.topEvent)},${escapeCsv(it.topResult)},${it.topPoints}\n")
            }
        }
        juniorVictor?.let {
            sb.append("\"JUNIOR VICTOR LUDORUM\",\"Junior Seuns (o/7-11)\",${escapeCsv(it.name)},${escapeCsv(it.school)},o/${it.ageGroup},${it.totalPoints},${escapeCsv(it.topEvent)},${escapeCsv(it.topResult)},${it.topPoints}\n")
        }

        overallVictrix?.let {
            sb.append("\"VICTRIX LUDORUM\",\"Algeheel Dogters\",${escapeCsv(it.name)},${escapeCsv(it.school)},o/${it.ageGroup},${it.totalPoints},${escapeCsv(it.topEvent)},${escapeCsv(it.topResult)},${it.topPoints}\n")
        }
        seniorVictrix?.let {
            if (it != overallVictrix) {
                sb.append("\"SENIOR VICTRIX LUDORUM\",\"Senior Dogters (o/12-13)\",${escapeCsv(it.name)},${escapeCsv(it.school)},o/${it.ageGroup},${it.totalPoints},${escapeCsv(it.topEvent)},${escapeCsv(it.topResult)},${it.topPoints}\n")
            }
        }
        juniorVictrix?.let {
            sb.append("\"JUNIOR VICTRIX LUDORUM\",\"Junior Dogters (o/7-11)\",${escapeCsv(it.name)},${escapeCsv(it.school)},o/${it.ageGroup},${it.totalPoints},${escapeCsv(it.topEvent)},${escapeCsv(it.topResult)},${it.topPoints}\n")
        }

        sb.append("\nTOP 10 SEUNS (VICTOR LUDORUM KANDIDATE)\n")
        sb.append("Rang,Atleet,Skool,Ouderdom,Totale Punte,Beste Item,Beste Tyd/Afstand,Items Deelgeneem\n")
        boys.take(10).forEachIndexed { index, b ->
            sb.append("${index + 1},${escapeCsv(b.name)},${escapeCsv(b.school)},o/${b.ageGroup},${b.totalPoints},${escapeCsv(b.topEvent)},${escapeCsv(b.topResult)},${b.eventsCount}\n")
        }

        sb.append("\nTOP 10 DOGTERS (VICTRIX LUDORUM KANDIDATE)\n")
        sb.append("Rang,Atleet,Skool,Ouderdom,Totale Punte,Beste Item,Beste Tyd/Afstand,Items Deelgeneem\n")
        girls.take(10).forEachIndexed { index, g ->
            sb.append("${index + 1},${escapeCsv(g.name)},${escapeCsv(g.school)},o/${g.ageGroup},${g.totalPoints},${escapeCsv(g.topEvent)},${escapeCsv(g.topResult)},${g.eventsCount}\n")
        }

        sb.append("\n========================================================================================\n\n")

        // =========================================================================
        // SECTION 3: BEST PERFORMANCES PER DISCIPLINE PER AGE GROUP
        // =========================================================================
        sb.append("AFDELING 3: BESTE PRESTASIES PER DISSIPLINE EN OUDERDOMSGROEP (BEST PERFORMANCES)\n")
        sb.append("Item,Ouderdom,Geslag,Beste Atleet (Wenner),Skool,Wenprestasie (Tyd/Afstand),ASA Punte,Bestaande Rekord,Rekord Status\n")

        val eventGroups = results.groupBy { Triple(it.eventName, it.ageGroup, it.gender) }
            .toSortedMap(compareBy({ it.first }, { it.second }, { it.third }))

        eventGroups.forEach { (triple, resList) ->
            val eventName = triple.first
            val ageGroup = triple.second
            val gender = triple.third

            val winner = resList.minByOrNull { it.place } ?: resList.maxByOrNull { it.asaPoints }
            if (winner != null) {
                val existingRecord = records.find {
                    it.eventName.equals(eventName, ignoreCase = true) &&
                            it.gender.equals(gender, ignoreCase = true) &&
                            it.ageGroup == ageGroup
                }

                val recordText = existingRecord?.let { "${it.recordValue} (${it.holder})" } ?: "Geen"
                val isRecordBroken = existingRecord != null && winner.resultValue.isNotBlank() &&
                        winner.resultValue.equals(existingRecord.recordValue, ignoreCase = true)

                val recordStatus = if (isRecordBroken) "Ewenaar Rekord" else "Standaard Wenner"

                sb.append("${escapeCsv(eventName)},o/$ageGroup,$gender,${escapeCsv(winner.athleteName)},${escapeCsv(winner.school)},${winner.resultValue},${winner.asaPoints},\"$recordText\",\"$recordStatus\"\n")
            }
        }

        sb.append("\n========================================================================================\n\n")

        // =========================================================================
        // SECTION 4: FULL INDIVIDUAL RESULTS
        // =========================================================================
        sb.append("AFDELING 4: VOLLEDIGE INDIVIDUELE BYEENKOMS UITSLASLYS\n")
        sb.append("Item,Ouderdom,Geslag,Plek,Atleet Naam,Skool,Prestasie (Tyd/Afstand),ASA Punte\n")

        val sortedResults = results.sortedWith(
            compareBy<EventResult> { it.eventName }
                .thenBy { it.ageGroup }
                .thenBy { it.gender }
                .thenBy { it.place }
        )

        sortedResults.forEach { r ->
            sb.append("${escapeCsv(r.eventName)},o/${r.ageGroup},${r.gender},${r.place},${escapeCsv(r.athleteName)},${escapeCsv(r.school)},${r.resultValue},${r.asaPoints}\n")
        }

        return sb.toString()
    }

    /**
     * Legacy shorthand export kept for backward compatibility.
     */
    fun exportBulkResultsCsv(results: List<EventResult>): String {
        return exportComprehensiveMeetReportCsv(results)
    }

    /**
     * Generates a beautifully formatted human-readable summary text report
     * ready for instant sharing to WhatsApp, Email, or printing for schools.
     */
    fun generateTextSummaryReport(results: List<EventResult>): String {
        if (results.isEmpty()) {
            return "LAERSKOOL SWARTLAND ATLETIEKBYEENKOMS\nGeen uitslae is tans geregistreer nie."
        }

        val sb = StringBuilder()
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🏆 LAERSKOOL SWARTLAND ATLETIEKBYEENKOMS\n")
        sb.append("AMPTELIKE FINALE UITSLASVERSLAG\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")

        // 1. Team Standings
        sb.append("🥇 HOOGSTE SPAN WENNERS (SKOLE RANGSLYS):\n")
        val schoolStandings = results.groupBy { it.school.trim() }
            .filter { it.key.isNotBlank() }
            .map { (school, resList) ->
                val points = resList.sumOf { it.asaPoints }
                val g = resList.count { it.place == 1 }
                val s = resList.count { it.place == 2 }
                val b = resList.count { it.place == 3 }
                val total = resList.size
                SchoolStandingsItem(school, points, g, s, b, total, 0)
            }.sortedByDescending { it.points }

        schoolStandings.forEachIndexed { idx, s ->
            val icon = when (idx) {
                0 -> "🏆 1ste"
                1 -> "🥈 2de"
                2 -> "🥉 3de"
                else -> "${idx + 1}de"
            }
            sb.append("$icon: ${s.school} — ${s.points} Punte (${s.gold} Goud, ${s.silver} Silwer, ${s.bronze} Brons)\n")
        }

        sb.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🌟 VICTOR & VICTRIX LUDORUM:\n\n")

        val athleteAggregates = results.groupBy {
            val key = "${it.athleteName.trim().lowercase()}|${it.school.trim().lowercase()}"
            if (it.athleteId.isNotBlank()) it.athleteId else key
        }.map { (_, list) ->
            val first = list.first()
            AthleteAggregate(
                name = first.athleteName.trim(),
                school = first.school.trim(),
                gender = first.gender.trim(),
                ageGroup = first.ageGroup.trim(),
                totalPoints = list.sumOf { it.asaPoints },
                eventsCount = list.size,
                topEvent = list.maxByOrNull { it.asaPoints }?.eventName ?: "",
                topResult = list.maxByOrNull { it.asaPoints }?.resultValue ?: "",
                topPoints = list.maxByOrNull { it.asaPoints }?.asaPoints ?: 0,
                allEvents = emptyList()
            )
        }

        val boys = athleteAggregates.filter {
            it.gender.equals("Seuns", ignoreCase = true) || it.gender.lowercase().startsWith("s")
        }.sortedByDescending { it.totalPoints }

        val girls = athleteAggregates.filter {
            it.gender.equals("Dogters", ignoreCase = true) || it.gender.lowercase().startsWith("d")
        }.sortedByDescending { it.totalPoints }

        val overallVictor = boys.firstOrNull()
        val juniorVictor = boys.firstOrNull { it.ageGroup in listOf("6", "7", "8", "9", "10", "11") }

        val overallVictrix = girls.firstOrNull()
        val juniorVictrix = girls.firstOrNull { it.ageGroup in listOf("6", "7", "8", "9", "10", "11") }

        overallVictor?.let {
            sb.append("🏆 VICTOR LUDORUM (Algeheel Seuns):\n")
            sb.append("   ${it.name} (${it.school}) - o/${it.ageGroup}\n")
            sb.append("   Totale Punte: ${it.totalPoints} pt | Beste: ${it.topEvent} (${it.topResult})\n\n")
        }

        overallVictrix?.let {
            sb.append("👑 VICTRIX LUDORUM (Algeheel Dogters):\n")
            sb.append("   ${it.name} (${it.school}) - o/${it.ageGroup}\n")
            sb.append("   Totale Punte: ${it.totalPoints} pt | Beste: ${it.topEvent} (${it.topResult})\n\n")
        }

        juniorVictor?.let {
            if (it != overallVictor) {
                sb.append("⭐ JUNIOR VICTOR (o/7-11): ${it.name} (${it.school}) - ${it.totalPoints} pt\n")
            }
        }

        juniorVictrix?.let {
            if (it != overallVictrix) {
                sb.append("⭐ JUNIOR VICTRIX (o/7-11): ${it.name} (${it.school}) - ${it.totalPoints} pt\n")
            }
        }

        sb.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("⚡ BESTE PRESTASIES PER DISSIPLINE & OUDERDOM:\n")

        val eventGroups = results.groupBy { Triple(it.eventName, it.ageGroup, it.gender) }
            .toSortedMap(compareBy({ it.first }, { it.second }, { it.third }))

        eventGroups.forEach { (triple, resList) ->
            val winner = resList.minByOrNull { it.place } ?: resList.maxByOrNull { it.asaPoints }
            if (winner != null) {
                sb.append("• ${triple.first} o/${triple.second} ${triple.third}: ")
                sb.append("${winner.athleteName} (${winner.school}) — ${winner.resultValue} (${winner.asaPoints} pt)\n")
            }
        }

        sb.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("Baie geluk aan alle atlete, skole en amptenare!\n")
        sb.append("Volledige uitslae beskikbaar in die amptelike Laerskool Swartland Atletiek-toepassing.")

        return sb.toString()
    }

    private data class SchoolStandingsItem(
        val school: String,
        val points: Int,
        val gold: Int,
        val silver: Int,
        val bronze: Int,
        val total: Int,
        val avg: Int
    )

    private data class AthleteAggregate(
        val name: String,
        val school: String,
        val gender: String,
        val ageGroup: String,
        val totalPoints: Int,
        val eventsCount: Int,
        val topEvent: String,
        val topResult: String,
        val topPoints: Int,
        val allEvents: List<String>
    )

    // =========================================================================
    // 5. UTILITY FUNCTIONS
    // =========================================================================

    private fun splitCsvLine(line: String): List<String> {
        val delimiter = when {
            line.contains("\t") -> '\t'
            line.contains(";") && !line.contains(",") -> ';'
            else -> ','
        }

        val result = mutableListOf<String>()
        var cur = StringBuilder()
        var inQuotes = false

        for (c in line) {
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == delimiter && !inQuotes) {
                result.add(cur.toString().trim())
                cur = StringBuilder()
            } else {
                cur.append(c)
            }
        }
        result.add(cur.toString().trim())
        return result.map { it.removeSurrounding("\"").trim() }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * Triggers the Android system share sheet with CSV/text content.
     */
    fun shareContent(context: Context, title: String, content: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }
}
