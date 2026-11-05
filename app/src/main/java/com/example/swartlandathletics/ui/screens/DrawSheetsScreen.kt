package com.example.swartlandathletics.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.swartlandathletics.ui.theme.*

@Composable
fun DrawSheetsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOfficiating: (event: String, gender: String, age: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val athletes by AthleticsRepository.athletes.collectAsState()
    val results by AthleticsRepository.results.collectAsState()

    var selectedAge by remember { mutableStateOf("13") }
    var selectedGender by remember { mutableStateOf("Seuns") }

    val availableEvents = remember(selectedAge, selectedGender) {
        AppConstants.getEventsForAgeAndGender(selectedAge, selectedGender)
    }
    var selectedEvent by remember { mutableStateOf("100m") }

    // Ensure selectedEvent is valid
    LaunchedEffect(availableEvents) {
        if (!availableEvents.contains(selectedEvent) && availableEvents.isNotEmpty()) {
            selectedEvent = availableEvents.first()
        }
    }

    // Matching athletes for this event
    val matchingAthletes = remember(athletes, selectedAge, selectedGender, selectedEvent) {
        athletes.filter { a ->
            a.ageGroup == selectedAge &&
                    a.gender.equals(selectedGender, ignoreCase = true) &&
                    a.events.any { it.equals(selectedEvent, ignoreCase = true) }
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
        SmallTopAppBar(
            title = {
                Text(
                    text = "📄 Lotingslyste (Draw Sheets)",
                    color = LssGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_draw_sheets")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Terug", tint = LssGold)
                }
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = LssNavyHeader)
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
                        Text("Kies Item Besonderhede", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Ouderdomsgroep:", color = LssTextMuted, fontSize = 12.sp)
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
                        Text("Geslag:", color = LssTextMuted, fontSize = 12.sp)
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
                        Text("Item / Dissipline:", color = LssTextMuted, fontSize = 12.sp)
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

            // Action banner
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "O/$selectedAge $selectedGender - $selectedEvent",
                            color = LssGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${matchingAthletes.size} atlete geregistreer",
                            color = LssTextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = { onNavigateToOfficiating(selectedEvent, selectedGender, selectedAge) },
                        colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_go_to_officiating")
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Beoordeel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                            Text("Baan/Pos", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(64.dp))
                            Text("Atleet Naam", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                            Text("Skool", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.2f))
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (matchingAthletes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Geen atlete vir hierdie item gevind nie.", color = LssTextMuted, fontSize = 13.sp)
                            }
                        } else {
                            matchingAthletes.forEachIndexed { index, athlete ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Baan ${index + 1}",
                                        color = LssTextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.width(64.dp)
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
                                if (index < matchingAthletes.size - 1) {
                                    Divider(color = LssNavyCardLight.copy(alpha = 0.5f))
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
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LssGreenSuccess)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reeds Gefinaliseerde Uitslae", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
