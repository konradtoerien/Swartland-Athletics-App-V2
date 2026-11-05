package com.example.swartlandathletics.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.swartlandathletics.data.AthleticsRepository
import com.example.swartlandathletics.ui.theme.*

@Composable
fun HomeScreen(
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val athletes by AthleticsRepository.athletes.collectAsState()
    val records by AthleticsRepository.records.collectAsState()
    val results by AthleticsRepository.results.collectAsState()
    val program by AthleticsRepository.program.collectAsState()

    val completedItemsCount = program.count { it.status == "Voltooi" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LssNavyDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_header_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LssGold))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(LssNavyHeader)
                            .border(2.dp, LssGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.lss_logo),
                            contentDescription = "Laerskool Swartland Logo",
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Laerskool Swartland",
                        color = LssGold,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Atletiek Portaal & Byeenkomsbestuur",
                        color = LssTextLight,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Key metrics row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatBadge(count = athletes.size.toString(), label = "Atlete", icon = Icons.Default.People)
                        StatBadge(count = completedItemsCount.toString(), label = "Voltooi", icon = Icons.Default.CheckCircle)
                        StatBadge(count = records.size.toString(), label = "Rekords", icon = Icons.Default.EmojiEvents)
                        StatBadge(count = results.size.toString(), label = "Uitslae", icon = Icons.Default.Timer)
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "Byeenkoms Modules",
                color = LssGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Module Navigation Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ModuleCard(
                    title = "1. Setup & Inskrywings",
                    subtitle = "Bestuur atlete, laai spanne in of voeg individuele atlete by",
                    icon = Icons.Default.Assignment,
                    tag = "nav_setup_import",
                    onClick = { onNavigateTo("setup_import") }
                )
                ModuleCard(
                    title = "2. Lotingslyste (Draw Sheets)",
                    subtitle = "Baanindelings, posisies en amptelike deelnemerlyste per item",
                    icon = Icons.Default.Description,
                    tag = "nav_draw_sheets",
                    onClick = { onNavigateTo("draw_sheets") }
                )
                ModuleCard(
                    title = "3. Beoordeling (Officiating)",
                    subtitle = "Baanitems heats (1-10), hoogspring-lat en velditem afstande",
                    icon = Icons.Default.Timer,
                    tag = "nav_officiating",
                    onClick = { onNavigateTo("officiating") }
                )
                ModuleCard(
                    title = "4. Byeenkomsrekords",
                    subtitle = "Amptelike byeenkomsrekords, nuwe rekords en geskiedenis",
                    icon = Icons.Default.EmojiEvents,
                    tag = "nav_records",
                    onClick = { onNavigateTo("records") }
                )
                ModuleCard(
                    title = "5. Regstreekse Uitslae (Live Feed)",
                    subtitle = "Intydse uitslae vir toeskouers, ouers en afrigters",
                    icon = Icons.Default.Leaderboard,
                    tag = "nav_live_feed",
                    onClick = { onNavigateTo("live_feed") }
                )
                ModuleCard(
                    title = "6. Medaljes & Prysdeling",
                    subtitle = "Outomatiese Top 4 prysdelingstafel vir voltooide items",
                    icon = Icons.Default.MilitaryTech,
                    tag = "nav_medals",
                    onClick = { onNavigateTo("medals") }
                )
                ModuleCard(
                    title = "7. Verslae & Punte",
                    subtitle = "Totale skoolpunte, Victor & Victrix Ludorum ranglyste",
                    icon = Icons.Default.Assessment,
                    tag = "nav_reports",
                    onClick = { onNavigateTo("reports") }
                )
                ModuleCard(
                    title = "8. Program & Tydrooster",
                    subtitle = "Volledige item-skedule, lokale en lewendige baanstatusse",
                    icon = Icons.Default.CalendarMonth,
                    tag = "nav_program",
                    onClick = { onNavigateTo("program") }
                )
            }
        }
    }
}

@Composable
private fun StatBadge(
    count: String,
    label: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = LssGold,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count,
            color = LssTextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = label,
            color = LssTextMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LssNavyCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LssNavyHeader),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = LssGold,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = LssGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = LssTextMuted,
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Gaan",
                tint = LssGold
            )
        }
    }
}
