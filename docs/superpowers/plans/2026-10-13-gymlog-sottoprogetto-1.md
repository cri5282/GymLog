# GymLog Sotto-progetto 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** APK Android offline che sostituisce Hevy: schede, libreria esercizi con foto, registrazione serie con precompilazione e progressione, timer di recupero, storico, import CSV Hevy.

**Architecture:** Un modulo `:app`, MVVM. Composable → ViewModel (`StateFlow`) → Repository → DAO Room. Logica di dominio in Kotlin puro (`domain/`), testata su JVM. Dipendenze create a mano in `AppContainer` (in `GymLogApp : Application`).

**Tech Stack:** Kotlin 2.1.0, AGP 8.7.3, Gradle 8.11.1, JDK 17, KSP 2.1.0-1.0.29, Compose BOM 2024.12.01 + Material 3, Room 2.6.1, Navigation Compose 2.8.5, DataStore Preferences 1.1.1, Coil 2.7.0 (foto), JUnit 4.13.2, Robolectric 4.14.1, kotlinx-coroutines-test 1.9.0. compileSdk/targetSdk 35, minSdk 26.
(Set di versioni compatibili tra loro e verificate; aggiornare solo se la build lo richiede.)

**Spec:** `docs/superpowers/specs/2026-10-13-gymlog-sottoprogetto-1-design.md`

## Global Constraints

- Nessuna permission INTERNET; nessun login, nessun server.
- Package `it.gymlog.app`, nome app **GymLog**, interfaccia in italiano, solo kg.
- `minSdk 26`; tema Material 3 chiaro/scuro di sistema, colori dinamici su Android 12+.
- Un solo modulo Gradle `:app`; niente Hilt/Dagger/Koin.
- Target tattili ≥ 48 dp; pulsante principale ≥ 64 dp; comandi principali nella metà bassa.
- APK release firmato con keystore dedicato in `~/GymLog/keystore/` (escluso da git); `keystore.properties` escluso da git.
- Commit come `nicolosimonini3 <nicolosimonini3@gmail.com>`, **senza** righe Co-Authored-By.
- Comandi Gradle sempre con `JAVA_HOME` = JDK 17 e `ANDROID_HOME=~/Android/Sdk`.

## Review Focus

1. **App chiusa/telefono spento a metà allenamento** → al riavvio "Riprendi" riapre la sessione con tutte le serie confermate. Test: Task 8 `startOrResume_returnsUnfinishedSession`.
2. **CSV Hevy localizzato** (decimali con virgola `82,5`, date italiane `12 ott 2026, 18:30`) → importato correttamente. Test: Task 5 `parsesItalianLocaleDecimalsAndDates`.
3. **Esercizio archiviato o rimosso dalla scheda** → lo storico continua a mostrarne nome e serie. Test: Task 2 `archivedExercise_stillJoinedInHistory`.
4. **Ultima sessione con esercizio sostituito o serie in più/in meno** → precompilazione e progressione non crashano e usano solo le serie di quell'esercizio. Test: Task 4 `prefill_ignoresOtherExercisesAndExtraSets`.
5. **Timer con app in background** → il tempo residuo è calcolato da un istante di fine assoluto, non da un contatore, quindi resta corretto dopo pausa/blocco schermo. Test: Task 10 `remaining_isComputedFromEndTime`.

---

## File Structure

```
app/src/main/java/it/gymlog/app/
  GymLogApp.kt            Application + AppContainer
  AppContainer.kt         crea DB, repository, settings
  MainActivity.kt         setContent { GymLogTheme { AppNavHost() } }
  ui/theme/               Theme.kt, Color.kt, Type.kt
  ui/nav/AppNavHost.kt    bottom bar + route
  data/db/                Enums.kt, Entities.kt, Converters.kt, Daos.kt, AppDatabase.kt, SeedData.kt, Relations.kt
  data/repo/              ExerciseRepository.kt, ProgramRepository.kt, WorkoutRepository.kt, PhotoStore.kt
  data/settings/          SettingsRepository.kt
  data/hevy/              HevyCsvParser.kt, HevyImporter.kt
  domain/                 Progression.kt, Prefill.kt, FractionalVolume.kt
  timer/                  RestTimer.kt, RestTimerService.kt
  ui/today/  ui/workout/  ui/history/  ui/exercises/  ui/programs/  ui/settings/   (Screen + ViewModel per cartella)
app/src/test/java/it/gymlog/app/   test JVM (dominio, parser, DAO con Robolectric)
```

---

### Task 1: Toolchain, scheletro progetto, firma

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `MainActivity.kt`, `GymLogApp.kt`, `ui/theme/*`, `app/src/main/res/values/strings.xml`, icona adattiva in `res/mipmap-anydpi-v26/`, `.gitignore`, `keystore.properties` (non versionato)
- Test: `app/src/test/java/it/gymlog/app/SmokeTest.kt`

