package com.example.swartlandathletics.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import com.example.swartlandathletics.ui.theme.*

data class SchoolScore(
    val school: String,
    val totalPoints: Int,
    val goldMedals: Int,
    val totalEvents: Int
)

data class AthleteScore(
    val name: String,
    val school: String,
    val gender: String,
    val ageGroup: String,
    val totalPoints: Int,
    val topResult: String
)

@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val results by AthleticsRepository.results.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var ludorumGender by remember { mutableStateOf("Alle") }

    // Aggregate School Points
    val schoolScores = remember(results) {
        results.groupBy { it.school }.map { (school, resList) ->
            SchoolScore(
                school = school,
                totalPoints = resList.sumOf { it.asaPoints },
                goldMedals = resList.count { it.place == 1 },
                totalEvents = resList.size
            )
        }.sortedByDescending { it.totalPoints }
    }

    // Aggregate Athlete Scores (Victor & Victrix Ludorum)
    val athleteScores = remember(results, ludorumGender) {
        results.filter { ludorumGender == "Alle" || it.gender.equals(ludorumGender, ignoreCase = true) }
            .groupBy { it.athleteId }
            .map { (_, list) ->
                val first = list.first()
                AthleteScore(
                    name = first.athleteName,
                    school = first.school,
                    gender = first.gender,
                    ageGroup = first.ageGroup,
                    totalPoints = list.sumOf { it.asaPoints },
                    topResult = list.minByOrNull { it.place }?.resultValue ?: ""
                )
            }.sortedByDescending { it.totalPoints }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LssNavyDark)
    ) {
        SmallTopAppBar(
            title = {
                Text(
                    text = "🖨️ Byeenkoms Verslae & Punte",
                    color = LssGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_reports")) {
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
                text = { Text("Spanpunte", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Victor / Victrix", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Opsomming", fontSize = 12.sp) }
            )
        }

        when (selectedTab) {
            0 -> {
                // School Team Points Leaderboard
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
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("🏆 Totale Skoolpunte (ASA Punte Ranglys)", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Punte word outomaties bereken vanaf alle goedgekeurde uitslae.", color = LssTextMuted, fontSize = 12.sp)
                            }
                        }
                    }

                    if (schoolScores.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Nog geen uitslae om spanpunte te bereken nie.", color = LssTextMuted)
                            }
                        }
                    } else {
                        itemsIndexed(schoolScores) { index, item ->
                            val rank = index + 1
                            val rankColor = when (rank) {
                                1 -> LssGold
                                2 -> LssTextWhite
                                3 -> LssOrangePending
                                else -> LssNavyCardLight
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(rankColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$rank",
                                            color = if (rank <= 3) LssNavyHeader else LssTextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.school,
                                            color = if (rank == 1) LssGold else LssTextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "${item.goldMedals} Goud • ${item.totalEvents} Inskrywings",
                                            color = LssTextMuted,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${item.totalPoints}",
                                            color = LssGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                        Text(
                                            text = "ASA Punte",
                                            color = LssTextLight,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Victor & Victrix Ludorum
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
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("🌟 Victor & Victrix Ludorum Kandidaten", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Top individuele atletiese prestasies volgens gekombineerde ASA punte.", color = LssTextMuted, fontSize = 12.sp)

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Alle", "Seuns (Victor)", "Dogters (Victrix)").forEach { opt ->
                                        val filterVal = if (opt.startsWith("S")) "Seuns" else if (opt.startsWith("D")) "Dogters" else "Alle"
                                        FilterChip(
                                            selected = ludorumGender == filterVal,
                                            onClick = { ludorumGender = filterVal },
                                            label = { Text(opt, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (athleteScores.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Nog geen uitslae om topsprekers te toon nie.", color = LssTextMuted)
                            }
                        }
                    } else {
                        itemsIndexed(athleteScores.take(15)) { index, ath ->
                            val rank = index + 1
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
                                    Text(
                                        text = "#$rank",
                                        color = if (rank == 1) LssGold else LssTextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        modifier = Modifier.width(36.dp)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ath.name,
                                            color = LssTextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${ath.school} • O/${ath.ageGroup} ${ath.gender}",
                                            color = LssTextMuted,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${ath.totalPoints}",
                                            color = LssGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "Punte",
                                            color = LssTextLight,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Official Meet Summary
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("📄 Amptelike Byeenkoms Opsomming", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Laerskool Swartland Inter-Skole Atletiekbyeenkoms.",
                                    color = LssTextLight,
                                    fontSize = 13.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                Divider(color = LssNavyCardLight)
                                Spacer(modifier = Modifier.height(14.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Totale Vasgelegde Uitslae:", color = LssTextMuted)
                                    Text("${results.size}", color = LssGold, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Deelnemende Skole:", color = LssTextMuted)
                                    Text("${schoolScores.size}", color = LssGold, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Voorloop Skool:", color = LssTextMuted)
                                    Text(schoolScores.firstOrNull()?.school ?: "Nog Geen", color = LssGold, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Hoogste Individuele Punte:", color = LssTextMuted)
                                    Text(athleteScores.firstOrNull()?.let { "${it.name} (${it.totalPoints} pt)" } ?: "Nog Geen", color = LssGold, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
