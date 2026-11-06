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
import com.example.swartlandathletics.model.EventRecord
import com.example.swartlandathletics.ui.theme.*
import com.example.swartlandathletics.util.AppLanguage
import com.example.swartlandathletics.util.CsvManager
import com.example.swartlandathletics.util.L10n
import com.example.swartlandathletics.util.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by LanguageManager.currentLanguage.collectAsState()
    val isEnglish = language == AppLanguage.ENGLISH

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

    // CSV Bulk States
    var parsedRecordsPreview by remember { mutableStateOf<List<EventRecord>?>(null) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var pastedContent by remember { mutableStateOf("") }
    var isImportingLoading by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                val found = CsvManager.parseRecordsCsv(content)
                if (found.isNotEmpty()) {
                    parsedRecordsPreview = found
                } else {
                    message = if (isEnglish) "No valid records found in file." else "Geen geldige rekords in lêer gevind nie."
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
                    text = if (isEnglish) "🏆 Meet Records" else "🏆 Byeenkomsrekords",
                    color = LssGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_records")) {
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
                text = { Text(if (isEnglish) "Current Records (${records.size})" else "Huidige Rekords (${records.size})", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(if (isEnglish) "Add Record" else "Voeg Rekord By", fontSize = 12.sp) }
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
                                        Icon(Icons.Default.Star, contentDescription = null, tint = LssGold)
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
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stoor Rekord", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            2 -> {
                // ==================== TAB 2: EXCEL / CSV BULK RECORDS (IMAGE 3) ====================
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
                                    Icon(Icons.Default.Star, contentDescription = null, tint = LssGold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "Official Records Document (Image 3)" else "Amptelike Rekorddokument (Image 3)",
                                        color = LssGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isEnglish)
                                        "Matches your official Rekorddokument: Item, Ouderdom, Geslag, Tyd of Afstand, Skool of Atleet, Jaar (Image 3). Import meet records directly from Excel."
                                    else
                                        "Stem ooreen met jou amptelike Rekorddokument: Item, Ouderdom, Geslag, Tyd of Afstand, Skool of Atleet, Jaar (Image 3). Voer byeenkomsrekords direk vanaf Excel in.",
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
                                            val template = CsvManager.generateRecordsTemplateCsv()
                                            CsvManager.shareContent(
                                                context = context,
                                                title = "Swartland_Rekords_Sjabloon.csv",
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
                                            val csv = CsvManager.exportRecordsToCsv(records)
                                            CsvManager.shareContent(
                                                context = context,
                                                title = "Swartland_Rekords_Huidig.csv",
                                                content = csv
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
                                        Text(if (isEnglish) "Import Records CSV" else "Voer Rekords CSV In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                        AthleticsRepository.clearAllRecords()
                                        message = if (isEnglish) "Records cleared." else "Rekords skoongemaak."
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = LssRedError.copy(alpha = 0.85f), contentColor = LssTextWhite)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isEnglish) "Clear Entire Records List" else "Wis Alle Rekords Uit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==================== PREVIEW CONFIRMATION DIALOG ====================
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
                            message = if (isEnglish) "$count records successfully imported!" else "$count rekords suksesvol ingevoer!"
                            parsedRecordsPreview = null
                            selectedTab = 0
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
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = {
                Text(
                    text = if (isEnglish) "Paste Records Rows" else "Plak Rekord Rye",
                    color = LssGold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isEnglish)
                            "Copy rows from Excel or Google Sheets and paste below. Expected: Item, Age, Gender, Time/Distance, School/Athlete, Year"
                        else
                            "Kopieer rye uit Excel of Google Sigblaaie en plak hieronder. Verwag: Item, Ouderdom, Geslag, Tyd/Afstand, Skool/Atleet, Jaar",
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
                            Text("100m\t13\tSeuns\t11.42s\tLiam van der Merwe\t2023\nHigh Jump\t12\tDogters\t1.45m\tAnika Kotze\t2022", fontSize = 11.sp)
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
                        val found = CsvManager.parseRecordsCsv(pastedContent)
                        if (found.isNotEmpty()) {
                            showPasteDialog = false
                            parsedRecordsPreview = found
                            pastedContent = ""
                        } else {
                            message = if (isEnglish) "Could not parse records from text." else "Kon nie rekords uit teks lees nie."
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
