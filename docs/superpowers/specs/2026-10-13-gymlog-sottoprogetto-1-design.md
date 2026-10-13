# GymLog – Sotto-progetto 1: base, schede, esercizi, registrazione allenamento, import Hevy

Data: 2026-10-13 · Stato: in revisione

## 1. Obiettivo

App Android personale (un solo utente, APK installato a mano, niente Play Store) che sostituisce Hevy
in palestra ogni giorno. Completamente offline: nessun login, nessun server, dati solo sul telefono.

Successo = l'utente registra ogni allenamento con l'app invece che con Hevy, con carichi precompilati
dalla sessione precedente (anche importata da Hevy) e suggerimenti di progressione affidabili.

### Roadmap (fuori da questo sotto-progetto)
- **SP2** – grafici storico (carico max, 1RM Epley/Brzycki, volume, set settimanali per muscolo).
- **SP3** – composizione corporea (pliche, circonferenze, JP7/JP3, Durnin-Womersley, US Navy, FFMI, promemoria).
- **SP4** – peso giornaliero con media mobile 7 gg, export/backup JSON/CSV, poi (facoltativo) pasti.

## 2. Vincoli tecnici

- Kotlin, Jetpack Compose, Material 3, tema chiaro/scuro che segue il sistema (colori dinamici su Android 12+).
- Room (database locale), DataStore Preferences (impostazioni), Navigation Compose.
- Un solo modulo Gradle `:app`, MVVM: Composable → ViewModel (`StateFlow`) → Repository → DAO Room.
- Dipendenze collegate a mano in un `AppContainer` creato nella classe `Application`. Niente Hilt.
- `minSdk 26` (Android 8.0), `targetSdk`/`compileSdk` = ultima API stabile disponibile.
- Unità: solo kg. Lingua interfaccia: italiano.
- Package: `it.gymlog.app`. Nome app: **GymLog**.

### Compilazione (metodo A)
- Compilo io sul PC dell'utente (Fedora) con Android SDK command-line tools in `~/Android/Sdk`
  e Gradle wrapper; JDK 17 o 21 (quello di sistema è 25, non supportato da AGP).
