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
fun RecordsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val records by AthleticsRepository.records.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Filter states
    var filterGender by remember { mutableStateOf("Alle") }
    var filterAge by remember { mutableStateOf("Alle") }

    // Manual Form states
    var formEvent by remember { mutableStateOf(AppConstants.ALL_DISCIPLINES.first()) }
    var formGender by remember { mutableStateOf("Seuns") }
    var formAge by remember { mutableStateOf("13") }
    var formValue by remember { mutableStateOf("") }
    var formHolder by remember { mutableStateOf("") }
    var formYear by remember { mutableStateOf("2024") }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LssNavyDark)
    ) {
        SmallTopAppBar(
            title = {
                Text(
                    text = "🏆 Byeenkomsrekords",
                    color = LssGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_records")) {
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
                text = { Text("Huidige Rekords (${records.size})", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Voeg Rekord By", fontSize = 12.sp) }
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
                val filteredRecords = records.filter { r ->
                    (filterGender == "Alle" || r.gender.equals(filterGender, ignoreCase = true)) &&
                            (filterAge == "Alle" || r.ageGroup == filterAge)
                }

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
                                Text("Filter Rekords", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("Alle", "Seuns", "Dogters").forEach { g ->
                                        FilterChip(
                                            selected = filterGender == g,
                                            onClick = { filterGender = g },
                                            label = { Text(g, fontSize = 11.sp) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(listOf("Alle") + AppConstants.AGE_GROUPS) { a ->
                                        FilterChip(
                                            selected = filterAge == a,
                                            onClick = { filterAge = a },
                                            label = { Text(if (a == "Alle") "Alle Ouderdomme" else "O/$a", fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredRecords.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Geen rekords vir hierdie keuse gevind nie.", color = LssTextMuted)
                            }
                        }
                    } else {
                        items(filteredRecords, key = { it.id }) { rec ->
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
                                            .size(40.dp)
                                            .background(LssNavyHeader, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = LssGold)
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${rec.eventName} - O/${rec.ageGroup} ${rec.gender}",
                                                color = LssGold,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Rekord: ${rec.recordValue}",
                                            color = LssTextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Houer: ${rec.holder} (${rec.year})",
                                            color = LssTextMuted,
                                            fontSize = 12.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            AthleticsRepository.deleteRecord(rec.id)
                                            message = "Rekord vir ${rec.eventName} verwyder."
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "✍️ Voeg Nuwe Byeenkomsrekord By",
                            color = LssGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        Text("Item / Dissipline:", color = LssTextLight, fontSize = 13.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(AppConstants.ALL_DISCIPLINES) { dis ->
                                FilterChip(
                                    selected = formEvent == dis,
                                    onClick = { formEvent = dis },
                                    label = { Text(dis, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    item {
                        Text("Geslag:", color = LssTextLight, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppConstants.GENDERS.forEach { g ->
                                FilterChip(
                                    selected = formGender == g,
                                    onClick = { formGender = g },
                                    label = { Text(g) }
                                )
                            }
                        }
                    }

                    item {
                        Text("Ouderdomsgroep:", color = LssTextLight, fontSize = 13.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(AppConstants.AGE_GROUPS) { a ->
                                FilterChip(
                                    selected = formAge == a,
                                    onClick = { formAge = a },
                                    label = { Text("O/$a") }
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = formValue,
                            onValueChange = { formValue = it },
                            label = { Text("Tyd of Afstand (bv. 11.62s of 5.45m)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_record_value"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LssGold,
                                unfocusedBorderColor = LssNavyCardLight
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = formHolder,
                            onValueChange = { formHolder = it },
                            label = { Text("Skool of Atleet Naam (bv. L. van der Merwe (LSS))") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_record_holder"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LssGold,
                                unfocusedBorderColor = LssNavyCardLight
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = formYear,
                            onValueChange = { formYear = it },
                            label = { Text("Jaar") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LssGold,
                                unfocusedBorderColor = LssNavyCardLight
                            )
                        )
                    }

                    item {
                        Button(
                            onClick = {
                                if (formValue.isNotBlank() && formHolder.isNotBlank()) {
                                    val yr = formYear.toIntOrNull() ?: 2024
                                    AthleticsRepository.addRecord(
                                        eventName = formEvent,
                                        gender = formGender,
                                        ageGroup = formAge,
                                        recordValue = formValue,
                                        holder = formHolder,
                                        year = yr
                                    )
                                    message = "Rekord vir $formEvent O/$formAge $formGender suksesvol gestoor!"
                                    formValue = ""
                                    formHolder = ""
                                    selectedTab = 0
                                } else {
                                    message = "Vul asseblief al die verpligte velde in."
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_save_record"),
                            colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stoor Rekord", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
