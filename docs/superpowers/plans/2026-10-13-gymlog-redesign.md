# GymLog Ridisegno interfaccia Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ridisegnare tutta l'interfaccia di GymLog in stile openGym (tema scuro, accento selezionabile, barra con Start centrale, Home con settimana, Piano, Statistiche, allenamento a tabella) senza toccare il database.

**Architecture:** Nuovo pacchetto `ui/design/` (tema + componenti condivisi) usato da tutte le schermate. Logica nuova in `domain/` (Kotlin puro, testata). ViewModel e repository esistenti estesi, non riscritti. DB Room versione 1 invariato; nuove preferenze in DataStore.

**Tech Stack:** come SP1 (Kotlin 2.1.0, AGP 8.7.3, Compose BOM 2024.12.01, Material 3, Room 2.6.1, DataStore 1.1.1, Coil 2.7.0, JUnit 4 + Robolectric). Nessuna nuova dipendenza.

**Spec:** `docs/superpowers/specs/2026-10-13-gymlog-redesign-design.md`

## Global Constraints

- Nessun codice, icona o immagine copiata da openGym (AGPL-3.0).
- Nessuna nuova dipendenza; Material Icons "Rounded" (già in `material-icons-extended`).
- Room versione 1, nessuna migrazione; nuove preferenze solo in DataStore.
- I test esistenti (83) restano verdi; comandi: `scripts/test.sh` (test JVM), `source scripts/env.sh && ./gradlew assembleDebug -q`.
- Italiano, kg, target tattili ≥ 48 dp, pulsante principale ≥ 64 dp, schede con angoli 20 dp, margini laterali 16 dp.
- Default: tema Scuro, accento Verde, schermo acceso durante l'allenamento attivo.
- Commit come `nicolosimonini3`, senza righe Co-Authored-By.

## Review Focus

1. **Aggiornamento dalla 1.0.0** (DataStore con le sole chiavi vecchie) → tema Scuro, accento Verde, schermo acceso; le impostazioni vecchie restano. Test: Task 1 `upgradeFrom100_keepsOldValuesAndNewDefaults`.
2. **Settimana a cavallo d'anno** (28 dic 2026 – 3 gen 2027) → 7 giorni corretti, pallini sulle date giuste. Test: Task 2 `weekMarkers_acrossYearBoundary`.
3. **Allenamento iniziato prima di mezzanotte** → conta nel giorno di inizio, sia per la mappa sia per le settimane consecutive. Test: Task 2 `activity_sessionAcrossMidnight_countsOnStartDay`.
4. **Assegnare un giorno in un programma** → non tocca le schede di altri programmi. Test: Task 2 `assignDay_onlyAffectsSameProgram`.
5. **Riga attiva quando tutte le serie sono fatte o l'indice selezionato non esiste più** → nessun crash; riga attiva = nessuna / prima non fatta. Test: Task 6 `activeRow_allDone_isNull`, `activeRow_invalidSelection_fallsBack`.

---

### Task 1: Impostazioni, tema e componenti di design

**Files:**
- Modify: `data/settings/SettingsRepository.kt`, `ui/theme/Theme.kt` (diventa wrapper di `ui/design/GymTheme`), `MainActivity.kt`
- Create: `ui/design/Palette.kt`, `ui/design/GymTheme.kt`, `ui/design/Components.kt`
- Test: `data/settings/SettingsRepositoryTest.kt` (estendere), `ui/design/PaletteTest.kt`

