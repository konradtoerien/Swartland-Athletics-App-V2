package com.example.swartlandathletics.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.swartlandathletics.ui.theme.*

@Composable
fun MedalsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val results by AthleticsRepository.results.collectAsState()
    val completedCeremonies by AthleticsRepository.completedCeremonies.collectAsState()

    // Group by (eventName, gender, ageGroup)
    val grouped = remember(results) {
        results.groupBy { Triple(it.eventName, it.gender, it.ageGroup) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LssNavyDark)
    ) {
        SmallTopAppBar(
            title = {
                Text(
                    text = "🥇 Medaljetafel & Prysdeling",
                    color = LssGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_medals")) {
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
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = LssGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Amptelike Prysdelingstafel", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Hierdie tafel vertoon outomaties die Top 4 atlete vir alle afgehandelde items vir vinnige en foutlose medalje-oorhandiging.",
                            color = LssTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (grouped.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = LssTextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Nog geen items gefinaliseer vir medalje-oorhandiging nie.",
                                color = LssTextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(grouped.entries.toList(), key = { "${it.key.first}_${it.key.second}_${it.key.third}" }) { entry ->
                    val (eventName, gender, ageGroup) = entry.key
                    val groupResults = entry.value.sortedBy { it.place }.take(4)
                    val ceremonyKey = "${eventName}_${gender}_${ageGroup}"
                    val isDone = completedCeremonies.contains(ceremonyKey)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = if (isDone) LssGreenSuccess else LssGold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "O/$ageGroup $gender - $eventName",
                                        color = LssGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                if (isDone) {
                                    Surface(
                                        color = LssGreenSuccess.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Oorhandig ✓",
                                            color = LssGreenSuccess,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Podiums 1..4
                            val medalIcons = listOf("🥇 1ste Plek (Goud)", "🥈 2de Plek (Silwer)", "🥉 3de Plek (Brons)", "4️⃣ 4de Plek")
                            val medalColors = listOf(LssGold, LssTextWhite, LssOrangePending, LssTextMuted)

                            groupResults.forEachIndexed { idx, athRes ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = LssNavyHeader),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = medalIcons.getOrElse(idx) { "${idx + 1}e Plek" },
                                                color = medalColors.getOrElse(idx) { LssTextMuted },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = athRes.athleteName,
                                                color = LssTextWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Skool: ${athRes.school}",
                                                color = LssTextMuted,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = athRes.resultValue,
                                                color = LssGold,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "${athRes.asaPoints} pt",
                                                color = LssTextLight,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (!isDone) {
                                Button(
                                    onClick = { AthleticsRepository.markCeremonyCompleted(ceremonyKey) },
                                    colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Merk Prysdeling as Voltooi", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
