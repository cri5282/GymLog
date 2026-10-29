# Piano di miglioramento di GymLog

## Ispirato a openGym: accessibilità, comodità, navigabilità, modificabilità, visualizzazione

- **Data:** 26 ottobre 2026
- **GymLog analizzata:** 1.3.1 (`versionCode` 6)
- **openGym analizzata:** v1.4.2 (commit `08eabf4f`)
- **Tipo di analisi:** lettura del codice, README, roadmap, changelog e screenshot dei due progetti. Non è stato eseguito un test manuale su dispositivo di openGym.

---

## 1. Sintesi esecutiva

GymLog ha già una base solida: è offline-first, veloce da usare in palestra, ha un modello dati
coerente, backup completo, import Hevy e 181 test JVM. Il problema non è la mancanza di funzioni,
ma la **fluidità complessiva**: alcune azioni richiedono troppi passaggi, le gesture non hanno
sempre un'alternativa accessibile, i controlli personalizzati non espongono stato e ruolo a
TalkBack, e le schermate più dense (Piano, Statistiche, Impostazioni) crescono in verticale senza
una vera gerarchia.

openGym è utile soprattutto come **catalogo di pattern**, non come modello da copiare in blocco.
I cinque pattern più interessanti sono:

1. **Allenamento "quieto":** un solo menu per esercizio, il numero della serie è il menu della
   serie, chip di navigazione tra gli esercizi.
2. **Azioni reversibili:** swipe per eliminare o copiare, ma sempre con un'azione "Annulla" e con
   un pulsante visibile come alternativa.
3. **Poche destinazioni primarie:** Home, Piano, Start, Statistiche, Esercizi; il resto vive
   dentro le schermate.
4. **Dati modificabili davvero:** una sessione salvata si può correggere, ripetere, spostare di
   data, salvare come scheda.
5. **Progressi leggibili:** mappa muscolare, filtri temporali, obiettivo peso, foto prima/dopo,
   grafici con una spiegazione testuale.

La proposta è procedere in **tre ondate**, ognuna con un tema chiaro:

| Ondata | Versione proposta | Tema | Obiettivo |
| --- | --- | --- | --- |
| 1 | **1.4.0** | *Accessibile e reversibile* | Rendere l'app usabile con TalkBack, testo grande e una sola mano; eliminare la paura di sbagliare con Annulla e azioni esplicite. |
| 2 | **1.5.0** | *Modificare e ritrovare* | Permettere di correggere qualsiasi dato, ripetere un allenamento, riorganizzare schede e ritrovare una sessione. |
| 3 | **1.6.0** | *Capire i progressi* | Rendere statistiche, corpo e piano più leggibili e utili, con mappa muscolare, intervalli temporali e foto progressi. |

---

## 2. Cosa vale la pena prendere da openGym

| Pattern openGym | Perché funziona | Adattamento a GymLog |
| --- | --- | --- |
| **Un menu per esercizio e il numero della serie come menu** | Toglie 8–10 pulsanti dalla schermata e lascia in primo piano solo ciò che serve tra una serie e l'altra. | Spostare note, tipo serie, copia, elimina e dettagli nel menu del badge serie; tenere visibili solo kg, reps, RIR e ✓. |
| **Swipe con Annulla** | La gesture è veloce per chi la conosce, ma non è l'unica via e l'errore è recuperabile. | Aggiungere `SwipeToDismissBox` per eliminare/copiare una serie, con Snackbar "Annulla" e le stesse azioni in un menu accessibile. |
| **Chip degli esercizi** | Dà una mappa mentale della sessione: cosa è finito, cosa è iniziato, dove sono. | Barra orizzontale sticky con un chip per esercizio, stato vuoto/parziale/completo, tap per saltare alla pagina. |
| **Cards / List / Compact / Focus** | Lo stesso allenamento può essere usato in modi diversi: uno esercizio alla volta, tutto in lista, solo numeri. | Aggiungere almeno "Pagina" (attuale) e "Lista"; in futuro "Compatta". La scelta si salva nelle impostazioni. |
| **Impostazioni dentro l'allenamento** | Evita di uscire dalla sessione per cambiare suono, vibrazione, schermo acceso o layout. | Bottom sheet "Impostazioni allenamento" dal menu ⋯, con le stesse preferenze della schermata Impostazioni. |
| **Calcolatore dischi** | Elimina il calcolo mentale e gli errori di carico. | Dialog con peso bilanciere, dischi disponibili e carico per lato; riga "Dischi: 20 + 10 per lato" sotto la serie. |
| **Ultima volta / Record** | Dà un riferimento immediato e permette di scegliere cosa inseguire. | Riga "Ultima volta" toccabile per passare a "Record"; badge record quando si conferma una serie. |
| **Piano diviso in Settimana e Schede** | Separa "quando mi alleno" da "cosa faccio", riducendo la lunghezza della schermata. | Segmented control in Piano: **Settimana** / **Schede**, con azioni di scheda (duplica, elimina, riordina). |
| **Libreria con filtri, preferiti e simili** | Con molti esercizi, la ricerca da sola non basta. | Filtri per muscolo, attrezzo, preferiti e recenti; conteggio risultati; suggerimenti "simili" quando non c'è match. |
| **Modifica dello storico e ripetizione** | I dati sono dell'utente e devono poter essere corretti. | Editor completo della sessione salvata e "Ripeti oggi" dallo storico. |
| **Mappa muscolare e filtri temporali** | Trasforma i numeri in una risposta: dove sono andato, cosa ho trascurato, sto migliorando? | Mappa semplificata fronte/retro con modalità Volume / Recupero / Non allenati; filtri 1M / 3M / 1A / Tutto. |
| **Obiettivo peso e foto progressi** | Collega le misure a un traguardo visivo. | Linea obiettivo nel grafico peso; foto progressi con timeline e confronto prima/dopo. |
| **Accessibilità "by default"** | Target ≥ 44 px, focus visibile, `aria-*`, riduzione del movimento, stato selezionato. | In Compose: target ≥ 48 dp, semantica esplicita, `stateDescription`, `heading`, `liveRegion`, test con TalkBack e font scale. |