- [ ] **Step 1: JDK 17** — scaricare Temurin 17 (tar.gz, Adoptium) in `~/.local/jdk-17`; verificare `~/.local/jdk-17/bin/java -version` → `17.x`.
- [ ] **Step 2: Android SDK** — scaricare commandlinetools-linux in `~/Android/Sdk/cmdline-tools/latest`; `sdkmanager --licenses` (accetta) e `sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"`. Verifica: `ls ~/Android/Sdk/build-tools/35.0.0/apksigner`.
- [ ] **Step 3: Gradle wrapper 8.11.1** — scaricare la distribuzione Gradle 8.11.1, eseguire `gradle wrapper --gradle-version 8.11.1` nella root, poi usare solo `./gradlew`.
- [ ] **Step 4: Scheletro** — version catalog con le versioni del Tech Stack; plugin `com.android.application`, `org.jetbrains.kotlin.android`, `org.jetbrains.kotlin.plugin.compose`, `com.google.devtools.ksp`. `app`: `namespace`/`applicationId = "it.gymlog.app"`, `versionCode 1`, `versionName "1.0.0"`, `testOptions.unitTests.isIncludeAndroidResources = true`, release con `isMinifyEnabled = false`. Manifest: `android:name=".GymLogApp"`, nessuna permission INTERNET. `MainActivity` mostra "GymLog" in `GymLogTheme` (dynamicColor su API ≥ 31, `isSystemInDarkTheme()`).
- [ ] **Step 5: Keystore** — `keytool -genkeypair -v -keystore ~/GymLog/keystore/gymlog.jks -alias gymlog -keyalg RSA -keysize 2048 -validity 36500` con password generata casualmente; scriverla in `keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). `signingConfigs.release` legge `keystore.properties`. `.gitignore`: `keystore/`, `keystore.properties`, `local.properties`, `build/`, `.gradle/`, `dist/`.
- [ ] **Step 6: Smoke test** — `SmokeTest.appIdIsCorrect()` asserisce `BuildConfig.APPLICATION_ID == "it.gymlog.app"` (`buildFeatures.buildConfig = true`).
- [ ] **Step 7: Verifica** — `./gradlew test assembleRelease` → BUILD SUCCESSFUL; `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk` → stampa il certificato `CN=GymLog`.
- [ ] **Step 8: Commit** — `git commit -m "Scheletro progetto Android, tema, firma release"`

### Task 2: Database Room

**Files:**
- Create: `data/db/Enums.kt`, `Entities.kt`, `Converters.kt`, `Relations.kt`, `Daos.kt`, `AppDatabase.kt`
- Test: `app/src/test/java/it/gymlog/app/data/db/DaoTest.kt` (`@RunWith(RobolectricTestRunner::class)`, `Room.inMemoryDatabaseBuilder(...).allowMainThreadQueries()`)

**Interfaces:**
- Produces:
  - `enum class Muscle { PETTO, DORSALI, TRAPEZI, DELTOIDE_ANTERIORE, DELTOIDE_LATERALE, DELTOIDE_POSTERIORE, BICIPITI, TRICIPITI, AVAMBRACCI, ADDOME, QUADRICIPITI, FEMORALI, GLUTEI, POLPACCI, ADDUTTORI, ABDUTTORI }` con `val label: String` italiano ("Petto", "Deltoide laterale"…)
  - `enum class LengthBias { LENGTHENED, SHORTENED, NONE }`, `enum class SetType { WARMUP, WORKING, DROP, FAILURE }`, `enum class SessionSource { MANUAL, HEVY_IMPORT }`
  - Entity (`@Entity`, nomi tabella = nome classe) con i campi della spec §3; in più `Exercise.needsReview: Boolean = false` (badge "da completare"). Indici: `Exercise.name` unique, `Exercise.hevyName` unique, FK indicizzate. Tempi come `Long` epoch millis.
  - `ExerciseWithDetails(exercise, secondary: List<ExerciseSecondaryMuscle>, photos: List<ExercisePhoto>)`
  - `ExerciseDao`: `observeActive(): Flow<List<ExerciseWithDetails>>`, `getWithDetails(id: Long): ExerciseWithDetails?`, `findByHevyName(name: String): Exercise?`, `insert(e: Exercise): Long`, `update(e)`, `replaceSecondary(exerciseId: Long, muscles: List<Muscle>)` (@Transaction), `insertPhoto(p): Long`, `deletePhoto(p)`, `updatePhotos(list)`, `isUsed(exerciseId: Long): Boolean` (presente in SetEntry o RoutineExercise)
  - `ProgramDao`: `observePrograms(): Flow<List<Program>>`, `getActive(): Program?`, `setActive(programId: Long)` (@Transaction: tutti false, poi quello true), `observeDays(programId): Flow<List<RoutineDay>>`, `observeDayExercises(dayId): Flow<List<RoutineExerciseWithExercise>>`, insert/update/delete per Program, RoutineDay, RoutineExercise, `getDayExercises(dayId): List<RoutineExerciseWithExercise>`
  - `WorkoutDao`: `insertSession(s): Long`, `updateSession(s)`, `getUnfinished(): WorkoutSession?`, `observeUnfinished(): Flow<WorkoutSession?>`, `observeFinishedSessions(): Flow<List<SessionSummary>>` (id, title, startedAt, setCount), `getSession(id): WorkoutSession?`, `observeSets(sessionId): Flow<List<SetWithExercise>>`, `insertSet(s): Long`, `updateSet(s)`, `deleteSet(s)`, `deleteSession(s)`, `lastFinishedSetsFor(exerciseId: Long, excludeSessionId: Long): List<SetEntry>` (serie dell'ultima sessione **conclusa** contenente l'esercizio, ordinate per `setIndex`), `existsSession(title: String, startedAt: Long): Boolean`, `historyFor(exerciseId): Flow<List<SetWithSession>>`
  - `AppDatabase` (version 1, `exportSchema = true`, schema in `app/schemas`), `AppDatabase.build(context, onCreateSeed: Boolean = true)`

- [ ] **Step 1: Test che falliscono** in `DaoTest`:
  - `setActive_leavesExactlyOneActiveProgram` — 2 programmi, `setActive(b)` → solo b `isActive`.
  - `deletingProgram_cascadesDaysAndRoutineExercises`.
  - `archivedExercise_stillJoinedInHistory` — esercizio con una SetEntry, `archived = true` → `observeActive()` non lo contiene; `observeSets(sessionId)` restituisce la serie con `exercise.name` corretto.
  - `isUsed_trueWhenInSetOrRoutine_falseOtherwise`.
  - `lastFinishedSetsFor_skipsUnfinishedAndExcludedSession` — sessione conclusa A (80×8,80×8), sessione in corso B → restituisce le serie di A.
  - `replaceSecondary_replacesAll`.
- [ ] **Step 2:** `./gradlew test --tests '*DaoTest*'` → FAIL (classi mancanti).
- [ ] **Step 3:** Implementare entity, converter enum↔String, relazioni, DAO, database.
- [ ] **Step 4:** `./gradlew test --tests '*DaoTest*'` → PASS.
- [ ] **Step 5: Commit** — `"Database Room: entity, DAO, test"`

### Task 3: Dati precaricati

**Files:**
- Create: `data/db/SeedData.kt`
- Modify: `data/db/AppDatabase.kt` (callback `onCreate` che esegue il seed in una transazione)
- Test: `app/src/test/java/it/gymlog/app/data/db/SeedTest.kt`

**Interfaces:**
- Produces: `object SeedData { val exercises: List<SeedExercise>; val program: SeedProgram; suspend fun insertInto(db: AppDatabase) }`. Dati copiati **esattamente** dalle due tabelle della spec §7 (nomi, primari, secondari, tipo, note, serie, reps, RIR min-max, recupero, giorno 1–5, ordine). `hevyName = name` per tutti.

- [ ] **Step 1: Test che falliscono:**
  - `seed_creates31ExercisesAndActiveProgram` — 31 esercizi; un programma "Ipertrofia – ottobre 2026" attivo; 5 giorni con `dayOfWeek` 1..5 e nomi "Push A", "Pull A", "Legs", "Push B", "Pull B".
  - `seed_routineCounts` — esercizi per giorno: 6, 6, 7, 6, 8.
  - `seed_pullB_firstExercise` — "Single Lat Pulldown (Macchina)", 3 serie, 8-10, RIR 0-1, 120 s.
  - `seed_legExtensionNotes` — note di "Single Leg Extensions" = "Gamba infortunata: ROM 90°→45°, cuscinetto prossimale sulla tibia".
  - `seed_isNotRepeatedOnReopen` — chiudere e riaprire il DB su file → sempre 31 esercizi.
- [ ] **Step 2:** `./gradlew test --tests '*SeedTest*'` → FAIL.
- [ ] **Step 3:** Implementare `SeedData` e callback.
- [ ] **Step 4:** `./gradlew test --tests '*SeedTest*'` → PASS.
- [ ] **Step 5: Commit** — `"Seed: libreria esercizi e programma ottobre 2026"`

### Task 4: Logica di dominio

**Files:**
- Create: `domain/Progression.kt`, `domain/Prefill.kt`, `domain/FractionalVolume.kt`
- Test: `app/src/test/java/it/gymlog/app/domain/ProgressionTest.kt`, `PrefillTest.kt`, `FractionalVolumeTest.kt`

**Interfaces:**
- Produces:
  - `data class SetDraft(val weightKg: Double, val reps: Int, val rir: Int?, val type: SetType)`
  - `data class Target(val sets: Int, val repMin: Int, val repMax: Int, val rirMin: Int, val rirMax: Int)`
  - `fun suggestLoad(lastSets: List<SetDraft>, target: Target, stepKg: Double): Double?` — regola spec §4 (solo WORKING; ≥ `target.sets` serie; tutte `reps ≥ repMax`; tutte `rir != null && rir ≤ rirMax`; base = carico minimo). Restituisce `base + stepKg` o `null`.
  - `fun prefill(lastSets: List<SetDraft>, targetSets: Int): List<SetDraft>` — copia le serie; se meno di `targetSets` serie WORKING, aggiunge copie dell'ultima WORKING fino a `targetSets`; senza storico → `targetSets` × `SetDraft(0.0, 0, null, WORKING)`.
  - `fun fractionalSets(sets: List<Pair<SetType, ExerciseMuscles>>): Map<Muscle, Double>` con `data class ExerciseMuscles(val primary: Muscle, val secondary: List<Muscle>)`.

- [ ] **Step 1: Test che falliscono:**
  - `suggest_allSetsAtTopWithRirOk_returnsPlusStep` — target 3×6-8 RIR 1-2, serie 80×8 RIR 2 ×3, step 2.5 → `82.5`.
  - `suggest_oneSetBelowRepMax_null` (80×8, 80×8, 80×7).
  - `suggest_rirAboveMax_null` (una serie RIR 3).
  - `suggest_missingRir_null`.
  - `suggest_fewerSetsThanTarget_null` (2 serie su 3).
  - `suggest_warmupIgnored` — riscaldamento 40×12 RIR 5 + 3 serie valide → `82.5`.
  - `suggest_differentLoads_usesMin` — 82.5, 80, 80 tutte valide → `82.5`.
  - `prefill_noHistory_returnsEmptyRows` — 3 righe a 0.
  - `prefill_padsWithLastWorkingSet` — storico 2 serie, target 3 → 3ª = copia della 2ª.
  - `prefill_ignoresOtherExercisesAndExtraSets` — 4 serie storiche con target 3 → restituite tutte e 4 (nessun troncamento), nessuna eccezione.
  - `fractional_primaryOneSecondaryHalf_warmupExcluded` — 3 WORKING di panca (PETTO; TRICIPITI, DELTOIDE_ANTERIORE) + 1 WARMUP → PETTO 3.0, TRICIPITI 1.5, DELTOIDE_ANTERIORE 1.5.
- [ ] **Step 2:** `./gradlew test --tests '*domain*'` → FAIL.
- [ ] **Step 3:** Implementare le tre funzioni.
- [ ] **Step 4:** `./gradlew test --tests '*domain*'` → PASS.
- [ ] **Step 5: Commit** — `"Dominio: progressione, precompilazione, volume frazionario"`

### Task 5: Import CSV Hevy

**Files:**
- Create: `data/hevy/HevyCsvParser.kt`, `data/hevy/HevyImporter.kt`
- Test: `app/src/test/java/it/gymlog/app/data/hevy/HevyCsvParserTest.kt`, `HevyImporterTest.kt` (Robolectric), fixture `app/src/test/resources/hevy_sample.csv`, `hevy_sample_it.csv`

**Interfaces:**
- Consumes: DAO del Task 2.
- Produces:
  - `data class HevyRow(val line: Int, val title: String, val startTime: Long, val endTime: Long?, val exerciseTitle: String, val setIndex: Int, val setType: SetType, val weightKg: Double, val reps: Int, val rir: Int?, val notes: String)`
  - `data class ParseResult(val rows: List<HevyRow>, val invalidLines: List<Int>)`
  - `object HevyCsvParser { fun parse(input: java.io.Reader): ParseResult }` — lancia `HevyFormatException(message)` se mancano le colonne `title, start_time, exercise_title, set_type, weight_kg, reps`.
  - `data class ImportReport(val sessionsImported: Int, val sessionsSkipped: Int, val exercisesCreated: Int, val invalidLines: List<Int>)`
  - `class HevyImporter(private val db: AppDatabase) { suspend fun import(input: java.io.Reader): ImportReport }`

- [ ] **Step 0: File reale** — chiedere all'utente l'export CSV di Hevy; copiarne intestazione e ~10 righe anonimizzabili in `hevy_sample.csv`. Se l'export reale differisce dalla spec §5 (nomi colonne, formato data, separatore), adeguare i test sotto al formato reale e annotarlo nella spec.
- [ ] **Step 1: Test parser che falliscono:**
  - `parsesStandardRow` — `"Push","12 Oct 2026, 18:30","12 Oct 2026, 19:45","","Chest Fly (Machine)","","",0,"normal",25,5,,,8` → title "Push", setType WORKING, 25.0 kg, 5 reps, rir 2.
  - `handlesQuotedCommas` — note `"lento, controllato"` → notes intatte.
  - `mapsSetTypes` — normal/warmup/dropset/failure → WORKING/WARMUP/DROP/FAILURE.
  - `rpeToRir_clampedAndNullable` — rpe 10 → 0; rpe 4 → 5; rpe vuoto → null; rpe 8.5 → 2 (arrotondamento half-up di 1.5).
  - `parsesItalianLocaleDecimalsAndDates` — `weight_kg` `"82,5"` → 82.5; data `"12 ott 2026, 18:30"` → stesso istante di `"12 Oct 2026, 18:30"` (fuso del dispositivo); anche ISO `2026-10-12T18:30:00`.
  - `skipsRowsWithoutWeightAndReps_andReportsInvalidLines` — riga cardio senza peso/reps saltata in silenzio; riga con reps `"abc"` → in `invalidLines` con numero riga corretto.
  - `missingColumns_throwsHevyFormatException`.
- [ ] **Step 2: Test importer che falliscono:**
  - `import_createsSessionsAndSets` — fixture con 2 allenamenti → 2 `WorkoutSession` `HEVY_IMPORT` concluse, serie con `exerciseId` corretto (match su `hevyName` del seed).
  - `import_unknownExercise_createdWithNeedsReview` — "Cable Crossover" → nuovo `Exercise(primaryMuscle = ADDOME, needsReview = true, hevyName = "Cable Crossover")`; report `exercisesCreated = 1`.
  - `import_twice_isIdempotent` — seconda importazione → `sessionsImported = 0`, `sessionsSkipped = 2`, nessuna serie duplicata.
  - `import_badFile_writesNothing` — `HevyFormatException` → DB invariato.
- [ ] **Step 3:** `./gradlew test --tests '*hevy*'` → FAIL.
- [ ] **Step 4:** Implementare. Parser CSV RFC 4180 scritto a mano (virgolette doppie, `""` escape, newline dentro virgolette), colonne per nome. Date: provare in sequenza `d MMM yyyy, HH:mm` con `Locale.ENGLISH` e `Locale.ITALIAN`, poi ISO. Importer: tutto in `db.withTransaction { }`; una sessione per coppia (`title`, `startTime`); `endedAt = endTime ?: startTime`.
- [ ] **Step 5:** `./gradlew test --tests '*hevy*'` → PASS.
- [ ] **Step 6: Commit** — `"Import CSV Hevy con test"`

### Task 6: Container, impostazioni, navigazione, schermata Impostazioni

**Files:**
- Create: `AppContainer.kt`, `data/settings/SettingsRepository.kt`, `ui/nav/AppNavHost.kt`, `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsViewModel.kt`
- Modify: `GymLogApp.kt`, `MainActivity.kt`
- Test: `app/src/test/java/it/gymlog/app/data/settings/SettingsRepositoryTest.kt`

**Interfaces:**
- Produces:
  - `data class Settings(val defaultLoadStepKg: Double = 2.5, val oneRmFormula: OneRmFormula = OneRmFormula.EPLEY, val timerVibration: Boolean = true, val timerSound: Boolean = true)`; `enum class OneRmFormula { EPLEY, BRZYCKI }`
  - `class SettingsRepository(dataStore) { val settings: Flow<Settings>; suspend fun update(transform: (Settings) -> Settings) }`
  - `class AppContainer(context) { val db; val settings; val exercises: ExerciseRepository; val programs: ProgramRepository; val workouts: WorkoutRepository; val photos: PhotoStore; val hevyImporter: HevyImporter }` (repository creati nei Task 7–9; in questo task i campi non ancora esistenti si aggiungono man mano)
  - ViewModel creati con `viewModelFactory { initializer { XViewModel((this[APPLICATION_KEY] as GymLogApp).container ...) } }`
  - Route: `today`, `workout/{sessionId}`, `history`, `history/{sessionId}`, `exerciseHistory/{exerciseId}`, `exercises`, `exercise/{exerciseId}` (`0` = nuovo), `programs`, `program/{programId}`, `day/{dayId}`, `settings`. Bottom bar sulle 5 route principali, nascosta in `workout/*`.

- [ ] **Step 1: Test che falliscono:** `defaults_whenEmpty` (2.5, EPLEY, true, true); `update_persistsStep` (3.0 → riletto 3.0).
- [ ] **Step 2:** `./gradlew test --tests '*SettingsRepositoryTest*'` → FAIL.
- [ ] **Step 3:** Implementare repository, container, navigazione con schermate segnaposto e `SettingsScreen`: passo di carico (campo numerico, 0,25–20), formula 1RM (radio), vibrazione/suono (switch), pulsante "Importa da Hevy (CSV)" (`ActivityResultContracts.OpenDocument` con `text/*`, `text/csv`, `text/comma-separated-values`) → dialogo con `ImportReport` ("Importati N allenamenti, M saltati perché già presenti, K esercizi nuovi da completare"; righe non valide elencate) o messaggio di `HevyFormatException`.
- [ ] **Step 4:** `./gradlew test assembleDebug` → PASS / BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** — `"Container, impostazioni, navigazione, import da Impostazioni"`

### Task 7: Libreria esercizi e foto

**Files:**
- Create: `data/repo/ExerciseRepository.kt`, `data/repo/PhotoStore.kt`, `ui/exercises/ExerciseListScreen.kt`, `ExerciseListViewModel.kt`, `ExerciseEditScreen.kt`, `ExerciseEditViewModel.kt`, `ui/common/PhotoCarousel.kt`, `res/xml/file_paths.xml`
- Modify: `AndroidManifest.xml` (FileProvider `${applicationId}.fileprovider`), `AppContainer.kt`
- Test: `app/src/test/java/it/gymlog/app/data/repo/ExerciseRepositoryTest.kt`, `PhotoStoreTest.kt`

**Interfaces:**
- Produces:
  - `class PhotoStore(filesDir: File) { fun newCameraFile(): File; suspend fun importFrom(uri: Uri, resolver: ContentResolver): File; fun delete(path: String) }` — file in `filesDir/photos/<uuid>.jpg`
  - `class ExerciseRepository(db, photos) { fun observeLibrary(query: String, muscle: Muscle?): Flow<List<ExerciseWithDetails>>; suspend fun get(id: Long): ExerciseWithDetails?; suspend fun save(draft: ExerciseDraft): Long; suspend fun deleteOrArchive(id: Long): DeleteResult; suspend fun addPhoto(exerciseId: Long, file: File); suspend fun removePhoto(photo: ExercisePhoto); suspend fun movePhoto(photo: ExercisePhoto, up: Boolean) }`
  - `data class ExerciseDraft(val id: Long = 0, val name: String, val primary: Muscle, val secondary: List<Muscle>, val bias: LengthBias, val notes: String, val hevyName: String?, val loadStepKg: Double?)`; `enum class DeleteResult { DELETED, ARCHIVED }`
  - `@Composable fun PhotoCarousel(paths: List<String>, modifier: Modifier)` — tap su foto → schermo intero; file mancante → icona segnaposto.

- [ ] **Step 1: Test che falliscono:**
  - `save_new_thenEdit_updatesFieldsAndSecondary`; `save_clearsNeedsReview`.
  - `save_duplicateName_throwsDuplicateNameException`.
  - `deleteOrArchive_unused_deletesAndRemovesPhotoFiles`; `deleteOrArchive_used_archives`.
  - `observeLibrary_filtersByQueryCaseInsensitiveAndMuscle` ("pull" + DORSALI → i 2 pulldown).
  - `PhotoStoreTest.delete_removesFile`.
- [ ] **Step 2:** `./gradlew test --tests '*ExerciseRepositoryTest*' --tests '*PhotoStoreTest*'` → FAIL.
- [ ] **Step 3:** Implementare repository, PhotoStore e schermate (spec §6.4): lista con ricerca, chip filtro muscolo, badge "da completare", FAB **+**; modifica con campi spec, multi-selezione secondari (FilterChip), pulsanti "Scatta" (`TakePicture` su URI FileProvider) e "Galleria" (`PickVisualMedia` ImageOnly), frecce/elimina su ogni foto, "Elimina" con conferma che dice se l'esercizio sarà archiviato. Nome vuoto → errore sul campo.
- [ ] **Step 4:** `./gradlew test assembleDebug` → PASS.
- [ ] **Step 5: Commit** — `"Libreria esercizi con foto"`

### Task 8: Schede e logica allenamento

**Files:**
- Create: `data/repo/ProgramRepository.kt`, `data/repo/WorkoutRepository.kt`, `ui/programs/ProgramListScreen.kt`, `ProgramEditScreen.kt`, `DayEditScreen.kt`, `ProgramsViewModel.kt`, `DayEditViewModel.kt`, `ui/today/TodayScreen.kt`, `TodayViewModel.kt`
- Modify: `AppContainer.kt`
- Test: `app/src/test/java/it/gymlog/app/data/repo/ProgramRepositoryTest.kt`, `WorkoutRepositoryTest.kt`

**Interfaces:**
- Consumes: `prefill`, `suggestLoad`, `SetDraft`, `Target` (Task 4).
- Produces:
  - `ProgramRepository`: `observePrograms()`, `create(name): Long`, `rename(id, name)`, `setActive(id)`, `delete(id)`, `observeDays(programId)`, `addDay(programId, name, dayOfWeek: Int?)`, `updateDay(day)`, `deleteDay(day)`, `moveDay(day, up: Boolean)`, `observeDayExercises(dayId)`, `addExerciseToDay(dayId, exerciseId)` (default 3 serie, 8-12, RIR 1-2, 120 s), `updateRoutineExercise(re)`, `removeRoutineExercise(re)`, `moveRoutineExercise(re, up: Boolean)`, `todayDay(date: LocalDate): RoutineDay?`
  - `data class PlannedExercise(val exercise: ExerciseWithDetails, val target: Target?, val restSeconds: Int, val prefill: List<SetDraft>, val suggestedKg: Double?, val lastTime: List<SetDraft>)`
  - `WorkoutRepository`: `startOrResume(dayId: Long?): Long` (se esiste una sessione non conclusa la restituisce, altrimenti ne crea una con `title` = nome giorno o "Allenamento libero"), `plan(sessionId): List<PlannedExercise>` (esercizi del giorno + quelli aggiunti nella sessione), `addExerciseToSession(sessionId, exerciseId)`, `replaceExerciseInSession(sessionId, oldExerciseId, newExerciseId)` (solo per la sessione: elenco extra/sostituzioni tenuto in memoria nel ViewModel e persistito tramite le SetEntry), `confirmSet(sessionId, exerciseId, exerciseOrder, setIndex, draft: SetDraft, notes: String): Long`, `updateSet`, `deleteSet`, `finish(sessionId): SessionRecap`, `observeUnfinished()`
  - `data class SessionRecap(val durationMin: Long, val workingSets: Int, val volumeKg: Double)` — volume = Σ kg × reps delle serie non WARMUP.

- [ ] **Step 1: Test che falliscono:**
  - `todayDay_monday_returnsPushA_sunday_returnsNull` (programma seed).
  - `moveRoutineExercise_swapsOrder`; `setActive_switchesProgram`.
  - `startOrResume_returnsUnfinishedSession` — `startOrResume(pushA)` due volte → stesso id; dopo `finish` → nuovo id.
  - `plan_usesLastFinishedSessionAndSuggestion` — sessione conclusa con Incline Chest Press 3×(80×8 RIR 2) → nella nuova sessione `prefill` = 80×8 ×3, `suggestedKg = 82.5`.
  - `finish_recapCountsOnlyNonWarmup` — 1 WARMUP 40×10 + 2 WORKING 80×8 → workingSets 2, volume 1280.0.
- [ ] **Step 2:** `./gradlew test --tests '*ProgramRepositoryTest*' --tests '*WorkoutRepositoryTest*'` → FAIL.
- [ ] **Step 3:** Implementare repository e schermate: **Schede** (spec §6.5; giorno della settimana con menu lun–dom/"nessuno"; target con campi numerici validati `repMin ≤ repMax`, `rirMin ≤ rirMax`) e **Oggi** (spec §6.1; domenica/nessuna scheda → "Oggi riposo" + elenco schede; banner "Riprendi" da `observeUnfinished`; **Inizia** → `startOrResume` → route `workout/{id}`).
- [ ] **Step 4:** `./gradlew test assembleDebug` → PASS.
- [ ] **Step 5: Commit** — `"Schede, schermata Oggi, repository allenamento"`

### Task 9: Schermata Allenamento in corso

**Files:**
- Create: `ui/workout/WorkoutScreen.kt`, `WorkoutViewModel.kt`, `ui/workout/SetRow.kt`, `ui/workout/ExercisePage.kt`, `ui/workout/RecapDialog.kt`
- Test: `app/src/test/java/it/gymlog/app/ui/workout/WorkoutViewModelTest.kt` (coroutines-test, Robolectric, repository reali su DB in memoria)

**Interfaces:**
- Consumes: `WorkoutRepository`, `PlannedExercise`, `PhotoCarousel`, `RestTimer` (Task 10: in questo task chiamare `container.restTimer.start(seconds)`; il Task 10 fornisce l'implementazione, qui basta l'interfaccia `interface RestTimer { val state: StateFlow<TimerState>; fun start(seconds: Int); fun addSeconds(s: Int); fun skip() }` dichiarata in `timer/RestTimer.kt`).
- Produces: `WorkoutViewModel(sessionId, workouts, settings, restTimer)` con `state: StateFlow<WorkoutUiState>` (pagine, righe serie per pagina con flag `done`, passo kg, recap) e azioni `changeWeight(page, row, delta)`, `setWeight`, `changeReps`, `setRir`, `setType`, `setNote`, `confirm(page, row)`, `applySuggestion(page)`, `addSet(page)`, `addExercise(id)`, `replaceExercise(page, id)`, `finish()`.

- [ ] **Step 1: Test che falliscono:**
  - `confirm_persistsSetAndStartsTimerWithExerciseRest` — conferma riga 0 di Incline Chest Press → SetEntry in DB, `FakeRestTimer.started == 150`.
  - `applySuggestion_setsWeightOnUndoneWorkingRows` — righe già fatte invariate.
  - `changeWeight_usesLoadStep_andNeverBelowZero`.
  - `reopenViewModel_restoresDoneRows` — nuova istanza sulla stessa sessione → righe confermate segnate `done` con i valori salvati.
- [ ] **Step 2:** `./gradlew test --tests '*WorkoutViewModelTest*'` → FAIL.
- [ ] **Step 3:** Implementare ViewModel e UI (spec §6.2): `HorizontalPager` con indicatore "3/7"; testata con `PhotoCarousel`, note, target "3 × 6-8 @ RIR 1-2", "Ultima volta: 80 kg × 8, 8, 7"; banner suggerimento; `SetRow` con −/+ da 48 dp, tap sul numero → tastiera numerica decimale (accetta virgola), chip RIR 0–5, chip tipo, nota; ✓ da 64 dp; barra timer fissa in basso; menu "Aggiungi esercizio", "Sostituisci esercizio" (dialogo di ricerca in libreria), "Termina" → `RecapDialog`. Serie confermate restano modificabili (modifica → `updateSet`).
- [ ] **Step 4:** `./gradlew test assembleDebug` → PASS.
- [ ] **Step 5: Commit** — `"Schermata allenamento in corso"`

### Task 10: Timer di recupero (servizio in primo piano)

**Files:**
- Create: `timer/RestTimerImpl.kt`, `timer/RestTimerService.kt`, `timer/TimerState.kt`
- Modify: `timer/RestTimer.kt`, `AndroidManifest.xml` (permission `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `VIBRATE`; service `foregroundServiceType="specialUse"` con property `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` = "Timer di recupero tra le serie"), `AppContainer.kt`, `ui/workout/WorkoutScreen.kt` (richiesta permesso notifiche al primo avvio allenamento su API 33+; se negato, Snackbar una volta: "Senza notifiche il timer suona solo con l'app aperta")
- Test: `app/src/test/java/it/gymlog/app/timer/RestTimerTest.kt`

**Interfaces:**
- Produces: `data class TimerState(val endAtMillis: Long?, val totalSeconds: Int) { fun remainingSeconds(nowMillis: Long): Int }` (0 se finito/nullo, arrotondato per eccesso); `class RestTimerImpl(context, clock: () -> Long, settings) : RestTimer`. Il servizio aggiorna la notifica ogni secondo dal `endAtMillis`; a zero: notifica canale "Fine recupero" (alta priorità, suono se `timerSound`), vibrazione 3 impulsi se `timerVibration`, poi `stopSelf()`. Canali creati in `GymLogApp.onCreate`.

- [ ] **Step 1: Test che falliscono:**
  - `remaining_isComputedFromEndTime` — start(120) a t=0; clock a 61 000 ms → 59; a 200 000 → 0.
  - `addSeconds_extendsEnd` (+30 → 150 a t=0).
  - `skip_clearsState`.
- [ ] **Step 2:** `./gradlew test --tests '*RestTimerTest*'` → FAIL.
- [ ] **Step 3:** Implementare timer, servizio, permessi.
- [ ] **Step 4:** `./gradlew test assembleDebug` → PASS.
- [ ] **Step 5: Commit** — `"Timer di recupero con notifica e vibrazione"`

### Task 11: Storico

**Files:**
- Create: `ui/history/HistoryScreen.kt`, `HistoryViewModel.kt`, `SessionDetailScreen.kt`, `SessionDetailViewModel.kt`, `ExerciseHistoryScreen.kt`, `ExerciseHistoryViewModel.kt`
- Test: `app/src/test/java/it/gymlog/app/ui/history/SessionDetailViewModelTest.kt`

**Interfaces:**
- Consumes: `WorkoutDao.observeFinishedSessions`, `observeSets`, `historyFor`, `WorkoutRepository.updateSet/deleteSet`.

- [ ] **Step 1: Test che falliscono:**
  - `deleteLastSet_keepsSession`; `deleteSession_removesSets`.
  - `editSet_updatesValues` (80×8 → 82.5×7).
- [ ] **Step 2:** `./gradlew test --tests '*SessionDetailViewModelTest*'` → FAIL.
- [ ] **Step 3:** Implementare (spec §6.3): elenco sessioni (data in formato italiano "lun 12 ott 2026", titolo, n. serie, etichetta "Hevy" per le importate); dettaglio raggruppato per esercizio con modifica (dialogo) ed eliminazione con conferma; tap sul nome esercizio → tabella storico (data | serie "80×8 @2").
- [ ] **Step 4:** `./gradlew test assembleDebug` → PASS.
- [ ] **Step 5: Commit** — `"Storico allenamenti ed esercizi"`

### Task 12: Build di consegna e istruzioni

**Files:**
- Create: `INSTALLA.md`, `scripts/build-apk.sh`
- Modify: nessuno

- [ ] **Step 1:** `scripts/build-apk.sh`: esporta `JAVA_HOME`/`ANDROID_HOME`, esegue `./gradlew clean test assembleRelease`, copia l'APK in `dist/GymLog-<versionName>.apk`, esegue `apksigner verify`.
- [ ] **Step 2:** Eseguire lo script → tutti i test PASS, BUILD SUCCESSFUL, `Verified using v2 scheme: true`.
- [ ] **Step 3:** `INSTALLA.md` in italiano semplice: copiare l'APK sul telefono (cavo USB o Google Drive/Telegram a se stessi), aprirlo, consentire "Installa app sconosciute" per l'app usata, installare; primo avvio (permesso notifiche); importare il CSV Hevy (dove trovarlo in Hevy, come selezionarlo); aggiornamenti futuri (installare sopra, **mai disinstallare** o si perdono i dati); **backup della chiave di firma** (`~/GymLog/keystore/` e `keystore.properties` su chiavetta/cloud: senza, gli aggiornamenti richiedono di disinstallare).
- [ ] **Step 4:** Test manuale sul telefono con l'utente (checklist in `INSTALLA.md`): installazione, import CSV reale, allenamento Push A completo, timer a schermo bloccato, chiusura forzata a metà e "Riprendi".
- [ ] **Step 5: Commit** — `"Script di build e istruzioni di installazione"`
