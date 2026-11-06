package com.example.swartlandathletics.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.swartlandathletics.data.AthleticsRepository
import com.example.swartlandathletics.model.AppConstants
import com.example.swartlandathletics.model.Athlete
import com.example.swartlandathletics.model.EventRecord
import com.example.swartlandathletics.model.ProgramItem
import com.example.swartlandathletics.ui.theme.*
import com.example.swartlandathletics.util.AppLanguage
import com.example.swartlandathletics.util.CsvManager
import com.example.swartlandathletics.util.L10n
import com.example.swartlandathletics.util.LanguageManager

data class DigitalAthleteRow(
    val id: String = java.util.UUID.randomUUID().toString(),
    var firstName: String = "",
    var lastName: String = "",
    var school: String = "Laerskool Swartland",
    var gender: String = "Seuns",
    var ageGroup: String = "13",
    var events: MutableSet<String> = mutableSetOf()
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SetupImportScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by LanguageManager.currentLanguage.collectAsState()
    val isEnglish = language == AppLanguage.ENGLISH

    var selectedTab by remember { mutableIntStateOf(0) }
    val athletes by AthleticsRepository.athletes.collectAsState()

    val currentMeetName by AthleticsRepository.meetName.collectAsState()
    val currentMeetDate by AthleticsRepository.meetDate.collectAsState()

    var inputMeetName by remember(currentMeetName) { mutableStateOf(currentMeetName) }
    var inputMeetDate by remember(currentMeetDate) { mutableStateOf(currentMeetDate) }

    var filterSchool by remember { mutableStateOf("Alle Skole") }
    var filterGender by remember { mutableStateOf("Alle Geslagte") }
    var filterAge by remember { mutableStateOf("Alle Ouderdomme") }

    // Form state for single athlete
    var inputFirstName by remember { mutableStateOf("") }
    var inputLastName by remember { mutableStateOf("") }
    var inputSchool by remember { mutableStateOf("Laerskool Swartland") }
    var inputGender by remember { mutableStateOf("Seuns") }
    var inputAge by remember { mutableStateOf("13") }
    var selectedEvents by remember { mutableStateOf(setOf<String>()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Digital Entry Sheet State (Interactive pop-up matrix with checkboxes - replaces raw CSV upload)
    var showDigitalSheetDialog by remember { mutableStateOf(false) }
    var digitalDefaultSchool by remember { mutableStateOf("Laerskool Swartland") }
    var digitalRows by remember {
        mutableStateOf(
            listOf(
                DigitalAthleteRow(school = "Laerskool Swartland", gender = "Seuns", ageGroup = "13"),
                DigitalAthleteRow(school = "Laerskool Swartland", gender = "Seuns", ageGroup = "13"),
                DigitalAthleteRow(school = "Laerskool Swartland", gender = "Dogters", ageGroup = "13")
            )
        )
    }
    var showQuickNamePasteDialog by remember { mutableStateOf(false) }
    var quickNamePasteContent by remember { mutableStateOf("") }

    // Bulk Import / Export States
    val records by AthleticsRepository.records.collectAsState()
    val program by AthleticsRepository.program.collectAsState()

    var currentFilePickerMode by remember { mutableStateOf("athletes") } // "athletes", "program", "records"
    var importDefaultGender by remember { mutableStateOf("Seuns") }
    var parsedAthletesPreview by remember { mutableStateOf<List<Athlete>?>(null) }
    var parsedProgramPreview by remember { mutableStateOf<List<ProgramItem>?>(null) }
    var parsedRecordsPreview by remember { mutableStateOf<List<EventRecord>?>(null) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteDialogType by remember { mutableStateOf("athletes") } // "athletes", "program", "records"
    var pastedContent by remember { mutableStateOf("") }
    var isImportingLoading by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                when (currentFilePickerMode) {
                    "athletes" -> {
                        val found = CsvManager.parseAthletesCsv(content, fallbackGender = importDefaultGender)
                        if (found.isNotEmpty()) {
                            parsedAthletesPreview = found
                        } else {
                            statusMessage = if (isEnglish)
                                "No valid athletes found. Check CSV format (Image 1 Inskrywingsdokument or list)."
                            else
                                "Geen geldige atlete gevind nie. Kontroleer asseblief die CSV-formaat."
                        }
                    }
                    "program" -> {
                        val found = CsvManager.parseProgramCsv(content)
                        if (found.isNotEmpty()) {
                            parsedProgramPreview = found
                        } else {
                            statusMessage = if (isEnglish)
                                "No valid program items found. Expected columns: Item No, Begin Tyd, Ouderdom, Geslag, Item, Plek, Status."
                            else
                                "Geen geldige program gevind nie. Verwagte kolomme: Item No, Begin Tyd, Ouderdom, Geslag, Item, Plek, Status."
                        }
                    }
                    "records" -> {
                        val found = CsvManager.parseRecordsCsv(content)
                        if (found.isNotEmpty()) {
                            parsedRecordsPreview = found
                        } else {
                            statusMessage = if (isEnglish)
                                "No valid records found. Expected columns: Item, Ouderdom, Geslag, Tyd of Afstand, Skool of Atleet, Jaar."
                            else
                                "Geen geldige rekords gevind nie. Verwagte kolomme: Item, Ouderdom, Geslag, Tyd of Afstand, Skool of Atleet, Jaar."
                        }
                    }
                    "meet_template" -> {
                        AthleticsRepository.importMeetTemplateJson(content) { success, msg ->
                            statusMessage = msg
                        }
                    }
                }
            } catch (e: Exception) {
                statusMessage = "Fout met lêerlees: ${e.localizedMessage}"
            }
        }
    }

    val availableEventsForForm = remember(inputAge, inputGender) {
        AppConstants.getEventsForAgeAndGender(inputAge, inputGender)
    }

    val existingSchools = remember(athletes) {
        athletes.map { it.school.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val schoolsForFilter = remember(existingSchools) {
        listOf("Alle Skole") + existingSchools
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LssNavyDark)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = if (isEnglish) "📋 Athlete Entries & Import" else "📋 Inskrywings & Sigblaaie",
                    color = LssGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_setup")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = L10n.back(language), tint = LssGold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = LssNavyHeader)
        )

        // Clean tabs that never clip
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = LssNavyCard,
            contentColor = LssGold
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(if (isEnglish) "🏆 Event Setup" else "🏆 Byeenkoms Setup", fontSize = 11.sp, maxLines = 1) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(if (isEnglish) "Athletes (${athletes.size})" else "Atlete (${athletes.size})", fontSize = 11.sp, maxLines = 1) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(if (isEnglish) "Add Athlete" else "Nuwe Atleet", fontSize = 11.sp, maxLines = 1) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text(if (isEnglish) "✍️ Digital Sheet" else "✍️ Digitale Sigblad", fontSize = 11.sp, maxLines = 1) }
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
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusMessage ?: "",
                        color = LssGold,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { statusMessage = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Maak toe", tint = LssTextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // ==================== TAB 0: EVENT NAME & DATE SETUP / BACKUP ====================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, LssGold)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = LssGold, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "🏆 Event Name & Date Setup" else "🏆 Stel Byeenkoms Naam & Datum In",
                                        color = LssGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Text(
                                    text = if (isEnglish)
                                        "Specify the name and date for the current athletics meet. This metadata is saved with your backup templates so you can easily reload or share programs between different events."
                                    else
                                        "Stel die naam en datum in vir die huidige atletiekbyeenkoms. Hierdie inligting word saam met jou rugsteun-sjablone gestoort sodat jy maklik programme tussen verskillende byeenkomste kan hergebruik.",
                                    color = LssTextLight,
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                OutlinedTextField(
                                    value = inputMeetName,
                                    onValueChange = { inputMeetName = it },
                                    label = { Text(if (isEnglish) "Event / Meet Name" else "Byeenkoms Naam") },
                                    placeholder = { Text("bv. Laerskool Swartland Interhuis 2026") },
                                    modifier = Modifier.fillMaxWidth().testTag("input_meet_name"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LssGold,
                                        unfocusedBorderColor = LssNavyCardLight,
                                        focusedLabelColor = LssGold
                                    )
                                )

                                OutlinedTextField(
                                    value = inputMeetDate,
                                    onValueChange = { inputMeetDate = it },
                                    label = { Text(if (isEnglish) "Event Date (YYYY-MM-DD)" else "Byeenkoms Datum (JJJJ-MM-DD)") },
                                    placeholder = { Text("2026-02-15") },
                                    modifier = Modifier.fillMaxWidth().testTag("input_meet_date"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LssGold,
                                        unfocusedBorderColor = LssNavyCardLight,
                                        focusedLabelColor = LssGold
                                    )
                                )

                                Button(
                                    onClick = {
                                        AthleticsRepository.updateMeetInfo(inputMeetName, inputMeetDate) { success, msg ->
                                            statusMessage = msg
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader),
                                    modifier = Modifier.fillMaxWidth().height(46.dp).testTag("btn_save_meet_info")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isEnglish) "Save Event Details" else "Stoor Byeenkoms Inligting", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = LssGold, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "💾 Export Event Backup / Template" else "💾 Stoor Byeenkoms Sjabloon (.json)",
                                        color = LssGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Text(
                                    text = if (isEnglish)
                                        "Exports the program and records along with the event name ('$currentMeetName') and date ('$currentMeetDate'). Upload this file next year to instantly recreate the meet setup."
                                    else
                                        "Voer die huidige program en rekords uit saam met die byeenkomsnaam ('$currentMeetName') en datum ('$currentMeetDate'). Laai hierdie lêer volgende jaar op om die byeenkoms outomaties te herstel.",
                                    color = LssTextMuted,
                                    fontSize = 12.sp
                                )

                                Button(
                                    onClick = {
                                        val jsonStr = AthleticsRepository.exportMeetTemplateJson(inputMeetName, inputMeetDate)
                                        val safeName = inputMeetName.trim().replace("\\s+".toRegex(), "_")
                                        val fileName = "${safeName}_${inputMeetDate}_backup.json"
                                        CsvManager.shareContent(context = context, title = fileName, content = jsonStr)
                                        statusMessage = if (isEnglish) "Event backup ($fileName) exported successfully!" else "Byeenkoms rugsteun ($fileName) suksesvol uitgevoer!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader),
                                    modifier = Modifier.fillMaxWidth().height(46.dp).testTag("btn_export_meet_backup")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isEnglish) "Export Event Backup (.json)" else "Stoor Byeenkoms Rugsteun (.json)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = LssGreenSuccess, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "📂 Load Stored Event Backup" else "📂 Laai Byeenkoms Sjabloon In",
                                        color = LssGreenSuccess,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Text(
                                    text = if (isEnglish)
                                        "Upload a saved event JSON file from a previous year or another school to automatically import its event name, date, program, and records."
                                    else
                                        "Laai 'n gestoorde JSON-lêer van 'n vorige jaar of ander skool op om die byeenkoms se naam, datum, program en rekords outomaties in te voer.",
                                    color = LssTextMuted,
                                    fontSize = 12.sp
                                )

                                Button(
                                    onClick = {
                                        currentFilePickerMode = "meet_template"
                                        filePickerLauncher.launch("*/*")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LssGreenSuccess, contentColor = LssNavyHeader),
                                    modifier = Modifier.fillMaxWidth().height(46.dp).testTag("btn_import_meet_backup")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isEnglish) "Load Backup (.json)" else "Laai Gestoorde Sjabloon (.json)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, LssRedError.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = LssRedError, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "🔄 Reset for New Event" else "🔄 Nuwe Byeenkoms / Vars Begin",
                                        color = LssRedError,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Text(
                                    text = if (isEnglish)
                                        "Clears entered athletes and results while retaining program and records for the new event."
                                    else
                                        "Vee geregistreerde atlete en uitslae uit terwyl die program en rekords behou word vir die nuwe byeenkoms.",
                                    color = LssTextMuted,
                                    fontSize = 12.sp
                                )

                                OutlinedButton(
                                    onClick = {
                                        AthleticsRepository.resetMeetForNewYear(clearAthletes = true, resetProgramStatus = true, clearResults = true) {
                                            statusMessage = if (isEnglish) "Meet reset for new event!" else "Byeenkoms herstel vir nuwe byeenkoms!"
                                        }
                                    },
                                    border = BorderStroke(1.dp, LssRedError),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LssRedError),
                                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("btn_reset_meet")
                                ) {
                                    Text(if (isEnglish) "Reset Athletes & Results" else "Vee Atlete & Uitslae Uit vir Nuwe Byeenkoms", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // ==================== TAB 1: ATHLETES LIST ====================
                val filteredAthletes = remember(athletes, filterSchool, filterGender, filterAge) {
                    athletes.filter { a ->
                        (filterSchool == "Alle Skole" || a.school == filterSchool) &&
                                (filterGender == "Alle Geslagte" || a.gender == filterGender) &&
                                (filterAge == "Alle Ouderdomme" || a.ageGroup == filterAge)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDigitalSheetDialog = true }
                                .testTag("btn_open_digital_sheet_tab0"),
                            colors = CardDefaults.cardColors(containerColor = LssGold.copy(alpha = 0.15f)),
                            border = BorderStroke(1.5.dp, LssGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp).background(LssGold, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = LssNavyHeader, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEnglish) "✍️ Open Digital Entry Sheet (Tick Items)" else "✍️ Maak Digitale Sigblad Oop (Merk Items)",
                                        color = LssGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (isEnglish)
                                            "Enter athletes in bulk and tick checkboxes under each event. No CSV!"
                                        else
                                            "Voer atlete in grootmaat in en merk blokkies onder elke item. Geen CSV!",
                                        color = LssTextLight,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = LssGold, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isEnglish) "Filter Athletes:" else "Filtreer Atlete:",
                                    color = LssGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // School filter
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(schoolsForFilter) { sch ->
                                        FilterChip(
                                            selected = filterSchool == sch,
                                            onClick = { filterSchool = sch },
                                            label = { Text(sch, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Gender and Age filter row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Alle Geslagte", "Seuns", "Dogters").forEach { g ->
                                        FilterChip(
                                            selected = filterGender == g,
                                            onClick = { filterGender = g },
                                            label = { Text(if (isEnglish && g == "Seuns") "Boys" else if (isEnglish && g == "Dogters") "Girls" else g, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(listOf("Alle Ouderdomme") + AppConstants.AGE_GROUPS) { age ->
                                        FilterChip(
                                            selected = filterAge == age,
                                            onClick = { filterAge = age },
                                            label = { Text(if (age == "Alle Ouderdomme") (if (isEnglish) "All Ages" else age) else "O/$age", fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${filteredAthletes.size} ${if (isEnglish) "athletes found" else "atlete gevind"}",
                                color = LssTextMuted,
                                fontSize = 12.sp
                            )
                            TextButton(onClick = { selectedTab = 2 }) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = LssGold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEnglish) "Import / Export" else "Invoer / Uitvoer", color = LssGold, fontSize = 12.sp)
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
                                Text(
                                    text = if (isEnglish) "No athletes match filters. Add athletes or import spreadsheet." else "Geen atlete gevind nie. Voeg by of voer 'n sigblad in.",
                                    color = LssTextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        items(filteredAthletes, key = { it.id }) { athlete ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = athlete.fullName,
                                            color = LssGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${athlete.school} • o/${athlete.ageGroup} ${athlete.gender}",
                                            color = LssTextLight,
                                            fontSize = 12.sp
                                        )
                                        if (athlete.events.isNotEmpty()) {
                                            Text(
                                                text = "Items: ${athlete.events.joinToString(", ")}",
                                                color = LssTextMuted,
                                                fontSize = 11.sp
                                            )
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

            2 -> {
                // ==================== TAB 2: ADD SINGLE ATHLETE ====================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = if (isEnglish) "✍️ Add New Athlete" else "✍️ Voeg Nuwe Atleet By",
                            color = LssGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDigitalSheetDialog = true }
                                .testTag("btn_open_digital_sheet_tab1"),
                            colors = CardDefaults.cardColors(containerColor = LssGold.copy(alpha = 0.15f)),
                            border = BorderStroke(1.5.dp, LssGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp).background(LssGold, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = LssNavyHeader, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEnglish) "✍️ Open Digital Entry Sheet (Multiple Athletes)" else "✍️ Maak Digitale Sigblad Oop (Veelvuldige Atlete)",
                                        color = LssGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (isEnglish)
                                            "Prefer spreadsheet entry? Enter bulk athletes and tick event boxes!"
                                        else
                                            "Verkies jy 'n sigblad? Tik atlete in en merk blokkies vir elke item!",
                                        color = LssTextLight,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = LssGold, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = inputFirstName,
                            onValueChange = { inputFirstName = it },
                            label = { Text(if (isEnglish) "First Name" else "Voornaam") },
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
                            label = { Text(if (isEnglish) "Last Name / Surname" else "Van") },
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

                    // POINT 3: Free-form School Textbox (Open input for any school name)
                    item {
                        OutlinedTextField(
                            value = inputSchool,
                            onValueChange = { inputSchool = it },
                            label = { Text(if (isEnglish) "School Name (Type any school)" else "Skool Naam (Tik enige skool)") },
                            placeholder = { Text(if (isEnglish) "e.g. Laerskool Swartland" else "bv. Laerskool Swartland") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_school"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LssGold,
                                unfocusedBorderColor = LssNavyCardLight,
                                focusedLabelColor = LssGold
                            )
                        )

                        // Quick-tap chips from schools already entered into the system
                        if (existingSchools.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isEnglish) "Or tap existing school:" else "Of kies 'n bestaande skool:",
                                color = LssTextMuted,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(existingSchools) { sch ->
                                    FilterChip(
                                        selected = inputSchool.equals(sch, ignoreCase = true),
                                        onClick = { inputSchool = sch },
                                        label = { Text(sch, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(if (isEnglish) "Gender:" else "Geslag:", color = LssTextLight, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Seuns", "Dogters").forEach { g ->
                                FilterChip(
                                    selected = inputGender == g,
                                    onClick = {
                                        inputGender = g
                                        selectedEvents = emptySet()
                                    },
                                    label = { Text(if (isEnglish && g == "Seuns") "Boys" else if (isEnglish && g == "Dogters") "Girls" else g) }
                                )
                            }
                        }
                    }

                    item {
                        Text(if (isEnglish) "Age Group:" else "Ouderdomsgroep:", color = LssTextLight, fontSize = 13.sp)
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
                        Text(if (isEnglish) "Select Events / Disciplines:" else "Kies Items / Dissiplines:", color = LssGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                                if (inputFirstName.isNotBlank() && inputLastName.isNotBlank() && inputSchool.isNotBlank()) {
                                    AthleticsRepository.addAthlete(
                                        firstName = inputFirstName,
                                        lastName = inputLastName,
                                        school = inputSchool,
                                        gender = inputGender,
                                        ageGroup = inputAge,
                                        events = selectedEvents.toList().ifEmpty { listOf("100m") }
                                    )
                                    statusMessage = if (isEnglish)
                                        "Athlete $inputFirstName $inputLastName ($inputSchool) registered!"
                                    else
                                        "Atleet $inputFirstName $inputLastName ($inputSchool) geregistreer!"
                                    inputFirstName = ""
                                    inputLastName = ""
                                    selectedEvents = emptySet()
                                    selectedTab = 1
                                } else {
                                    statusMessage = if (isEnglish) "Please fill in first name, surname, and school." else "Vul voornaam, van en skool in."
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_save_athlete"),
                            colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isEnglish) "Save Athlete" else "Stoor Atleet", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            3 -> {
                // ==================== TAB 3: EXCEL / CSV BULK IMPORT & EXPORT ====================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // CARD 1: INTERACTIVE DIGITAL ENTRY SHEET (CHECKBOXES MATRIX - NO CSV NEEDED)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, LssGold)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(34.dp).background(LssGold, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, tint = LssNavyHeader, modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isEnglish) "✍️ Interactive Digital Entry Sheet" else "✍️ Interaktiewe Inskrywingsrooster",
                                                color = LssGold,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = if (isEnglish) "No CSV needed! Enter athletes and tick checkboxes." else "Geen CSV nodig nie! Tik atlete in en merk blokkies.",
                                                color = LssTextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { showDigitalSheetDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                                    ) {
                                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Full Screen" else "Volskerm", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Quick Defaults Row (School for all, Paste names)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = digitalDefaultSchool,
                                        onValueChange = { digitalDefaultSchool = it },
                                        label = { Text(if (isEnglish) "School for All" else "Skool vir Almal", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1.3f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = LssGold,
                                            unfocusedBorderColor = LssNavyCardLight
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            if (digitalDefaultSchool.isNotBlank()) {
                                                digitalRows = digitalRows.map { it.copy(school = digitalDefaultSchool) }
                                                statusMessage = if (isEnglish) "Applied '$digitalDefaultSchool' to all rows." else "'$digitalDefaultSchool' toegepas op alle rye."
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = LssNavyCardLight, contentColor = LssGold),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(if (isEnglish) "Apply All" else "Pas Almal Toe", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { showQuickNamePasteDialog = true },
                                        border = BorderStroke(1.dp, LssGold),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = LssGold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Paste Names" else "Plak Name", fontSize = 10.sp, color = LssGold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Horizontally scrollable table
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, LssNavyCardLight, RoundedCornerShape(8.dp))
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        // Header Row
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .background(LssNavyHeader, RoundedCornerShape(4.dp))
                                                .padding(vertical = 8.dp)
                                        ) {
                                            Text("#", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(30.dp))
                                            Text(if (isEnglish) "First Name" else "Voornaam", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(110.dp))
                                            Text(if (isEnglish) "Surname" else "Van", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(110.dp))
                                            Text(if (isEnglish) "School" else "Skool", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(120.dp))
                                            Text(if (isEnglish) "Gender" else "Geslag", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(75.dp))
                                            Text(if (isEnglish) "Age" else "Oud", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(55.dp))

                                            // Event Columns Headers
                                            AppConstants.ENTRY_SHEET_DISCIPLINES.forEach { disc ->
                                                val disp = when (disc) {
                                                    "4x100m Relay" -> "4x100m"
                                                    "Long Jump" -> "Verspring"
                                                    "High Jump" -> "Hoogspring"
                                                    "Shot Put" -> "Gewigstoot"
                                                    "Discus" -> "Skyfwerp"
                                                    "Javelin" -> "Spiesgooi"
                                                    "70m Hurdles" -> "70mH"
                                                    "75m Hurdles" -> "75mH"
                                                    "80m Hurdles" -> "80mH"
                                                    "150m Hurdles" -> "150mH"
                                                    "200m Hurdles" -> "200mH"
                                                    "1500m Walk" -> "1500m S"
                                                    else -> disc
                                                }
                                                val isTrack = disc in AppConstants.TRACK_EVENTS
                                                val isField = disc in AppConstants.FIELD_EVENTS
                                                val isHurdle = disc in AppConstants.HURDLE_EVENTS
                                                val headerColor = if (isTrack) LssGold else if (isField) androidx.compose.ui.graphics.Color(0xFF60A5FA) else if (isHurdle) androidx.compose.ui.graphics.Color(0xFFC084FC) else androidx.compose.ui.graphics.Color(0xFF34D399)

                                                Box(
                                                    modifier = Modifier.width(62.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = disp,
                                                        color = headerColor,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(40.dp))
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Athlete Rows
                                        digitalRows.forEachIndexed { index, row ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                Text("${index + 1}", color = LssTextMuted, fontSize = 11.sp, modifier = Modifier.width(30.dp))

                                                OutlinedTextField(
                                                    value = row.firstName,
                                                    onValueChange = { newVal ->
                                                        digitalRows = digitalRows.mapIndexed { i, r -> if (i == index) r.copy(firstName = newVal) else r }
                                                    },
                                                    modifier = Modifier.width(105.dp),
                                                    placeholder = { Text("Naam", fontSize = 10.sp) },
                                                    singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LssGold)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))

                                                OutlinedTextField(
                                                    value = row.lastName,
                                                    onValueChange = { newVal ->
                                                        digitalRows = digitalRows.mapIndexed { i, r -> if (i == index) r.copy(lastName = newVal) else r }
                                                    },
                                                    modifier = Modifier.width(105.dp),
                                                    placeholder = { Text("Van", fontSize = 10.sp) },
                                                    singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LssGold)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))

                                                OutlinedTextField(
                                                    value = row.school,
                                                    onValueChange = { newVal ->
                                                        digitalRows = digitalRows.mapIndexed { i, r -> if (i == index) r.copy(school = newVal) else r }
                                                    },
                                                    modifier = Modifier.width(115.dp),
                                                    singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LssGold)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))

                                                // Gender toggle chip
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (row.gender == "Seuns") LssNavyHeader else LssGold.copy(alpha = 0.2f),
                                                    border = BorderStroke(1.dp, LssGold.copy(alpha = 0.5f)),
                                                    modifier = Modifier
                                                        .width(70.dp)
                                                        .clickable {
                                                            val nextG = if (row.gender == "Seuns") "Dogters" else "Seuns"
                                                            digitalRows = digitalRows.mapIndexed { i, r -> if (i == index) r.copy(gender = nextG) else r }
                                                        }
                                                ) {
                                                    Text(
                                                        text = if (row.gender == "Seuns") (if (isEnglish) "Boys" else "Seuns") else (if (isEnglish) "Girls" else "Dogters"),
                                                        color = LssGold,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(5.dp))

                                                // Age toggle
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = LssNavyHeader,
                                                    border = BorderStroke(1.dp, LssGold.copy(alpha = 0.5f)),
                                                    modifier = Modifier
                                                        .width(50.dp)
                                                        .clickable {
                                                            val curAge = row.ageGroup.toIntOrNull() ?: 13
                                                            val nextAge = if (curAge >= 13) 7 else curAge + 1
                                                            digitalRows = digitalRows.mapIndexed { i, r -> if (i == index) r.copy(ageGroup = nextAge.toString()) else r }
                                                        }
                                                ) {
                                                    Text(
                                                        text = "o/${row.ageGroup}",
                                                        color = LssTextLight,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(5.dp))

                                                // Event Checkboxes
                                                AppConstants.ENTRY_SHEET_DISCIPLINES.forEach { disc ->
                                                    val isChecked = row.events.contains(disc)
                                                    Box(
                                                        modifier = Modifier
                                                            .width(62.dp)
                                                            .background(if (isChecked) LssGold.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
                                                            .clickable {
                                                                val updatedEvents = row.events.toMutableSet()
                                                                if (isChecked) updatedEvents.remove(disc) else updatedEvents.add(disc)
                                                                digitalRows = digitalRows.mapIndexed { i, r -> if (i == index) r.copy(events = updatedEvents) else r }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Checkbox(
                                                            checked = isChecked,
                                                            onCheckedChange = { checked ->
                                                                val updatedEvents = row.events.toMutableSet()
                                                                if (checked) updatedEvents.add(disc) else updatedEvents.remove(disc)
                                                                digitalRows = digitalRows.mapIndexed { i, r -> if (i == index) r.copy(events = updatedEvents) else r }
                                                            },
                                                            colors = CheckboxDefaults.colors(
                                                                checkedColor = LssGold,
                                                                checkmarkColor = LssNavyHeader,
                                                                uncheckedColor = LssTextMuted
                                                            )
                                                        )
                                                    }
                                                }

                                                // Delete row button
                                                IconButton(
                                                    onClick = {
                                                        if (digitalRows.size > 1) {
                                                            digitalRows = digitalRows.filterIndexed { i, _ -> i != index }
                                                        } else {
                                                            digitalRows = listOf(DigitalAthleteRow(school = digitalDefaultSchool))
                                                        }
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Skrap", tint = LssRedError, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Bottom Row within Card
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            digitalRows = digitalRows + DigitalAthleteRow(school = digitalDefaultSchool, gender = "Seuns", ageGroup = "13")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = LssNavyCardLight, contentColor = LssGold)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isEnglish) "Add Athlete Row" else "Voeg Atleet Ry By", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }

                                    val totalCheckedItems = digitalRows.sumOf { it.events.size }
                                    Text(
                                        text = "${digitalRows.size} ${if (isEnglish) "athletes" else "atlete"} • $totalCheckedItems items",
                                        color = LssGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Button(
                                        onClick = {
                                            val validAthletes = digitalRows.filter { it.firstName.isNotBlank() && it.lastName.isNotBlank() }
                                            if (validAthletes.isNotEmpty()) {
                                                val toSave = validAthletes.map { r ->
                                                    Athlete(
                                                        id = "",
                                                        firstName = r.firstName.trim(),
                                                        lastName = r.lastName.trim(),
                                                        school = r.school.trim().ifBlank { digitalDefaultSchool },
                                                        gender = r.gender,
                                                        ageGroup = r.ageGroup,
                                                        events = r.events.toList().ifEmpty { listOf("100m") }
                                                    )
                                                }
                                                AthleticsRepository.bulkAddAthletes(toSave) { count ->
                                                    statusMessage = if (isEnglish) "$count athletes saved to database!" else "$count atlete suksesvol in databasis gestoor!"
                                                }
                                                statusMessage = if (isEnglish) "${toSave.size} athletes saved!" else "${toSave.size} atlete suksesvol gestoor!"
                                                selectedTab = 0
                                            } else {
                                                statusMessage = if (isEnglish) "Fill in First Name and Surname for at least one athlete." else "Vul asseblief Voornaam en Van in vir ten minste een atleet."
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isEnglish) "Save All to Meet" else "Stoor na Byeenkoms", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // CARD 1b: EXCEL SPREADSHEET TEMPLATES & EXPORT (FOR SCHOOLS WHO STILL WANT PAPER/CSV)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (isEnglish) "📥 Blank Templates & CSV Export (Optional)" else "📥 Leë Sjablone & CSV Uitvoer (Opsioneel)",
                                    color = LssGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isEnglish)
                                        "Share printable/fillable blank spreadsheets to other schools, or export all currently registered athletes to a CSV spreadsheet file."
                                    else
                                        "Deel leë Excel-sjablone aan ander skole, of voer alle tans geregistreerde atlete uit na 'n CSV-lêer.",
                                    color = LssTextMuted,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val template = CsvManager.generateAthleteMatrixTemplateCsv("Seuns")
                                            CsvManager.shareContent(context = context, title = "Swartland_Inskrywingsdokument_Seuns.csv", content = template)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Boys Template" else "Seuns Sjabloon", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val template = CsvManager.generateAthleteMatrixTemplateCsv("Dogters")
                                            CsvManager.shareContent(context = context, title = "Swartland_Inskrywingsdokument_Dogters.csv", content = template)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Girls Template" else "Dogters Sjabloon", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        val csvData = CsvManager.exportAthletesToMatrixCsv(athletes)
                                        CsvManager.shareContent(context = context, title = "Swartland_Geregistreerde_Atlete_Matriks.csv", content = csvData)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LssTextLight)
                                ) {
                                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "Export Registered Athletes (${athletes.size}) to CSV" else "Voer Huidige Atlete (${athletes.size}) Uit na CSV",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // CARD 2: PROGRAM SPREADSHEET (IMAGE 2: AMPTELIKE PROGRAMDOKUMENT)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = LssGold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "2. Meet Program Document (Image 2)" else "2. Amptelike Programdokument (Image 2)",
                                        color = LssGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isEnglish)
                                        "Columns: Item No, Begin Tyd, Ouderdom, Geslag, Item, Plek, Status (Image 2). Type in Excel or export/import directly to manage meet schedule in bulk."
                                    else
                                        "Kolomme: Item No, Begin Tyd, Ouderdom, Geslag, Item, Plek, Status (Image 2). Tik jou tydrooster in Excel of voer dit hier in grootmaat in.",
                                    color = LssTextMuted,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val template = CsvManager.generateProgramTemplateCsv()
                                            CsvManager.shareContent(
                                                context = context,
                                                title = "Swartland_Amptelike_Program_Sjabloon.csv",
                                                content = template
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Program Template" else "Program Sjabloon", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val exportData = CsvManager.exportProgramToCsv(program)
                                            CsvManager.shareContent(
                                                context = context,
                                                title = "Swartland_Program_Uitvoer.csv",
                                                content = exportData
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssNavyHeader, contentColor = LssGold)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Export (${program.size})" else "Voer Uit (${program.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            currentFilePickerMode = "program"
                                            filePickerLauncher.launch("*/*")
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssGreenSuccess, contentColor = LssNavyHeader)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Import Program CSV" else "Voer Program CSV In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            pasteDialogType = "program"
                                            showPasteDialog = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LssGold)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Paste Program" else "Plak Program Teks", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // CARD 3: RECORDS SPREADSHEET (IMAGE 3: AMPTELIKE REKORDDOKUMENT)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = LssGold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "3. Meet Records Document (Image 3)" else "3. Amptelike Rekorddokument (Image 3)",
                                        color = LssGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isEnglish)
                                        "Columns: Item, Ouderdom, Geslag, Tyd of Afstand, Skool of Atleet, Jaar (Image 3). Import all historical meet records in one easy spreadsheet."
                                    else
                                        "Kolomme: Item, Ouderdom, Geslag, Tyd of Afstand, Skool of Atleet, Jaar (Image 3). Voer alle geskiedkundige byeenkomsrekords in een sigblad in.",
                                    color = LssTextMuted,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val template = CsvManager.generateRecordsTemplateCsv()
                                            CsvManager.shareContent(
                                                context = context,
                                                title = "Swartland_Amptelike_Rekord_Sjabloon.csv",
                                                content = template
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Records Template" else "Rekord Sjabloon", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val exportData = CsvManager.exportRecordsToCsv(records)
                                            CsvManager.shareContent(
                                                context = context,
                                                title = "Swartland_Rekords_Uitvoer.csv",
                                                content = exportData
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssNavyHeader, contentColor = LssGold)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Export (${records.size})" else "Voer Uit (${records.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            currentFilePickerMode = "records"
                                            filePickerLauncher.launch("*/*")
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssGreenSuccess, contentColor = LssNavyHeader)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Import Records CSV" else "Voer Rekords CSV In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            pasteDialogType = "records"
                                            showPasteDialog = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LssGold)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Paste Records" else "Plak Rekord Teks", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // CARD 4: MEET BACKUP & NEW YEAR RESET
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (isEnglish) "4. Meet Backup & Next Year Setup" else "4. Byeenkoms Sjabloon & Nuwe Byeenkoms",
                                    color = LssGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isEnglish)
                                        "Save program & records to a backup file for next year's event, upload a stored backup, or reset for a new meet."
                                    else
                                        "Stoor program en rekords na 'n sjabloon-lêer vir volgende jaar, laai 'n gestoorde sjabloon in, of gee 'n nuwe byeenkoms 'n vars begin.",
                                    color = LssTextMuted,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val jsonStr = AthleticsRepository.exportMeetTemplateJson(
                                                meetName = "Laerskool Swartland Atletiek",
                                                meetDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                                            )
                                            val fileName = "Swartland_Atletiek_Sjabloon_${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}.json"
                                            CsvManager.shareContent(context = context, title = fileName, content = jsonStr)
                                            statusMessage = if (isEnglish) "Meet backup template exported!" else "Byeenkoms sjabloon uitgevoer!"
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "💾 Save Template" else "💾 Stoor Sjabloon", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            currentFilePickerMode = "meet_template"
                                            filePickerLauncher.launch("*/*")
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssGreenSuccess, contentColor = LssNavyHeader)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "📂 Load Template" else "📂 Laai Sjabloon", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            AthleticsRepository.resetMeetForNewYear(
                                                clearAthletes = true,
                                                resetProgramStatus = true,
                                                clearResults = true
                                            ) {
                                                statusMessage = if (isEnglish) "Meet reset for new year! (Program & records kept)" else "Nuwe byeenkoms gereed! (Program & rekords behou)"
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssGold.copy(alpha = 0.2f), contentColor = LssGold)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "🔄 New Meet" else "🔄 Nuwe Byeenkoms", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            AthleticsRepository.resetAll()
                                            statusMessage = if (isEnglish) "Entire meet reset to zero." else "Alle byeenkomsdata skoongemaak."
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssRedError, contentColor = LssTextWhite)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Clear All" else "Vee Alles Uit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Mandatory Footer: Created by Konrad Toerien
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, top = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Created by Konrad Toerien",
                color = LssTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    // ==================== 1. PREVIEW DIALOG FOR ATHLETES CSV ====================
    if (parsedAthletesPreview != null) {
        val previewList = parsedAthletesPreview!!
        val schoolsFound = remember(previewList) { previewList.map { it.school }.filter { it.isNotBlank() }.distinct() }
        val totalEventTicks = remember(previewList) { previewList.sumOf { it.events.size } }

        AlertDialog(
            onDismissRequest = { parsedAthletesPreview = null },
            title = {
                Text(
                    text = if (isEnglish) "Confirm Bulk Athlete Import" else "Bevestig Atlete Grootmaat-Invoer",
                    color = LssGold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isEnglish)
                            "${previewList.size} athletes ($totalEventTicks total event entries) detected from ${schoolsFound.size} schools: ${schoolsFound.joinToString(", ")}"
                        else
                            "${previewList.size} atlete ($totalEventTicks totale item-inskrywings) gevind vanaf ${schoolsFound.size} skole: ${schoolsFound.joinToString(", ")}",
                        color = LssTextLight,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isEnglish) "Sample parsed entries:" else "Voorbeelde van geleesde atlete:",
                        color = LssTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    previewList.take(5).forEach { a ->
                        Text(
                            text = "• ${a.fullName} (${a.school}) - o/${a.ageGroup} ${a.gender} [${a.events.joinToString(", ")}]",
                            color = LssTextWhite,
                            fontSize = 11.sp
                        )
                    }
                    if (previewList.size > 5) {
                        Text(
                            text = "... en nog ${previewList.size - 5} atlete",
                            color = LssTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isImportingLoading = true
                        AthleticsRepository.bulkAddAthletes(previewList) { count ->
                            isImportingLoading = false
                            statusMessage = if (isEnglish)
                                "$count athletes successfully imported into Firestore!"
                            else
                                "$count atlete suksesvol ingevoer na Firestore!"
                            parsedAthletesPreview = null
                            selectedTab = 0
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader),
                    enabled = !isImportingLoading
                ) {
                    if (isImportingLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = LssNavyHeader)
                    } else {
                        Text(if (isEnglish) "Import All ${previewList.size}" else "Voer Al ${previewList.size} In", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { parsedAthletesPreview = null }) {
                    Text(if (isEnglish) "Cancel" else "Kanselleer", color = LssTextMuted)
                }
            },
            containerColor = LssNavyCard
        )
    }

    // ==================== 2. PREVIEW DIALOG FOR PROGRAM CSV ====================
    if (parsedProgramPreview != null) {
        val previewProg = parsedProgramPreview!!
        AlertDialog(
            onDismissRequest = { parsedProgramPreview = null },
            title = {
                Text(
                    text = if (isEnglish) "Confirm Program Import" else "Bevestig Program Invoer",
                    color = LssGold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isEnglish)
                            "${previewProg.size} program items found in spreadsheet."
                        else
                            "${previewProg.size} program items in sigblad gevind.",
                        color = LssTextLight,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    previewProg.take(6).forEach { p ->
                        Text(
                            text = "• #${p.itemNo} ${p.startTime} - o/${p.ageGroup} ${p.gender} ${p.eventName} (${p.venue})",
                            color = LssTextWhite,
                            fontSize = 11.sp
                        )
                    }
                    if (previewProg.size > 6) {
                        Text(
                            text = "... en nog ${previewProg.size - 6} items",
                            color = LssTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isImportingLoading = true
                        AthleticsRepository.bulkAddProgram(previewProg) { count ->
                            isImportingLoading = false
                            statusMessage = if (isEnglish)
                                "$count program items imported successfully!"
                            else
                                "$count program items suksesvol ingevoer!"
                            parsedProgramPreview = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader),
                    enabled = !isImportingLoading
                ) {
                    if (isImportingLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = LssNavyHeader)
                    } else {
                        Text(if (isEnglish) "Import All ${previewProg.size}" else "Voer Al ${previewProg.size} In", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { parsedProgramPreview = null }) {
                    Text(if (isEnglish) "Cancel" else "Kanselleer", color = LssTextMuted)
                }
            },
            containerColor = LssNavyCard
        )
    }

    // ==================== 3. PREVIEW DIALOG FOR RECORDS CSV ====================
    if (parsedRecordsPreview != null) {
        val previewRec = parsedRecordsPreview!!
        AlertDialog(
            onDismissRequest = { parsedRecordsPreview = null },
            title = {
                Text(
                    text = if (isEnglish) "Confirm Records Import" else "Bevestig Rekords Invoer",
                    color = LssGold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isEnglish)
                            "${previewRec.size} records found in spreadsheet."
                        else
                            "${previewRec.size} rekords in sigblad gevind.",
                        color = LssTextLight,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    previewRec.take(6).forEach { r ->
                        Text(
                            text = "• ${r.eventName} o/${r.ageGroup} ${r.gender}: ${r.recordValue} (${r.holder}, ${r.year})",
                            color = LssTextWhite,
                            fontSize = 11.sp
                        )
                    }
                    if (previewRec.size > 6) {
                        Text(
                            text = "... en nog ${previewRec.size - 6} rekords",
                            color = LssTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isImportingLoading = true
                        AthleticsRepository.bulkAddRecords(previewRec) { count ->
                            isImportingLoading = false
                            statusMessage = if (isEnglish)
                                "$count records imported successfully!"
                            else
                                "$count rekords suksesvol ingevoer!"
                            parsedRecordsPreview = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader),
                    enabled = !isImportingLoading
                ) {
                    if (isImportingLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = LssNavyHeader)
                    } else {
                        Text(if (isEnglish) "Import All ${previewRec.size}" else "Voer Al ${previewRec.size} In", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { parsedRecordsPreview = null }) {
                    Text(if (isEnglish) "Cancel" else "Kanselleer", color = LssTextMuted)
                }
            },
            containerColor = LssNavyCard
        )
    }

    // ==================== PASTE TEXT DIALOG ====================
    if (showPasteDialog) {
        val titleText = when (pasteDialogType) {
            "program" -> if (isEnglish) "Paste Program Rows" else "Plak Program Rye"
            "records" -> if (isEnglish) "Paste Records Rows" else "Plak Rekord Rye"
            else -> if (isEnglish) "Paste Athlete Rows" else "Plak Atlete Rye"
        }

        val placeholderText = when (pasteDialogType) {
            "program" -> "1\t08:00\t13\tSeuns\t100m\tHoofbaan\tWagtend\n2\t08:15\t13\tDogters\t100m\tHoofbaan\tWagtend"
            "records" -> "100m\t13\tSeuns\t11.42s\tLiam van der Merwe\t2023\nHigh Jump\t12\tDogters\t1.45m\tAnika Kotze\t2022"
            else -> "13\tLiam\tvan der Merwe\tLaerskool Swartland\tSeuns\t100m;Long Jump\n12\tAnri\tSmit\tDirkie Uys\tDogters\t80m;High Jump"
        }

        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = {
                Text(
                    text = titleText,
                    color = LssGold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isEnglish)
                            "Copy rows directly from Excel or Google Sheets and paste below."
                        else
                            "Kopieer rye direk uit Excel of Google Sigblaaie en plak hieronder.",
                        color = LssTextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pastedContent,
                        onValueChange = { pastedContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = {
                            Text(placeholderText, fontSize = 11.sp)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LssGold,
                            unfocusedBorderColor = LssNavyCardLight
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (pasteDialogType) {
                            "athletes" -> {
                                val found = CsvManager.parseAthletesCsv(pastedContent, fallbackGender = importDefaultGender)
                                if (found.isNotEmpty()) {
                                    showPasteDialog = false
                                    parsedAthletesPreview = found
                                    pastedContent = ""
                                } else {
                                    statusMessage = if (isEnglish) "Could not parse athletes from pasted text." else "Kon nie atlete uit geplakte teks lees nie."
                                }
                            }
                            "program" -> {
                                val found = CsvManager.parseProgramCsv(pastedContent)
                                if (found.isNotEmpty()) {
                                    showPasteDialog = false
                                    parsedProgramPreview = found
                                    pastedContent = ""
                                } else {
                                    statusMessage = if (isEnglish) "Could not parse program from pasted text." else "Kon nie program uit geplakte teks lees nie."
                                }
                            }
                            "records" -> {
                                val found = CsvManager.parseRecordsCsv(pastedContent)
                                if (found.isNotEmpty()) {
                                    showPasteDialog = false
                                    parsedRecordsPreview = found
                                    pastedContent = ""
                                } else {
                                    statusMessage = if (isEnglish) "Could not parse records from pasted text." else "Kon nie rekords uit geplakte teks lees nie."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                ) {
                    Text(if (isEnglish) "Parse & Preview" else "Verwerk & Bekyk", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) {
                    Text(if (isEnglish) "Cancel" else "Kanselleer", color = LssTextMuted)
                }
            },
            containerColor = LssNavyCard
        )
    }

    // ==================== FULL SCREEN DIGITAL ENTRY SHEET DIALOG (POP-UP WINDOW) ====================
    if (showDigitalSheetDialog) {
        Dialog(
            onDismissRequest = { showDigitalSheetDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                shape = RoundedCornerShape(14.dp),
                color = LssNavyDark,
                border = BorderStroke(2.dp, LssGold)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(LssGold, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = LssNavyHeader, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isEnglish) "✍️ Interactive Athlete Entry Sheet" else "✍️ Interaktiewe Inskrywingsrooster",
                                    color = LssGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = if (isEnglish)
                                        "Tick checkboxes under each event. Add more athletes with one tap!"
                                    else
                                        "Merk blokkies onder elke item met 'n regmerkie. Geen CSV nodig nie!",
                                    color = LssTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(onClick = { showDigitalSheetDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Maak toe", tint = LssGold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Toolbar (School for all, Paste names, Add athlete)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = digitalDefaultSchool,
                            onValueChange = { digitalDefaultSchool = it },
                            label = { Text(if (isEnglish) "School for All" else "Skool vir Almal", fontSize = 10.sp) },
                            modifier = Modifier.weight(1.2f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LssGold,
                                unfocusedBorderColor = LssNavyCardLight
                            )
                        )
                        Button(
                            onClick = {
                                if (digitalDefaultSchool.isNotBlank()) {
                                    digitalRows = digitalRows.map { it.copy(school = digitalDefaultSchool) }
                                    statusMessage = if (isEnglish) "Applied '$digitalDefaultSchool' to all rows." else "'$digitalDefaultSchool' toegepas op alle rye."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LssNavyCardLight, contentColor = LssGold),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isEnglish) "Apply All" else "Pas Almal Toe", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { showQuickNamePasteDialog = true },
                            border = BorderStroke(1.dp, LssGold),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = LssGold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isEnglish) "Paste Names" else "Plak Name", fontSize = 10.sp, color = LssGold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scrollable Table (Takes flexible weight)
                    val dlgHScroll = rememberScrollState()
                    val dlgVScroll = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, LssNavyCardLight, RoundedCornerShape(8.dp))
                            .horizontalScroll(dlgHScroll)
                            .verticalScroll(dlgVScroll)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Header Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(LssNavyHeader, RoundedCornerShape(4.dp))
                                    .padding(vertical = 8.dp)
                            ) {
                                Text("#", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(30.dp))
                                Text(if (isEnglish) "First Name" else "Voornaam", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(110.dp))
                                Text(if (isEnglish) "Surname" else "Van", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(110.dp))
                                Text(if (isEnglish) "School" else "Skool", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(120.dp))
                                Text(if (isEnglish) "Gender" else "Geslag", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(75.dp))
                                Text(if (isEnglish) "Age" else "Oud", color = LssGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(55.dp))

                                // Event Columns Headers
                                AppConstants.ENTRY_SHEET_DISCIPLINES.forEach { disc ->
                                    val disp = when (disc) {
                                        "4x100m Relay" -> "4x100m"
                                        "Long Jump" -> "Verspring"
                                        "High Jump" -> "Hoogspring"
                                        "Shot Put" -> "Gewigstoot"
                                        "Discus" -> "Skyfwerp"
                                        "Javelin" -> "Spiesgooi"
                                        "70m Hurdles" -> "70mH"
                                        "75m Hurdles" -> "75mH"
                                        "80m Hurdles" -> "80mH"
                                        "150m Hurdles" -> "150mH"
                                        "200m Hurdles" -> "200mH"
                                        "1500m Walk" -> "1500m S"
                                        else -> disc
                                    }
                                    val isTrack = disc in AppConstants.TRACK_EVENTS
                                    val isField = disc in AppConstants.FIELD_EVENTS
                                    val isHurdle = disc in AppConstants.HURDLE_EVENTS
                                    val headerColor = if (isTrack) LssGold else if (isField) androidx.compose.ui.graphics.Color(0xFF60A5FA) else if (isHurdle) androidx.compose.ui.graphics.Color(0xFFC084FC) else androidx.compose.ui.graphics.Color(0xFF34D399)

                                    Box(
                                        modifier = Modifier.width(62.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = disp,
                                            color = headerColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(40.dp))
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            digitalRows.forEachIndexed { index, row ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text("${index + 1}", color = LssTextMuted, fontSize = 11.sp, modifier = Modifier.width(30.dp))

                                    OutlinedTextField(
                                        value = row.firstName,
                                        onValueChange = { newVal ->
                                            digitalRows = digitalRows.mapIndexed { i, r -> if (i == index) r.copy(firstName = newVal) else r }
                                        },
                                        modifier = Modifier.width(105.dp),
                                        placeholder = { Text("Naam", fontSize = 10.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LssGold)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))

                                    OutlinedTextField(
                                        value = row.lastName,
                                        onValueChange = { newVal ->
                                            digitalRows = digitalRows.mapIndexed { i, r -> if (i == index) r.copy(lastName = newVal) else r }
                                        },
                                        modifier = Modifier.width(105.dp),
                                        placeholder = { Text("Van", fontSize = 10.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LssGold)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))

                                    OutlinedTextField(
                                        value = row.school,
                                        onValueChange = { newVal ->
                                            digitalRows = digitalRows.mapIndexed { i, r -> if (i == index) r.copy(school = newVal) else r }
                                        },
                                        modifier = Modifier.width(115.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LssGold)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))

                                    // Gender toggle chip
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (row.gender == "Seuns") LssNavyHeader else LssGold.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, LssGold.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .width(70.dp)
                                            .clickable {
                                                val nextG = if (row.gender == "Seuns") "Dogters" else "Seuns"
                                                digitalRows = digitalRows.mapIndexed { i, r -> if (i == index) r.copy(gender = nextG) else r }
                                            }
                                    ) {
                                        Text(
                                            text = if (row.gender == "Seuns") (if (isEnglish) "Boys" else "Seuns") else (if (isEnglish) "Girls" else "Dogters"),
                                            color = LssGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(5.dp))

                                    // Age toggle
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = LssNavyHeader,
                                        border = BorderStroke(1.dp, LssGold.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .width(50.dp)
                                            .clickable {
                                                val curAge = row.ageGroup.toIntOrNull() ?: 13
                                                val nextAge = if (curAge >= 13) 7 else curAge + 1
                                                digitalRows = digitalRows.mapIndexed { i, r -> if (i == index) r.copy(ageGroup = nextAge.toString()) else r }
                                            }
                                    ) {
                                        Text(
                                            text = "o/${row.ageGroup}",
                                            color = LssTextLight,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(5.dp))

                                    // Event Checkboxes
                                    AppConstants.ENTRY_SHEET_DISCIPLINES.forEach { disc ->
                                        val isChecked = row.events.contains(disc)
                                        Box(
                                            modifier = Modifier
                                                .width(62.dp)
                                                .background(if (isChecked) LssGold.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
                                                .clickable {
                                                    val updatedEvents = row.events.toMutableSet()
                                                    if (isChecked) updatedEvents.remove(disc) else updatedEvents.add(disc)
                                                    digitalRows = digitalRows.mapIndexed { i, r -> if (i == index) r.copy(events = updatedEvents) else r }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    val updatedEvents = row.events.toMutableSet()
                                                    if (checked) updatedEvents.add(disc) else updatedEvents.remove(disc)
                                                    digitalRows = digitalRows.mapIndexed { i, r -> if (i == index) r.copy(events = updatedEvents) else r }
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = LssGold,
                                                    checkmarkColor = LssNavyHeader,
                                                    uncheckedColor = LssTextMuted
                                                )
                                            )
                                        }
                                    }

                                    // Delete row button
                                    IconButton(
                                        onClick = {
                                            if (digitalRows.size > 1) {
                                                digitalRows = digitalRows.filterIndexed { i, _ -> i != index }
                                            } else {
                                                digitalRows = listOf(DigitalAthleteRow(school = digitalDefaultSchool))
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Skrap", tint = LssRedError, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dialog Bottom Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                digitalRows = digitalRows + DigitalAthleteRow(school = digitalDefaultSchool, gender = "Seuns", ageGroup = "13")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LssNavyCardLight, contentColor = LssGold)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEnglish) "Add Athlete Row" else "Voeg Atleet Ry By", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        val totalCheckedItems = digitalRows.sumOf { it.events.size }
                        Text(
                            text = "${digitalRows.size} ${if (isEnglish) "athletes" else "atlete"} • $totalCheckedItems items",
                            color = LssGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { showDigitalSheetDialog = false }) {
                                Text(if (isEnglish) "Close" else "Maak Toe", color = LssTextMuted)
                            }

                            Button(
                                onClick = {
                                    val validAthletes = digitalRows.filter { it.firstName.isNotBlank() && it.lastName.isNotBlank() }
                                    if (validAthletes.isNotEmpty()) {
                                        val toSave = validAthletes.map { r ->
                                            Athlete(
                                                id = "",
                                                firstName = r.firstName.trim(),
                                                lastName = r.lastName.trim(),
                                                school = r.school.trim().ifBlank { digitalDefaultSchool },
                                                gender = r.gender,
                                                ageGroup = r.ageGroup,
                                                events = r.events.toList().ifEmpty { listOf("100m") }
                                            )
                                        }
                                        AthleticsRepository.bulkAddAthletes(toSave) { count ->
                                            statusMessage = if (isEnglish) "$count athletes saved to database!" else "$count atlete suksesvol in databasis gestoor!"
                                        }
                                        statusMessage = if (isEnglish) "${toSave.size} athletes saved to database!" else "${toSave.size} atlete suksesvol gestoor!"
                                        showDigitalSheetDialog = false
                                        selectedTab = 0
                                    } else {
                                        statusMessage = if (isEnglish) "Fill in First Name and Surname for at least one athlete." else "Vul asseblief Voornaam en Van in vir ten minste een atleet."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isEnglish) "Save All to Meet" else "Stoor na Byeenkoms", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // ==================== QUICK NAME PASTE HELPER DIALOG ====================
    if (showQuickNamePasteDialog) {
        AlertDialog(
            onDismissRequest = { showQuickNamePasteDialog = false },
            title = {
                Text(
                    text = if (isEnglish) "📋 Quick Paste Athlete Names" else "📋 Plak Lys van Atleetname",
                    color = LssGold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isEnglish)
                            "Paste names from Word or Excel (one per line, e.g. 'Pieter Smit'). Rows will be created automatically:"
                        else
                            "Plak name uit Word of Excel (een per ry, bv. 'Pieter Smit'). Rye sal outomaties geskep word:",
                        color = LssTextMuted,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = quickNamePasteContent,
                        onValueChange = { quickNamePasteContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = {
                            Text("Pieter Smit\nJohan Botha\nAnri Louw\nKarel Visser", fontSize = 12.sp)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LssGold,
                            unfocusedBorderColor = LssNavyCardLight
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lines = quickNamePasteContent.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                        if (lines.isNotEmpty()) {
                            val newRows = lines.map { line ->
                                val parts = line.split("\\s+".toRegex())
                                val fn = parts.firstOrNull() ?: ""
                                val ln = parts.drop(1).joinToString(" ")
                                DigitalAthleteRow(
                                    firstName = fn,
                                    lastName = ln,
                                    school = digitalDefaultSchool,
                                    gender = "Seuns",
                                    ageGroup = "13",
                                    events = mutableSetOf()
                                )
                            }
                            digitalRows = digitalRows + newRows
                            quickNamePasteContent = ""
                            showQuickNamePasteDialog = false
                            statusMessage = if (isEnglish) "${lines.size} athlete rows added! Now tick their events." else "${lines.size} atlete bygevoeg! Merk nou hul items."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                ) {
                    Text(if (isEnglish) "Add Rows" else "Voeg Rye By", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickNamePasteDialog = false }) {
                    Text(if (isEnglish) "Cancel" else "Kanselleer", color = LssTextMuted)
                }
            },
            containerColor = LssNavyCard
        )
    }
}