### Cosa non copiare (almeno ora)

- **Account, passkey, sincronizzazione cloud, self-hosting:** GymLog è volutamente locale e
  offline-first. Sono un altro prodotto, non un miglioramento UX.
- **AI coach, social, trainer, admin:** aggiungono superficie e rischi senza risolvere i problemi
  quotidiani.
- **Catalogo da 5.600 esercizi con animazioni:** pesa decine/centinaia di MB e richiede licenze.
  Meglio migliorare ricerca, preferiti e media personali.
- **19 lingue e RTL:** non prioritario per un'app personale in italiano; però conviene non
  introdurre nuove stringhe hardcoded e preparare il terreno.
- **Health Connect / Apple Health / wearable:** utile, ma richiede permessi e integrazioni che
  esulano dal primo ciclo.
- **Motore di progressione complesso (linear, Greyskull, tripla, deload):** la doppia progressione
  attuale è già un buon inizio; prima rendiamo trasparente e modificabile quella.

---

## 3. Dove siamo: punti di forza e gap

### Punti di forza da non perdere

- **Velocità di registrazione:** kg, reps, RIR e ✓ sono già in primo piano; la precompilazione
  dall'ultima sessione funziona.
- **Modello dati ricco:** serie con tipo, RIR, note, unilaterale, superserie, recupero per esercizio.
- **Timer di recupero robusto:** foreground service, notifica, wake lock, suono e vibrazione.
- **Backup e import:** ZIP completo, CSV, Hevy; questo è un vantaggio concreto rispetto a molte app.
- **Statistiche già presenti:** heatmap, volume, serie per muscolo, record, 1RM, asimmetria.
- **Test:** 181 test JVM e script di build release; è una base su cui costruire senza paura.

### Gap principali

| Area | Stato attuale | Conseguenza |
| --- | --- | --- |
| **Navigazione** | 6 destinazioni nella bottom bar (Home, Piano, Start, Statistiche, Corpo, Esercizi), etichette a una riga, altezza fissa 64 dp. | Affollamento, rischio di troncamento con testo grande, "Corpo" è troppo prominente per una sezione facoltativa. |
| **Controlli custom** | Vari `Box.clickable` da 32–44 dp (badge tipo serie, chip, stepper, celle giorno). | Target sotto i 48 dp, stato non annunciato, difficoltà con Switch Access e TalkBack. |
| **Semantica** | Pochi `contentDescription`; niente `role`, `selected`, `stateDescription`, `heading`. | TalkBack legge il testo ma non dice "selezionato", "serie salvata", "tipo riscaldamento", "grafico". |
| **Grafici** | `Canvas` e heatmap senza alternativa testuale. | Informazioni inaccessibili a screen reader e a chi non distingue i colori. |
| **Undo** | Eliminazioni con dialog di conferma, nessun Annulla. | Paura di sbagliare, più tap, nessun recupero dopo una conferma. |
| **Allenamento** | Solo `HorizontalPager`; nessuna panoramica degli esercizi; eliminazione solo sulla riga attiva. | Difficile saltare a un esercizio, correggere una serie già fatta, copiare una serie. |
| **Timer** | Solo −15 / +15 / salta. | Non si può mettere in pausa, impostare un tempo preciso o cambiare recupero al volo. |
| **Carico** | Nessun calcolatore dischi. | Calcolo mentale e rischio di caricare male il bilanciere. |
| **Piano** | Settimana e schede nello stesso flusso; niente duplica/elimina/riordina scheda dalla panoramica. | Modifiche lente e poco reversibili. |
| **Libreria** | Ricerca testo + filtro muscolo. | Niente preferiti, recenti, attrezzi, conteggio, suggerimenti. |
| **Storico** | Elenco per mese; modifica di una singola serie; nessuna ricerca. | Non si ritrova una sessione, non si corregge un allenamento intero, non si ripete. |
| **Statistiche** | Sezioni statiche in un'unica pagina lunga. | Poca gerarchia, nessun filtro temporale, nessuna mappa muscolare. |
| **Corpo** | Peso, media, circonferenze, pliche, stime. | Manca obiettivo, foto progressi, confronto visuale. |
| **Impostazioni** | Una pagina lunga, nessuna ricerca, nessuna sezione accessibilità. | Difficile trovare un'opzione; impossibile adattare l'app a testo grande o movimento ridotto. |
| **Onboarding** | Nessun tour iniziale; i permessi notifiche vengono chiesti all'avvio dell'allenamento. | Primo uso meno guidato e permesso negato più facilmente. |

