package com.example.swartlandathletics.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
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

data class SchoolScore(
    val school: String,
    val totalPoints: Int,
    val goldMedals: Int,
    val silverMedals: Int,
    val bronzeMedals: Int,
    val totalEvents: Int,
    val averagePoints: Int
)

data class AthleteScore(
    val name: String,
    val school: String,
    val gender: String,
    val ageGroup: String,
    val totalPoints: Int,
    val eventsCount: Int,
    val topEvent: String,
    val topResult: String,
    val topPoints: Int
)

data class BestPerformanceItem(
    val eventName: String,
    val ageGroup: String,
    val gender: String,
    val athleteName: String,
    val school: String,
    val resultValue: String,
    val asaPoints: Int,
    val existingRecord: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val language by LanguageManager.currentLanguage.collectAsState()
    val isEnglish = language == AppLanguage.ENGLISH

    val results by AthleticsRepository.results.collectAsState()
    val records by AthleticsRepository.records.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var ludorumGenderFilter by remember { mutableStateOf("Alle") }

    // Best performance filters
    var perfFilterDiscipline by remember { mutableStateOf("Alle Items") }
    var perfFilterGender by remember { mutableStateOf("Alle") }
    var perfFilterAge by remember { mutableStateOf("Alle") }

    // 1. School Standings (Highest Team Winners)
    val schoolScores = remember(results) {
        results.groupBy { it.school.trim() }
            .filter { it.key.isNotBlank() }
            .map { (school, resList) ->
                val points = resList.sumOf { it.asaPoints }
                val g = resList.count { it.place == 1 }
                val s = resList.count { it.place == 2 }
                val b = resList.count { it.place == 3 }
                val total = resList.size
                val avg = if (total > 0) points / total else 0
                SchoolScore(
                    school = school,
                    totalPoints = points,
                    goldMedals = g,
                    silverMedals = s,
                    bronzeMedals = b,
                    totalEvents = total,
                    averagePoints = avg
                )
            }.sortedByDescending { it.totalPoints }
    }

    // 2. Victor and Victrix Ludorum Aggregates
    val allAthleteScores = remember(results) {
        results.groupBy {
            val key = "${it.athleteName.trim().lowercase()}|${it.school.trim().lowercase()}"
            if (it.athleteId.isNotBlank()) it.athleteId else key
        }.map { (_, list) ->
            val first = list.first()
            val totalPts = list.sumOf { it.asaPoints }
            val topPerf = list.maxByOrNull { it.asaPoints }
            AthleteScore(
                name = first.athleteName.trim(),
                school = first.school.trim(),
                gender = first.gender.trim(),
                ageGroup = first.ageGroup.trim(),
                totalPoints = totalPts,
                eventsCount = list.size,
                topEvent = topPerf?.eventName ?: "",
                topResult = topPerf?.resultValue ?: "",
                topPoints = topPerf?.asaPoints ?: 0
            )
        }
    }

    val boysScores = remember(allAthleteScores) {
        allAthleteScores.filter {
            it.gender.equals("Seuns", ignoreCase = true) ||
                    it.gender.lowercase().startsWith("s") ||
                    it.gender.lowercase().startsWith("m")
        }.sortedByDescending { it.totalPoints }
    }

    val girlsScores = remember(allAthleteScores) {
        allAthleteScores.filter {
            it.gender.equals("Dogters", ignoreCase = true) ||
                    it.gender.lowercase().startsWith("d") ||
                    it.gender.lowercase().startsWith("g") ||
                    it.gender.lowercase().startsWith("f")
        }.sortedByDescending { it.totalPoints }
    }

    val overallVictor = boysScores.firstOrNull()
    val seniorVictor = boysScores.firstOrNull { it.ageGroup == "12" || it.ageGroup == "13" }
    val juniorVictor = boysScores.firstOrNull { it.ageGroup in listOf("6", "7", "8", "9", "10", "11") }

    val overallVictrix = girlsScores.firstOrNull()
    val seniorVictrix = girlsScores.firstOrNull { it.ageGroup == "12" || it.ageGroup == "13" }
    val juniorVictrix = girlsScores.firstOrNull { it.ageGroup in listOf("6", "7", "8", "9", "10", "11") }

    // 3. Best Performances per Discipline per Age Group
    val bestPerformances = remember(results, records) {
        results.groupBy { Triple(it.eventName, it.ageGroup, it.gender) }
            .mapNotNull { (triple, list) ->
                val winner = list.minByOrNull { it.place } ?: list.maxByOrNull { it.asaPoints }
                if (winner != null) {
                    val rec = records.find {
                        it.eventName.equals(triple.first, ignoreCase = true) &&
                                it.gender.equals(triple.third, ignoreCase = true) &&
                                it.ageGroup == triple.second
                    }
                    BestPerformanceItem(
                        eventName = triple.first,
                        ageGroup = triple.second,
                        gender = triple.third,
                        athleteName = winner.athleteName,
                        school = winner.school,
                        resultValue = winner.resultValue,
                        asaPoints = winner.asaPoints,
                        existingRecord = rec?.let { "${it.recordValue} (${it.holder})" }
                    )
                } else null
            }.sortedWith(compareBy({ it.eventName }, { it.ageGroup }, { it.gender }))
    }

    val filteredBestPerformances = remember(bestPerformances, perfFilterDiscipline, perfFilterGender, perfFilterAge) {
        bestPerformances.filter { item ->
            (perfFilterDiscipline == "Alle Items" || item.eventName == perfFilterDiscipline) &&
                    (perfFilterGender == "Alle" || item.gender.equals(perfFilterGender, ignoreCase = true)) &&
                    (perfFilterAge == "Alle" || item.ageGroup == perfFilterAge)
        }
    }

    val disciplineOptions = remember(bestPerformances) {
        listOf("Alle Items") + bestPerformances.map { it.eventName }.distinct().sorted()
    }

    // Text Summary Report
    val textSummary = remember(results) {
        CsvManager.generateTextSummaryReport(results)
    }

    BackHandler(onBack = onNavigateBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LssNavyDark)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = if (isEnglish) "🖨️ Meet Reports & Awards" else "🖨️ Byeenkoms Verslae & Pryse",
                    color = LssGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_reports")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = L10n.back(language), tint = LssGold)
                }
            },
            actions = {
                // Bulk Export Action in TopAppBar
                Button(
                    onClick = {
                        val csv = CsvManager.exportComprehensiveMeetReportCsv(results, records)
                        CsvManager.shareContent(
                            context = context,
                            title = "Swartland_Finale_Uitslae_Verslag.csv",
                            content = csv
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("btn_export_all_results_top")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isEnglish) "Share CSV" else "Deel Sigblad",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = LssNavyHeader)
        )

        // 4 Clean Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = LssNavyCard,
            contentColor = LssGold
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(if (isEnglish) "🏆 Team Winners" else "🏆 Span Wenners", fontSize = 11.sp, maxLines = 1) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(if (isEnglish) "🌟 Ludorum" else "🌟 Ludorum", fontSize = 11.sp, maxLines = 1) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(if (isEnglish) "⚡ Best Marks" else "⚡ Prestasies", fontSize = 11.sp, maxLines = 1) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text(if (isEnglish) "📄 Export Report" else "📄 Deel Verslag", fontSize = 11.sp, maxLines = 1) }
            )
        }

        when (selectedTab) {
            0 -> {
                // ==================== TAB 0: HIGHEST TEAM WINNERS & SCHOOL STANDINGS ====================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Podium Hero Banner
                    if (schoolScores.isNotEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.5.dp, LssGold, RoundedCornerShape(14.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🏆", fontSize = 28.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isEnglish) "INTER-SCHOOL CHAMPION TROPHY WINNER" else "INTER-SKOLE KAMPIOENSKOOL WENNER",
                                                color = LssGold,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = schoolScores.first().school,
                                                color = LssTextWhite,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 20.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = LssNavyCardLight)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${schoolScores.first().totalPoints}", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            Text(if (isEnglish) "Total Points" else "Totale Punte", color = LssTextMuted, fontSize = 11.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${schoolScores.first().goldMedals}", color = LssTextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            Text(if (isEnglish) "Gold Medals" else "Goud (1ste)", color = LssTextMuted, fontSize = 11.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${schoolScores.first().totalEvents}", color = LssTextLight, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            Text(if (isEnglish) "Events" else "Inskrywings", color = LssTextMuted, fontSize = 11.sp)
                                        }
                                    }

                                    // 2nd and 3rd Runners-up summary
                                    if (schoolScores.size > 1) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            color = LssNavyDark.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("🥈 2de Plek (Naaswenner):", color = LssTextLight, fontSize = 12.sp)
                                                    Text("${schoolScores[1].school} (${schoolScores[1].totalPoints} pt)", color = LssTextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                                if (schoolScores.size > 2) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("🥉 3de Plek:", color = LssOrangePending, fontSize = 12.sp)
                                                        Text("${schoolScores[2].school} (${schoolScores[2].totalPoints} pt)", color = LssTextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section Heading
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEnglish) "Official Team Standings (${schoolScores.size} Schools)" else "Amptelike Skole Ranglys (${schoolScores.size} Skole)",
                                color = LssGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = {
                                    val csv = CsvManager.exportComprehensiveMeetReportCsv(results, records)
                                    CsvManager.shareContent(context, "Swartland_Skole_Ranglys.csv", csv)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LssNavyCardLight, contentColor = LssGold),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEnglish) "Share" else "Deel", fontSize = 11.sp)
                            }
                        }
                    }

                    if (schoolScores.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(if (isEnglish) "No recorded event results yet." else "Nog geen vasgelegde uitslae nie.", color = LssTextMuted)
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
                                        .padding(14.dp),
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

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.school,
                                            color = if (rank == 1) LssGold else LssTextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "🥇 ${item.goldMedals} Goud • 🥈 ${item.silverMedals} Silwer • 🥉 ${item.bronzeMedals} Brons (${item.totalEvents} inskrywings)",
                                            color = LssTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${item.totalPoints}",
                                            color = LssGold,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp
                                        )
                                        Text(
                                            text = "ASA Punte",
                                            color = LssTextLight,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // ==================== TAB 1: VICTOR & VICTRIX LUDORUM ====================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // VICTOR LUDORUM SHOWCASE CARD
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, LssGold, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🏆", fontSize = 26.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isEnglish) "VICTOR LUDORUM (BOYS)" else "VICTOR LUDORUM (SEUNS)",
                                            color = LssGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Surface(
                                        color = LssGold.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isEnglish) "Top Boy Athlete" else "Beste Seun",
                                            color = LssGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (overallVictor != null) {
                                    Text(
                                        text = overallVictor.name,
                                        color = LssTextWhite,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp
                                    )
                                    Text(
                                        text = "${overallVictor.school} • o/${overallVictor.ageGroup} Seuns",
                                        color = LssTextLight,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("${overallVictor.totalPoints}", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                            Text(if (isEnglish) "Total ASA Points" else "Totale ASA Punte", color = LssTextMuted, fontSize = 11.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${overallVictor.topEvent}: ${overallVictor.topResult}", color = LssTextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(if (isEnglish) "Best Performance" else "Beste Prestasie", color = LssTextMuted, fontSize = 11.sp)
                                        }
                                    }

                                    if (juniorVictor != null && juniorVictor != overallVictor) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "⭐ Junior Victor (o/7-11): ${juniorVictor.name} (${juniorVictor.school}) — ${juniorVictor.totalPoints} pt",
                                            color = LssOrangePending,
                                            fontSize = 11.sp
                                        )
                                    }
                                } else {
                                    Text(if (isEnglish) "No male results recorded yet." else "Nog geen seunsuitslae aangeteken nie.", color = LssTextMuted)
                                }
                            }
                        }
                    }

                    // VICTRIX LUDORUM SHOWCASE CARD
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, LssGold, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("👑", fontSize = 26.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isEnglish) "VICTRIX LUDORUM (GIRLS)" else "VICTRIX LUDORUM (DOGTERS)",
                                            color = LssGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Surface(
                                        color = LssGold.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isEnglish) "Top Girl Athlete" else "Beste Dogter",
                                            color = LssGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (overallVictrix != null) {
                                    Text(
                                        text = overallVictrix.name,
                                        color = LssTextWhite,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp
                                    )
                                    Text(
                                        text = "${overallVictrix.school} • o/${overallVictrix.ageGroup} Dogters",
                                        color = LssTextLight,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("${overallVictrix.totalPoints}", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                            Text(if (isEnglish) "Total ASA Points" else "Totale ASA Punte", color = LssTextMuted, fontSize = 11.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${overallVictrix.topEvent}: ${overallVictrix.topResult}", color = LssTextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(if (isEnglish) "Best Performance" else "Beste Prestasie", color = LssTextMuted, fontSize = 11.sp)
                                        }
                                    }

                                    if (juniorVictrix != null && juniorVictrix != overallVictrix) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "⭐ Junior Victrix (o/7-11): ${juniorVictrix.name} (${juniorVictrix.school}) — ${juniorVictrix.totalPoints} pt",
                                            color = LssOrangePending,
                                            fontSize = 11.sp
                                        )
                                    }
                                } else {
                                    Text(if (isEnglish) "No female results recorded yet." else "Nog geen dogtersuitslae aangeteken nie.", color = LssTextMuted)
                                }
                            }
                        }
                    }

                    // Filter row: Alle, Seuns, Dogters
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEnglish) "Individual Leaderboard:" else "Individuele Ranglys:",
                                color = LssGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Alle", "Seuns", "Dogters").forEach { g ->
                                    FilterChip(
                                        selected = ludorumGenderFilter == g,
                                        onClick = { ludorumGenderFilter = g },
                                        label = { Text(g, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    // Display candidates
                    val displayedAthletes = when (ludorumGenderFilter) {
                        "Seuns" -> boysScores
                        "Dogters" -> girlsScores
                        else -> (boysScores + girlsScores).sortedByDescending { it.totalPoints }
                    }

                    itemsIndexed(displayedAthletes.take(15)) { index, ath ->
                        val rank = index + 1
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#$rank",
                                    color = if (rank == 1) LssGold else LssTextMuted,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.width(32.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ath.name,
                                        color = LssTextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${ath.school} • o/${ath.ageGroup} ${ath.gender}",
                                        color = LssTextMuted,
                                        fontSize = 11.sp
                                    )
                                    if (ath.topEvent.isNotBlank()) {
                                        Text(
                                            text = "Beste: ${ath.topEvent} (${ath.topResult} • ${ath.topPoints}pt)",
                                            color = LssTextLight,
                                            fontSize = 10.sp
                                        )
                                    }
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
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // ==================== TAB 2: BEST PERFORMANCES PER DISCIPLINE PER AGE GROUP ====================
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
                                Text(
                                    text = if (isEnglish) "⚡ Best Performances per Discipline & Age Group" else "⚡ Beste Prestasies per Dissipline & Ouderdom",
                                    color = LssGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isEnglish)
                                        "Winning athletes and top marks achieved for each event and age group at the meet."
                                    else
                                        "Wen-atlete en top tye/afstande behaal vir elke item en ouderdomsgroep tydens die byeenkoms.",
                                    color = LssTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Filters row
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(if (isEnglish) "Filter by Discipline:" else "Filtreer volgens Item:", color = LssTextLight, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(disciplineOptions) { d ->
                                        FilterChip(
                                            selected = perfFilterDiscipline == d,
                                            onClick = { perfFilterDiscipline = d },
                                            label = { Text(d, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Alle", "Seuns", "Dogters").forEach { g ->
                                        FilterChip(
                                            selected = perfFilterGender == g,
                                            onClick = { perfFilterGender = g },
                                            label = { Text(g, fontSize = 10.sp) }
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))
                                    listOf("Alle", "13", "12", "11", "10", "9").forEach { a ->
                                        FilterChip(
                                            selected = perfFilterAge == a,
                                            onClick = { perfFilterAge = a },
                                            label = { Text(if (a == "Alle") "Alle" else "o/$a", fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredBestPerformances.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(if (isEnglish) "No events match filter criteria." else "Geen items pas filter kriteria nie.", color = LssTextMuted)
                            }
                        }
                    } else {
                        items(filteredBestPerformances) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${item.eventName} • o/${item.ageGroup} ${item.gender}",
                                            color = LssGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Surface(
                                            color = LssGold.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "${item.asaPoints} ASA pt",
                                                color = LssGold,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("🥇", fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = item.athleteName,
                                                    color = LssTextWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Text(
                                                text = item.school,
                                                color = LssTextMuted,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(start = 22.dp)
                                            )
                                        }

                                        Text(
                                            text = item.resultValue,
                                            color = LssTextWhite,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp
                                        )
                                    }

                                    if (item.existingRecord != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Bestaande Rekord: ${item.existingRecord}",
                                            color = LssTextLight,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // ==================== TAB 3: CONCLUDED MEET REPORT & EXPORT TO SCHOOLS ====================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Export Actions Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Send, contentDescription = null, tint = LssGold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "Distribute Results to Schools" else "Versprei Finale Uitslae aan Skole",
                                        color = LssGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isEnglish)
                                        "Generate and share the structured post-meet report with participating schools, containing Team Standings, Victor/Victrix Ludorum, Best performances per age/discipline, and complete individual results."
                                    else
                                        "Genereer en deel die gestruktureerde na-byeenkomsverslag met alle skole: Skole Ranglys, Victor/Victrix Ludorum, Beste prestasies per item/ouderdom, en volledige uitslae.",
                                    color = LssTextMuted,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                // Button 1: Share Full CSV / Excel
                                Button(
                                    onClick = {
                                        val csv = CsvManager.exportComprehensiveMeetReportCsv(results, records)
                                        CsvManager.shareContent(
                                            context = context,
                                            title = "Swartland_Finale_Uitslae_Amptelik.csv",
                                            content = csv
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_share_full_csv_report"),
                                    colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "Share Full CSV Spreadsheet (Excel)" else "Deel Volledige Sigblad (CSV / Excel)",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Button 2: Share WhatsApp / Email text summary
                                Button(
                                    onClick = {
                                        CsvManager.shareContent(
                                            context = context,
                                            title = "Swartland_Atletiek_Uitslasopsomming.txt",
                                            content = textSummary
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_share_text_report"),
                                    colors = ButtonDefaults.buttonColors(containerColor = LssGreenSuccess, contentColor = LssNavyHeader)
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "Share Summary (WhatsApp / Email)" else "Deel Opsomming (WhatsApp / E-pos)",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Button 3: Copy text to clipboard
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(textSummary))
                                        Toast.makeText(context, if (isEnglish) "Report copied to clipboard!" else "Verslag gekopieer na knipbord!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("btn_copy_report_clipboard"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LssGold)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isEnglish) "Copy Report to Clipboard" else "Kopieer Verslag na Knipbord", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Live Preview of the Text Report
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (isEnglish) "Live Report Preview:" else "Regstreekse Verslag Voorskou:",
                                    color = LssGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = LssNavyDark,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = textSummary,
                                        color = LssTextLight,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
