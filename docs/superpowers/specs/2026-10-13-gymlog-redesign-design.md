# GymLog – Ridisegno dell'interfaccia (stile openGym)

Data: 2026-10-13 · Stato: in revisione · Base: sotto-progetto 1 (`master` @ 242ca40)

## 1. Obiettivo

L'interfaccia attuale usa i componenti Material 3 di base: è poco leggibile e poco intuitiva.
Il ridisegno rende GymLog più chiara e piacevole, ispirandosi a **openGym**
(github.com/arvids-unavailable/openGym): tema scuro, schede arrotondate, un colore d'accento,
navigazione con pulsante centrale "Start", allenamento guidato con tabella delle serie.

Successo = in palestra l'utente trova subito l'allenamento di oggi, registra una serie con pochi tocchi
a una mano e capisce a colpo d'occhio cosa ha fatto e cosa manca.

### Vincoli
- **Nessun codice, icona o immagine copiata da openGym** (licenza AGPL-3.0): si riprendono solo
  disposizione e stile, scritti da zero in Jetpack Compose.
- Restano Material 3, Kotlin, Room, MVVM, offline. Nessuna nuova dipendenza.
- **Database invariato** (Room versione 1, nessuna migrazione). Le nuove preferenze vanno in DataStore.
- Logica esistente (ViewModel, repository, import Hevy, timer, regole di progressione) invariata
  salvo le aggiunte elencate al §6; i test esistenti devono restare verdi.
- Lingua italiana, unità kg, target tattili ≥ 48 dp, pulsante principale ≥ 64 dp.

### Fuori scope
Grafici di carichi/1RM e peso corporeo (sotto-progetti 2 e 4): la scheda Statistiche è predisposta
per accoglierli più avanti. Nessuna libreria di esercizi con animazioni.

## 2. Sistema visivo

### Tema
- Impostazione **Tema**: Sistema / Chiaro / Scuro (default **Scuro**).
- Impostazione **Colore d'accento** (default Verde). I colori dinamici di Material You vengono rimossi.

| Accento | Scuro | Chiaro |
|---|---|---|
| Verde | `#32D74B` | `#1FA33A` |
| Blu | `#4C8DFF` | `#1F63E0` |
| Viola | `#A06BFF` | `#7A3FE0` |
| Arancione | `#FF9F1C` | `#D97A00` |
| Rosso | `#FF5A5F` | `#D9363C` |
| Ciano | `#22D3EE` | `#0596B5` |

Ruoli (tutti esposti tramite `MaterialTheme.colorScheme` + un `GymColors` aggiuntivo via CompositionLocal):

