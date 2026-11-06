package com.example.swartlandathletics.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swartlandathletics.data.AthleticsRepository
import com.example.swartlandathletics.model.AppConstants
import com.example.swartlandathletics.ui.theme.*
import com.example.swartlandathletics.util.AppLanguage
import com.example.swartlandathletics.util.CsvManager
import com.example.swartlandathletics.util.L10n
import com.example.swartlandathletics.util.LanguageManager
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawSheetsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOfficiating: (event: String, gender: String, age: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by LanguageManager.currentLanguage.collectAsState()
    val isEnglish = language == AppLanguage.ENGLISH

    val athletes by AthleticsRepository.athletes.collectAsState()
    val results by AthleticsRepository.results.collectAsState()

    var selectedAge by remember { mutableStateOf("13") }
    var selectedGender by remember { mutableStateOf("Seuns") }

    val availableEvents = remember(selectedAge, selectedGender) {
        AppConstants.getEventsForAgeAndGender(selectedAge, selectedGender)
    }
    var selectedEvent by remember { mutableStateOf("100m") }

    // Randomization state (Point 4)
    var isRandomized by remember { mutableStateOf(false) }
    var randomSeed by remember { mutableIntStateOf(1) }

    // Ensure selectedEvent is valid
    LaunchedEffect(availableEvents) {
        if (!availableEvents.contains(selectedEvent) && availableEvents.isNotEmpty()) {
            selectedEvent = availableEvents.first()
        }
    }

    // Reset randomization when item selection changes
    LaunchedEffect(selectedEvent, selectedGender, selectedAge) {
        isRandomized = false
    }

    // Matching athletes for this event
    val matchingAthletes = remember(athletes, selectedAge, selectedGender, selectedEvent) {
        athletes.filter { a ->
            a.ageGroup == selectedAge &&
                    a.gender.equals(selectedGender, ignoreCase = true) &&
                    a.events.any { it.equals(selectedEvent, ignoreCase = true) }
        }
    }

    val displayedAthletes = remember(matchingAthletes, isRandomized, randomSeed) {
        if (isRandomized) {
            matchingAthletes.shuffled(Random(randomSeed))
        } else {
            matchingAthletes
        }
    }

    val eventResults = remember(results, selectedEvent, selectedGender, selectedAge) {
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
                    text = if (isEnglish) "📄 Event Draw Sheets" else "📄 Byeenkoms Lotingslyste",
                    color = LssGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_draw_sheets")) {
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
            // Filter card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (isEnglish) "Select Event Details" else "Kies Item Besonderhede",
                            color = LssGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(if (isEnglish) "Age Group:" else "Ouderdomsgroep:", color = LssTextMuted, fontSize = 12.sp)
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
                        Text(if (isEnglish) "Gender:" else "Geslag:", color = LssTextMuted, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppConstants.GENDERS.forEach { g ->
                                FilterChip(
                                    selected = selectedGender == g,
                                    onClick = { selectedGender = g },
                                    label = { Text(if (isEnglish && g == "Seuns") "Boys" else if (isEnglish && g == "Dogters") "Girls" else g) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(if (isEnglish) "Event / Discipline:" else "Item / Dissipline:", color = LssTextMuted, fontSize = 12.sp)
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

            // POINT 4: Randomization & Export Action Toolbar
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "O/$selectedAge $selectedGender - $selectedEvent",
                                    color = LssGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isRandomized)
                                        "${displayedAthletes.size} atlete • 🎲 Ewekansig Geloot (Randomized)"
                                    else
                                        "${displayedAthletes.size} atlete • Standaard Inskrywingsvolgorde",
                                    color = if (isRandomized) LssGreenSuccess else LssTextMuted,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = { onNavigateToOfficiating(selectedEvent, selectedGender, selectedAge) },
                                colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_go_to_officiating")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEnglish) "Officiate" else "Beoordeel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Controls: Randomize Lanes & Export Draw Sheet
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Randomize Button
                            Button(
                                onClick = {
                                    randomSeed = (1..999999).random()
                                    isRandomized = true
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRandomized) LssNavyHeader else LssGold,
                                    contentColor = if (isRandomized) LssGold else LssNavyHeader
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isRandomized)
                                        (if (isEnglish) "Re-Shuffle" else "Herloot")
                                    else
                                        (if (isEnglish) "🎲 Randomize Lanes" else "🎲 Ewekansige Loting"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            // Export Draw Sheet Button (with randomized lanes!)
                            OutlinedButton(
                                onClick = {
                                    val csvContent = CsvManager.generateDrawSheetCsv(
                                        eventName = selectedEvent,
                                        ageGroup = selectedAge,
                                        gender = selectedGender,
                                        athletes = displayedAthletes,
                                        isRandomized = isRandomized
                                    )
                                    CsvManager.shareContent(
                                        context = context,
                                        title = "Lotingslys_${selectedEvent}_o${selectedAge}_${selectedGender}.csv",
                                        content = csvContent
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LssGold),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isEnglish) "Export Sheet (CSV)" else "Deel Lotingslys",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Lane / Position Table
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LssNavyHeader, RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Lane/Pos" else "Baan/Pos",
                                color = LssGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.width(68.dp)
                            )
                            Text(
                                text = if (isEnglish) "Athlete Name" else "Atleet Naam",
                                color = LssGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1.5f)
                            )
                            Text(
                                text = if (isEnglish) "School" else "Skool",
                                color = LssGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1.2f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (displayedAthletes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isEnglish) "No athletes registered for this event." else "Geen atlete vir hierdie item gevind nie.",
                                    color = LssTextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            displayedAthletes.forEachIndexed { index, athlete ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${if (isEnglish) "Lane" else "Baan"} ${index + 1}",
                                        color = if (isRandomized) LssGold else LssTextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.width(68.dp)
                                    )
                                    Text(
                                        text = athlete.fullName,
                                        color = LssTextLight,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1.5f)
                                    )
                                    Text(
                                        text = athlete.school,
                                        color = LssTextMuted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }
                                if (index < displayedAthletes.size - 1) {
                                    HorizontalDivider(color = LssNavyCardLight.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }

            // Results if already completed
            if (eventResults.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = LssGreenSuccess)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isEnglish) "Finalized Results for this Event" else "Reeds Gefinaliseerde Uitslae",
                                    color = LssGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            eventResults.forEach { res ->
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
