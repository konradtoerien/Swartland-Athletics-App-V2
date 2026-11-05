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
import com.example.swartlandathletics.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupImportScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val athletes by AthleticsRepository.athletes.collectAsState()

    var filterSchool by remember { mutableStateOf("Alle Skole") }
    var filterGender by remember { mutableStateOf("Alle Geslagte") }
    var filterAge by remember { mutableStateOf("Alle Ouderdomme") }

    // Form state
    var inputFirstName by remember { mutableStateOf("") }
    var inputLastName by remember { mutableStateOf("") }
    var inputSchool by remember { mutableStateOf("Laerskool Swartland") }
    var inputGender by remember { mutableStateOf("Seuns") }
    var inputAge by remember { mutableStateOf("13") }
    var selectedEvents by remember { mutableStateOf(setOf<String>()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val schools = listOf("Laerskool Swartland", "Laerskool Vredenburg", "Laerskool Malmesbury", "Weskus Skool", "Dirkie Uys")
    val availableEventsForForm = remember(inputAge, inputGender) {
        AppConstants.getEventsForAgeAndGender(inputAge, inputGender)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LssNavyDark)
    ) {
        // Top App Bar
        SmallTopAppBar(
            title = {
                Text(
                    text = "📋 Setup & Inskrywings",
                    color = LssGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_setup")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Terug", tint = LssGold)
                }
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = LssNavyHeader)
        )

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = LssNavyCard,
            contentColor = LssGold
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Atlete (${athletes.size})", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Handmatige Inskrywing", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Data Bestuur", fontSize = 12.sp) }
            )
        }

        if (statusMessage != null) {
            Surface(
                color = LssNavyCardLight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = LssGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = statusMessage ?: "", color = LssTextLight, fontSize = 13.sp)
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // Athletes list with filters
                val filteredAthletes = athletes.filter { a ->
                    (filterSchool == "Alle Skole" || a.school == filterSchool) &&
                            (filterGender == "Alle Geslagte" || a.gender == filterGender) &&
                            (filterAge == "Alle Ouderdomme" || a.ageGroup == filterAge)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        // Filters row
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Filter Atlete", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                // School chips
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(listOf("Alle Skole") + schools) { sch ->
                                        FilterChip(
                                            selected = filterSchool == sch,
                                            onClick = { filterSchool = sch },
                                            label = { Text(sch, fontSize = 11.sp) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                // Gender & Age chips
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("Alle Geslagte", "Seuns", "Dogters").forEach { g ->
                                        FilterChip(
                                            selected = filterGender == g,
                                            onClick = { filterGender = g },
                                            label = { Text(g, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredAthletes.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Geen atlete gevind vir die huidige filter nie.", color = LssTextMuted)
                            }
                        }
                    } else {
                        items(filteredAthletes, key = { it.id }) { athlete ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = athlete.fullName,
                                            color = LssGold,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${athlete.school} • O/${athlete.ageGroup} ${athlete.gender}",
                                            color = LssTextLight,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            athlete.events.forEach { ev ->
                                                Surface(
                                                    color = LssNavyHeader,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = ev,
                                                        color = LssTextWhite,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            AthleticsRepository.deleteAthlete(athlete.id)
                                            statusMessage = "${athlete.fullName} verwyder."
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Skrap", tint = LssRedError)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Manual Entry Form
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "✍️ Voeg Nuwe Atleet By",
                            color = LssGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = inputFirstName,
                            onValueChange = { inputFirstName = it },
                            label = { Text("Voornaam") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_first_name"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LssGold,
                                unfocusedBorderColor = LssNavyCardLight,
                                focusedLabelColor = LssGold
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = inputLastName,
                            onValueChange = { inputLastName = it },
                            label = { Text("Van") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_last_name"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LssGold,
                                unfocusedBorderColor = LssNavyCardLight,
                                focusedLabelColor = LssGold
                            )
                        )
                    }

                    item {
                        Text("Kies Skool:", color = LssTextLight, fontSize = 13.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(schools) { sch ->
                                FilterChip(
                                    selected = inputSchool == sch,
                                    onClick = { inputSchool = sch },
                                    label = { Text(sch, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    item {
                        Text("Geslag:", color = LssTextLight, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Seuns", "Dogters").forEach { g ->
                                FilterChip(
                                    selected = inputGender == g,
                                    onClick = {
                                        inputGender = g
                                        selectedEvents = emptySet()
                                    },
                                    label = { Text(g) }
                                )
                            }
                        }
                    }

                    item {
                        Text("Ouderdomsgroep:", color = LssTextLight, fontSize = 13.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(AppConstants.AGE_GROUPS) { age ->
                                FilterChip(
                                    selected = inputAge == age,
                                    onClick = {
                                        inputAge = age
                                        selectedEvents = emptySet()
                                    },
                                    label = { Text("O/$age") }
                                )
                            }
                        }
                    }

                    item {
                        Text("Kies Deelnemende Items:", color = LssGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableEventsForForm.forEach { ev ->
                                val isSelected = selectedEvents.contains(ev)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedEvents = if (isSelected) {
                                            selectedEvents - ev
                                        } else {
                                            selectedEvents + ev
                                        }
                                    },
                                    label = { Text(ev, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                if (inputFirstName.isNotBlank() && inputLastName.isNotBlank()) {
                                    AthleticsRepository.addAthlete(
                                        firstName = inputFirstName,
                                        lastName = inputLastName,
                                        school = inputSchool,
                                        gender = inputGender,
                                        ageGroup = inputAge,
                                        events = selectedEvents.toList().ifEmpty { listOf("100m") }
                                    )
                                    statusMessage = "Atleet ${inputFirstName} ${inputLastName} suksesvol geregistreer!"
                                    inputFirstName = ""
                                    inputLastName = ""
                                    selectedEvents = emptySet()
                                    selectedTab = 0
                                } else {
                                    statusMessage = "Vul asseblief beide voornaam en van in."
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_save_athlete"),
                            colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stoor Atleet", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            2 -> {
                // Data Management
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Herlaai Voorafgelaaide Byeenkomsdata", color = LssGold, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Herlaai die standaard Laerskool Swartland atletiekspanne, amptelike rekords, uitslae en program.",
                                color = LssTextMuted,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    AthleticsRepository.seedInitialData()
                                    statusMessage = "Byeenkomsdata suksesvol herlaai!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Herlaai Data", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Vee Alle Byeenkomsdata Uit (Reset)", color = LssRedError, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Wis alle atlete, rekords, uitslae en tydroosteritems uit vir 'n skoon nuwe byeenkoms.",
                                color = LssTextMuted,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    AthleticsRepository.resetAll()
                                    statusMessage = "Alle byeenkomsdata is skoongemaak."
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LssRedError, contentColor = LssTextWhite)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Vee Alles Uit", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