**Interfaces:**
- Produces:
  - `enum class ThemeMode(val label: String) { SYSTEM("Sistema"), LIGHT("Chiaro"), DARK("Scuro") }`
  - `enum class Accent(val label: String, val dark: Long, val light: Long)` con i 6 valori del §2 della spec (`GREEN` "Verde" 0xFF32D74B/0xFF1FA33A, `BLUE` "Blu", `PURPLE` "Viola", `ORANGE` "Arancione", `RED` "Rosso", `CYAN` "Ciano").
  - `Settings` + `themeMode: ThemeMode = DARK`, `accent: Accent = GREEN`, `keepScreenOn: Boolean = true` (chiavi DataStore `theme_mode`, `accent`, `keep_screen_on`; valore sconosciuto → default).
  - `data class GymColors(val background, card, raised, border, text, textSecondary, accent, onAccent, accentContainer, resume, error: Color, val dark: Boolean)`
  - `fun gymColors(accent: Accent, dark: Boolean): GymColors` con i valori della tabella §2 (accentContainer = accento con alpha 0.18 scuro / 0.14 chiaro; resume = `#FF9F1C`/`#D97A00`, ma `#FFD60A`/`#B38F00` se accent == ORANGE).
  - `val LocalGymColors: ProvidableCompositionLocal<GymColors>`; `object Gym { val colors: GymColors @Composable get() }`
  - `@Composable fun GymTheme(settings: Settings, content: @Composable () -> Unit)` — mappa GymColors su `darkColorScheme`/`lightColorScheme` (primary=accent, onPrimary=onAccent, background, surface=card, surfaceVariant=raised, outline=border, onSurface=text, onSurfaceVariant=textSecondary, error), tipografia del §2 (`GymType.screenTitle` 34sp ExtraBold, `cardTitle` 22sp Bold, `number` 24sp Bold tnum, `timer` 32sp, `stat` 30sp, `label` 12sp maiuscolo letterSpacing 0.8sp), forme (`extraLarge` 20dp).
  - Componenti in `Components.kt`: `GymCard(modifier, onClick: (() -> Unit)? = null, content: ColumnScope.() -> Unit)`, `SectionLabel(text)`, `ScreenHeader(title, subtitle: String? = null, action: @Composable (() -> Unit)? = null)`, `GymChip(text, selected = false, onClick: (() -> Unit)? = null)`, `AccentPill(text, icon: ImageVector? = null, onClick: (() -> Unit)? = null)`, `Stepper(value: String, label: String, onMinus, onPlus, onTapValue, modifier)`, `StatTile(icon: ImageVector, label, value, valueColor: Color? = null, modifier)`, `CircleCheck(done: Boolean, onClick)` (48 dp), `PrimaryButton(text, onClick, modifier, icon: ImageVector? = null, color: Color? = null)` (≥ 64 dp, pill), `EmptyState(icon, text, action: (@Composable () -> Unit)? = null)`.

- [ ] **Step 1: Test che falliscono:**
  - `defaults_whenEmpty` aggiornato: `Settings(2.5, EPLEY, true, true, ThemeMode.DARK, Accent.GREEN, true)`.
  - `update_persistsAppearance` — `themeMode = LIGHT`, `accent = PURPLE`, `keepScreenOn = false` → riletti.
  - `upgradeFrom100_keepsOldValuesAndNewDefaults` — DataStore pre-popolato solo con `default_load_step_kg = 5.0` e `timer_sound = false` → step 5.0, sound false, DARK, GREEN, keepScreenOn true.
  - `unknownAccent_fallsBackToGreen` — chiave `accent = "ROSA"` → GREEN.
  - `PaletteTest.darkGreen_matchesSpec` — `gymColors(GREEN, true)`: background `Color(0xFF000000)`, card `0xFF1C1C1E`, raised `0xFF2C2C2E`, accent `0xFF32D74B`, onAccent `0xFF000000`, resume `0xFFFF9F1C`.
  - `PaletteTest.lightBlue_matchesSpec` — background `0xFFF2F2F7`, card `0xFFFFFFFF`, accent `0xFF1F63E0`, onAccent `0xFFFFFFFF`.
  - `PaletteTest.orangeAccent_resumeIsYellow` — resume `0xFFFFD60A` (scuro).
- [ ] **Step 2:** `scripts/test.sh` → FAIL (compilazione).
- [ ] **Step 3:** Implementare settings, palette, tema, componenti. `MainActivity`: raccoglie `container.settings.settings` con `collectAsStateWithLifecycle(Settings())` e avvolge l'app in `GymTheme`; barre di sistema trasparenti con icone chiare/scure secondo il tema (`enableEdgeToEdge(SystemBarStyle.auto/dark)`). Rimuovere i colori dinamici.
- [ ] **Step 4:** `scripts/test.sh` → PASS; `./gradlew assembleDebug -q` → BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** — `"Tema scuro/chiaro con accento selezionabile e componenti di design"`