---

## 4. Principi guida

1. **Una schermata, una domanda.** Home: "che cosa devo fare oggi?". Allenamento: "quale serie
   sto facendo?". Statistiche: "sto migliorando?". Piano: "cosa devo fare questa settimana?".
2. **Nessuna azione distruttiva senza recupero.** Ogni eliminazione ha un Annulla o una copia di
   sicurezza; le conferme restano solo per azioni davvero irreversibili.
3. **Le gesture accelerano, non sono l'unica via.** Swipe, long-press e trascinamento devono avere
   sempre un pulsante o una voce di menu equivalente.
4. **I dati sono modificabili in ogni momento.** Una sessione salvata, una misurazione, una scheda
   e un esercizio devono poter essere corretti senza cancellare e ricreare.
5. **Ogni grafico ha una versione testuale.** Un `contentDescription` con il riassunto e un
   pannello "Mostra dati" con i valori principali.
6. **Accessibilità non significa solo etichette.** Significa target grandi, stato esplicito,
   contrasto, testo scalabile, feedback non solo visivo, nessuna informazione affidata al solo colore.
7. **Release piccole e coerenti.** Meglio tre release da un tema ciascuna che una release enorme
   con venti funzioni scollegate.

---

## 5. Piano per dimensione

### 5.1 Accessibilità

| ID | Intervento | Dettaglio | Sforzo | Priorità |
| --- | --- | --- | --- | --- |
| **A1** | Target touch ≥ 48 dp | Portare a 48 dp `GymChip`, `TypeBadge`, `Stepper`, `RoundIconButton`, celle giorno, pillole e voci bottom bar. Usare `minimumInteractiveComponentSize()` per i controlli più piccoli senza cambiarne l'aspetto. | S | **P0** |
| **A2** | Semantica e stato | Aggiungere `Role.Button`, `Role.Tab`, `selected`, `stateDescription`, `heading`, `contentDescription` a tutti i controlli custom. Le righe cliccabili devono essere raggruppate con `mergeDescendants`. | M | **P0** |
| **A3** | Grafici accessibili | Ogni grafico espone un riassunto (`contentDescription`) e un pulsante "Mostra dati" con tabella/elenco. Heatmap: ogni giorno con descrizione "12 ottobre, Push A, 18 serie". | M | **P0** |
| **A4** | Testo grande e layout | Test a font scale 1.3, 1.5, 2.0. Togliere `softWrap = false` e altezze fisse dai testi critici; permettere alle chip di andare a capo o scorrere; bottom bar con fallback a sole icone sopra una certa scala. | M | **P0** |
| **A5** | Contrasto e colore | Verificare WCAG AA su testo, testo secondario, accento, giallo riscaldamento e viola drop. Aggiungere un tema ad alto contrasto. Non usare il solo colore: lettere R/D/C per i tipi, forme diverse per i marker del calendario, pattern nei grafici. | M | **P0** |
| **A6** | Movimento e feedback | Rispettare "Rimuovi animazioni" di Android; aggiungere un toggle "Riduci animazioni". Feedback aptico su conferma serie, eliminazione e fine recupero, con toggle. | S | P1 |
| **A7** | Tastiera e Switch Access | Ordine di focus esplicito, focus visibile, nessuna azione raggiungibile solo con swipe. Test con tastiera Bluetooth e Switch Access. | M | P1 |
| **A8** | Test e checklist | Aggiungere `ui-test-junit4` e test di semantica; checklist TalkBack manuale; Accessibility Scanner; screenshot a font scale 2.0. | M | P1 |

**Definition of done per l'ondata 1:**

- Accessibility Scanner non segnala target troppo piccoli nelle schermate principali.
- TalkBack annuncia, per ogni controllo: nome, ruolo, stato e azione.
- Tutti i grafici hanno un riassunto testuale e un elenco dati.
- A font scale 2.0 nessun testo critico è troncato o sovrapposto.
- Le azioni distruttive hanno un'alternativa accessibile e un Annulla.

---

### 5.2 Comodità

