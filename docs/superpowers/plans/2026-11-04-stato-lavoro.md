# GymLog – stato del lavoro al 4 novembre 2026

Documento di passaggio: cosa è pronto, cosa è parziale, cosa manca. Da leggere insieme a
`docs/superpowers/plans/2026-11-04-gymlog-catalogo-condiviso.md` (piano approvato).

## Obiettivo

GymLog 1.6.0: catalogo esercizi condiviso tra utenti, con account Google **facoltativo**.
Offline-first: senza account l'app resta identica alla 1.5.0. Un solo account amministratore
(`nicolosimonini3@gmail.com`), derivato dalla tabella `public.admin_emails` e dall'email
confermata in `auth.users`.

Vincoli: interfaccia e documentazione in italiano, nessuna telemetria, APK installabile sopra la
versione precedente senza perdere dati, immagini condivise modificabili solo dall'admin.

## Fatto e verificato

### Backend Supabase

- Progetto: `https://kbjiitwqmufppnqbdyzm.supabase.co` (regione UE).
- Chiavi pubbliche in `local.properties` (non versionato): `SUPABASE_URL`, `SUPABASE_ANON_KEY`
  (formato nuovo `sb_publishable_...`), `GOOGLE_WEB_CLIENT_ID`.
- Migrazione `backend/supabase/migrations/20261104120000_catalog.sql`: enum `muscle` e
  `length_bias`, tabella `admin_emails`, tabella `exercises`, RLS, grant per colonna,
  `is_admin()`, `set_exercise_image()`, `clear_exercise_image()`, `admin_delete_exercise()`,
  `delete_my_account()`, bucket `exercise-images`.
- Migrazione `20261104130000_tombstones.sql` (**da applicare al progetto reale**): rende visibili
  le righe cancellate agli utenti autenticati, così il client può archiviarle.
- Verifica locale: `backend/test.sh` crea Postgres 16 in Docker, applica le due migrazioni ed
  esegue `backend/supabase/tests/10_catalog.sql`. Tutto verde.
- Impostazioni Auth verificate via REST: `google: true`, `email: false`, `anonymous: false`.

### Android – dati e sincronizzazione

- Room versione 4, `MIGRATION_3_4` con soli `ADD COLUMN`; test 3→4 e catena 1→4. Provata sul
  backup reale `~/Backup-GymLog/gymlog-backup-2026-10-20.zip` (33 esercizi, 27 serie, 3 sessioni).
- Colonne nuove di `Exercise`: `remoteId`, `remoteCreatedBy`, `dirty`, `sharedImageKey`,
  `pendingImageFile`.
- `data/remote/CatalogRemote.kt`: contratto DTO/remote.
- `data/remote/SupabaseCatalogRemote.kt`: implementazione reale (select, insert con gestione nome
  duplicato `23505`, update per colonna, upload su Storage, RPC immagine/cancellazione).
- `data/sync/CatalogSync.kt`: push nuovi, push modifiche, push immagini, pull completo;
  merge che non tocca i campi personali. 10 test con fake remote.
- `data/sync/CatalogSyncRunner.kt`: sincronizza al login, all'avvio e 5 s dopo un salvataggio;
  espone `SyncStatus` (Idle/Running/Done/Failed) e `syncNow()`.
- `ExerciseRepository.save` ora conserva `remoteId`, `remoteCreatedBy`, `sharedImageKey`,
  `pendingImageFile` e marca `dirty` solo per gli esercizi già condivisi. Test dedicato.

### Android – account

- `data/remote/AuthRepository.kt`: login Google con Credential Manager + `signInWith(IDToken)`,
  nonce SHA-256, stato (`Unavailable`, `SignedOut`, `Loading`, `SignedIn`, `Error`), admin letto
  dal server con RPC `is_admin()`, logout anche offline, eliminazione account.
- Sessione in SharedPreferences `gymlog-auth`, esclusa da backup cloud, device transfer e backup
  ZIP (`res/xml/data_extraction_rules.xml`, `backup_rules.xml`).
- `ui/settings/SettingsScreen.kt`: sezione "Account e catalogo condiviso" con login, nome/email,
  badge Admin, esito ultima sincronizzazione, "Sincronizza ora", Esci, Elimina account.
- `ui/common/Activity.kt`: risalita dal Context all'Activity per Credential Manager.

### Build e versioni

- 1.5.0 (`versionCode 8`): rilasciata, commit `96418bd`.
- 1.5.1 (`versionCode 9`): build di prova del solo login, installata sul telefono e funzionante.
- 1.5.2 (`versionCode 10`): build con la prima sincronizzazione reale, in corso di creazione.
- Ultimo commit di codice: `4e1fd5b` (il commit con versione e documenti viene dopo la build).
- Test: **206 test JVM verdi**; `scripts/test.sh`.
- Backend: `backend/test.sh`.

## Fatto solo in parte

