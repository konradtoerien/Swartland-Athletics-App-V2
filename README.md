# Swartland Atletiek Portaal (Android Application)

'n Moderne Android-toepassing gebou met **Kotlin** en **Jetpack Compose (Material 3)** vir Laerskool Swartland se inter-skole atletiekbyeenkomsbestuur en intydse uitslae.

## 🏆 Kernkenmerke & Modules

1. **📋 Setup & Inskrywings:**
   - Bestuur geregistreerde atlete volgens skool, geslag en ouderdomsgroep (O/6 tot O/13).
   - Handmatige registrasievorm met outomatiese dissipline-filtrasie volgens geslag en ouderdom.
   - Herstel of reset van byeenkomsdata met voorafgelaaide mededingende skole (Laerskool Swartland, Laerskool Vredenburg, Laerskool Malmesbury, Weskus Skool, Dirkie Uys).

2. **📄 Lotingslyste (Draw Sheets):**
   - Amptelike baanindelings en posisies per item, geslag en ouderdom.
   - Snelkoppeling na die Beoordelingsmodule vir die gekose item.
   - Vertoon reeds gefinaliseerde uitslae indien die item afgehandel is.

3. **⏱️ Beoordeling (Officiating):**
   - **Baanitems (Track):** Dinamiese Heats (1 tot 10), invoer van tye vir plekke 1–6, outomatiese berekening van ASA punte en rangskikking van alle heats saam.
   - **Hoogspring (High Jump):** Aanpasbare lat-hoogte met snelknoppies (+2cm, +3cm, +5cm, -5cm), aantekening van pogings (O / X / P) en outomatiese bepaling van beste geslaagde hoogte.
   - **Velditems (Field):** Invoer van beste afstande (m), outomatiese ASA-punteberekening en rangskikking.
   - Vertoon huidige amptelike rekord ter verwysing.

4. **🏆 Rekords:**
   - Bekyk alle amptelike byeenkomsrekords met filters vir geslag en ouderdom.
   - Voeg nuwe rekords by met rekordhouer, jaar en prestasie.
   - Skrap of wysig rekords.

5. **📊 Regstreekse Uitslae (Live Feed):**
   - Intydse stroom van alle gepubliseerde uitslae chronologies.
   - Filter volgens skool en ouderdomsgroep.
   - Goue, silwer en brons kentekens met ASA punte.

6. **🥇 Medaljetafel & Prysdeling:**
   - Outomatiese groepering van voltooide items met die Top 4 atlete vir medalje-oorhandiging.
   - Merk prysdeling as voltooi met visuele vorderingstatus.

7. **🖨️ Verslae & Puntetabelle:**
   - **Totale Spanpunte:** Ranglys van alle deelnemende skole volgens ASA punte.
   - **Victor & Victrix Ludorum:** Ranglys van top individuele atlete (afsonderlik vir seuns en dogters).
   - **Opsomming:** Totale items, skole en medaljes.

8. **📅 Program & Tydrooster:**
   - Volledige program volgens itemnommer en begintyd.
   - Interaktiewe statusverandering (*Wagtend* ➔ *Besig* ➔ *Voltooi*).
   - Voeg nuwe program-items by met baan/lokaal.

## 🎨 Tema & Ontwerp

- Gebou met Laerskool Swartland se amptelike vlootblou (`#0A192F`, `#020C1B`, `#112240`) en goud (`#F4C430`) ontwerptema.
- Volledige ondersteuning vir Android Edge-to-Edge (`enableEdgeToEdge()`) en responsiewe insets.
- Toeganklike 48dp+ raakareas en unieke Compose testTags vir outomatisering.
