# GymLog – installazione e aggiornamenti

## 1. Copiare l'APK sul telefono

Il file da installare è `~/GymLog/dist/GymLog-1.0.0.apk`. Scegli uno di questi modi:

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
- **Non disinstallare mai l'app**: disinstallando si cancellano tutti i dati
  (finché non arriva l'esportazione/backup, prevista più avanti).

## 5. Backup della chiave di firma (importante)

Gli aggiornamenti si installano sopra la versione precedente solo se sono firmati con la
stessa chiave. La chiave è sul PC in:

- `~/GymLog/keystore/gymlog.jks`
- `~/GymLog/keystore.properties` (contiene la password)

Copia **entrambi** su una chiavetta o su un cloud privato. Se li perdi, per installare una
nuova versione dovrai disinstallare l'app e perderai i dati.

## 6. Verifica sul telefono (checklist)

- [ ] L'app si installa e si apre, con la scheda di oggi in *Oggi*.
- [ ] L'import del CSV di Hevy riporta circa 24 allenamenti.
- [ ] In un allenamento (es. Push A) i carichi sono precompilati dall'ultima sessione.
- [ ] Toccando ✓ parte il recupero; a schermo bloccato arrivano notifica e vibrazione a fine recupero.
- [ ] Chiudendo l'app a metà allenamento (anche dal multitasking), riaprendola compare "Riprendi"
      con le serie già confermate.
- [ ] Una foto scattata a un macchinario compare durante l'allenamento.

## Per ricompilare (sul PC)

```
cd ~/GymLog
scripts/build-apk.sh
```

L'APK nuovo finisce in `dist/`. Prima di ogni nuova versione va aumentato `versionCode`
in `app/build.gradle.kts`.
