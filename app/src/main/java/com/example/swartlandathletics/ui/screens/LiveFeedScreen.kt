package com.example.swartlandathletics.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveFeedScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by LanguageManager.currentLanguage.collectAsState()
    val isEnglish = language == AppLanguage.ENGLISH
    val results by AthleticsRepository.results.collectAsState()

    var filterSchool by remember { mutableStateOf("Alle Skole") }
    var filterAge by remember { mutableStateOf("Alle Ouderdomme") }

    // Aggregates for Victor & Victrix Ludorum (Senior o/12-13 and Junior o/7-11)
    val athleteScores = remember(results) {
        results.groupBy {
            val key = "${it.athleteName.trim().lowercase()}|${it.school.trim().lowercase()}"
            if (it.athleteId.isNotBlank()) it.athleteId else key
        }.map { (_, list) ->
            val first = list.first()
            val totalPts = list.sumOf { it.asaPoints }
            Triple(first, totalPts, list.size)
        }
    }

    val boysScores = remember(athleteScores) {
        athleteScores.filter { (first, _, _) ->
            val g = first.gender.trim().lowercase()
            g.startsWith("s") || g.startsWith("m") || g == "seuns"
        }.sortedByDescending { it.second }
    }

    val girlsScores = remember(athleteScores) {
        athleteScores.filter { (first, _, _) ->
            val g = first.gender.trim().lowercase()
            g.startsWith("d") || g.startsWith("g") || g.startsWith("f") || g == "dogters"
        }.sortedByDescending { it.second }
    }

    val seniorVictor = remember(boysScores) { boysScores.firstOrNull { it.first.ageGroup == "12" || it.first.ageGroup == "13" } }
    val seniorVictrix = remember(girlsScores) { girlsScores.firstOrNull { it.first.ageGroup == "12" || it.first.ageGroup == "13" } }
    val juniorVictor = remember(boysScores) { boysScores.firstOrNull { it.first.ageGroup in listOf("6", "7", "8", "9", "10", "11") } }
    val juniorVictrix = remember(girlsScores) { girlsScores.firstOrNull { it.first.ageGroup in listOf("6", "7", "8", "9", "10", "11") } }

    val schools = remember(results) {
        listOf("Alle Skole") + results.map { it.school }.distinct().sorted()
    }

    val filteredResults = remember(results, filterSchool, filterAge) {
        results.filter { r ->
            (filterSchool == "Alle Skole" || r.school == filterSchool) &&
                    (filterAge == "Alle Ouderdomme" || r.ageGroup == filterAge)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LssNavyDark)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = if (isEnglish) "📊 Live Results Feed" else "📊 Regstreekse Uitslae",
                    color = LssGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_live_feed")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = L10n.back(language), tint = LssGold)
                }
            },
            actions = {
                IconButton(onClick = {
                    val csv = CsvManager.exportBulkResultsCsv(filteredResults)
                    CsvManager.shareContent(
                        context = context,
                        title = "Swartland_Uitslae_Grootmaat.csv",
                        content = csv
                    )
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Deel Uitslae (CSV)", tint = LssGold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = LssNavyHeader)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live status banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(LssGreenSuccess, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Intydse Byeenkomsstroom", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text("${filteredResults.size} uitslae", color = LssTextMuted, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Filter by School
                        Text("Filter volgens Skool:", color = LssTextMuted, fontSize = 11.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(schools) { sch ->
                                FilterChip(
                                    selected = filterSchool == sch,
                                    onClick = { filterSchool = sch },
                                    label = { Text(sch, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        // Filter by Age
                        Text("Filter volgens Ouderdom:", color = LssTextMuted, fontSize = 11.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("Alle Ouderdomme") + AppConstants.AGE_GROUPS) { age ->
                                FilterChip(
                                    selected = filterAge == age,
                                    onClick = { filterAge = age },
                                    label = { Text(if (age == "Alle Ouderdomme") age else "O/$age", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Victor & Victrix Ludorum (Senior and Junior) Card
            if (seniorVictor != null || seniorVictrix != null || juniorVictor != null || juniorVictrix != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, LssGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEnglish) "🌟 Victor & Victrix Ludorum Leaders" else "🌟 Victor & Victrix Ludorum Leiers",
                                    color = LssGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isEnglish) "Senior & Junior" else "Senior & Junior",
                                    color = LssTextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Senior Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = LssNavyDark,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, LssGold.copy(alpha = 0.3f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(if (isEnglish) "🏅 Senior Victor (o/12-13)" else "🏅 Senior Victor (o/12-13)", color = LssGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(seniorVictor?.first?.athleteName ?: "-", color = LssTextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text("${seniorVictor?.first?.school ?: ""} • ${seniorVictor?.second ?: 0} pt", color = LssTextMuted, fontSize = 10.sp, maxLines = 1)
                                    }
                                }

                                Surface(
                                    color = LssNavyDark,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, LssGold.copy(alpha = 0.3f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(if (isEnglish) "🏅 Senior Victrix (o/12-13)" else "🏅 Senior Victrix (o/12-13)", color = LssGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(seniorVictrix?.first?.athleteName ?: "-", color = LssTextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text("${seniorVictrix?.first?.school ?: ""} • ${seniorVictrix?.second ?: 0} pt", color = LssTextMuted, fontSize = 10.sp, maxLines = 1)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Junior Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = LssNavyDark,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, LssGreenSuccess.copy(alpha = 0.4f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(if (isEnglish) "⭐ Junior Victor (o/7-11)" else "⭐ Junior Victor (o/7-11)", color = LssGreenSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(juniorVictor?.first?.athleteName ?: "-", color = LssTextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text("${juniorVictor?.first?.school ?: ""} • ${juniorVictor?.second ?: 0} pt", color = LssTextMuted, fontSize = 10.sp, maxLines = 1)
                                    }
                                }

                                Surface(
                                    color = LssNavyDark,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, LssGreenSuccess.copy(alpha = 0.4f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(if (isEnglish) "⭐ Junior Victrix (o/7-11)" else "⭐ Junior Victrix (o/7-11)", color = LssGreenSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(juniorVictrix?.first?.athleteName ?: "-", color = LssTextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text("${juniorVictrix?.first?.school ?: ""} • ${juniorVictrix?.second ?: 0} pt", color = LssTextMuted, fontSize = 10.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (filteredResults.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = LssTextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Geen uitslae vir die huidige filter nie.",
                                color = LssTextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredResults, key = { it.id }) { res ->
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
                            // Place badge
                            val badgeColor = when (res.place) {
                                1 -> LssGold
                                2 -> LssTextWhite
                                3 -> LssOrangePending
                                else -> LssNavyCardLight
                            }
                            val textColor = when (res.place) {
                                1 -> LssNavyHeader
                                2 -> LssNavyHeader
                                3 -> LssNavyHeader
                                else -> LssTextLight
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(badgeColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${res.place}",
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = res.athleteName,
                                    color = LssTextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${res.school} • O/${res.ageGroup} ${res.gender}",
                                    color = LssTextMuted,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = res.eventName,
                                    color = LssGold,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = res.resultValue,
                                    color = LssGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${res.asaPoints} ASA pt",
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
}