| ID | Intervento | Dettaglio | Sforzo | Priorità |
| --- | --- | --- | --- | --- |
| **C1** | Annulla globale | Snackbar con "Annulla" per eliminazione serie, misurazione, sessione, scheda, esercizio. Ripristino esatto tramite snapshot o soft-delete. | M | **P0** |
| **C2** | Menu serie + swipe | Tap sul badge serie apre un menu: copia, duplica, elimina, cambia tipo, nota. Swipe a sinistra elimina, a destra duplica; entrambe le azioni restano nel menu. | M | **P0** |
| **C3** | Chip esercizi | Barra orizzontale sticky con numero, stato (vuoto/parziale/completo) e tap per saltare. Si nasconde se c'è un solo esercizio. | M | **P0** |
| **C4** | Timer recupero completo | Pausa/riprendi, tap sul clock per impostare un tempo preciso (preset 1:00, 1:30, 2:00, 3:00 + wheel), modifica rapida del recupero dell'esercizio. | M | **P0** |
| **C5** | Impostazioni in allenamento | Bottom sheet dal menu ⋯: suono, vibrazione, schermo acceso, layout, RIR/RPE, chip, swipe. Le modifiche valgono anche per le sessioni successive. | M | **P0** |
| **C6** | Calcolatore dischi | Peso bilanciere + dischi disponibili; carico per lato; riga sotto la serie con i dischi da mettere. Supporto bilanciere, EZ, trap bar e "nessun bilanciere". | M | P1 |
| **C7** | Ultima volta / Record | Riga "Ultima volta (3 giorni fa)" toccabile per passare a "Record"; badge record live alla conferma della serie. | S | P1 |
| **C8** | Azioni rapide in Home | Pesata rapida, prossima sessione, "Riprendi", "Scegli un'altra scheda"; obiettivo peso. | M | P1 |
| **C9** | Ripeti / copia | "Ripeti oggi" da una sessione passata; "Salva come scheda" da una sessione; duplica serie e duplica scheda. | M | P1 |
| **C10** | Promemoria | Notifica opzionale il giorno dell'allenamento e promemoria "prossima sessione"; spiegazione del permesso prima della richiesta. | M | P1 |
| **C11** | Unità e preferenze | kg/lb, inizio settimana lunedì/domenica, recupero predefinito, formula 1RM già presente. | S | P1 |
| **C12** | Media esercizi | Galleria e video oltre alla fotocamera; miniatura nella lista e nella pagina allenamento. | M | P2 |
| **C13** | Esercizi a tempo e cardio | Durata, distanza, velocità, recupero per lato; richiede migrazione del modello dati. | L | P2 |

---

### 5.3 Navigabilità

| ID | Intervento | Dettaglio | Sforzo | Priorità |
| --- | --- | --- | --- | --- |
| **N1** | Bottom bar a 5 voci | Home, Piano, Start, Statistiche, Esercizi. "Corpo" diventa una scheda dentro Statistiche (o un accesso dal riepilogo). Impostazioni raggiungibili dall'intestazione di ogni schermata. | M | **P0** |
| **N2** | Piano diviso | Segmented "Settimana" / "Schede". Nella parte Schede: duplica, elimina con Annulla, riordina, "Nuova scheda". | M | P1 |
| **N3** | Salto esercizio | Oltre alle chip, un pulsante "Esercizi" apre un elenco con stato e tap per andare; mantiene la pagina corrente. | S | **P0** |
| **N4** | Storico cercabile | Ricerca per titolo/esercizio, filtro per scheda e periodo, calendario mensile, "Ripeti" e "Modifica". | M | P1 |
| **N5** | Statistiche a schede | "Panoramica", "Esercizi", "Corpo" in segmented control o sezioni collassabili; meno scroll, più drill-down. | M | P1 |
| **N6** | Libreria completa | Campo di ricerca con clear e conteggio; filtri muscolo, attrezzo, preferiti, recenti; "nessun risultato" con suggerimenti simili; creazione esercizio sempre visibile. | M | P1 |
| **N7** | Impostazioni con ricerca | Sezioni: Aspetto, Allenamento, Accessibilità, Corpo, Dati, Backup. Ricerca in stile openGym. | M | P1 |
| **N8** | Stato e back | Conservare scroll, pagina allenamento, tab selezionata e filtri; gestire il back predittivo di Android 14+. | M | P1 |
| **N9** | Onboarding e stati vuoti | Primo avvio con 3 schermate (cosa fa, permessi, import Hevy); ogni stato vuoto con un'azione chiara. | M | P2 |
| **N10** | Widget azioni rapide | Start/Riprendi, pesata, prossima sessione; oltre al semplice tap che apre l'app. | M | P2 |

---

### 5.4 Modificabilità

| ID | Intervento | Dettaglio | Sforzo | Priorità |
| --- | --- | --- | --- | --- |
| **M1** | Editor sessione completa | Dalla sessione salvata: titolo, data, ora di inizio, durata, note, aggiunta/rimozione serie ed esercizi, riordino. Ricalcolo di record e 1RM. | L | P1 |
| **M2** | Serie ed esercizi nel passato | Aggiungere una serie a una sessione già conclusa; spostare una sessione a un'altra data; correggere l'ordine degli esercizi. | M | P1 |
| **M3** | Azioni scheda | Duplica scheda, elimina con Annulla, riordina, salva sessione come scheda, scegli da template (Push/Pull/Legs, Upper/Lower, Full Body). | M | P1 |
| **M4** | Rotazione / più schede per giorno | Modalità alternativa alla settimana fissa: A, B, C, A… con "prossima sessione non ancora fatta". Più schede nello stesso giorno. | L | P2 |
| **M5** | Anagrafica esercizi | Archivia/ripristina, unisci duplicati, flag bodyweight/assistito/unilaterale, attrezzo, nome Hevy, foto/video. | M | P1 |
| **M6** | Import/export piano | File JSON leggibile per condividere una scheda o un programma, con merge invece di sostituzione. | M | P1 |
| **M7** | Note e sforzo | Nota di sessione, RPE opzionale oltre al RIR, sensazione/energia, eventuale dolore. | S | P1 |
| **M8** | Gestione dati | "Elimina tutti i dati" con doppia conferma e backup automatico; ripristino con scelta "sostituisci" o "unisci"; retention dei backup. | M | P2 |
| **M9** | Schemi avanzati | Recupero per serie, piramide, drop set, rest-pause, serie a tempo; richiede modello dati e UI dedicata. | XL | P2 |

---

### 5.5 Visualizzazione