| Ruolo | Scuro | Chiaro |
|---|---|---|
| Sfondo | `#000000` | `#F2F2F7` |
| Scheda (`surface`) | `#1C1C1E` | `#FFFFFF` |
| Elemento in rilievo (`surfaceVariant`: stepper, chip, campi) | `#2C2C2E` | `#E5E5EA` |
| Bordo | `#3A3A3C` | `#D1D1D6` |
| Testo | `#FFFFFF` | `#000000` |
| Testo secondario | `#8E8E93` | `#6C6C70` |
| Accento (`primary`) / testo su accento | accento / `#000000` | accento / `#FFFFFF` |
| Contenitore accento (pillole, chip selezionati) | accento al 18% su scheda | accento al 14% su scheda |
| "Riprendi" (`GymColors.resume`) | `#FF9F1C` (se l'accento è Arancione: `#FFD60A`) | `#D97A00` (idem `#B38F00`) |
| Errore | `#FF453A` | `#D70015` |

### Tipografia (font di sistema)
- Titolo di schermata: 34 sp, `FontWeight.ExtraBold`.
- Titolo di scheda/esercizio: 22 sp, Bold.
- Valori numerici (kg, reps, timer, riquadri statistiche): Bold con `fontFeatureSettings = "tnum"` (cifre a larghezza fissa);
  kg/reps 24 sp, timer 32 sp, riquadri 30 sp.
- Etichette di colonna/sezione: 12 sp, maiuscolo, testo secondario, letterSpacing 0.8.

### Forme e spazi
- Schede: angoli 20 dp, padding 16 dp, distanza tra schede 12 dp, margini laterali 16 dp.
- Chip e pillole: completamente arrotondati. Pulsante ✓ della serie: cerchio 48 dp.
- Icone: Material Icons stile Rounded.

### Componenti condivisi (`ui/design/`)
`GymCard`, `SectionLabel`, `ScreenHeader(title, subtitle, action)`, `GymChip`, `AccentPill`,
`Stepper(value, onMinus, onPlus, onTapValue)`, `StatTile(icon, label, value)`, `CircleCheck`,
`PrimaryButton` (≥ 64 dp), `BottomSheet` per i form brevi, `EmptyState`.

## 3. Navigazione

Barra inferiore (nascosta durante l'allenamento e nelle schermate di dettaglio):

`Home · Piano · ● Start · Statistiche · Esercizi`

- **Start**: cerchio d'accento 64 dp in rilievo sopra la barra, etichetta "Start".
  Tocco: se c'è un allenamento in corso lo riapre (il pulsante diventa **"Riprendi"** color `resume`);
  altrimenti avvia la scheda di oggi; se oggi non c'è una scheda apre il pannello "Scegli una scheda"
  (schede del programma attivo + "Allenamento libero").
- **Impostazioni**: icona ingranaggio in alto a destra nella Home.
- **Storico**: icona orologio in alto a destra in Statistiche e link "Vedi tutti".
- Rotte esistenti invariate; nuove: `stats`, `plan` (sostituisce `programs` come voce principale), `history` resta come dettaglio.

## 4. Schermate

### 4.1 Allenamento
- **Barra superiore**: ✕ (esce, l'allenamento resta in corso) · titolo scheda + sotto "mm:ss · fatte/totali serie"
  (cronometro dall'inizio sessione, aggiornato ogni secondo) · ✓ in cerchio d'accento (termina, con conferma)
  · ⋮ (Aggiungi esercizio, Sostituisci esercizio solo oggi).
  Sotto: barra di avanzamento sottile = serie confermate / serie totali previste (righe di tutte le pagine).
- **Pagina esercizio** (pager orizzontale, "Esercizio N/M" sopra la foto):
  - Scheda foto 4:3 (carosello se più foto, tap = schermo intero). Senza foto: scheda segnaposto con
    pulsante "📷 Aggiungi foto" che scatta e salva la foto sull'esercizio (stessa logica della modifica esercizio).
  - Nome 22 sp + pulsante ⓘ che apre un pannello con le note di esecuzione (visibile solo se ci sono note).
  - Chip: muscolo primario, tipo (se diverso da Nessuno), **"Record: X kg"** (se esiste).
  - "Ultima volta: …" (testo secondario) e, se c'è un suggerimento, la pillola d'accento
    "💡 Hai chiuso tutte le serie al massimo — prova X kg"; tap = applica alle serie non fatte.
  - Target sotto il nome: "3 × 6-8 · RIR 1-2 · recupero 2:30" (o "Aggiunto oggi").
- **Tabella serie** in una `GymCard` con intestazione `#  KG  REPS  ✓`:
  - **Riga attiva** = prima riga non confermata (o quella toccata): espansa, con stepper KG e REPS,
    sotto una fila di pulsanti RIR 0–5 e l'icona nota (campo nota sotto se aperto).
  - Righe confermate: compatte, valori in testo, "RIR n" piccolo, ✓ pieno d'accento.
  - Righe future: compatte, valori attenuati (alpha 0.6), ✓ vuoto (bordo). Tap su una riga compatta = diventa attiva.
  - Tap sul numero `#` = menu tipo serie; tipo mostrato come lettera colorata nel badge:
    numero (Allenante), **R** (Riscaldamento, giallo), **D** (Drop, viola), **C** (Cedimento, rosso).
  - Toccare ✓ su una riga non fatta = conferma (come ora) e la riga attiva passa alla successiva.
  - Sotto la tabella: "+ Aggiungi serie".
- **Barra timer** fluttuante (scheda arrotondata sopra il bordo inferiore, ombra): tempo 32 sp, barra di
  avanzamento del recupero, "+15 s", "Salta" (pulsante d'accento). A recupero finito: "Pronto!" e "Chiudi".
- **Schermo acceso**: `FLAG_KEEP_SCREEN_ON` finché la schermata di allenamento è visibile e l'impostazione è attiva (default sì).
- Riepilogo finale: pannello con durata, serie allenanti, volume, record battuti nella sessione.

### 4.2 Home
- `ScreenHeader("GymLog", data di oggi "lunedì 13 ottobre", ⚙)`.
- **Scheda settimana**: "‹ Questa settimana ›" (frecce per settimane precedenti/successive; etichetta
  "Settimana del 6 ott" se non è la corrente). Sette colonne LU…DO con numero del giorno; oggi in cerchio
  d'accento; sotto ogni giorno un pallino: d'accento = allenamento concluso quel giorno, grigio = scheda
  prevista e non ancora fatta (solo per oggi e giorni futuri), niente = riposo. Tap su un giorno = la
  scheda sotto mostra quel giorno.
- **Scheda giorno** (riga dentro la scheda settimana, come openGym): icona d'accento, "OGGI" / nome del giorno,
  nome scheda, "6 esercizi", pulsante **Start** (o **Riprendi** se c'è una sessione in corso di quella scheda).
  Giorno di riposo: "Riposo" + pulsante "Scegli una scheda". Giorno passato già allenato: "Fatto · 52 min" e tap = dettaglio sessione.
- **Scheda serie settimanale**: "🔥 N settimane di fila" + "X/Y questa settimana · T allenamenti in totale"
  (Y = schede assegnate a giorni nel programma attivo).
- **Ultimo allenamento**: titolo, data, durata, serie; tap = dettaglio.

### 4.3 Piano
- `ScreenHeader("Piano", nome del programma attivo ▾)`; il tap su ▾ apre il pannello programmi
  (elenco con spunta sull'attivo, "+ Nuovo programma", rinomina, elimina).
- Sezione **SETTIMANA**: 7 righe (Lunedì…Domenica) come `GymCard` compatte con pillola d'accento del nome
  scheda o pillola grigia "Riposo", chevron. Tap = pannello "Scheda del lunedì" con le schede del programma
  + "Riposo". Assegnare una scheda a un giorno toglie quel giorno all'eventuale altra scheda
  (una scheda per giorno; una scheda ha al più un giorno).
- Sezione **SCHEDE** con "+ Nuova": card per scheda (icona, nome, "N esercizi", giorno se assegnato, chevron).
- **Dettaglio scheda**: header con nome (tap = rinomina), elenco esercizi come card (miniatura foto o icona,
  nome, "3 × 6-8 · RIR 1-2 · 2:30", frecce su/giù), tap = pannello target (campi attuali + "Rimuovi");
  pulsante "+ Esercizio" in fondo; ⋮ = elimina scheda.

### 4.4 Statistiche
- `ScreenHeader("Statistiche", "Progressi e storico", 🕘 → storico)`.
- Griglia 2×2 di `StatTile`: **Allenamenti** (totale sessioni concluse), **Questo mese**,
  **Settimane di fila**, **Serie questa settimana** (serie non riscaldamento).
- **Attività – ultimi 12 mesi**: griglia settimane (colonne) × giorni LU–DO (righe), etichette dei mesi sopra,
  5 livelli: nessun allenamento / ≤ 30 / ≤ 60 / ≤ 90 / > 90 minuti totali nel giorno
  (sessioni importate da Hevy incluse); legenda "Meno ▢▢▢▢ Più"; oggi con bordo. Scorre orizzontalmente,
  aperta sulla settimana corrente.
- **Allenamenti recenti**: ultimi 5 (card con titolo, data, durata, serie) + "Vedi tutti" → storico.
- Storico e dettaglio sessione: stessi contenuti di oggi, ridisegnati con card raggruppate per mese/esercizio.

### 4.5 Esercizi
- `ScreenHeader("Esercizi", "N esercizi")`, campo di ricerca arrotondato, chip muscoli scorrevoli ("Tutti" + 16).
- Prima card: "✨ Crea il tuo esercizio — nome e muscolo bastano" → nuovo esercizio. (Il FAB viene rimosso.)
- Card esercizio: miniatura 56 dp (prima foto o icona su `surfaceVariant`), nome, "Petto · Allungamento",
  badge d'accento "da completare" se `needsReview`.
- Modifica esercizio: stessi campi, raggruppati in card **Dati** (nome, primario, tipo, note), **Muscoli secondari**,
  **Foto**, **Avanzate** (nome Hevy, passo di carico); "Salva" fisso in basso.

### 4.6 Impostazioni
Card raggruppate: **Aspetto** (Tema, Colore d'accento con 6 cerchi selezionabili), **Allenamento**
(passo di carico, formula 1RM, schermo sempre acceso), **Timer** (vibrazione, suono), **Dati** (importa da Hevy).

## 5. Dati nuovi

- `Settings` + `themeMode: ThemeMode {SYSTEM, LIGHT, DARK}` (default DARK), `accent: Accent` (default GREEN),
  `keepScreenOn: Boolean` (default true).
- `WorkoutDao.bestWeight(exerciseId): Double?` = massimo `weightKg` tra le serie non WARMUP di sessioni concluse.
- `WorkoutDao.observeFinishedSessionTimes(): Flow<List<SessionTime>>` (`id, title, startedAt, endedAt, setCount`).
- `WorkoutDao.observeSetsSince(fromMillis): Flow<List<SetType + completedAt>>` per "Serie questa settimana".
- `ProgramRepository.assignDay(programId, dayOfWeek, routineDayId?)`: in transazione imposta `dayOfWeek` della scheda
  scelta e azzera quello dell'eventuale scheda che lo aveva (null = Riposo).
- `PlannedExercise.bestKg: Double?` (riempito da `bestWeight`).

## 6. Logica nuova (Kotlin puro in `domain/`, testata)

- `weekStreak(sessionDates: List<LocalDate>, today: LocalDate): Int` — settimane (lun–dom) consecutive con almeno
  un allenamento; la settimana corrente conta se ha un allenamento, altrimenti si parte dalla precedente senza interrompere.
- `activityLevels(sessions: List<SessionTime>, today: LocalDate, zone): Map<LocalDate, Int>` — livello 0–4 per giorno
  degli ultimi 12 mesi (soglie §4.4; durata = `endedAt − startedAt`, sessioni senza durata contano come livello 1).
- `weekMarkers(weekStart: LocalDate, today, sessionDates, plannedDays: Set<Int>): List<DayMarker>` —
  `DONE` / `PLANNED` / `NONE` per ciascun giorno (PLANNED solo per oggi e futuro).
- `activeRowIndex(rows: List<RowState>, selected: Int?): Int?` — riga selezionata se valida, altrimenti prima non fatta.
- `sessionProgress(pages): Pair<done, total>`.
- `recordsBeaten(sessionSets, previousBest): List<String>` per il riepilogo.

## 7. Test

- JVM: tutti i test esistenti restano verdi.
- Nuovi test per ogni funzione del §6 (casi: nessun allenamento, buco di una settimana, settimana corrente vuota,
  cambio anno, sessione senza `endedAt`, soglie esatte 30/60/90 min, riga selezionata non valida).
- `assignDay`: assegnare un giorno già occupato lo sposta; "Riposo" lo libera.
- `bestWeight`: ignora riscaldamento e sessione in corso.
- `SettingsRepository`: default DARK/GREEN/true e persistenza.
- Tema: test che `gymColorScheme(Accent.X, dark)` restituisce i valori della tabella §2.
- Build release + `apksigner verify`; verifica visiva manuale sul telefono (checklist in `INSTALLA.md` aggiornata).

## 8. Consegna
APK `dist/GymLog-1.1.0.apk` (`versionCode 2`), installabile sopra la 1.0.0 senza perdere dati.
