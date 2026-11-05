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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swartlandathletics.data.AthleticsRepository
import com.example.swartlandathletics.model.AppConstants
import com.example.swartlandathletics.ui.theme.*

@Composable
fun ProgramScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val program by AthleticsRepository.program.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    var filterVenue by remember { mutableStateOf("Alle Plekke") }
    var filterStatus by remember { mutableStateOf("Alle Statusse") }

    // Add form states
    var newItemNo by remember { mutableStateOf((program.maxOfOrNull { it.itemNo } ?: 0) + 1) }
    var newStartTime by remember { mutableStateOf("11:30") }
    var newAgeGroup by remember { mutableStateOf("13") }
    var newGender by remember { mutableStateOf("Seuns") }
    var newEventName by remember { mutableStateOf("100m") }
    var newVenue by remember { mutableStateOf("Hoofbaan") }
    var message by remember { mutableStateOf<String?>(null) }

    val venues = remember(program) {
        listOf("Alle Plekke") + (AppConstants.VENUES + program.map { it.venue }).distinct()
    }

    val filteredProgram = remember(program, filterVenue, filterStatus) {
        program.filter { item ->
            (filterVenue == "Alle Plekke" || item.venue == filterVenue) &&
                    (filterStatus == "Alle Statusse" || item.status == filterStatus)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LssNavyDark)
    ) {
        SmallTopAppBar(
            title = {
                Text(
                    text = "📅 Byeenkoms Program & Tydrooster",
                    color = LssGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_program")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Terug", tint = LssGold)
                }
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = LssNavyHeader)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = LssNavyCard,
            contentColor = LssGold
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Tydrooster (${program.size})", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Voeg Item By", fontSize = 12.sp) }
            )
        }

        if (message != null) {
            Surface(
                color = LssNavyCardLight,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = message ?: "",
                    color = LssGold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        when (selectedTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Filter Tydrooster", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))

                                // Status chips
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("Alle Statusse", "Wagtend", "Besig", "Voltooi").forEach { st ->
                                        FilterChip(
                                            selected = filterStatus == st,
                                            onClick = { filterStatus = st },
                                            label = { Text(st, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                // Venue chips
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(venues) { v ->
                                        FilterChip(
                                            selected = filterVenue == v,
                                            onClick = { filterVenue = v },
                                            label = { Text(v, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredProgram.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Geen program-items vir hierdie keuse nie.", color = LssTextMuted)
                            }
                        }
                    } else {
                        items(filteredProgram, key = { it.id }) { item ->
                            val statusColor = when (item.status) {
                                "Voltooi" -> LssGreenSuccess
                                "Besig" -> LssOrangePending
                                else -> LssTextMuted
                            }

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
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(LssNavyHeader, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "#${item.itemNo}",
                                                color = LssGold,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = item.startTime,
                                                color = LssTextLight,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${item.eventName} (O/${item.ageGroup} ${item.gender})",
                                            color = LssTextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Plek: ${item.venue}",
                                            color = LssTextMuted,
                                            fontSize = 12.sp
                                        )
                                    }

                                    // Status Button (click to cycle status!)
                                    Button(
                                        onClick = {
                                            val nextStatus = when (item.status) {
                                                "Wagtend" -> "Besig"
                                                "Besig" -> "Voltooi"
                                                else -> "Wagtend"
                                            }
                                            AthleticsRepository.updateProgramStatus(item.id, nextStatus)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = statusColor.copy(alpha = 0.2f),
                                            contentColor = statusColor
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(item.status, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Voeg Nuwe Program Item By", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    item {
                        OutlinedTextField(
                            value = newItemNo.toString(),
                            onValueChange = { newItemNo = it.toIntOrNull() ?: 1 },
                            label = { Text("Item Nommer") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LssGold,
                                unfocusedBorderColor = LssNavyCardLight
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = newStartTime,
                            onValueChange = { newStartTime = it },
                            label = { Text("Begintyd (bv. 11:30)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LssGold,
                                unfocusedBorderColor = LssNavyCardLight
                            )
                        )
                    }

                    item {
                        Text("Ouderdomsgroep:", color = LssTextLight, fontSize = 13.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(AppConstants.AGE_GROUPS) { a ->
                                FilterChip(
                                    selected = newAgeGroup == a,
                                    onClick = { newAgeGroup = a },
                                    label = { Text("O/$a") }
                                )
                            }
                        }
                    }

                    item {
                        Text("Geslag:", color = LssTextLight, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppConstants.GENDERS.forEach { g ->
                                FilterChip(
                                    selected = newGender == g,
                                    onClick = { newGender = g },
                                    label = { Text(g) }
                                )
                            }
                        }
                    }

                    item {
                        Text("Item / Dissipline:", color = LssTextLight, fontSize = 13.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(AppConstants.ALL_DISCIPLINES) { d ->
                                FilterChip(
                                    selected = newEventName == d,
                                    onClick = { newEventName = d },
                                    label = { Text(d, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    item {
                        Text("Baan / Lokaal:", color = LssTextLight, fontSize = 13.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(AppConstants.VENUES) { v ->
                                FilterChip(
                                    selected = newVenue == v,
                                    onClick = { newVenue = v },
                                    label = { Text(v, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                AthleticsRepository.addProgramItem(
                                    itemNo = newItemNo,
                                    startTime = newStartTime,
                                    ageGroup = newAgeGroup,
                                    gender = newGender,
                                    eventName = newEventName,
                                    venue = newVenue
                                )
                                message = "Item #$newItemNo ($newEventName) bygevoeg!"
                                newItemNo += 1
                                selectedTab = 0
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_save_program_item"),
                            colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stoor Program Item", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
