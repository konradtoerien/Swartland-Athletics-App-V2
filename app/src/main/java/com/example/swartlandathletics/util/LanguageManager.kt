package com.example.swartlandathletics.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val code: String, val label: String, val flag: String) {
    AFRIKAANS("af", "Afrikaans", "🇿🇦"),
    ENGLISH("en", "English", "🇬🇧")
}

object LanguageManager {
    private val _currentLanguage = MutableStateFlow(AppLanguage.AFRIKAANS)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
    }

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == AppLanguage.AFRIKAANS) {
            AppLanguage.ENGLISH
        } else {
            AppLanguage.AFRIKAANS
        }
    }

    // Helper accessor
    val isEnglish: Boolean get() = _currentLanguage.value == AppLanguage.ENGLISH
}

object L10n {
    fun t(afrikaans: String, english: String, lang: AppLanguage = LanguageManager.currentLanguage.value): String {
        return if (lang == AppLanguage.ENGLISH) english else afrikaans
    }

    // Common
    fun back(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Back" else "Terug"
    fun save(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Save" else "Stoor"
    fun cancel(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Cancel" else "Kanselleer"
    fun search(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Search..." else "Soek..."
    fun all(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "All" else "Alle"
    fun boys(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Boys" else "Seuns"
    fun girls(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Girls" else "Dogters"
    fun gender(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Gender" else "Geslag"
    fun ageGroup(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Age Group" else "Ouderdom"
    fun school(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "School" else "Skool"
    fun event(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Event" else "Item"
    fun place(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Place" else "Plek"
    fun points(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Points" else "Punte"
    fun pointsCount(count: Int, l: AppLanguage) = if (l == AppLanguage.ENGLISH) "$count points" else "$count punte"
    fun gold(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Gold" else "Goud"
    fun silver(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Silver" else "Silwer"
    fun bronze(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Bronze" else "Brons"
    fun inProgress(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "● IN PROGRESS" else "● BESIG"
    fun finished(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "✓ FINISHED" else "✓ KLAAR"
    fun pending(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "PENDING" else "WAGTEND"
    fun synced(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Synced (Firestore)" else "Gesinkroniseer (Firestore)"
    fun signOut(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Sign out" else "Teken uit"

    // Dashboard
    fun meetDashboardTitle(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Live Athletics Meet Dashboard" else "Regstreekse Byeenkoms Dashboard"
    fun meetProgress(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Meet Progress:" else "Byeenkoms Vordering:"
    fun itemsCompleted(done: Int, total: Int, pct: Int, l: AppLanguage) =
        if (l == AppLanguage.ENGLISH) "$done of $total items completed ($pct%)" else "$done van $total items voltooi ($pct%)"
    fun liveBadge(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "LIVE" else "INTYDS"
    fun meetStatsTitle(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Meet Statistics" else "Byeenkoms Statistieke"
    fun athletesEntered(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Athletes Entered" else "Atlete Ingeskryf"
    fun itemsDone(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Items Completed" else "Items Afgehandel"
    fun establishedRecords(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Established Records" else "Gevestigde Rekords"
    fun resultsRecorded(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Results Recorded" else "Uitslae Aangeteken"
    fun medalsAwarded(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Medals Awarded" else "Medaljes Toegeken"
    fun participatingSchools(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Participating Schools" else "Deelnemende Skole"
    fun nowOnTrackField(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Now on Track & Field" else "Tans op die Baan & Veld"
    fun nextOnProgram(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Next on Program" else "Volgende Items op Program"
    fun viewProgram(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "View Program →" else "Sien Program →"
    fun noActiveEvents(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "No active events right now. Check program for schedule." else "Geen aktiewe items tans nie. Kyk na die program vir skedule."
    fun schoolStandings(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "🏆 School Points Standings" else "🏆 Skole Punte Ranglys"
    fun fullReport(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Full Report →" else "Volledige Verslag →"
    fun noResultsYet(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "No results recorded yet. Points will appear in real time." else "Nog geen uitslae aangeteken nie. Punte sal intyds hier verskyn."
    fun latestResults(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "⏱️ Latest Results" else "⏱️ Jongste Uitslae"
    fun liveFeed(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Live Feed →" else "Regstreekse Voer →"
    fun waitingForResults(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Waiting for first results from officials..." else "Wag vir eerste uitslae vanaf die beoordelaars..."
    fun meetModulesTitle(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Meet Management & Modules" else "Byeenkoms Bestuur & Toegang"

    // Modules
    fun modSetupTitle(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "1. Setup & Entries" else "1. Setup & Inskrywings"
    fun modSetupSub(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Manage athletes and team entries" else "Bestuur atlete en inskrywings"
    fun modDrawsTitle(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "2. Draw Sheets" else "2. Lotingslyste"
    fun modDrawsSub(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Lane assignments and event sheets" else "Baanindelings en posisies"
    fun modOfficiatingTitle(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "3. Officiating" else "3. Beoordeling"
    fun modOfficiatingSub(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Record times, heights and distances" else "Teken tye, hoogtes en afstande aan"
    fun modRecordsTitle(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "4. Records" else "4. Rekords"
    fun modRecordsSub(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Official meet records & new marks" else "Amptelike rekords en prestasies"
    fun modResultsTitle(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "5. Live Results" else "5. Regstreekse Uitslae"
    fun modResultsSub(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Full real-time results for spectators" else "Volledige intydse uitslae"
    fun modMedalsTitle(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "6. Medals" else "6. Medaljes"
    fun modMedalsSub(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Top 4 prize-giving & medal ceremonies" else "Top 4 prysdeling en seremonies"
    fun modReportsTitle(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "7. Reports" else "7. Verslae"
    fun modReportsSub(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Total points and Victor Ludorum" else "Totale punte en Victor Ludorum"
    fun modProgramTitle(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "8. Program" else "8. Program"
    fun modProgramSub(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Timetable and live track statuses" else "Item-rooster en baanstatusse"

    // Navigation items
    fun navHome(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Dashboard" else "Dashboard"
    fun navDraws(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Draws" else "Lotings"
    fun navOfficiating(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Officiate" else "Beoordeel"
    fun navResults(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Results" else "Uitslae"
    fun navMedals(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Medals" else "Medaljes"

    // Role & Access Control
    fun roleSpectator(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Spectator (View Only)" else "Toeskouer (Slegs-Kyk)"
    fun roleOfficial(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Official (Full Access)" else "Beampte (Volle Toegang)"
    fun roleSwitchToOfficial(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Switch to Official Mode" else "Skakel oor na Beampte Modus"
    fun roleSwitchToSpectator(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Lock to Spectator Mode" else "Sluit as Toeskouer Modus"
    fun roleEnterPinPrompt(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Enter Official 4-digit PIN:" else "Voer Beampte 4-syfer PIN in:"
    fun rolePinIncorrect(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Incorrect PIN. Please try again." else "Verkeerde PIN. Probeer asseblief weer."
    fun spectatorNotice(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "👀 Spectator Mode: Live results are view-only. Organizers tap to unlock." else "👀 Toeskouer Modus: Uitslae is slegs-kyk. Beoordelaars tik om te ontsluit."
    fun shareWhatsAppLink(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Share Live Web Link (WhatsApp)" else "Deel Lewendige Web-Skakel (WhatsApp)"
    fun webDashboardDesc(l: AppLanguage) = if (l == AppLanguage.ENGLISH) "Anyone with this link (iPhone, Android, PC) can follow results live in any browser without editing permissions." else "Enigeen met hierdie skakel (iPhone, Android, PC) kan uitslae intyds in hul webblaaier volg sonder wysigingsregte."
}