### Task 2: Dati e logica per Home, Piano e Statistiche

**Files:**
- Create: `domain/Stats.kt`
- Modify: `data/db/Daos.kt`, `data/db/Relations.kt`, `data/repo/WorkoutRepository.kt`, `data/repo/ProgramRepository.kt`
- Test: `domain/StatsTest.kt`, `data/repo/ProgramRepositoryTest.kt` (estendere), `data/repo/WorkoutRepositoryTest.kt` (estendere)

**Interfaces:**
- Produces:
  - `data class SessionTime(val id: Long, val title: String, val startedAt: Long, val endedAt: Long?, val setCount: Int)` (Relations.kt)
  - `WorkoutDao.observeFinishedSessionTimes(): Flow<List<SessionTime>>` (ordine `startedAt DESC`), `WorkoutDao.countWorkingSetsSince(fromMillis: Long): Flow<Int>` (serie non WARMUP di sessioni concluse con `completedAt ≥ from`), `WorkoutDao.bestWeight(exerciseId: Long): Double?` (max `weightKg`, non WARMUP, solo sessioni concluse)
  - Gli stessi esposti da `WorkoutRepository` con lo stesso nome.
  - `ProgramRepository.assignDay(programId: Long, dayOfWeek: Int, routineDayId: Long?)` — in transazione: le schede di `programId` con quel `dayOfWeek` → `null`; poi la scheda scelta → `dayOfWeek`.
  - `enum class DayMarker { DONE, PLANNED, NONE }`
  - `fun weekStart(date: LocalDate): LocalDate` (lunedì)
  - `fun weekMarkers(weekStart: LocalDate, today: LocalDate, sessionDates: Set<LocalDate>, plannedDays: Set<Int>): List<DayMarker>` (7 valori; DONE se c'è una sessione quel giorno; altrimenti PLANNED se `dayOfWeek ∈ plannedDays` e data ≥ today; altrimenti NONE)
  - `fun weekStreak(sessionDates: Set<LocalDate>, today: LocalDate): Int`
  - `fun activityLevels(sessions: List<SessionTime>, today: LocalDate, zone: ZoneId): Map<LocalDate, Int>` (solo giorni con livello > 0 negli ultimi 365 giorni; minuti sommati per data di inizio; `endedAt == null` o durata 0 → livello 1; soglie: ≤30 → 1, ≤60 → 2, ≤90 → 3, >90 → 4)
  - `fun LocalDate.fromMillis(...)`: `fun localDate(millis: Long, zone: ZoneId): LocalDate`

- [ ] **Step 1: Test che falliscono** (`StatsTest`, date fisse, zona `ZoneId.of("Europe/Rome")`):
  - `weekStart_isMonday` — mer 2026-10-14 → 2026-10-12.
  - `weekMarkers_doneAndPlanned` — settimana del 12 ott, oggi mer 14, sessioni lun 12 e mar 13, plannedDays {1..5} → `[DONE, DONE, PLANNED, PLANNED, PLANNED, NONE, NONE]`.
  - `weekMarkers_pastPlannedNotDone_isNone` — oggi mer 14, plannedDays {1}, nessuna sessione → lunedì NONE.
  - `weekMarkers_acrossYearBoundary` — weekStart 2026-12-28, sessione 2027-01-02 → indice 5 DONE, gli altri NONE (oggi 2027-01-05).
  - `streak_none_isZero`; `streak_consecutiveWeeks` — sessioni nelle settimane del 28 set, 5 ott, 12 ott, oggi 14 ott → 3.
  - `streak_currentWeekEmpty_countsFromPrevious` — sessioni settimane 28 set e 5 ott, oggi 14 ott (nessuna questa settimana) → 2.
  - `streak_gapBreaks` — sessioni 21 set e 5 ott, oggi 7 ott → 1.
  - `activity_thresholds` — sessioni da 30, 31, 60, 90, 91 minuti in giorni diversi → livelli 1, 2, 2, 3, 4.
  - `activity_sumsSameDay` — due sessioni da 40 min stesso giorno → 3.
  - `activity_unfinishedOrZeroDuration_isLevelOne`.
  - `activity_olderThanYear_excluded` — sessione di 400 giorni fa assente.
  - `activity_sessionAcrossMidnight_countsOnStartDay` — 23:30 → 00:40 del giorno dopo → livello 3 sul giorno di inizio, niente sul successivo.
- [ ] **Step 2: Test repository che falliscono:**
  - `assignDay_movesDayFromOtherRoutine` — seed: assegnare lunedì (1) a "Pull A" → Pull A dayOfWeek 1, Push A null.
  - `assignDay_rest_clearsDay` — `assignDay(p, 3, null)` → nessuna scheda con dayOfWeek 3.
  - `assignDay_onlyAffectsSameProgram` — secondo programma con scheda su lunedì → resta su lunedì.
  - `bestWeight_ignoresWarmupAndUnfinished` — conclusa: WARMUP 100, WORKING 80; in corso: WORKING 90 → 80.0.
  - `countWorkingSetsSince_excludesWarmupAndOlder`.
- [ ] **Step 3:** `scripts/test.sh` → FAIL.
- [ ] **Step 4:** Implementare.
- [ ] **Step 5:** `scripts/test.sh` → PASS.
- [ ] **Step 6: Commit** — `"Logica settimana, serie consecutive, mappa attività, record, assegnazione giorni"`

### Task 3: Navigazione con pulsante Start centrale

**Files:**
- Create: `ui/nav/BottomBar.kt`, `ui/nav/StartViewModel.kt`, `ui/nav/RoutinePickerSheet.kt`
- Modify: `ui/nav/AppNavHost.kt`
- Test: `ui/nav/StartActionTest.kt`

**Interfaces:**
- Consumes: `ProgramRepository.todayDay`, `WorkoutRepository.startOrResume/observeUnfinished`.
- Produces:
  - `sealed interface StartAction { data class Resume(val sessionId: Long); data class StartDay(val dayId: Long); data object Pick }`
  - `fun startAction(unfinished: WorkoutSession?, today: RoutineDay?): StartAction` (in corso → Resume; scheda di oggi → StartDay; altrimenti Pick)
  - `StartViewModel(programs, workouts)` con `val resuming: StateFlow<Boolean>`, `val days: StateFlow<List<RoutineDay>>` (programma attivo), `fun onStart(open: (Long) -> Unit, pick: () -> Unit)`, `fun startDay(dayId: Long?, open: (Long) -> Unit)` (guardia doppio tocco come `TodayViewModel.start`).
  - `@Composable fun RoutinePickerSheet(days: List<RoutineDay>, onPick: (Long?) -> Unit, onDismiss: () -> Unit)` — `ModalBottomSheet`, voce finale "Allenamento libero" (`null`).
  - Route: `home` (start destination, sostituisce `today`), `plan`, `stats`, `exercises`, `settings` (da ingranaggio, con freccia indietro), dettagli invariati. Barra visibile solo su `home`, `plan`, `stats`, `exercises`.

- [ ] **Step 1: Test che falliscono:** `start_unfinished_resumes`, `start_todayRoutine_startsIt`, `start_restDay_picks`.
- [ ] **Step 2:** `scripts/test.sh` → FAIL.
- [ ] **Step 3:** Implementare `startAction`, ViewModel, `BottomBar` (NavigationBar con 4 voci + slot centrale: `Box` 64 dp cerchio `accent` o `resume`, icona `Rounded.FitnessCenter`, etichetta "Start"/"Riprendi", offset verticale −16 dp; icone voci: `Rounded.Home`, `Rounded.CalendarMonth`, `Rounded.BarChart`, `Rounded.FormatListBulleted`) e grafo. Le schermate Home/Plan/Stats non ancora ridisegnate puntano temporaneamente a `TodayScreen`, `ProgramEditScreen(active)`, `HistoryScreen`.
- [ ] **Step 4:** `scripts/test.sh` → PASS; `assembleDebug` → OK.
- [ ] **Step 5: Commit** — `"Barra di navigazione con Start centrale"`

### Task 4: Home

**Files:**
- Create: `ui/home/HomeScreen.kt`, `ui/home/HomeViewModel.kt`
- Delete: `ui/today/TodayScreen.kt`, `ui/today/TodayViewModel.kt`, `ui/today/` test spostato in `ui/home/HomeViewModelTest.kt`
- Test: `ui/home/HomeViewModelTest.kt`

**Interfaces:**
- Consumes: Task 2 (`weekStart`, `weekMarkers`, `weekStreak`, `observeFinishedSessionTimes`), Task 3 (`StartViewModel.startDay`).
- Produces: `HomeViewModel(programs, workouts, today: () -> LocalDate = LocalDate::now, zone: ZoneId = ZoneId.systemDefault())` con `state: StateFlow<HomeState>`:
  `HomeState(weekStart: LocalDate, days: List<LocalDate>, markers: List<DayMarker>, selected: LocalDate, selectedRoutine: RoutineDay?, selectedExerciseCount: Int, selectedSession: SessionTime?, unfinished: WorkoutSession?, streak: Int, thisWeekDone: Int, plannedPerWeek: Int, totalWorkouts: Int, last: SessionTime?)`; azioni `previousWeek()`, `nextWeek()`, `select(date)`, `start(dayId: Long?, open: (Long) -> Unit)` (guardia doppio tocco).

- [ ] **Step 1: Test che falliscono** (seed, oggi = lun 2026-10-12):
  - `doubleTapStart_navigatesOnce` (spostato da `TodayViewModelTest`).
  - `initial_selectsTodayWithPushA` — `selectedRoutine.name == "Push A"`, `selectedExerciseCount == 6`, `plannedPerWeek == 5`.
  - `nextWeek_movesWeekStart` — `weekStart == 2026-10-19`; `previousWeek` due volte → 2026-10-05.
  - `select_sunday_isRest` — `selectedRoutine == null`.
  - `finishedSessionToday_markerDoneAndSelectedSession` — sessione conclusa oggi → `markers[0] == DONE`, `selectedSession != null`, `thisWeekDone == 1`.
- [ ] **Step 2:** `scripts/test.sh` → FAIL.
- [ ] **Step 3:** Implementare la schermata come §4.2 della spec: `ScreenHeader("GymLog", data "lunedì 13 ottobre", ⚙ → settings)`, card settimana (frecce `Rounded.ChevronLeft/Right`, colonne giorno con cerchio d'accento su oggi, pallino 6 dp accento/grigio), riga giorno dentro la card (icona in quadrato d'accento 40 dp, "OGGI"/nome giorno in `label`, nome scheda, "N esercizi", `AccentPill("Start")` o "Riprendi" color resume; riposo: "Riposo" + "Scegli una scheda" che apre `RoutinePickerSheet`; giorno passato allenato: "Fatto · 52 min" → dettaglio), card serie settimanale (`Rounded.LocalFireDepartment` arancione), card ultimo allenamento. Collegare route `home`.
- [ ] **Step 4:** `scripts/test.sh` → PASS; `assembleDebug` → OK.
- [ ] **Step 5: Commit** — `"Home con settimana, scheda del giorno e serie consecutive"`

### Task 5: Piano e dettaglio scheda

**Files:**
- Create: `ui/plan/PlanScreen.kt`, `ui/plan/PlanViewModel.kt`, `ui/plan/ProgramsSheet.kt`
- Modify: `ui/programs/DayEditScreen.kt` (restyle: card esercizi con miniatura, target in `ModalBottomSheet`, "+ Esercizio" in fondo, nome nell'header tap = rinomina, ⋮ elimina)
- Delete: `ui/programs/ProgramListScreen.kt`, `ui/programs/ProgramEditScreen.kt`, `ProgramEditViewModel` (logica assorbita da `PlanViewModel`)
- Test: `ui/plan/PlanViewModelTest.kt`

**Interfaces:**
- Consumes: `ProgramRepository.assignDay`, `observeDays`, `observePrograms`, `create`, `rename`, `setActive`, `delete`, `addDay`, `getDayExercises`.
- Produces: `PlanViewModel(programs)` con `state: StateFlow<PlanState>` = `PlanState(program: Program?, programs: List<Program>, week: List<RoutineDay?>  /* 7, indice 0 = lunedì */, routines: List<RoutineSummary>)`, `data class RoutineSummary(val day: RoutineDay, val exerciseCount: Int)`; azioni `assign(dayOfWeek: Int, routineDayId: Long?)`, `createRoutine(name, onCreated: (Long) -> Unit)`, `selectProgram(id)`, `createProgram(name)`, `renameProgram(id, name)`, `deleteProgram(id)`.

- [ ] **Step 1: Test che falliscono:**
  - `week_matchesSeed` — `week.map { it?.name }` == `["Push A","Pull A","Legs","Push B","Pull B",null,null]`.
  - `assign_saturdayToLegs_movesLegs` — dopo `assign(6, legsId)`: week[5] = Legs, week[2] = null.
  - `routines_haveExerciseCounts` — `[6, 6, 7, 6, 8]`.
  - `createProgram_thenSelect_switchesWeek` — nuovo programma attivo → week tutta null.
- [ ] **Step 2:** `scripts/test.sh` → FAIL.
- [ ] **Step 3:** Implementare come §4.3 (header con nome programma e ▾ → `ProgramsSheet`; sezione SETTIMANA con 7 `GymCard` compatte + pillola; tap → `ModalBottomSheet` con schede + "Riposo"; sezione SCHEDE con "+ Nuova"). Collegare route `plan`.
- [ ] **Step 4:** `scripts/test.sh` → PASS; `assembleDebug` → OK.
- [ ] **Step 5: Commit** — `"Piano settimanale e dettaglio scheda"`

### Task 6: Schermata di allenamento

**Files:**
- Create: `domain/WorkoutUi.kt`, `ui/workout/SetTable.kt`, `ui/workout/RestTimerBar.kt`, `ui/workout/WorkoutTopBar.kt`
- Modify: `ui/workout/WorkoutScreen.kt`, `ui/workout/ExercisePage.kt`, `ui/workout/WorkoutViewModel.kt`, `ui/workout/RecapDialog.kt` (→ `ModalBottomSheet`), `data/repo/WorkoutRepository.kt` (`PlannedExercise.bestKg`)
- Delete: `ui/workout/SetRow.kt` (sostituito da `SetTable.kt`)
- Test: `domain/WorkoutUiTest.kt`, `ui/workout/WorkoutViewModelTest.kt` (estendere)

**Interfaces:**
- Consumes: Task 1 componenti, Task 2 `bestWeight`, `ExerciseRepository.addPhoto`, `PhotoStore.newCameraFile`.
- Produces:
  - `fun activeRowIndex(done: List<Boolean>, selected: Int?): Int?` — `selected` se in range e non fatto, altrimenti prima non fatta, altrimenti null.
  - `fun sessionProgress(pages: List<List<Boolean>>): Pair<Int, Int>` (fatte, totali).
  - `fun recordsBeaten(sessionBest: Map<String, Double>, previousBest: Map<String, Double?>): List<String>` — nomi con `sessionBest > previousBest` (previousBest null = nuovo record solo se > 0), ordine di inserimento.
  - `PlannedExercise.bestKg: Double?` (record prima di questa sessione: `bestWeight` esclude la sessione in corso perché non conclusa).
  - `WorkoutViewModel`: `selectRow(page: Int, row: Int)`, `WorkoutUiState.selectedRow: Map<Int, Int>` (pagina → riga), `fun activeRow(page): Int?` usa `activeRowIndex`; `confirm` dopo il salvataggio azzera la selezione della pagina (la riga attiva passa alla successiva); `addRestSeconds(15)`; `addPhoto(page: Int, file: File)` che salva via `ExerciseRepository.addPhoto` e ricarica le foto della pagina; `recap` + `records: List<String>`.
  - `WorkoutViewModel(sessionId, workouts, settings, timer, exercises: ExerciseRepository)` (nuovo parametro).

- [ ] **Step 1: Test che falliscono:**
  - `WorkoutUiTest.activeRow_firstUndone` — `[true,false,false]`, null → 1.
  - `activeRow_selectedUndone_wins` — selected 2 → 2.
  - `activeRow_selectedDone_fallsBackToFirstUndone`.
  - `activeRow_allDone_isNull`.
  - `activeRow_invalidSelection_fallsBack` — selected 7 su 3 righe → prima non fatta.
  - `progress_countsAllPages` — `[[true,false],[true,true,false]]` → (3, 5).
  - `records_onlyHigherThanPrevious` — session {A:85, B:60, C:20}, previous {A:80, B:60, C:null} → `["A","C"]`.
  - `WorkoutViewModelTest.bestKg_fromPreviousSessions` — sessione conclusa con Incline 80 e 85 → nella nuova `pages[0].planned.bestKg == 85.0`.
  - `confirm_movesActiveRowToNext` — dopo confirm(0,0) `activeRow(0) == 1`; `selectRow(0, 2)` → 2.
  - `finish_reportsRecords` — prima sessione 80 kg, seconda 85 kg → `records == ["Incline Chest Press (Machine)"]`.
- [ ] **Step 2:** `scripts/test.sh` → FAIL.
- [ ] **Step 3:** Implementare come §4.1:
  - `WorkoutTopBar`: ✕, titolo + "mm:ss · x/y serie" (cronometro da `startedAt`, `produceState` ogni 1 s), ✓ in cerchio d'accento (conferma termina), ⋮ menu; `LinearProgressIndicator` sottile (4 dp, accento).
  - `ExercisePage`: "Esercizio N/M" (`label`), foto 4:3 in `GymCard` (o segnaposto con "📷 Aggiungi foto" → `TakePicture` su FileProvider → `vm.addPhoto`), nome + ⓘ (pannello note), `GymChip` muscolo/tipo/"Record: X kg", "Ultima volta", `AccentPill` suggerimento, target.
  - `SetTable`: intestazione `#  KG  REPS  ✓`; riga attiva espansa (Stepper KG/REPS, fila RIR 0–5 come `GymChip` selezionabili, icona nota); righe compatte (fatte: testo + "RIR n" + `CircleCheck(done=true)`; future: alpha 0.6, `CircleCheck(false)`); badge `#` con lettera R (giallo `#FFD60A`), D (viola `#A06BFF`), C (rosso `error`) e menu tipo; "+ Aggiungi serie".
  - `RestTimerBar` fluttuante (card 20 dp con ombra 8 dp sopra il bordo, padding 12): tempo `GymType.timer`, `LinearProgressIndicator` = rimanenti/totali, "+15 s", `PrimaryButton("Salta")`; a zero "Pronto!" + "Chiudi". Il contenuto della pagina aggiunge 120 dp di padding inferiore quando il timer è visibile.
  - Schermo acceso: `DisposableEffect` che imposta `view.keepScreenOn = settings.keepScreenOn` e lo rimette a false all'uscita.
  - Riepilogo in `ModalBottomSheet` con durata, serie, volume e "🏆 Record: …".
- [ ] **Step 4:** `scripts/test.sh` → PASS; `assembleDebug` → OK.
- [ ] **Step 5: Commit** — `"Allenamento: tabella serie, riga attiva, record, timer fluttuante"`

### Task 7: Statistiche e storico

**Files:**
- Create: `ui/stats/StatsScreen.kt`, `ui/stats/StatsViewModel.kt`, `ui/stats/ActivityHeatmap.kt`
- Modify: `ui/history/HistoryScreen.kt` (card raggruppate per mese: intestazione `SectionLabel("OTTOBRE 2026")`), `ui/history/SessionDetailScreen.kt` (card per esercizio, serie come righe), `ui/history/ExerciseHistoryScreen.kt` (card per data)
- Test: `ui/stats/StatsViewModelTest.kt`

**Interfaces:**
- Consumes: Task 2 (`activityLevels`, `weekStreak`, `observeFinishedSessionTimes`, `countWorkingSetsSince`).
- Produces: `StatsViewModel(workouts, today, zone)` con `state: StateFlow<StatsState>` = `StatsState(total: Int, thisMonth: Int, streak: Int, setsThisWeek: Int, levels: Map<LocalDate, Int>, recent: List<SessionTime> /* max 5 */)`; `@Composable fun ActivityHeatmap(levels: Map<LocalDate, Int>, today: LocalDate)` — 53 colonne settimana × 7 righe, celle 12 dp gap 3 dp, colori livello 0 = `raised`, 1–4 = accento alpha 0.3/0.5/0.75/1.0, oggi con bordo `text`; etichette mesi sopra la prima settimana di ogni mese ("ott"); `LazyRow` con scroll iniziale sull'ultima colonna; legenda "Meno ▢▢▢▢▢ Più".

- [ ] **Step 1: Test che falliscono:**
  - `stats_countsTotalsMonthAndWeek` — oggi 2026-10-14; sessioni concluse il 2 ott, 12 ott (3 WORKING + 1 WARMUP), 28 set → total 3, thisMonth 2, setsThisWeek 3.
  - `stats_recentMaxFiveNewestFirst` — 7 sessioni → 5, la prima è la più recente.
- [ ] **Step 2:** `scripts/test.sh` → FAIL.
- [ ] **Step 3:** Implementare la schermata come §4.4 (`ScreenHeader("Statistiche","Progressi e storico", 🕘 → history)`, griglia 2×2 `StatTile` con icone `Rounded.FitnessCenter`, `CalendarToday`, `LocalFireDepartment`, `Repeat`; card heatmap; "Allenamenti recenti" + "Vedi tutti"). Collegare route `stats`; restyle storico e dettagli.
- [ ] **Step 4:** `scripts/test.sh` → PASS; `assembleDebug` → OK.
- [ ] **Step 5: Commit** — `"Statistiche con riquadri e mappa di attività, storico ridisegnato"`

### Task 8: Esercizi e Impostazioni

**Files:**
- Modify: `ui/exercises/ExerciseListScreen.kt`, `ui/exercises/ExerciseEditScreen.kt`, `ui/exercises/ExercisePickerDialog.kt` (→ `ModalBottomSheet` a tutta altezza), `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsViewModel.kt`
- Test: `ui/settings/SettingsViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1 (`ThemeMode`, `Accent`, `keepScreenOn`, componenti).
- Produces: `SettingsViewModel.setThemeMode(ThemeMode)`, `setAccent(Accent)`, `setKeepScreenOn(Boolean)`.

- [ ] **Step 1: Test che falliscono:** `setAccent_persists` (PURPLE), `setThemeMode_persists` (LIGHT), `setKeepScreenOn_persists` (false).
- [ ] **Step 2:** `scripts/test.sh` → FAIL.
- [ ] **Step 3:** Implementare come §4.5/§4.6: lista esercizi (`ScreenHeader("Esercizi","N esercizi")`, ricerca in campo `raised` arrotondato, chip muscoli, prima card "✨ Crea il tuo esercizio", card con miniatura 56 dp; FAB rimosso); modifica esercizio in card Dati/Muscoli secondari/Foto/Avanzate con "Salva" fisso; Impostazioni in card Aspetto (segmented Sistema/Chiaro/Scuro, 6 cerchi colore 40 dp con spunta sul selezionato), Allenamento, Timer, Dati; header con freccia indietro.
- [ ] **Step 4:** `scripts/test.sh` → PASS; `assembleDebug` → OK.
- [ ] **Step 5: Commit** — `"Esercizi e impostazioni ridisegnati"`

### Task 9: Versione 1.1.0

**Files:**
- Modify: `app/build.gradle.kts` (`versionCode 2`, `versionName "1.1.0"`), `INSTALLA.md` (sezione "Aggiornare alla 1.1.0" + checklist visiva)

- [ ] **Step 1:** Aggiornare versione e `INSTALLA.md`: aggiornamento sopra la 1.0.0 senza disinstallare; checklist: tema scuro di default, cambio accento, Start centrale avvia la scheda di oggi / Riprendi in arancione, settimana in Home con pallini, assegnazione giorni nel Piano, tabella serie con riga attiva, timer fluttuante che non copre l'ultima serie, schermo che resta acceso, Statistiche con mappa di attività.
- [ ] **Step 2:** `scripts/build-apk.sh` → tutti i test PASS, `Verified using v2 scheme ... true`, `dist/GymLog-1.1.0.apk`.
- [ ] **Step 3:** `aapt2 dump badging dist/GymLog-1.1.0.apk | head -1` → `versionCode='2' versionName='1.1.0'`.
- [ ] **Step 4: Commit** — `"Versione 1.1.0: ridisegno interfaccia"`