| ID | Intervento | Dettaglio | Sforzo | Priorità |
| --- | --- | --- | --- | --- |
| **V1** | Componente grafico unico e accessibile | `GymChart` con: titolo, unità, intervallo 1M/3M/1A/Tutto, tooltip al tap, asse e valori, `contentDescription`, "Mostra dati". Applicarlo a carico, 1RM, volume, peso, asimmetria. | M | **P0** |
| **V2** | Mappa muscolare | Fronte/retro semplificata, tre modalità: **Volume** (serie per muscolo), **Recupero** (ore/giorni dall'ultimo stimolo), **Non allenati** (ultimi 14/30 giorni). Tap su un muscolo per vedere gli esercizi. | L | P1 |
| **V3** | Panoramica statistiche | Card con trend (↑/↓), confronto con la settimana precedente, record recenti, costanza, volume per gruppo muscolare. | M | P1 |
| **V4** | Corpo: obiettivo e foto | Linea obiettivo peso, foto progressi con timeline, confronto prima/dopo, andamento per circonferenza. | L | P1 |
| **V5** | Dettaglio sessione | Volume per esercizio, serie per muscolo, record battuti, note, durata effettiva; accesso a "Ripeti" e "Modifica". | M | P1 |
| **V6** | Copertura del piano | Nella settimana: serie previste per muscolo, buchi evidenti, avviso "questo gruppo non è allenato". | M | P2 |
| **V7** | Allenamento più leggibile | Tipo serie con lettera + colore, record live, barra di avanzamento per esercizio, miniatura, timer circolare. | S | P1 |
| **V8** | Heatmap interattiva | Tap su un giorno per aprire la sessione; marker riposo/nota; legenda accessibile. | S | P1 |
| **V9** | Calendario storico | Vista mese con giorni allenati, riposo e note; tap per il dettaglio. | M | P2 |
| **V10** | PDF e widget | PDF più leggibile, widget con azione rapida e stato "in corso". | S | P2 |

---

## 6. Roadmap consigliata

### Sprint 0 — Fondamenta tecniche (3–5 giorni)

Prima di aggiungere funzioni, preparare il terreno:

1. Creare un `GymScaffold` con `SnackbarHostState` e un canale di eventi UI (`UiEvent`) per
   messaggi e Annulla.
2. Rendere accessibili i componenti condivisi: `GymCard`, `GymChip`, `AccentPill`, `Stepper`,
   `RoundIconButton`, `CircleCheck`, `BottomBar`.
3. Aggiungere a `Settings` i campi: `workoutLayout`, `effortScale`, `showExerciseChips`,
   `haptics`, `reduceMotion`, `highContrast`, `weekStart`, `defaultRestSeconds`, `barWeightKg`,
   `plateInventory`, `goalWeightKg`, `reminderEnabled`.
4. Aggiungere `ui-test-junit4` e i primi test di semantica.
5. Estrarre le stringhe italiane ripetute in un oggetto `Strings` o in `strings.xml` (facoltativo
   ma consigliato per coerenza e futuro i18n).

### Ondata 1 — 1.4.0 "Accessibile e reversibile"

**Obiettivo:** l'app si può usare con TalkBack, testo grande e una mano; sbagliare non fa paura.

- **A1–A5** target, semantica, grafici accessibili, font scale, contrasto.
- **C1–C5** Annulla, menu serie, chip esercizi, timer completo, impostazioni in allenamento.
- **N1, N3** bottom bar a 5 voci e salto esercizio.
- **V1, V7** grafico accessibile e allenamento più leggibile.
- **C6** calcolatore dischi, se resta tempo.

**Criterio di rilascio:** nessuna regressione nei 181 test; almeno 15 nuovi test su Annulla,
semantica e timer; APK installabile sopra la 1.3.x senza perdita dati.

### Ondata 2 — 1.5.0 "Modificare e ritrovare"

**Obiettivo:** ogni dato è correggibile e ogni cosa si ritrova.

- **M1–M3, M5** editor sessione, serie nel passato, azioni scheda, anagrafica esercizi.
- **N2, N4–N7** Piano diviso, storico cercabile, statistiche a schede, libreria completa,
  impostazioni con ricerca.
- **C7–C9** ultima volta/record, azioni rapide Home, ripeti/copia.
- **V5, V8** dettaglio sessione e heatmap interattiva.

### Ondata 3 — 1.6.0 "Capire i progressi"

**Obiettivo:** l'app risponde a "sto migliorando?" e "cosa sto trascurando?".

- **V2–V4, V6** mappa muscolare, panoramica, corpo con obiettivo e foto, copertura del piano.
- **C10–C12** promemoria, unità/preferenze, media esercizi.
- **M6–M8** import/export piano, note e sforzo, gestione dati.
- **N9–N10** onboarding e widget evoluto.

### Dopo — 1.7 / 2.0

- **M4** rotazione e più schede per giorno.
- **M9 / C13** schemi avanzati, esercizi a tempo, cardio.
- **V9–V10** calendario storico, PDF/widget evoluti.
- **i18n** solo se serve davvero.

---

## 7. Primi dieci interventi in dettaglio

### 7.1 A1 + A2 — Fondamenta accessibili

**File:** `ui/design/Components.kt`, `ui/nav/BottomBar.kt`, `ui/home/HomeScreen.kt`,
`ui/workout/SetTable.kt`, `ui/exercises/ExerciseListScreen.kt`, `ui/settings/SettingsScreen.kt`.

**Cosa fare:**

- Sostituire i `Box.clickable` con `Surface(onClick=...)`, `IconButton` o aggiungere
  `Modifier.minimumInteractiveComponentSize()` + `semantics { role = Role.Button }`.
- `GymChip`: usare `Modifier.selectable(selected, role = Role.RadioButton)` o
  `semantics { selected = ... }`.
- `BottomBar`: `Role.Tab`, `selected`, `stateDescription` ("Home, selezionata").
- `TypeBadge`: `contentDescription = "Tipo serie: Riscaldamento"`,
  `stateDescription = "R"`.
- `Stepper`: il valore è un pulsante con etichetta "Inserisci kg"; − e + hanno
  "Diminuisci kg" / "Aumenta kg".
- `DayCell`: `selected`, `stateDescription` ("Oggi, Push A, programmato").
- `GymCard(onClick)`: `semantics(mergeDescendants = true) { role = Role.Button }`.
- Titoli di schermata e sezioni: `heading()`.

**Done when:** con TalkBack si sente sempre "cosa è, che stato ha, cosa succede se lo tocchi".

### 7.2 C1 — Annulla globale

**File:** `ui/nav/AppNavHost.kt`, `ui/common/Dialogs.kt`, `data/repo/*`, `ui/*ViewModel.kt`.

**Cosa fare:**

- Aggiungere `SnackbarHost` allo `Scaffold` principale.
- Definire un `UiEvent.ShowUndo(message, onUndo)`.
- Per le eliminazioni: snapshot dell'entità prima della cancellazione; `onUndo` la reinserisce
  con lo stesso id (o soft-delete).
- Messaggi: "Serie rimossa", "Misurazione eliminata", "Allenamento eliminato", "Scheda eliminata".
- Durata 5 s; il pulsante "Annulla" resta raggiungibile con TalkBack.

**Test:** eliminare una serie salvata, annullare, verificare che la serie torni con lo stesso
`setIndex` e gli stessi valori.

### 7.3 C2 — Menu serie e swipe

**File:** `ui/workout/SetTable.kt`, `ui/workout/WorkoutViewModel.kt`, `ui/workout/WorkoutScreen.kt`.

**Cosa fare:**

- Tap sul badge serie → `DropdownMenu` o `ModalBottomSheet` con:
  - Copia serie
  - Duplica serie
  - Elimina serie
  - Cambia tipo (riscaldamento, allenante, drop, cedimento)
  - Aggiungi/modifica nota
- Swipe a sinistra → elimina; swipe a destra → duplica.
- Le azioni da swipe devono essere disponibili anche nel menu (mai solo gesture).
- Dopo l'eliminazione, Snackbar con Annulla.
- Mantenere la rinumerazione dei `setIndex` già implementata.

**Test:** eliminazione, Annulla, duplicazione, copia, rinumerazione, serie già salvata.

### 7.4 C3 + N3 — Chip e salto esercizio

**File:** `ui/workout/WorkoutScreen.kt`, nuovo `ui/workout/ExerciseChips.kt`,
`ui/workout/WorkoutViewModel.kt`.

**Cosa fare:**

- `LazyRow` sticky sotto la top bar.
- Un chip per esercizio (o per superserie), con stato:
  - vuoto: nessuna serie fatta
  - parziale: almeno una serie fatta
  - completo: tutte le serie fatte
- Chip corrente con bordo accento.
- Tap → `pager.animateScrollToPage(index)`.
- `contentDescription`: "Esercizio 2: Panca piana, iniziato".
- Pulsante "Esercizi" nel menu ⋯ che apre un elenco completo con stato.

**Test:** con 6 esercizi, saltare all'ultimo, tornare al primo, stato aggiornato dopo ✓.

### 7.5 C4 — Timer di recupero completo

**File:** `ui/workout/RestTimerBar.kt`, `ui/workout/WorkoutViewModel.kt`, `timer/RestTimer.kt`,
`ui/settings/SettingsScreen.kt`.

**Cosa fare:**

- Pausa/riprendi.
- Tap sul clock → dialog con preset (1:00, 1:30, 2:00, 3:00) e campo personalizzato.
- Il timer continua a funzionare in background; la notifica riflette pausa/ripresa.
- Nel menu esercizio: "Recupero" con preset e "Usa predefinito".
- A fine recupero: suono/vibrazione/flash secondo impostazioni.

**Test:** pausa, ripresa, modifica tempo, skip, fine con notifica.

### 7.6 C5 — Impostazioni in allenamento

**File:** `ui/workout/WorkoutTopBar.kt`, nuovo `ui/workout/WorkoutSettingsSheet.kt`,
`data/settings/SettingsRepository.kt`.

**Cosa fare:**

- Bottom sheet con:
  - Suono / vibrazione / schermo acceso
  - Layout: Pagina / Lista / Compatta
  - Sforzo: Off / RIR / RPE
  - Chip esercizi on/off
  - Swipe on/off
- Le modifiche sono persistenti e condivise con Impostazioni.
- Nessuna impostazione "solo per oggi", tranne il layout se si vuole.

**Test:** cambiare layout, tornare in allenamento, riaprire l'app, verificare la persistenza.

### 7.7 C6 — Calcolatore dischi

**File:** nuovo `ui/workout/PlateCalculator.kt`, `data/settings/SettingsRepository.kt`,
`ui/workout/SetTable.kt` o `ExercisePage.kt`.

**Cosa fare:**

- Impostazioni: peso bilanciere, dischi disponibili (coppie), eventuale "nessun bilanciere".
- Dialog: peso target, peso bilanciere, dischi per lato, resto non caricabile.
- Riga sotto la serie: "Dischi: 20 + 10 per lato" oppure "Barra scarica".
- Quick pick: 20 / 15 / 10 / 5 / 2,5 / 1,25 kg.
- Testo accessibile: "Per lato: un disco da 20 kg e uno da 10 kg".

### 7.8 N1 — Bottom bar a 5 voci

**File:** `ui/nav/BottomBar.kt`, `ui/nav/AppNavHost.kt`, `ui/stats/StatsScreen.kt`.

**Cosa fare:**

- Voci: Home, Piano, Start, Statistiche, Esercizi.
- "Corpo" diventa una card/scheda dentro Statistiche; l'accesso diretto resta dal riepilogo.
- Ingranaggio Impostazioni nell'intestazione di Home, Piano, Statistiche, Esercizi.
- `Role.Tab`, `selected`, etichette con `maxLines = 2` o fallback icon-only a font scale alto.
- Start centrale resta l'azione primaria.

**Perché:** riduce l'affollamento, dà più respiro alle etichette e rende "Corpo" una sezione
facoltativa invece di una destinazione primaria.

### 7.9 V1 — Grafico accessibile e filtrabile

**File:** `ui/stats/Charts.kt`, `ui/stats/ExerciseStatsScreen.kt`, `ui/body/BodyScreen.kt`,
`ui/stats/ActivityHeatmap.kt`.

**Cosa fare:**

- Creare `GymChart` con:
  - titolo e unità
  - selettore 1M / 3M / 1A / Tutto
  - tooltip al tap (valore + data)
  - `contentDescription` con min, max, ultimo valore e periodo
  - pulsante "Mostra dati" che apre un elenco o una tabella
- Applicarlo a carico, 1RM, volume, peso, asimmetria.
- Heatmap: tap su un giorno → dettaglio sessione; descrizione per ogni cella.

### 7.10 M1 — Editor sessione completa

**File:** `ui/history/SessionDetailScreen.kt`, nuovo `ui/history/SessionEditScreen.kt`,
`data/repo/WorkoutRepository.kt`, `data/db/Daos.kt`.

**Cosa fare:**

- Pulsante "Modifica" nella sessione.
- Campi: titolo, data, ora di inizio, durata, note.
- Azioni: aggiungi serie, elimina serie, aggiungi esercizio, riordina esercizi.
- Salvataggio transazionale; ricalcolo di record e 1RM.
- "Ripeti oggi" e "Salva come scheda".
- Annulla dopo eliminazione.

**Test:** modificare una sessione passata, verificare che le statistiche e i record si aggiornino.

---

## 8. Fondamenta tecniche

### 8.1 Design system accessibile

Creare in `ui/design` un piccolo set di primitive con semantica già corretta:

- `GymButton`, `GymIconButton`, `GymChip`, `GymCard`, `GymRow`, `GymSectionHeader`,
  `GymScaffold`, `GymChart`.
- Ogni primitiva: target ≥ 48 dp, ruolo, stato, content description, supporto font scale.
- Evitare `Box.clickable` sparsi: se serve un controllo nuovo, si aggiunge al design system.

### 8.2 Stato e eventi UI

- `UiEvent` sigillato: `ShowUndo`, `ShowMessage`, `ShowError`.
- `GymScaffold` ospita `SnackbarHost` e ascolta gli eventi.
- Le ViewModel espongono `SharedFlow<UiEvent>`; nessuna chiamata diretta a `Toast` Android
  (tranne PDF/export, dove serve il contesto di sistema).

### 8.3 Impostazioni

Aggiungere a `Settings` e `SettingsRepository`:

- `workoutLayout`: PAGE / LIST / COMPACT
- `effortScale`: RIR / RPE / OFF
- `showExerciseChips`: Boolean
- `haptics`: Boolean
- `reduceMotion`: Boolean
- `highContrast`: Boolean
- `weekStart`: MONDAY / SUNDAY
- `defaultRestSeconds`: Int
- `barWeightKg`: Double
- `plateInventory`: List<Double>
- `goalWeightKg`: Double?
- `reminderEnabled`: Boolean
- `reminderTime`: String

### 8.4 Database e migrazioni

- **Progress photos:** nuova tabella `ProgressPhoto` (id, epochDay, filePath, note, createdAt).
- **Session photos:** opzionale, stessa tabella con `sessionId` nullable.
- **Exercise:** `isBodyweight`, `isAssisted`, `equipment`, `videoPath`.
- **SetEntry:** `durationSec`, `distanceM`, `restSec` (per schemi avanzati).
- **WorkoutSession:** già presente `notes`; aggiungere `movedFrom` se serve tracciare gli spostamenti.
- **Body goal:** può stare in `Settings` o in una tabella `Goal` se si vogliono più obiettivi.
- Ogni migrazione deve avere un test Robolectric come le attuali.

### 8.5 Test

- Continuare con JUnit + Robolectric per repository e ViewModel.
- Aggiungere `androidx.compose.ui:ui-test-junit4` per test di semantica:
  - `onNodeWithContentDescription("Tipo serie: Riscaldamento")`
  - `onNodeWithRole(Role.Tab)` e `isSelected()`
  - `onNodeWithText("Annulla")`
- Test di font scale: almeno uno screenshot/measure test a 2.0.
- Test manuale TalkBack: percorrere Home → Piano → Allenamento → Statistiche → Corpo → Esercizi.
- Accessibility Scanner su APK release.

### 8.6 Performance e privacy

- Nessuna telemetria: le metriche di successo si valutano con test manuali e feedback, non con
  analytics.
- Attenzione a non appesantire l'APK: niente cataloghi di animazioni enormi.
- Le mappe muscolari e i grafici devono essere calcolati in modo leggero e memorizzato
  (`derivedStateOf`, `remember`), senza bloccare il main thread.

---

## 9. Metriche di successo

| Dimensione | Metrica | Target |
| --- | --- | --- |
| Accessibilità | Target < 48 dp nelle schermate principali | 0 |
| Accessibilità | Controlli con nome/ruolo/stato | 100% |
| Accessibilità | Grafici con alternativa testuale | 100% |
| Accessibilità | Testo troncato a font scale 2.0 | 0 |
| Comodità | Tempo per registrare una serie | < 10 s |
| Comodità | Eliminazione + Annulla | < 3 s |
| Navigabilità | Tap per iniziare l'allenamento da Home | ≤ 1 |
| Navigabilità | Tap per saltare a un esercizio | ≤ 2 |
| Modificabilità | Correggere una sessione passata | possibile senza cancellarla |
| Visualizzazione | Statistiche con intervallo temporale | 4 opzioni |
| Qualità | Test JVM | ≥ 220 |
| Qualità | Crash-free nelle prove manuali | 100% |
| Dimensione | Aumento APK | < 2 MB nell'ondata 1 |

---

## 10. Rischi e mitigazioni

| Rischio | Mitigazione |
| --- | --- |
| Troppe modifiche insieme | Tre ondate, una per tema; release piccole. |
| Regressioni nel timer o nella persistenza | Test prima/dopo, migrazioni con Robolectric, backup automatico prima dei test distruttivi. |
| Swipe poco scopribile | Menu sempre visibile + hint al primo uso + Annulla. |
| Font scale grande rompe la bottom bar | Fallback icon-only e test a 2.0. |
| Grafici pesanti | Componente unico, cache, dati già aggregati. |
| Troppa complessità nel modello dati | Rimandare timed/cardio e schemi avanzati all'ultima ondata. |
| Perdita della semplicità attuale | Ogni nuova funzione deve sostituire un tap, non aggiungerne uno. |

---

## 11. Prossimi passi

1. **Scegliere l'ondata 1** e confermare i P0 (A1–A5, C1–C5, N1, N3, V1).
2. **Creare le issue** con i criteri di accettazione di questa pagina.
3. **Sprint 0:** `GymScaffold`, primitive accessibili, campi `Settings`, test di semantica.
4. **Implementare in ordine:** A1/A2 → C1 → C2 → C3/N3 → C4 → C5 → N1 → V1.
5. **Test manuale:** TalkBack, font scale 2.0, Switch Access, una mano, Android 8 e Android 15.
6. **Rilasciare 1.4.0** e raccogliere feedback prima di aprire l'ondata 2.

---

## Appendice — Mappa openGym → GymLog

| openGym | GymLog | Nota |
| --- | --- | --- |
| `components/SwipeRow.jsx`, `components/Toast.jsx` | `ui/workout/SetTable.kt`, `ui/common/Dialogs.kt`, `AppNavHost.kt` | Swipe + Annulla. |
| `components/WorkoutChips.jsx` | nuovo `ui/workout/ExerciseChips.kt` | Chip esercizi. |
| `components/WorkoutSettingsSheet.jsx` | nuovo `ui/workout/WorkoutSettingsSheet.kt` | Impostazioni in allenamento. |
| `components/DurationWheel.jsx`, `components/RestTimer.jsx` | `ui/workout/RestTimerBar.kt`, `timer/RestTimer.kt` | Timer con pausa e tempo editabile. |
| `views/Workout.jsx` (menu serie, piatto, ultima volta) | `ui/workout/SetTable.kt`, `ExercisePage.kt` | Menu serie, dischi, riferimento. |
| `views/Library.jsx` | `ui/exercises/ExerciseListScreen.kt` | Filtri, preferiti, simili. |
| `components/BodyMap.jsx`, `views/Muscles.jsx` | nuovo `ui/stats/MuscleMap.kt` | Mappa muscolare. |
| `views/Stats.jsx` | `ui/stats/StatsScreen.kt`, `Charts.kt` | Intervalli, mappa, trend. |
| `views/Plan.jsx` | `ui/plan/PlanScreen.kt` | Settimana/Schede, azioni scheda. |
| `views/Settings.jsx` | `ui/settings/SettingsScreen.kt` | Sezioni e ricerca. |
| `views/History.jsx` | `ui/history/HistoryScreen.kt`, `SessionDetailScreen.kt` | Ricerca, modifica, ripeti. |

---

*Documento di pianificazione. Nessuna modifica al codice applicativo è stata fatta in questa fase.*