- Output: `app-release.apk` firmato con un **keystore dedicato** creato una volta sola e conservato
  (`~/GymLog/keystore/`, escluso da git, con copia di backup indicata all'utente).
  Motivo: gli aggiornamenti futuri si installano sopra solo se la firma è la stessa;
  cambiare firma obbliga a disinstallare = perdere i dati.
- Il progetto resta apribile in Android Studio.

## 3. Schema database (Room, versione 1)

Enum salvati come stringa.

`Muscle`: PETTO, DORSALI, TRAPEZI, DELTOIDE_ANTERIORE, DELTOIDE_LATERALE, DELTOIDE_POSTERIORE,
BICIPITI, TRICIPITI, AVAMBRACCI, ADDOME, QUADRICIPITI, FEMORALI, GLUTEI, POLPACCI, ADDUTTORI, ABDUTTORI.

`LengthBias`: LENGTHENED, SHORTENED, NONE. · `SetType`: WARMUP, WORKING, DROP, FAILURE.
`SessionSource`: MANUAL, HEVY_IMPORT.

| Tabella | Campi |
|---|---|
| `Exercise` | `id` PK, `name` (unico), `primaryMuscle: Muscle`, `lengthBias`, `executionNotes`, `hevyName?` (unico se presente), `loadStepKg?`, `archived` |
| `ExerciseSecondaryMuscle` | `exerciseId` FK, `muscle` · PK composta |
| `ExercisePhoto` | `id` PK, `exerciseId` FK (cascade), `filePath`, `sortOrder` |
| `Program` | `id` PK, `name`, `isActive` (al più uno true) |
| `RoutineDay` | `id` PK, `programId` FK (cascade), `name`, `dayOfWeek?` (1=lun…7=dom), `sortOrder` |
| `RoutineExercise` | `id` PK, `routineDayId` FK (cascade), `exerciseId` FK, `sortOrder`, `targetSets`, `repMin`, `repMax`, `targetRirMin`, `targetRirMax`, `restSeconds`, `notes` |
| `WorkoutSession` | `id` PK, `routineDayId?` FK (set null), `title`, `startedAt`, `endedAt?`, `notes`, `source` |
| `SetEntry` | `id` PK, `sessionId` FK (cascade), `exerciseId` FK, `exerciseOrder`, `setIndex`, `setType`, `weightKg`, `reps`, `rir?` (0–5), `notes`, `completedAt` |

Note:
- Il target RIR è un intervallo (es. "RIR 1-2") → `targetRirMin`/`targetRirMax`; la regola di progressione usa `targetRirMax`.
- `WorkoutSession` con `endedAt = null` = allenamento in corso (al più uno).
- Le foto sono copiate in `filesDir/photos/<uuid>.jpg` (memoria privata dell'app); eliminare la foto
  dall'app cancella anche il file.
- Un esercizio presente in `SetEntry` o `RoutineExercise` non si cancella: si archivia (`archived = true`,
  nascosto dalla libreria e dalla scelta esercizi, visibile nello storico).

### Impostazioni (DataStore)
`defaultLoadStepKg` (default 2.5), `oneRmFormula` (EPLEY | BRZYCKI, usata da SP2), `timerVibration` (default true),
`timerSound` (default true).

## 4. Logica di dominio (Kotlin puro, testata)

- **Precompilazione**: per un esercizio, le serie dell'ultima `WorkoutSession` conclusa che lo contiene
  (qualsiasi `source`), in ordine. Se la scheda prevede più serie di quelle trovate, le mancanti copiano l'ultima.
  Se non c'è storico: righe vuote quante `targetSets`.
- **Progressione** (`ProgressionRule`): considera solo le serie WORKING dell'ultima sessione per quell'esercizio.
  Suggerisce `carico + passo` (passo = `Exercise.loadStepKg ?: defaultLoadStepKg`) se:
  numero serie WORKING ≥ `targetSets`, tutte con `reps ≥ repMax`, tutte con `rir ≤ targetRirMax`
  (una serie senza RIR registrato non soddisfa la condizione), e stesso carico su tutte le serie
  (se carichi diversi, si usa il minimo come base). Altrimenti nessun suggerimento.
- **Volume frazionario** (base per SP2, implementato ora come funzione): serie WORKING/FAILURE/DROP
  contano 1 sul muscolo primario e 0,5 su ciascun secondario; WARMUP non conta.

## 5. Import CSV Hevy

- File scelto con il selettore di sistema (Storage Access Framework).
- Colonne attese dell'export Hevy: `title, start_time, end_time, description, exercise_title, superset_id,
  exercise_notes, set_index, set_type, weight_kg, reps, distance_km, duration_seconds, rpe`.
  Parser tollerante: colonne per nome (non per posizione), campi tra virgolette con virgole interne,
  formati data tentati in sequenza (es. `12 Oct 2026, 18:30` e ISO). **Da verificare sul file reale
  dell'utente prima dell'implementazione** (l'utente fornisce un export).
- Verificato sull'export reale (2026-10-13, 241 serie, 24 allenamenti): date in italiano `12 ott 2026, 07:57`,
  decimali con il punto, `set_type` sempre `normal`, `rpe` quasi sempre vuoto, alcune serie senza `weight_kg`
  (corpo libero) → importate con 0 kg.
- Mappatura: righe raggruppate per (`title`, `start_time`) → una `WorkoutSession` (`source = HEVY_IMPORT`).
  `exercise_title` → `Exercise` con `hevyName` uguale; se assente, nuovo esercizio
  (`primaryMuscle` = ADDOME come segnaposto e flag "da completare" mostrato in libreria).
  `set_type`: normal→WORKING, warmup→WARMUP, dropset→DROP, failure→FAILURE.
  `rpe` → `rir = clamp(round(10 − rpe), 0, 5)`; rpe vuoto → `rir = null`.
  Righe senza `weight_kg` e `reps` (es. cardio) saltate.
- Idempotente: una sessione già presente con stessa `startedAt` e `title` non viene reimportata.
- Tutto in una transazione; al termine riepilogo: sessioni importate, saltate (doppioni), esercizi creati,
  righe non valide (con numero riga).

## 6. Schermate

Barra di navigazione in basso: **Oggi · Storico · Esercizi · Schede · Impostazioni**.
Comandi principali nella metà bassa dello schermo (uso a una mano), target tattili ≥ 48 dp,
pulsante principale ≥ 64 dp di altezza.

1. **Oggi** – scheda del giorno (programma attivo, `dayOfWeek` = oggi) con pulsante **Inizia**;
   elenco delle altre schede del programma e "Allenamento libero". Banner "Riprendi" se c'è una sessione in corso.
2. **Allenamento in corso**
   - Pager orizzontale: un esercizio per pagina, indicatore "3/7".
   - Testata: carosello foto (tap → schermo intero), note di esecuzione, target ("3 × 6-8 @ RIR 1-2"),
     ultima volta ("80 kg × 8, 8, 7").
   - Banner progressione se applicabile: "Prova 82,5 kg (+2,5)" → tap applica a tutte le serie WORKING non fatte.
   - Righe serie precompilate: kg e reps con pulsanti −/+ (passo kg = passo di carico; reps ±1) e tap per tastiera
     numerica; RIR come chip 0–5; tipo serie (chip, default WORKING); nota opzionale.
   - Pulsante ✓ per riga: salva subito su DB (`completedAt`), avvia il timer con `restSeconds` dell'esercizio.
   - Barra timer fissa in basso: conto alla rovescia, +30 s, Salta. A zero: vibrazione/suono + notifica.
   - Azioni: aggiungi serie, aggiungi esercizio (solo oggi), sostituisci esercizio (solo oggi), termina
     → riepilogo (durata, serie allenanti, volume kg) e salvataggio `endedAt`.
3. **Storico** – elenco sessioni (data, titolo, n. serie); dettaglio con modifica/eliminazione serie e sessione.
   Da un esercizio: tabella storico (data, serie). Grafici in SP2.
4. **Esercizi** – libreria con ricerca e filtro per muscolo; badge "da completare" per gli importati.
   **+** crea; tap modifica: nome, primario, secondari (multi-selezione), tipo, note, foto (fotocamera via
   `TakePicture` o galleria via Photo Picker; riordina, elimina), nome Hevy, passo di carico. Elimina = archivia se usato.
5. **Schede** – programmi (crea, rinomina, attiva, elimina); giorni (nome, giorno settimana, ordine);
   esercizi del giorno (aggiungi dalla libreria, modifica target, frecce su/giù, rimuovi).
6. **Impostazioni** – passo di carico, formula 1RM, vibrazione/suono timer, **Importa da Hevy (CSV)** con riepilogo.

### Timer di recupero
Foreground service con notifica persistente che mostra il tempo residuo; a fine recupero notifica ad alta
priorità + vibrazione. Permesso `POST_NOTIFICATIONS` richiesto al primo avvio di un allenamento (Android 13+).
Se negato, il timer funziona solo con app in primo piano e l'app lo segnala una volta.

## 7. Dati precaricati (primo avvio)

Programma attivo **"Ipertrofia – ottobre 2026"**. Recuperi: "2-3'" = 150 s, "90-120"" = 120 s, "2'" = 120 s, "90"" = 90 s.

| Giorno | Esercizio | Serie | Reps | RIR | Rec. (s) |
|---|---|---|---|---|---|
| Lun – Push A | Incline Chest Press (Machine) | 3 | 6-8 | 1-2 | 150 |
| | Panca Piana (Macchina) | 3 | 6-8 | 1-1 | 150 |
| | Chest Fly (Machine) | 2 | 8-12 | 0-1 | 120 |
| | Overhead Triceps Extension (Cable) | 2 | 8-12 | 0-1 | 120 |
| | Shoulder Press (Machine Plates) | 2 | 6-8 | 1-2 | 150 |
| | Crunch (Machine) | 2 | 8-12 | 1-1 | 90 |
| Mar – Pull A | Lat Pulldown (Machine) | 3 | 6-8 | 1-2 | 150 |
| | Iso-Lateral Low Row | 3 | 6-8 | 1-1 | 150 |
| | Single Lat Pulldown (Macchina) | 2 | 8-10 | 0-1 | 120 |
| | Shrug (Machine) | 3 | 8-12 | 0-1 | 120 |
| | Rear Delt Reverse Fly (Machine) | 2 | 10-15 | 0-1 | 90 |
| | Seated Incline Curl (Dumbbell) | 2 | 8-10 | 0-1 | 120 |
| Mer – Legs | Single Leg Extensions | 3 | 10-12 | 2-2 | 120 |
| | Single Leg Press (Machine) | 3 | 8-12 | 1-2 | 150 |
| | Seated Leg Curl (Machine) | 3 | 8-12 | 0-1 | 120 |
| | Calf Press (Machine) | 3 | 8-12 | 0-1 | 90 |
| | Hip Abduction (Machine) | 2 | 10-15 | 1-1 | 90 |
| | Hip Adduction (Machine) | 2 | 10-15 | 1-1 | 90 |
| | Crunch (Machine) | 2 | 8-12 | 1-1 | 90 |
| Gio – Push B | Incline Chest Press (Machine) | 3 | 6-8 | 1-2 | 150 |
| | Chest Press Declinata (Macchina) | 2 | 6-8 | 1-1 | 150 |
| | Croci Petto Alto (Macchina) | 2 | 8-12 | 0-1 | 120 |
| | Overhead Triceps Extension (Cable) | 2 | 8-12 | 0-1 | 120 |
| | Lateral Raise (Cable) | 2 | 10-15 | 0-1 | 90 |
| | Crunch (Machine) | 2 | 8-12 | 1-1 | 90 |
| Ven – Pull B | Single Lat Pulldown (Macchina) | 3 | 8-10 | 0-1 | 120 |
| | Seated Cable Row - Bar Grip | 3 | 6-8 | 1-1 | 150 |
| | Lat Pulldown (Machine) | 2 | 6-8 | 1-2 | 150 |
| | Shrug (Machine) | 3 | 8-12 | 0-1 | 120 |
| | Rear Delt Reverse Fly (Machine) | 2 | 10-15 | 0-1 | 90 |
| | Single Hammer Curl (Cavo) | 2 | 8-10 | 0-1 | 90 |
| | Single Leg Extensions | 2 | 10-12 | 2-2 | 90 |
| | Seated Leg Curl (Machine) | 2 | 10-12 | 2-2 | 90 |

### Libreria precaricata (`name` = `hevyName`)

| Esercizio | Primario | Secondari | Tipo | Note di esecuzione |
|---|---|---|---|---|
| Incline Chest Press (Machine) | PETTO | TRICIPITI, DELTOIDE_ANTERIORE | NONE | |
| Panca Piana (Macchina) | PETTO | TRICIPITI, DELTOIDE_ANTERIORE | NONE | |
| Chest Press Declinata (Macchina) | PETTO | TRICIPITI, DELTOIDE_ANTERIORE | NONE | |
| Decline Bench Press (Machine) | PETTO | TRICIPITI, DELTOIDE_ANTERIORE | NONE | |
| Chest Fly (Machine) | PETTO | DELTOIDE_ANTERIORE | LENGTHENED | Pausa 1 s in allungamento |
| Croci Petto Alto (Macchina) | PETTO | DELTOIDE_ANTERIORE | LENGTHENED | Pausa 1 s in allungamento |
| Overhead Triceps Extension (Cable) | TRICIPITI | – | LENGTHENED | |
| Triceps Extension (Cable) | TRICIPITI | – | SHORTENED | |
| Single Arm Triceps Pushdown (Cable) | TRICIPITI | – | SHORTENED | |
| Shoulder Press (Machine Plates) | DELTOIDE_ANTERIORE | DELTOIDE_LATERALE, TRICIPITI | NONE | |
| Lateral Raise (Cable) | DELTOIDE_LATERALE | – | LENGTHENED | Cavo dietro il corpo |
| Crunch (Machine) | ADDOME | – | NONE | |
| Lat Pulldown (Machine) | DORSALI | BICIPITI | NONE | |
| Single Lat Pulldown (Macchina) | DORSALI | BICIPITI | LENGTHENED | Enfasi sull'allungamento in alto |
| Iso-Lateral Low Row | DORSALI | TRAPEZI, DELTOIDE_POSTERIORE, BICIPITI | NONE | |
| Seated Cable Row - Bar Grip | DORSALI | TRAPEZI, DELTOIDE_POSTERIORE, BICIPITI | NONE | |
| Shrug (Machine) | TRAPEZI | AVAMBRACCI | NONE | Pausa 1-2 s in alto |
| Rear Delt Reverse Fly (Machine) | DELTOIDE_POSTERIORE | TRAPEZI | NONE | |
| Seated Incline Curl (Dumbbell) | BICIPITI | – | LENGTHENED | |
| Single Hammer Curl (Cavo) | BICIPITI | AVAMBRACCI | NONE | |
| Preacher Curl (Machine) | BICIPITI | – | SHORTENED | |
| Behind the Back Curl (Cable) | BICIPITI | – | LENGTHENED | |
| Estensione Polsi alla Lat Machine (Macchina) | AVAMBRACCI | – | NONE | |
| Single Leg Extensions | QUADRICIPITI | – | NONE | Gamba infortunata: ROM 90°→45°, cuscinetto prossimale sulla tibia |
| Single Leg Press (Machine) | QUADRICIPITI | GLUTEI, ADDUTTORI | NONE | ROM 10°→90° di flessione, niente blocco in estensione, piede stabile |
| Seated Leg Curl (Machine) | FEMORALI | – | LENGTHENED | |
| Lying Leg Curl (Machine) | FEMORALI | – | NONE | |
| Calf Press (Machine) | POLPACCI | – | LENGTHENED | Pausa 1 s in allungamento |
| Standing Calf Raise (Machine) | POLPACCI | – | LENGTHENED | |
| Hip Abduction (Machine) | ABDUTTORI | GLUTEI | NONE | |
| Hip Adduction (Machine) | ADDUTTORI | – | NONE | |

Il seed viene inserito solo se il database è vuoto (callback `onCreate` di Room).

## 8. Gestione errori

- Ogni serie confermata è scritta subito nel DB: chiusura forzata/spegnimento non perde dati;
  al riavvio "Riprendi" riapre la sessione in corso.
- Import: errori di riga non bloccano; file illeggibile o senza colonne attese → messaggio chiaro, nessuna scrittura.
- Foto: se il file manca (es. dati cancellati) si mostra un segnaposto, nessun crash.
- Validazione input: kg ≥ 0 (passo 0,25), reps 0–100, RIR 0–5; `repMin ≤ repMax`, `targetRirMin ≤ targetRirMax`.

## 9. Test

Test JVM (JUnit, `./gradlew test`), eseguiti prima di consegnare ogni APK:
- `ProgressionRule`: tutti i casi della regola (serie mancanti, reps sotto, RIR sopra/mancante, carichi diversi, passo per esercizio).
- Precompilazione (copia ultima serie, nessuno storico).
- Volume frazionario (WARMUP escluso, secondari a 0,5).
- Parser CSV Hevy: virgolette, virgole interne, formati data, mappatura set_type e RPE→RIR, righe non valide, idempotenza.
- DAO Room con Robolectric + database in memoria: seed, archiviazione, cascade, sessione in corso unica.
- Build: `./gradlew assembleRelease` senza errori; verifica firma dell'APK (`apksigner verify`).

Test manuale sul telefono dell'utente: installazione, un allenamento completo, timer a schermo bloccato, import CSV reale.

## 10. Consegna

- APK in `~/GymLog/dist/GymLog-<versione>.apk` + istruzioni in italiano per installarlo
  (copia sul telefono, "installa app sconosciute", aggiornamenti futuri sopra la versione precedente).
- Fuori scope SP1: grafici, composizione corporea, peso giornaliero, pasti, export/backup
  (fino a SP4 il backup consigliato è il backup di sistema Android; nessuna promessa di ripristino).
