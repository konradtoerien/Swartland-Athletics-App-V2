package com.example.swartlandathletics.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.swartlandathletics.data.AthleticsRepository
import com.example.swartlandathletics.model.EventResult
import com.example.swartlandathletics.model.ProgramItem
import com.example.swartlandathletics.ui.theme.*
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.example.swartlandathletics.auth.RoleManager
import com.example.swartlandathletics.auth.UserRole
import com.example.swartlandathletics.util.WebDashboardConfig
import com.example.swartlandathletics.util.AppLanguage
import com.example.swartlandathletics.util.L10n
import com.example.swartlandathletics.util.LanguageManager

private data class DashboardSchoolScore(
    val school: String,
    val totalPoints: Int,
    val goldCount: Int,
    val silverCount: Int,
    val bronzeCount: Int
)

@Composable
fun EventDashboardScreen(
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val language by LanguageManager.currentLanguage.collectAsState()
    val role by RoleManager.currentRole.collectAsState()

    var showWebConfigDialog by remember { mutableStateOf(false) }
    var currentWebUrlInput by remember { mutableStateOf(WebDashboardConfig.getWebUrl(context)) }

    // 1. Live Firestore streams via StateFlow
    val athletes by AthleticsRepository.athletes.collectAsState()
    val records by AthleticsRepository.records.collectAsState()
    val results by AthleticsRepository.results.collectAsState()
    val program by AthleticsRepository.program.collectAsState()

    // 2. Computed Live Meet Metrics
    val completedItems = remember(program) { program.count { it.status == "Voltooi" } }
    val inProgressItems = remember(program) { program.filter { it.status == "Besig" } }
    val upcomingItems = remember(program) { program.filter { it.status == "Wagtend" }.take(3) }
    val progressFraction = remember(program, completedItems) {
        if (program.isNotEmpty()) completedItems.toFloat() / program.size.toFloat() else 0f
    }
    val progressPercent = (progressFraction * 100).toInt()

    val totalPointsScored = remember(results) { results.sumOf { it.asaPoints } }
    val medalsAwarded = remember(results) { results.count { it.place in 1..3 } }
    val uniqueSchools = remember(athletes) { athletes.map { it.school.trim() }.filter { it.isNotBlank() }.distinct() }

    // Aggregate Live School Standings
    val schoolStandings = remember(results) {
        results.groupBy { it.school.trim() }
            .filter { it.key.isNotBlank() }
            .map { (school, resList) ->
                DashboardSchoolScore(
                    school = school,
                    totalPoints = resList.sumOf { it.asaPoints },
                    goldCount = resList.count { it.place == 1 },
                    silverCount = resList.count { it.place == 2 },
                    bronzeCount = resList.count { it.place == 3 }
                )
            }.sortedByDescending { it.totalPoints }
    }

    val maxSchoolPoints = remember(schoolStandings) {
        (schoolStandings.maxOfOrNull { it.totalPoints } ?: 1).coerceAtLeast(1)
    }

    // Recent results (latest recorded in Firestore)
    val recentResults = remember(results) {
        results.takeLast(6).reversed()
    }

    // Pulsing animation for live status indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Responsive container measurement
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LssNavyDark)
    ) {
        val screenWidth = maxWidth
        val isCompact = screenWidth < 600.dp
        val isMedium = screenWidth in 600.dp..840.dp
        val isExpanded = screenWidth > 840.dp

        // Dynamic typography responsive to screen width
        val headerTitleSize = if (isCompact) 18.sp else if (isMedium) 21.sp else 23.sp
        val sectionTitleSize = if (isCompact) 15.sp else 17.sp
        val bodyTextSize = if (isCompact) 12.sp else 13.sp
        val captionSize = if (isCompact) 11.sp else 12.sp
        val metricValueSize = if (isCompact) 18.sp else 22.sp
        val horizontalPadding = if (isCompact) 14.dp else 24.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            contentPadding = PaddingValues(top = 16.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==========================================
            // 1. LIVE HERO MEET BANNER & LANGUAGE SWITCH
            // ==========================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_dashboard_hero"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LssGold))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(if (isCompact) 14.dp else 18.dp)
                    ) {
                        // Top row: School Logo, Title & Bilingual Language Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isCompact) 44.dp else 52.dp)
                                        .clip(CircleShape)
                                        .background(LssNavyHeader)
                                        .border(2.dp, LssGold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.lss_logo),
                                        contentDescription = "Laerskool Swartland Logo",
                                        modifier = Modifier.size(if (isCompact) 32.dp else 38.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Laerskool Swartland",
                                        color = LssGold,
                                        fontSize = headerTitleSize,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = L10n.meetDashboardTitle(language),
                                        color = LssTextLight,
                                        fontSize = bodyTextSize,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // AFRIKAANS / ENGLISH TOGGLE BUTTON ON HOMESCREEN
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = LssNavyDark,
                                border = BorderStroke(1.dp, LssGold.copy(alpha = 0.7f)),
                                modifier = Modifier.testTag("btn_language_segmented")
                            ) {
                                Row(
                                    modifier = Modifier.padding(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (language == AppLanguage.AFRIKAANS) LssGold else Color.Transparent)
                                            .clickable { LanguageManager.setLanguage(AppLanguage.AFRIKAANS) }
                                            .padding(horizontal = 7.dp, vertical = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "🇿🇦 AFR",
                                            color = if (language == AppLanguage.AFRIKAANS) LssNavyHeader else LssTextMuted,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (language == AppLanguage.ENGLISH) LssGold else Color.Transparent)
                                            .clickable { LanguageManager.setLanguage(AppLanguage.ENGLISH) }
                                            .padding(horizontal = 7.dp, vertical = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "🇬🇧 ENG",
                                            color = if (language == AppLanguage.ENGLISH) LssNavyHeader else LssTextMuted,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Live Progress Section with status indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(LssGreenSuccess.copy(alpha = pulseAlpha))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = L10n.meetProgress(language),
                                    color = LssTextMuted,
                                    fontSize = captionSize
                                )
                            }
                            Text(
                                text = L10n.itemsCompleted(completedItems, program.size, progressPercent, language),
                                color = LssGold,
                                fontSize = captionSize,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = LssGold,
                            trackColor = LssNavyDark,
                        )
                    }
                }
            }

            // ==========================================
            // 1b. WHATSAPP & WEB LIVE DASHBOARD LINK CARD
            // ==========================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("whatsapp_web_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                    border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(if (isCompact) 12.dp else 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF25D366), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = L10n.shareWhatsAppLink(language),
                                    color = Color(0xFF25D366),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = sectionTitleSize
                                )
                                Text(
                                    text = if (language == AppLanguage.ENGLISH)
                                        "Public View-Only Live Web Link (iPhones, Androids, PC)"
                                    else
                                        "Publieke Slegs-Kyk Skakel (iPhones, Androids, PC)",
                                    color = LssTextMuted,
                                    fontSize = captionSize
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = L10n.webDashboardDesc(language),
                            color = LssTextLight,
                            fontSize = bodyTextSize
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { WebDashboardConfig.shareToWhatsApp(context, language) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("btn_share_whatsapp_dashboard")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (language == AppLanguage.ENGLISH) "Share on WhatsApp" else "Deel op WhatsApp",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = captionSize
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    val currentUrl = WebDashboardConfig.getWebUrl(context)
                                    clipboardManager.setText(AnnotatedString(currentUrl))
                                    Toast.makeText(
                                        context,
                                        if (language == AppLanguage.ENGLISH) "Link copied to clipboard!" else "Skakel gekopieer na knipbord!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                border = BorderStroke(1.dp, LssGold),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LssGold),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (language == AppLanguage.ENGLISH) "Copy Link" else "Kopieer Skakel", fontSize = captionSize)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    currentWebUrlInput = WebDashboardConfig.getWebUrl(context)
                                    showWebConfigDialog = true
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.ENGLISH) "⚙️ Fix 404 / Web Link Settings →" else "⚙️ Hoekom 404? / Skakel Instellings →",
                                    color = LssGold,
                                    fontSize = captionSize,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            TextButton(
                                onClick = { WebDashboardConfig.openInBrowser(context) },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.ENGLISH) "Open in Browser ↗" else "Maak oop in Webblaaier ↗",
                                    color = LssTextMuted,
                                    fontSize = captionSize
                                )
                            }
                        }
                    }
                }
            }

            // 1c. DIGITAL ENTRY SHEET QUICK ACTION CARD (NO CSV NEEDED)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateTo("setup_import") }
                        .testTag("btn_dashboard_digital_sheet"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                    border = BorderStroke(1.5.dp, LssGold)
                ) {
                    Row(
                        modifier = Modifier.padding(if (isCompact) 12.dp else 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(LssGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = LssNavyHeader,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.ENGLISH)
                                    "✍️ Enter Athletes (Interactive Digital Sheet)"
                                else
                                    "✍️ Skryf Atlete In (Digitale Sigblad)",
                                color = LssGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = sectionTitleSize
                            )
                            Text(
                                text = if (language == AppLanguage.ENGLISH)
                                    "No CSV needed! Enter athletes and tick checkboxes under each event."
                                else
                                    "Geen CSV nodig nie! Tik atlete in en merk blokkies onder elke item.",
                                color = LssTextLight,
                                fontSize = captionSize
                            )
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = LssGold)
                    }
                }
            }

            if (role == UserRole.SPECTATOR) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LssGold.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, LssGold.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = LssGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = L10n.spectatorNotice(language),
                                color = LssGold,
                                fontSize = captionSize,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. RESPONSIVE DYNAMIC METRIC KPI CARDS
            // ==========================================
            item {
                Text(
                    text = L10n.meetStatsTitle(language),
                    color = LssGold,
                    fontSize = sectionTitleSize,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                val kpiColumns = if (isExpanded) 4 else if (isMedium) 3 else 2
                val statsList = listOf(
                    Triple(athletes.size.toString(), L10n.athletesEntered(language), Icons.Default.Person),
                    Triple("$completedItems/${program.size}", L10n.itemsDone(language), Icons.Default.Check),
                    Triple(records.size.toString(), L10n.establishedRecords(language), Icons.Default.Star),
                    Triple(results.size.toString(), L10n.resultsRecorded(language), Icons.Default.PlayArrow),
                    Triple(medalsAwarded.toString(), L10n.medalsAwarded(language), Icons.Default.Star),
                    Triple(uniqueSchools.size.toString(), L10n.participatingSchools(language), Icons.Default.Info)
                )

                val chunked = statsList.chunked(kpiColumns)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunked.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { (value, label, icon) ->
                                ResponsiveMetricCard(
                                    value = value,
                                    label = label,
                                    icon = icon,
                                    valueSize = metricValueSize,
                                    labelSize = captionSize,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size < kpiColumns) {
                                repeat(kpiColumns - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. TANS LEWENDIG OP BAAN & VELD (ACTIVE NOW)
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(LssGreenSuccess.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (inProgressItems.isNotEmpty()) L10n.nowOnTrackField(language) else L10n.nextOnProgram(language),
                            color = LssGold,
                            fontSize = sectionTitleSize,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = { onNavigateTo("program") },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(L10n.viewProgram(language), color = LssTextMuted, fontSize = captionSize)
                    }
                }
            }

            item {
                val displayItems = if (inProgressItems.isNotEmpty()) inProgressItems else upcomingItems
                if (displayItems.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = L10n.noActiveEvents(language),
                                color = LssTextMuted,
                                fontSize = bodyTextSize
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        displayItems.forEach { item ->
                            LiveProgramItemCard(
                                item = item,
                                language = language,
                                isCompact = isCompact,
                                onActionClick = { onNavigateTo("officiating") }
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 4. LIVE SCHOOL STANDINGS (LEADERBOARD)
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = L10n.schoolStandings(language),
                        color = LssGold,
                        fontSize = sectionTitleSize,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { onNavigateTo("reports") },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(L10n.fullReport(language), color = LssTextMuted, fontSize = captionSize)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("school_standings_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = LssNavyCard)
                ) {
                    if (schoolStandings.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = L10n.noResultsYet(language),
                                color = LssTextMuted,
                                fontSize = bodyTextSize
                            )
                        }
                    } else {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            schoolStandings.take(4).forEachIndexed { index, score ->
                                val rankBadge = when (index) {
                                    0 -> "🥇"
                                    1 -> "🥈"
                                    2 -> "🥉"
                                    else -> "#${index + 1}"
                                }
                                val ratio = (score.totalPoints.toFloat() / maxSchoolPoints.toFloat()).coerceIn(0f, 1f)

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(rankBadge, fontSize = if (isCompact) 13.sp else 15.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = score.school,
                                                color = if (index == 0) LssGold else LssTextWhite,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = bodyTextSize,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (score.goldCount > 0) {
                                                Text(
                                                    text = "${score.goldCount} ${L10n.gold(language)}",
                                                    color = LssGoldLight,
                                                    fontSize = captionSize,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                            }
                                            Text(
                                                text = L10n.pointsCount(score.totalPoints, language),
                                                color = LssGold,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = bodyTextSize
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (index == 0) LssGold else LssTextMuted,
                                        trackColor = LssNavyDark
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 5. NUUTSTE UITSLAE FEED (RECENT RESULTS)
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = L10n.latestResults(language),
                        color = LssGold,
                        fontSize = sectionTitleSize,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { onNavigateTo("live_feed") },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(L10n.liveFeed(language), color = LssTextMuted, fontSize = captionSize)
                    }
                }
            }

            if (recentResults.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = L10n.waitingForResults(language),
                                color = LssTextMuted,
                                fontSize = bodyTextSize
                            )
                        }
                    }
                }
            } else {
                items(recentResults, key = { it.id.ifBlank { "${it.athleteId}_${it.eventName}_${it.place}" } }) { res ->
                    RecentResultRow(
                        res = res,
                        isCompact = isCompact,
                        onClick = { onNavigateTo("live_feed") }
                    )
                }
            }

            // ==========================================
            // 6. MODULES VINNIGE TOEGANG (QUICK ACCESS)
            // ==========================================
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = L10n.meetModulesTitle(language),
                    color = LssGold,
                    fontSize = sectionTitleSize,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                val moduleCols = if (isExpanded) 4 else if (isMedium) 2 else 1
                val modules = listOf(
                    DashboardModule(
                        title = if (role == UserRole.OFFICIAL) L10n.modSetupTitle(language) else "🔒 " + L10n.modSetupTitle(language),
                        subtitle = if (role == UserRole.OFFICIAL) L10n.modSetupSub(language) else (if (language == AppLanguage.ENGLISH) "Officials Only (Requires PIN)" else "Slegs Beamptes (Vereis PIN)"),
                        icon = Icons.Default.List,
                        route = "setup_import"
                    ),
                    DashboardModule(L10n.modDrawsTitle(language), L10n.modDrawsSub(language), Icons.Default.Info, "draw_sheets"),
                    DashboardModule(
                        title = if (role == UserRole.OFFICIAL) L10n.modOfficiatingTitle(language) else "🔒 " + L10n.modOfficiatingTitle(language),
                        subtitle = if (role == UserRole.OFFICIAL) L10n.modOfficiatingSub(language) else (if (language == AppLanguage.ENGLISH) "Officials Only (Requires PIN)" else "Slegs Beamptes (Vereis PIN)"),
                        icon = Icons.Default.PlayArrow,
                        route = "officiating"
                    ),
                    DashboardModule(L10n.modRecordsTitle(language), L10n.modRecordsSub(language), Icons.Default.Star, "records"),
                    DashboardModule(L10n.modResultsTitle(language), L10n.modResultsSub(language), Icons.Default.List, "live_feed"),
                    DashboardModule(L10n.modMedalsTitle(language), L10n.modMedalsSub(language), Icons.Default.Star, "medals"),
                    DashboardModule(L10n.modReportsTitle(language), L10n.modReportsSub(language), Icons.Default.Info, "reports"),
                    DashboardModule(L10n.modProgramTitle(language), L10n.modProgramSub(language), Icons.Default.DateRange, "program")
                )

                val chunkedModules = modules.chunked(moduleCols)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunkedModules.forEach { rowModules ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowModules.forEach { mod ->
                                DashboardModuleTile(
                                    module = mod,
                                    isCompact = isCompact,
                                    onClick = { onNavigateTo(mod.route) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowModules.size < moduleCols) {
                                repeat(moduleCols - rowModules.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWebConfigDialog) {
        AlertDialog(
            onDismissRequest = { showWebConfigDialog = false },
            title = {
                Text(
                    text = if (language == AppLanguage.ENGLISH) "⚙️ How to Activate GitHub Pages (15 sec)" else "⚙️ Hoe om GitHub Pages te Aktiveer (15 sek)",
                    color = LssGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (language == AppLanguage.ENGLISH)
                            "GitHub shows '404 There isn't a GitHub Pages site here' because Pages must be switched ON once in your repository settings:"
                        else
                            "GitHub wys '404 There isn't a GitHub Pages site here' omdat Pages eenmalig in jou GitHub repository geaktiveer moet word:",
                        color = LssTextLight,
                        fontSize = 12.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LssNavyDark,
                        border = BorderStroke(1.dp, LssGold.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (language == AppLanguage.ENGLISH)
                                    "1. Click 'Open GitHub Settings' below."
                                else
                                    "1. Klik 'Maak GitHub Settings Oop' hieronder.",
                                color = LssGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (language == AppLanguage.ENGLISH)
                                    "2. Under 'Build and deployment', set Branch: 'main', Folder: '/ (root)'."
                                else
                                    "2. Onder 'Build and deployment', kies Branch: 'main', Folder: '/ (root)'.",
                                color = LssTextLight,
                                fontSize = 11.sp
                            )
                            Text(
                                text = if (language == AppLanguage.ENGLISH)
                                    "3. Click 'Save'. It will be live in ~60 seconds!"
                                else
                                    "3. Klik 'Save'. Die webwerf sal binne ~60 sekondes regstreeks werk!",
                                color = LssGreenSuccess,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = if (language == AppLanguage.ENGLISH) "Current Web Dashboard URL:" else "Huidige Web Dashboard Skakel:",
                        color = LssTextMuted,
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = currentWebUrlInput,
                        onValueChange = { currentWebUrlInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LssGold,
                            unfocusedBorderColor = LssGold.copy(alpha = 0.5f),
                            focusedTextColor = LssTextLight,
                            unfocusedTextColor = LssTextLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { WebDashboardConfig.openGitHubPagesSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.ENGLISH) "Open GitHub Pages Settings ↗" else "Maak GitHub Pages Settings Oop ↗",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        WebDashboardConfig.setWebUrl(context, currentWebUrlInput)
                        showWebConfigDialog = false
                        Toast.makeText(context, if (language == AppLanguage.ENGLISH) "Web URL saved!" else "Skakel gestoor!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                ) {
                    Text(if (language == AppLanguage.ENGLISH) "Save URL" else "Stoor Skakel", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWebConfigDialog = false }) {
                    Text(L10n.cancel(language), color = LssTextMuted)
                }
            },
            containerColor = LssNavyCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ResponsiveMetricCard(
    value: String,
    label: String,
    icon: ImageVector,
    valueSize: androidx.compose.ui.unit.TextUnit,
    labelSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = LssNavyCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    color = LssGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = valueSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = LssTextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = LssTextMuted,
                fontSize = labelSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LiveProgramItemCard(
    item: ProgramItem,
    language: AppLanguage,
    isCompact: Boolean,
    onActionClick: () -> Unit
) {
    val isEnglish = language == AppLanguage.ENGLISH
    val genderDisplay = if (isEnglish) {
        if (item.gender.equals("Seuns", ignoreCase = true)) "Boys" else "Girls"
    } else {
        item.gender
    }
    val agePrefix = if (isEnglish) "u/" else "o/"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = LssNavyCard),
        border = if (item.status == "Besig") CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LssGreenSuccess)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (item.status == "Besig") LssGreenSuccess.copy(alpha = 0.2f) else LssNavyHeader),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${item.itemNo}",
                        color = if (item.status == "Besig") LssGreenSuccess else LssGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${item.eventName} ($agePrefix${item.ageGroup} $genderDisplay)",
                        color = LssTextWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (isCompact) 13.sp else 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${if (isEnglish) "Time" else "Tyd"}: ${item.startTime} • ${item.venue}",
                        color = LssTextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = when (item.status) {
                    "Besig" -> LssGreenSuccess.copy(alpha = 0.15f)
                    "Voltooi" -> LssGold.copy(alpha = 0.15f)
                    else -> LssNavyHeader
                },
                modifier = Modifier.clickable(onClick = onActionClick)
            ) {
                Text(
                    text = when (item.status) {
                        "Besig" -> L10n.inProgress(language)
                        "Voltooi" -> L10n.finished(language)
                        else -> L10n.pending(language)
                    },
                    color = when (item.status) {
                        "Besig" -> LssGreenSuccess
                        "Voltooi" -> LssGold
                        else -> LssTextMuted
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentResultRow(
    res: EventResult,
    isCompact: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = LssNavyCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val medalSymbol = when (res.place) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> "${res.place}e"
                }
                Text(medalSymbol, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = res.athleteName,
                        color = LssTextWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (isCompact) 12.sp else 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${res.eventName} • ${res.school}",
                        color = LssTextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = res.resultValue,
                    color = LssGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isCompact) 12.sp else 13.sp
                )
                Text(
                    text = "${res.asaPoints} ASA",
                    color = LssTextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private data class DashboardModule(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String
)

@Composable
private fun DashboardModuleTile(
    module: DashboardModule,
    isCompact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag("tile_${module.route}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = LssNavyCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(LssNavyHeader),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = module.icon,
                    contentDescription = module.title,
                    tint = LssGold,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = module.title,
                    color = LssGold,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (isCompact) 13.sp else 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = module.subtitle,
                    color = LssTextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = LssTextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
