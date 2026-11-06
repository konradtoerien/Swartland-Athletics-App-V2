package com.example.swartlandathletics.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swartlandathletics.data.AthleticsRepository
import com.example.swartlandathletics.model.AppConstants
import com.example.swartlandathletics.model.ProgramItem
import com.example.swartlandathletics.ui.theme.*
import com.example.swartlandathletics.util.AppLanguage
import com.example.swartlandathletics.util.CsvManager
import com.example.swartlandathletics.util.L10n
import com.example.swartlandathletics.util.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language by LanguageManager.currentLanguage.collectAsState()
    val isEnglish = language == AppLanguage.ENGLISH

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

    val context = LocalContext.current
    var parsedProgramPreview by remember { mutableStateOf<List<ProgramItem>?>(null) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var pastedContent by remember { mutableStateOf("") }
    var isImportingLoading by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                val found = CsvManager.parseProgramCsv(content)
                if (found.isNotEmpty()) {
                    parsedProgramPreview = found
                } else {
                    message = if (isEnglish) "No valid program items found." else "Geen geldige program items gevind nie."
                }
            } catch (e: Exception) {
                message = "Fout met lees: ${e.localizedMessage}"
            }
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
                    text = if (isEnglish) "📅 Meet Program & Timetable" else "📅 Byeenkoms Program & Tydrooster",
                    color = LssGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_program")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = L10n.back(language), tint = LssGold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = LssNavyHeader)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = LssNavyCard,
            contentColor = LssGold
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(if (isEnglish) "Timetable (${program.size})" else "Tydrooster (${program.size})", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(if (isEnglish) "Add Item" else "Voeg Item By", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(if (isEnglish) "Excel / CSV" else "Sigblad / CSV", fontSize = 12.sp) }
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

            2 -> {
                // ==================== TAB 2: EXCEL / CSV BULK PROGRAM ====================
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = LssGold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "Official Program Document (Image 2)" else "Amptelike Programdokument (Image 2)",
                                        color = LssGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isEnglish)
                                        "Manage the full timetable in Excel. Columns match Image 2: Item No, Begin Tyd, Ouderdom, Geslag, Item, Plek, Status."
                                    else
                                        "Bestuur die hele tydrooster in Excel. Kolomme stem ooreen met Image 2: Item No, Begin Tyd, Ouderdom, Geslag, Item, Plek, Status.",
                                    color = LssTextMuted,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val template = CsvManager.generateProgramTemplateCsv()
                                            CsvManager.shareContent(
                                                context = context,
                                                title = "Swartland_Program_Sjabloon.csv",
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
                                            val csv = CsvManager.exportProgramToCsv(program)
                                            CsvManager.shareContent(
                                                context = context,
                                                title = "Swartland_Program_Huidig.csv",
                                                content = csv
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

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { filePickerLauncher.launch("*/*") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = LssGreenSuccess, contentColor = LssNavyHeader)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Import Program CSV" else "Voer Program CSV In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { showPasteDialog = true },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LssGold)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEnglish) "Paste Text" else "Plak Teks", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = LssNavyCardLight)
                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        AthleticsRepository.clearAllProgram()
                                        message = if (isEnglish) "Program items cleared." else "Program items skoongemaak."
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = LssRedError.copy(alpha = 0.85f), contentColor = LssTextWhite)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isEnglish) "Clear Entire Program" else "Wis Alle Program Items Uit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==================== PREVIEW CONFIRMATION DIALOG ====================
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
                            "${previewProg.size} items found in program spreadsheet."
                        else
                            "${previewProg.size} items in program sigblad gevind.",
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
                            message = if (isEnglish) "$count items successfully added to program!" else "$count items suksesvol by program gevoeg!"
                            parsedProgramPreview = null
                            selectedTab = 0
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

    // ==================== PASTE TEXT DIALOG ====================
    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = {
                Text(
                    text = if (isEnglish) "Paste Program Rows" else "Plak Program Rye",
                    color = LssGold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isEnglish)
                            "Copy rows from Excel or Google Sheets and paste below. Expected: Item No, Time, Age, Gender, Event, Venue, Status"
                        else
                            "Kopieer rye uit Excel of Google Sigblaaie en plak hieronder. Verwag: Item No, Tyd, Ouderdom, Geslag, Item, Plek, Status",
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
                            Text("1\t08:00\t13\tSeuns\t100m\tHoofbaan\tWagtend\n2\t08:15\t13\tDogters\t100m\tHoofbaan\tWagtend", fontSize = 11.sp)
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
                        val found = CsvManager.parseProgramCsv(pastedContent)
                        if (found.isNotEmpty()) {
                            showPasteDialog = false
                            parsedProgramPreview = found
                            pastedContent = ""
                        } else {
                            message = if (isEnglish) "Could not parse program from text." else "Kon nie program uit teks lees nie."
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
