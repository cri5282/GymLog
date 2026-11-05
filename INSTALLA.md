# GymLog – installazione e aggiornamenti

## 1. Copiare l'APK sul telefono

Il file da installare è `~/Progetti/GymLog/dist/GymLog-1.6.0.apk`. Scegli uno di questi modi:

- **Cavo USB:** collega il telefono, sul telefono scegli "Trasferimento file", copia l'APK nella cartella *Download*.
- **Google Drive:** carica l'APK su Drive dal PC e scaricalo dal telefono con l'app Drive.
- **Telegram:** invia l'APK a "Messaggi salvati" e aprilo dal telefono.

## 2. Installare

1. Sul telefono apri il file APK (dall'app *File*, da Drive o da Telegram).
2. Android chiede di **consentire l'installazione di app sconosciute** per l'app che stai usando
   (File, Drive o Telegram): attiva l'interruttore e torna indietro.
3. Tocca **Installa**. Se compare un avviso di Play Protect, scegli "Installa comunque":
   l'app non è sul Play Store perché è solo tua, non per un problema di sicurezza.

## 3. Primo avvio

1. Apri **GymLog**: la scheda "Ipertrofia – ottobre 2026" è già caricata e attiva.
2. Al primo allenamento l'app chiede il permesso per le **notifiche**: concedilo, serve al timer
   di recupero quando lo schermo è spento.
3. **Importa lo storico da Hevy**:
   - in Hevy: *Profilo → Impostazioni → Esporta e importa dati → Esporta allenamenti*, salva `workouts.csv`;
   - in GymLog: *Impostazioni → Importa da Hevy (CSV)* e scegli il file.
   Puoi ripetere l'importazione quando vuoi: gli allenamenti già presenti vengono saltati.
4. Se l'importazione crea esercizi nuovi, nella sezione *Esercizi* hanno l'etichetta
   **"da completare"**: aprili e imposta il muscolo giusto.

## 4. Aggiornamenti futuri

- Installa il nuovo APK **sopra** quello vecchio: i dati restano.
- **Non disinstallare mai l'app** senza aver prima fatto un backup: disinstallando si cancellano
  database, foto e impostazioni.
- Da **Impostazioni → Backup** puoi esportare tutto in uno zip e ripristinarlo in seguito.
  È disponibile anche il backup automatico settimanale in una cartella scelta.

## Aggiornare alla 1.6.0 (immagini condivise automatiche)

Prima di installare l'APK, applica al progetto Supabase le migrazioni non ancora presenti:
`backend/supabase/migrations/20261104130000_tombstones.sql` e
`backend/supabase/migrations/20261104140000_image_editors_realtime.sql`.
Puoi usare `supabase db push` se il progetto è già collegato, oppure eseguirle dal SQL Editor di
Supabase (una volta sola ciascuna). Installa poi `GymLog-1.6.0.apk` sopra la versione precedente.

Checklist:
- [ ] Al primo accesso compare la richiesta di consenso: nessun esercizio viene inviato prima di
      **Condividi**.
- [ ] Dopo il consenso l'esito mostra la sincronizzazione iniziale e la libreria non ha duplicati.
- [ ] L'admin e gli account autorizzati possono aggiungere/sostituire l'immagine predefinita di un esercizio.
- [ ] Se un altro dispositivo modifica il catalogo, l'app si aggiorna da sola (Realtime).
- [ ] Un utente normale può aggiungere foto personali, ma non pubblicarle né modificare i campi
      condivisi di un esercizio altrui.

Come funziona:
- Per autorizzare un account: **Impostazioni → Account → Permessi immagini**, inserisci la sua email
  Google GymLog e tocca **Autorizza**. La revoca è disponibile accanto all'email.
- Da quel momento la nuova foto aggiunta diventa automaticamente quella condivisa; le altre foto
  personali già presenti sul telefono non vengono caricate retroattivamente.
- **Sincronizza ora** resta disponibile come comando manuale.

## Aggiornare alla 1.5.2 (prima sincronizzazione reale)

Installa `GymLog-1.5.2.apk` **sopra** la 1.5.1. Con l'account già collegato, all'avvio parte la
sincronizzazione: il catalogo viene caricato una volta e poi tenuto allineato.

Checklist:
- [ ] In *Impostazioni → Account* compare l'esito, ad esempio "Sincronizzato alle 14:32: 31 inviati,
      0 nuovi".
- [ ] **Sincronizza ora** mostra "Sincronizzazione in corso…" e poi un nuovo esito.
- [ ] Nella libreria *Esercizi* non compaiono duplicati dopo la sincronizzazione.
- [ ] Modificando nome, muscoli o tipo di un esercizio, pochi secondi dopo l'esito segnala
      "1 inviati".
- [ ] Con *Esci* il catalogo locale resta consultabile e l'app funziona come prima.

Se la sincronizzazione fallisce, controlla la connessione e riprova con **Sincronizza ora**. La
prima volta devono essere presenti anche i permessi della migrazione `20261104130000_tombstones.sql`
sul progetto Supabase.

## Aggiornare alla 1.5.1 (account facoltativo e catalogo condiviso)

Installa `GymLog-1.5.1.apk` **sopra** la 1.5.0, senza disinstallare: tutti i dati restano. Senza
account l'app è identica alla 1.5.0.

Checklist delle novità:
- [ ] In *Impostazioni* compare la sezione **Account e catalogo condiviso**.
- [ ] **Accedi con Google** apre il selettore account e, al ritorno, mostra nome, email e pulsante
      **Esci**.
- [ ] Con l'account `nicolosimonini3@gmail.com` compare il badge **Admin**.
- [ ] **Esci** riporta alla schermata di accesso; chiudendo e riaprendo l'app la sessione resta
      (finché non esci).
- [ ] Senza account, allenamenti, schede, foto, backup e statistiche funzionano come prima.

Se l'accesso fallisce con un errore Google, manca probabilmente l'OAuth client Android nel progetto
Google Cloud (package `it.gymlog.app` e impronta SHA-1); va creato prima di riprovare.

## Aggiornare alla 1.5.0 (annulla, ripristino e scheda del giorno)

Installa `GymLog-1.5.0.apk` **sopra** la versione precedente, senza disinstallare: allenamenti,
schede, foto e impostazioni restano. La 1.5.0 include anche le novità della 1.4.0: accessibilità,
annullamento della rimozione di una serie e impostazioni rapide in allenamento.

Checklist delle novità:
- [ ] In *Oggi*, toccando il riquadro di un giorno si apre l'elenco delle **schede create**: se ne
      sceglie una oppure **Riposo**. La scelta si vede subito anche nel *Piano*.
- [ ] Assegnando a un giorno una scheda già usata in un altro giorno, la scheda viene spostata.
- [ ] Nel menu ⋯ dell'allenamento c'è **Annulla allenamento**: dopo la conferma sessione e serie
      vengono eliminate e l'allenamento non compare nello storico.
- [ ] Nello *Storico*, l'icona del cestino elimina un allenamento dopo la conferma; la snackbar
      **Annulla** lo ripristina con tutte le sue serie.
- [ ] Dopo aver rimosso una serie compare **Annulla** nella snackbar e il ripristino conserva
      carico, ripetizioni, RIR e posizione.
- [ ] Il **menu del numero di serie** permette di duplicare, rimuovere o cambiare tipo.
- [ ] La barra degli **esercizi** sopra la tabella permette di saltare da una pagina all'altra
      (si nasconde quando c'è un solo esercizio).
- [ ] Il **timer** si può mettere in pausa, riprendere e impostare con i preset o i secondi.
- [ ] Il menu ⋯ dell'allenamento apre le **impostazioni rapide** (timer, schermo acceso, chip,
      feedback aptico).
- [ ] La **barra inferiore** ha cinque voci: Oggi, Piano, Start, Statistiche, Esercizi; Corpo si
      apre da Statistiche.
- [ ] Con TalkBack i tab, le chip, gli switch e i grafici hanno etichette e descrizioni leggibili.
- [ ] Con il testo di sistema molto grande la barra inferiore mostra solo le icone.

## 5. Backup della chiave di firma (importante)

Gli aggiornamenti si installano sopra la versione precedente solo se sono firmati con la
stessa chiave. La chiave è sul PC in:

- `~/Progetti/GymLog/keystore/gymlog.jks`
- `~/Progetti/GymLog/keystore.properties` (contiene la password)

Copia **entrambi** su una chiavetta o su un cloud privato. Se li perdi, per installare una
nuova versione dovrai disinstallare l'app e perderai i dati.

## 6. Verifica sul telefono (checklist)

- [ ] L'app si installa e si apre, con la scheda di oggi in *Oggi*.
- [ ] L'import del CSV di Hevy riporta circa 24 allenamenti.
- [ ] In un allenamento (es. Push A) i carichi sono precompilati dall'ultima sessione e le serie
      di riscaldamento non compaiono automaticamente.
- [ ] Toccando ✓ parte il recupero; a schermo bloccato arrivano notifica e vibrazione a fine recupero.
- [ ] Chiudendo l'app a metà allenamento (anche dal multitasking), riaprendola compare "Riprendi"
      con le serie già confermate.
- [ ] Una foto scattata a un macchinario compare durante l'allenamento.
- [ ] Da *Impostazioni → Esporta backup* si crea uno zip; *Ripristina da backup* lo riporta indietro.
- [ ] Nella sezione *Corpo* una misurazione con peso e pliche mostra grasso stimato e FFMI.
- [ ] Il widget della home mostra l'allenamento in corso o la scheda di oggi.

## Per ricompilare (sul PC)

```
cd ~/Progetti/GymLog
scripts/build-apk.sh
```

L'APK nuovo finisce in `dist/`. Prima di ogni nuova versione va aumentato `versionCode`
in `app/build.gradle.kts`.
