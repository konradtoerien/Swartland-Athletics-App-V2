package com.example.swartlandathletics.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                text = { Text(if (isEnglish) "Athletes (${athletes.size})" else "Atlete (${athletes.size})", fontSize = 12.sp, maxLines = 1) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(if (isEnglish) "Add Athlete" else "Nuwe Atleet", fontSize = 12.sp, maxLines = 1) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(if (isEnglish) "Excel / CSV" else "Sigblaaie & Data", fontSize = 12.sp, maxLines = 1) }
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
                // ==================== TAB 0: ATHLETES LIST ====================
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

            1 -> {
                // ==================== TAB 1: ADD SINGLE ATHLETE ====================
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
                                    selectedTab = 0
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

            2 -> {
                // ==================== TAB 2: EXCEL / CSV BULK IMPORT & EXPORT ====================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // CARD 1: ATHLETE ENTRY SPREADSHEET (IMAGE 1: MATRIX WITH 'X' TICKS)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = LssGold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "1. School Athlete Entry Spreadsheets (Image 1)" else "1. Atlete Inskrywingsdokument vir Skole (Image 1)",
                                        color = LssGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isEnglish)
                                        "Matches your official Inskrywingsdokument: Teachers enter Age, First Name, Last Name, School, and tick their events with an 'x' at the top. Once received, upload the CSV here to register all athletes and their events automatically."
                                    else
                                        "Stem ooreen met jou amptelike Inskrywingsdokument: Onderwysers tik Ouderdom, Voornaam, Van, Skool in en merk die items met 'n 'x' bo-aan. Laai die voltooide CSV hier op om alle atlete en hul items outomaties te registreer.",
                                    color = LssTextMuted,
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isEnglish) "Share Blank Spreadsheet Templates to Schools:" else "Deel Leë Sigblad-Sjablone aan Skole:",
                                    color = LssTextLight,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val template = CsvManager.generateAthleteMatrixTemplateCsv("Seuns")
                                            CsvManager.shareContent(
                                                context = context,
                                                title = "Swartland_Inskrywingsdokument_Seuns.csv",
                                                content = template
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Boys Template" else "Seuns Sjabloon", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val template = CsvManager.generateAthleteMatrixTemplateCsv("Dogters")
                                            CsvManager.shareContent(
                                                context = context,
                                                title = "Swartland_Inskrywingsdokument_Dogters.csv",
                                                content = template
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssGold, contentColor = LssNavyHeader)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Girls Template" else "Dogters Sjabloon", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        val template = CsvManager.generateCombinedAthleteMatrixTemplateCsv()
                                        CsvManager.shareContent(
                                            context = context,
                                            title = "Swartland_Inskrywingsdokument_Gekombineerd.csv",
                                            content = template
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LssGold)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isEnglish) "Combined Template (Boys & Girls)" else "Gekombineerde Sjabloon (Seuns & Dogters)", fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = LssNavyCardLight)
                                Spacer(modifier = Modifier.height(10.dp))

                                // Gender fallback selector for import
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (isEnglish) "Default gender if not specified:" else "Verstek geslag indien onvermeld:",
                                        color = LssTextMuted,
                                        fontSize = 11.sp
                                    )
                                    listOf("Seuns", "Dogters").forEach { g ->
                                        FilterChip(
                                            selected = importDefaultGender == g,
                                            onClick = { importDefaultGender = g },
                                            label = { Text(g, fontSize = 10.sp) },
                                            modifier = Modifier.height(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            currentFilePickerMode = "athletes"
                                            filePickerLauncher.launch("*/*")
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssGreenSuccess, contentColor = LssNavyHeader)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isEnglish) "Import Athletes CSV" else "Voer Atlete CSV In", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            pasteDialogType = "athletes"
                                            showPasteDialog = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LssGold)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isEnglish) "Paste Athletes" else "Plak Atlete Teks", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        val csvData = CsvManager.exportAthletesToMatrixCsv(athletes)
                                        CsvManager.shareContent(
                                            context = context,
                                            title = "Swartland_Geregistreerde_Atlete_Matriks.csv",
                                            content = csvData
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LssTextLight)
                                ) {
                                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "Export Registered Athletes (${athletes.size}) to Matrix CSV" else "Voer Huidige Atlete (${athletes.size}) Uit na Matriks-CSV",
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

                    // CARD 4: RESET & CLEANUP OPTIONS
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LssNavyCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (isEnglish) "4. Reset Data & Start Anew" else "4. Vee Data Uit vir Nuwe Byeenkoms",
                                    color = LssGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isEnglish)
                                        "Permanently remove athletes, program, or records. The database stays 100% blank with zero auto-regeneration."
                                    else
                                        "Verwyder atlete, program of rekords permanent. Die databasis bly 100% skoon sonder dat enige voorbeelddata vanself terugkom.",
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
                                            AthleticsRepository.clearAllAthletes()
                                            statusMessage = if (isEnglish) "All athletes cleared." else "Alle atlete verwyder uit databasis."
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssRedError.copy(alpha = 0.85f), contentColor = LssTextWhite)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Clear Athletes" else "Wis Atlete Uit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            AthleticsRepository.clearAllProgram()
                                            statusMessage = if (isEnglish) "Program cleared." else "Program items verwyder."
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssRedError.copy(alpha = 0.85f), contentColor = LssTextWhite)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Clear Program" else "Wis Program Uit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            AthleticsRepository.clearAllRecords()
                                            statusMessage = if (isEnglish) "Records cleared." else "Rekords verwyder."
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssRedError.copy(alpha = 0.85f), contentColor = LssTextWhite)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Clear Records" else "Wis Rekords Uit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                        Text(if (isEnglish) "Reset All Data" else "Vee Alles Uit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        AthleticsRepository.seedInitialData()
                                        statusMessage = if (isEnglish) "Initial sample data loaded!" else "Byeenkomsdata herlaai!"
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LssGold)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isEnglish) "Optional: Load Sample Data" else "Opsioneel: Herlaai Voorbeelddata", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
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
}
