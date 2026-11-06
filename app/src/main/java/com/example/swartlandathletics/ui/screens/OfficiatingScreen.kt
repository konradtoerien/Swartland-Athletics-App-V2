package com.example.swartlandathletics.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swartlandathletics.data.AthleticsRepository
import com.example.swartlandathletics.model.AppConstants
import com.example.swartlandathletics.model.Athlete
import com.example.swartlandathletics.model.EventResult
import com.example.swartlandathletics.ui.theme.*
import com.example.swartlandathletics.util.AppLanguage
import com.example.swartlandathletics.util.L10n
import com.example.swartlandathletics.util.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficiatingScreen(
    initialEvent: String? = null,
    initialGender: String? = null,
    initialAge: String? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language by LanguageManager.currentLanguage.collectAsState()
    val isEnglish = language == AppLanguage.ENGLISH

    val athletes by AthleticsRepository.athletes.collectAsState()
    val results by AthleticsRepository.results.collectAsState()

    var selectedAge by remember { mutableStateOf(initialAge ?: "13") }
    var selectedGender by remember { mutableStateOf(initialGender ?: "Seuns") }

    val availableEvents = remember(selectedAge, selectedGender) {
        AppConstants.getEventsForAgeAndGender(selectedAge, selectedGender)
    }
    var selectedEvent by remember { mutableStateOf(initialEvent ?: "100m") }

    LaunchedEffect(availableEvents) {
        if (!availableEvents.contains(selectedEvent) && availableEvents.isNotEmpty()) {
            selectedEvent = availableEvents.first()
        }
    }

    val record = remember(selectedEvent, selectedGender, selectedAge) {
        AthleticsRepository.getRecord(selectedEvent, selectedGender, selectedAge)
    }

    val matchingAthletes = remember(athletes, selectedAge, selectedGender, selectedEvent) {
        athletes.filter { a ->
            a.ageGroup == selectedAge &&
                    a.gender.equals(selectedGender, ignoreCase = true) &&
                    a.events.any { it.equals(selectedEvent, ignoreCase = true) }
        }
    }

    val isTrack = AppConstants.isTrackEvent(selectedEvent)
    val isHighJump = AppConstants.isHighJump(selectedEvent)

    // Track Heats State
    var currentHeat by remember { mutableIntStateOf(1) }
    val heatResultsMap = remember { mutableStateMapOf<Int, List<Pair<Athlete, String>>>() }
    val currentHeatTimes = remember { mutableStateMapOf<Int, String>() }
    val currentHeatAthletes = remember { mutableStateMapOf<Int, Athlete?>() }

    // High Jump State
    var barHeight by remember { mutableDoubleStateOf(1.10) }
    val bestClearedHeight = remember { mutableStateMapOf<String, Double>() }
    val currentHeightStatus = remember { mutableStateMapOf<String, String>() }

    // Field Distances State
    val fieldDistances = remember { mutableStateMapOf<String, String>() }

    var feedbackMsg by remember { mutableStateOf<String?>(null) }

    val publishedResults = remember(results, selectedEvent, selectedGender, selectedAge) {
        AthleticsRepository.getEventResults(selectedEvent, selectedGender, selectedAge)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LssNavyDark)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = if (isEnglish) "⏱️ Event Officiating" else "⏱️ Byeenkoms Beoordeling",
                    color = LssGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_officiating")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = L10n.back(language), tint = LssGold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = LssNavyHeader)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Select Event Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Kies Item vir Beoordeling", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Age chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(AppConstants.AGE_GROUPS) { age ->
                                FilterChip(
                                    selected = selectedAge == age,
                                    onClick = { selectedAge = age },
                                    label = { Text("O/$age") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppConstants.GENDERS.forEach { g ->
                                FilterChip(
                                    selected = selectedGender == g,
                                    onClick = { selectedGender = g },
                                    label = { Text(g) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(availableEvents) { ev ->
                                FilterChip(
                                    selected = selectedEvent == ev,
                                    onClick = { selectedEvent = ev },
                                    label = { Text(ev, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Current Record Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LssNavyHeader),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = LssGold)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Bestaande Byeenkomsrekord",
                                color = LssGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (record != null) {
                                    "${record.recordValue} - ${record.holder} (${record.year})"
                                } else {
                                    "Geen amptelike rekord aangeteken nie."
                                },
                                color = LssTextLight,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            if (feedbackMsg != null) {
                item {
                    Surface(
                        color = LssNavyCardLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = feedbackMsg ?: "",
                            color = LssGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // ==================== TRACK ITEMS (HEATS) ====================
            if (isTrack) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("🏃 Baanitem Invoer (Dinamiese Heats & Tye)", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Heat selector chips
                            Text("Kies Huidige Heat (1 tot 10):", color = LssTextMuted, fontSize = 12.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items((1..10).toList()) { h ->
                                    val isSaved = heatResultsMap.containsKey(h)
                                    FilterChip(
                                        selected = currentHeat == h,
                                        onClick = { currentHeat = h },
                                        label = { Text("Heat $h${if (isSaved) " ✓" else ""}") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Voer tye en atlete in vir Plekke 1 tot 6:", color = LssTextLight, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            (1..6).forEach { pos ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Plek $pos",
                                        color = LssGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.width(55.dp)
                                    )

                                    OutlinedTextField(
                                        value = currentHeatTimes[pos] ?: "",
                                        onValueChange = { currentHeatTimes[pos] = it },
                                        placeholder = { Text("bv. 11.85", fontSize = 12.sp) },
                                        modifier = Modifier
                                            .width(100.dp)
                                            .testTag("track_time_pos_$pos"),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = LssGold,
                                            unfocusedBorderColor = LssNavyCardLight
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Athlete dropdown / selection
                                    var expanded by remember { mutableStateOf(false) }
                                    val selectedAth = currentHeatAthletes[pos]

                                    Box(modifier = Modifier.weight(1f)) {
                                        Surface(
                                            color = LssNavyHeader,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { expanded = true }
                                                .padding(horizontal = 8.dp, vertical = 12.dp)
                                        ) {
                                            Text(
                                                text = selectedAth?.let { "${it.fullName} (${it.school.take(8)})" } ?: "Kies Atleet...",
                                                color = if (selectedAth != null) LssTextWhite else LssTextMuted,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Geen Atleet / Oop") },
                                                onClick = {
                                                    currentHeatAthletes[pos] = null
                                                    expanded = false
                                                }
                                            )
                                            matchingAthletes.forEach { ath ->
                                                DropdownMenuItem(
                                                    text = { Text("${ath.fullName} (${ath.school})") },
                                                    onClick = {
                                                        currentHeatAthletes[pos] = ath
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val entries = mutableListOf<Pair<Athlete, String>>()
                                        (1..6).forEach { pos ->
                                            val t = currentHeatTimes[pos]?.trim()
                                            val ath = currentHeatAthletes[pos]
                                            if (!t.isNullOrBlank() && ath != null) {
                                                entries.add(Pair(ath, t))
                                            }
                                        }
                                        if (entries.isNotEmpty()) {
                                            heatResultsMap[currentHeat] = entries
                                            feedbackMsg = "Heat $currentHeat suksesvol gestoor! (${entries.size} atlete)"
                                        } else {
                                            feedbackMsg = "Voer ten minste een tyd en atleet in voor stoor."
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = LssNavyCardLight, contentColor = LssGold)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stoor Heat $currentHeat", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        if (heatResultsMap.isNotEmpty()) {
                                            val allEntries = mutableListOf<EventResult>()
                                            heatResultsMap.values.flatten().forEach { (ath, timeStr) ->
                                                val clean = timeStr.replace(",", ".").replace("s", "").trim()
                                                val numVal = clean.toDoubleOrNull() ?: 999.0
                                                val pts = maxOf(10, (1000 - ((numVal - 10.0) * 35)).toInt())
                                                allEntries.add(
                                                    EventResult(
                                                        eventName = selectedEvent,
                                                        gender = selectedGender,
                                                        ageGroup = selectedAge,
                                                        place = 0,
                                                        athleteId = ath.id,
                                                        athleteName = ath.fullName,
                                                        school = ath.school,
                                                        resultValue = "${timeStr}s",
                                                        asaPoints = pts
                                                    )
                                                )
                                            }
                                            // Sort fastest to slowest
                                            val sorted = allEntries.sortedBy {
                                                it.resultValue.replace(",", ".").replace("s", "").toDoubleOrNull() ?: 999.0
                                            }
                                            AthleticsRepository.saveEventResults(selectedEvent, selectedGender, selectedAge, sorted)
                                            feedbackMsg = "Item suksesvol gefinaliseer en uitslae gepubliseer!"
                                            heatResultsMap.clear()
                                        } else {
                                            feedbackMsg = "Stoor asseblief eers ten minste een heat."
                                        }
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Finaliseer Uitslae", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else if (isHighJump) {
                // ==================== HIGH JUMP ====================
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("🦘 Hoogspring Beoordeling", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Huidige Lat Hoogte: ${String.format("%.2f", barHeight)} m",
                                color = LssGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Height Adjusters
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { barHeight += 0.02 },
                                    colors = ButtonDefaults.buttonColors(containerColor = LssNavyHeader, contentColor = LssGold),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+2cm", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { barHeight += 0.03 },
                                    colors = ButtonDefaults.buttonColors(containerColor = LssNavyHeader, contentColor = LssGold),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+3cm", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { barHeight += 0.05 },
                                    colors = ButtonDefaults.buttonColors(containerColor = LssNavyHeader, contentColor = LssGold),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+5cm", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { if (barHeight > 0.05) barHeight -= 0.05 },
                                    colors = ButtonDefaults.buttonColors(containerColor = LssNavyHeader, contentColor = LssTextMuted),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("-5cm", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Teken pogings aan vir ${String.format("%.2f", barHeight)}m:", color = LssTextLight, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            matchingAthletes.forEach { ath ->
                                val status = currentHeightStatus[ath.id] ?: "O (Slaag)"
                                val best = bestClearedHeight[ath.id]

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = LssNavyHeader),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(ath.fullName, color = LssGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(
                                                text = if (best != null) "Beste: ${String.format("%.2f", best)}m" else "Nog geen",
                                                color = LssTextMuted,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("O (Slaag)", "X (Misluk)", "P (Pass)").forEach { opt ->
                                                FilterChip(
                                                    selected = status == opt,
                                                    onClick = {
                                                        currentHeightStatus[ath.id] = opt
                                                        if (opt.startsWith("O")) {
                                                            bestClearedHeight[ath.id] = maxOf(best ?: 0.0, barHeight)
                                                        }
                                                    },
                                                    label = { Text(opt, fontSize = 11.sp) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val valid = matchingAthletes.mapNotNull { ath ->
                                        val best = bestClearedHeight[ath.id]
                                        if (best != null && best > 0.0) {
                                            EventResult(
                                                eventName = selectedEvent,
                                                gender = selectedGender,
                                                ageGroup = selectedAge,
                                                place = 0,
                                                athleteId = ath.id,
                                                athleteName = ath.fullName,
                                                school = ath.school,
                                                resultValue = "${String.format("%.2f", best)}m",
                                                asaPoints = (best * 500).toInt()
                                            )
                                        } else null
                                    }.sortedByDescending { it.resultValue.replace("m", "").toDoubleOrNull() ?: 0.0 }

                                    if (valid.isNotEmpty()) {
                                        AthleticsRepository.saveEventResults(selectedEvent, selectedGender, selectedAge, valid)
                                        feedbackMsg = "Hoogspring uitslae suksesvol gerangskik en gepubliseer!"
                                    } else {
                                        feedbackMsg = "Teken eers geldige geslaagde spronge aan."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Finaliseer Hoogspring Uitslae", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // ==================== OTHER FIELD ITEMS ====================
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("🏋️ Velditem Invoer (Afstande)", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tik die beste afstand (in meter) vir elke atleet in:", color = LssTextMuted, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(10.dp))

                            matchingAthletes.forEach { ath ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(ath.fullName, color = LssTextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(ath.school, color = LssTextMuted, fontSize = 11.sp)
                                    }

                                    OutlinedTextField(
                                        value = fieldDistances[ath.id] ?: "",
                                        onValueChange = { fieldDistances[ath.id] = it },
                                        placeholder = { Text("bv. 8.45", fontSize = 12.sp) },
                                        modifier = Modifier.width(110.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = LssGold,
                                            unfocusedBorderColor = LssNavyCardLight
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    val valid = matchingAthletes.mapNotNull { ath ->
                                        val textVal = fieldDistances[ath.id]?.replace(",", ".")?.trim()
                                        val dist = textVal?.toDoubleOrNull()
                                        if (dist != null && dist > 0.0) {
                                            EventResult(
                                                eventName = selectedEvent,
                                                gender = selectedGender,
                                                ageGroup = selectedAge,
                                                place = 0,
                                                athleteId = ath.id,
                                                athleteName = ath.fullName,
                                                school = ath.school,
                                                resultValue = "${textVal}m",
                                                asaPoints = (dist * 180).toInt()
                                            )
                                        } else null
                                    }.sortedByDescending { it.resultValue.replace("m", "").toDoubleOrNull() ?: 0.0 }

                                    if (valid.isNotEmpty()) {
                                        AthleticsRepository.saveEventResults(selectedEvent, selectedGender, selectedAge, valid)
                                        feedbackMsg = "Velditem uitslae suksesvol gerangskik en gepubliseer!"
                                    } else {
                                        feedbackMsg = "Tik ten minste een geldige afstand in."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stoor & Rangskik Velditem", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ==================== PUBLISHED RESULTS ====================
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = LssGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🥇 Gepubliseerde Uitslae vir Hierdie Item", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (publishedResults.isEmpty()) {
                            Text(
                                "Nog geen uitslae vir hierdie item gefinaliseer nie. Uitslae sal hier verskyn sodra dit gepubliseer is.",
                                color = LssTextMuted,
                                fontSize = 12.sp
                            )
                        } else {
                            publishedResults.forEach { res ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${res.place}e", color = LssGold, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                                    Text(res.athleteName, color = LssTextLight, modifier = Modifier.weight(1.5f), fontSize = 13.sp)
                                    Text(res.resultValue, color = LssTextWhite, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 13.sp)
                                    Text("${res.asaPoints} pt", color = LssTextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