| Area | Stato | Cosa manca |
| --- | --- | --- |
| Sincronizzazione | Funziona al login, all'avvio e dopo un salvataggio, solo con app aperta. | Nessun `WorkManager`: niente sync periodico a app chiusa, niente retry automatico con backoff. Se la rete manca, si riprova al prossimo avvio/modifica. |
| Conflitti | Vince il server: la modifica locale viene scartata senza avviso (RLS aggiorna 0 righe). | Mostrare snackbar "modifica non applicata". Manca il confronto su `updated_at`. |
| Immagini | Remote e `CatalogSync` sanno caricare/impostare/rimuovere; DB ha `sharedImageKey` e `pendingImageFile`. | Nessuna UI per scegliere la foto condivisa; nessun `SharedImageProcessor` (ridimensionamento 1024 px, EXIF, JPEG 80); nessuna visualizzazione remota con Coil. |
| Login | Funziona su dispositivo. | Manca la schermata di conferma al primo accesso ("condividerai N esercizi"). Oggi il consenso è implicito nel testo della sezione account. |
| Catalogo in UI | Esercizi condivisi già visibili, ma senza distinzioni. | Icona ☁, pill "da sincronizzare", filtro Tutti/Miei, campi condivisi bloccati per chi non può modificarli, banner iniziale. |
| Amministrazione | Backend e badge pronti. | UI per sostituire/rimuovere l'immagine condivisa e per eliminare un esercizio. |
| Test | 206 JVM + backend SQL. | Nessun test del remote reale né di `CatalogSyncRunner`. Nessun test su dispositivo della sincronizzazione. |

## Cosa manca, in ordine

1. **Applicare `20261104130000_tombstones.sql`** al progetto Supabase (dashboard → SQL Editor).
   Senza, la cancellazione di un esercizio non arriva agli utenti normali.
2. **Provare la 1.5.2 sul telefono**: login già attivo, all'avvio deve comparire "Sincronizzato
   alle HH:mm: 31 inviati, 0 nuovi" (circa 31 esercizi del seed). Verificare che la libreria non
   abbia duplicati.
3. **Primo consenso**: schermata/riepilogo prima di caricare il catalogo, con numero di esercizi;
   escludere `needsReview` e `archived` (già esclusi dalla query `localOnly`).
4. **WorkManager**: `SyncWorker` + `SyncScheduler` (vincolo rete, backoff, periodico 12 h,
   `KEEP`), più "Sincronizza ora" collegato allo stesso percorso.
5. **Immagini condivise**:
   - `SharedImageProcessor` (decodifica con `inSampleSize`, rotazione EXIF, lato lungo ≤ 1024 px,
     JPEG q80, nessun metadato);
   - checkbox al salvataggio del nuovo esercizio e pulsante "Usa come immagine condivisa" su una
     foto locale se `sharedImageKey == null`;
   - admin: "Sostituisci" e "Rimuovi"; utente normale: badge 🔒;
   - `PhotoCarousel` mostra prima l'immagine remota (URL pubblico + Coil, cache disco), poi le
     foto locali; prefetch per gli esercizi della scheda attiva;
   - errore "qualcuno è arrivato prima" già gestito da `CatalogSync` con `imagesLost`.
6. **UI catalogo**: icona ☁, filtro Tutti/Miei, pill "da sincronizzare", campi condivisi
   disabilitati con spiegazione quando `!canEditShared`. Aggiungere `domain/ExercisePermissions.kt`
   con test puri.
7. **Conflitti**: far restituire a `CatalogRemote.update` almeno il numero di righe aggiornate
   (o usare `updated_at`) e mostrare l'avviso quando la modifica non viene applicata.
8. **QA e release 1.6.0**: test su due account (normale + admin), account cancellato, backup e
   ripristino, app offline, APK sopra la 1.5.2. Aggiornare README/INSTALLA e portare
   `versionCode` a 11.

## Regole da non perdere

- Una sola immagine per esercizio, via `set_exercise_image()` con
  `UPDATE … WHERE image_path IS NULL OR is_admin()`: la gara è risolta dal database.
- Admin deciso dal server (`admin_emails` + email confermata), mai da un ruolo salvato nel client.
- I campi personali non escono mai dal telefono: note di esecuzione, passo di carico, nome Hevy,
  foto locali, allenamenti, schede, misure, backup.
- Nessun seed SQL: il catalogo parte dal primo client che accede; il collegamento avviene per
  `name_key`.
- Il pull è a snapshot completo: `shortcut` nel codice, passare a un cursore keyset oltre ~2000
  esercizi.
- Senza `SUPABASE_*` in `local.properties`, `SupabaseProvider.create` restituisce `null` e
  l'app si comporta come la 1.5.0.

## Comandi

```bash
cd ~/Progetti/GymLog
scripts/test.sh        # 206 test JVM
backend/test.sh        # migrazioni + regole SQL su Postgres Docker
scripts/build-apk.sh   # APK firmato in dist/GymLog-<versione>.apk
```

## Rischi noti

- `supabase-kt` 3.1.4 è la prima versione compatibile con Kotlin 2.1.0; le versioni 3.2+ chiedono
  Kotlin più recente. Se un aggiornamento della libreria diventa necessario, va aggiornato Kotlin
  e verificato tutto il progetto.
- Il client reale non ha test automatici: la prima sincronizzazione va osservata sul telefono e
  sul dashboard Supabase.
- La publishable key è verificata via REST e dal login; se una futura versione di `supabase-kt`
  la rifiutasse, si usa la anon key JWT legacy dal dashboard senza cambiare codice.
- Il build completo richiede ~18 minuti.
