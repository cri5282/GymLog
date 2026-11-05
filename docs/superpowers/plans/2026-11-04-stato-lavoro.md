# GymLog – stato del lavoro al 5 novembre 2026

Documento di passaggio per la versione **1.6.0** (`versionCode` 11): catalogo esercizi condiviso
con account Google facoltativo, immagini condivise e aggiornamento in tempo reale.
Piano di riferimento: `docs/superpowers/plans/2026-11-04-gymlog-catalogo-condiviso.md`.

## Obiettivo e vincoli

- Offline-first: senza account l'app funziona come la 1.5.0; i dati personali restano sul telefono.
- Catalogo esercizi visibile a tutti gli utenti autenticati.
- Immagine condivisa: una per esercizio, caricabile/sostituibile solo da admin e account autorizzati.
- Admin derivato dal server (`public.admin_emails` + email confermata in `auth.users`), mai dal client.
- Interfaccia e documentazione in italiano, nessuna telemetria.
- APK installabile sopra la versione precedente senza perdere dati.

## Fatto e verificato

### Backend Supabase

- Progetto `https://kbjiitwqmufppnqbdyzm.supabase.co` (regione UE). Chiavi pubbliche in
  `local.properties` (non versionato): `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID`.
- `20261104120000_catalog.sql`: schema, enum, tabella `exercises`, RLS, grant per colonna,
  `is_admin()`, funzioni immagine, soft delete, eliminazione account, bucket `exercise-images`.
- `20261104130000_tombstones.sql`: i tombstone sono leggibili anche dagli utenti normali, così il
  client archivia l'esercizio cancellato. **Da applicare al progetto reale.**
- `20261104140000_image_editors_realtime.sql`: tabella `exercise_image_editors`, funzioni
  `can_manage_exercise_images()`, `set_exercise_image_editor()`, `list_exercise_image_editors()`,
  nuove regole per immagine e Storage, pubblicazione Realtime di `exercises`.
  **Da applicare al progetto reale.**
- Verifica locale: `backend/test.sh` crea Postgres 16 in Docker, applica le tre migrazioni ed
  esegue `backend/supabase/tests/10_catalog.sql`. Tutto verde.

### Android – catalogo e sincronizzazione

- Room versione 4, `MIGRATION_3_4` con soli `ADD COLUMN`; verificata sul backup reale.
- `data/remote/SupabaseCatalogRemote.kt`: select, insert con gestione del nome duplicato, update
  che restituisce `false` se RLS non applica la modifica, upload su Storage, RPC immagine.
- `data/sync/CatalogSync.kt`: push nuovi/modifiche/immagini, pull a snapshot completo, merge che non
  tocca i campi personali, archiviazione dei tombstone, conteggio delle modifiche rifiutate.
- `data/sync/CatalogSyncRunner.kt`: sync al login, all'avvio e 5 s dopo un salvataggio; Realtime
  richiede un nuovo scambio quando un altro dispositivo modifica il catalogo; `SyncStatus` con
  esito, modifiche rifiutate e immagini non pubblicate.
- `data/sync/SyncWorker.kt`: sync periodica ogni 12 h con vincolo di rete e backoff esponenziale,
  anche ad app chiusa. `CatalogSyncScheduler` registra un solo lavoro periodico (`KEEP`).
- Consenso esplicito **per account**: finché non viene dato, nessun esercizio lascia il telefono.
  L'UID che ha acconsentito è salvato in `Settings.catalogConsentFor`; un altro account sullo
  stesso telefono deve acconsentire di nuovo.
- `ExerciseRepository.save` conserva `remoteId`, `remoteCreatedBy`, `sharedImageKey` e
  `pendingImageFile`; marca `dirty` solo se cambia un campo condiviso (una nota personale non
  genera invii né falsi avvisi di conflitto).

### Android – immagini condivise

- `data/sync/SharedImageProcessor.kt`: ridimensiona a 1024 px sul lato lungo, corregge l'orientamento
  EXIF, salva JPEG senza metadati e resta sotto il limite di 1 MB del bucket.
- Nuova foto di un utente autorizzato: viene salvata anche sul telefono e diventa automaticamente
  l'immagine condivisa alla sincronizzazione successiva.
- `PhotoCarousel` e le miniature mostrano prima l'immagine remota (Coil, URL pubblico), poi le foto
  locali.
- Admin: autorizza/revoca gli editor da *Impostazioni → Account → Permessi immagini*; può rimuovere
  l'immagine condivisa e eliminare un esercizio dal catalogo condiviso dalla schermata dell'esercizio.
- Utente normale: le foto restano locali e non vengono pubblicate.

### Android – interfaccia catalogo

- Badge sull'elenco esercizi: condiviso, solo locale o da sincronizzare.
- Filtro **Tutti / Miei**.
- Campi condivisi (nome, muscoli, tipo, unilaterale) disabilitati con spiegazione per chi non è
  creatore né admin; note e dati personali restano modificabili.
- `domain/ExercisePermissions.kt` con test puri.

### Account

- Login Google con Credential Manager, stato `Unavailable/SignedOut/Loading/SignedIn/Error`, admin
  letto dal server, logout anche offline, eliminazione account.
- Sessione in `gymlog-auth`, esclusa da backup cloud, trasferimento dispositivo e backup ZIP.
- Sezione account con esito ultima sync, **Sincronizza ora**, consenso al primo accesso.

### Test e build

- `scripts/test.sh`: test JVM (repository, sync con fake remote, permessi, elaborazione immagini,
  migrazioni Room).
- `backend/test.sh`: migrazioni e regole SQL su Postgres usa-e-getta.
- `scripts/build-apk.sh`: `clean` + test + `assembleRelease` firmato in
  `dist/GymLog-<versione>.apk`.
- Versione corrente: **1.6.0**, `versionCode 11` (l'APK va rigenerato dopo le ultime modifiche).

## Fatto solo in parte

| Area | Stato | Cosa manca |
| --- | --- | --- |
| Sincronizzazione | Login, avvio, salvataggio, Realtime con app aperta e lavoro periodico di 12 h con backoff. | L'intervallo non è configurabile dall'utente; per una sync più frequente servirebbe un trigger in background. |
| Conflitti | Il server rifiuta la modifica e il client la conta in `refused`. | L'avviso è nel riepilogo della sync, non come messaggio immediato sulla schermata dell'esercizio. |
| Catalogo in UI | Badge, filtro Tutti/Miei, campi bloccati. | Nessun banner informativo la prima volta che appare un esercizio condiviso; nessun prefetch delle immagini della scheda attiva. |
| Amministrazione | Permessi immagini, rimozione immagine ed eliminazione dal catalogo condiviso. | Nessun ripristino di un esercizio cancellato (il soft delete non ha un'azione di undo). |
| Test su dispositivo | Build e suite automatiche verdi. | La 1.6.0 non è ancora stata provata sul telefono: login, sync reale, Realtime, immagine condivisa e aggiornamento sopra la 1.5.x. |

## Cosa manca, in ordine

1. **Applicare al progetto Supabase** le migrazioni non ancora presenti:
   `20261104130000_tombstones.sql` e `20261104140000_image_editors_realtime.sql`
   (dashboard → SQL Editor, oppure `supabase db push`).
2. **Provare sul telefono** l'APK 1.6.0 sopra la versione installata, senza disinstallare:
   consenso, sync iniziale, nessun duplicato, Realtime da un secondo dispositivo, immagine
   condivisa caricata e sostituita, revoca del permesso, logout.
3. **Rigenerare l'APK** con `scripts/build-apk.sh` e verificare `versionCode 11` / `versionName 1.6.0`.
4. **Eventuale ripristino** di un esercizio eliminato dal catalogo, se servirà.

## Regole da non perdere

- Admin deciso dal server, mai da un ruolo salvato nel client.
- Una sola immagine condivisa per esercizio; la sostituzione è riservata ad admin/editor.
- I campi personali non escono mai dal telefono: note, passo di carico, nome Hevy, foto locali,
  allenamenti, schede, misure, backup.
- Nessun seed SQL: il catalogo parte dal primo client che accede; il collegamento avviene per nome.
- Il pull è a snapshot completo (`shortcut`: passare a un cursore keyset oltre ~2000 esercizi).
- Senza `SUPABASE_*` in `local.properties` l'app si comporta come la 1.5.0.
- Un consenso vale solo per l'account che l'ha dato.

## Comandi

```bash
cd ~/Progetti/GymLog
scripts/test.sh        # test JVM
backend/test.sh        # migrazioni + regole SQL su Postgres Docker
scripts/build-apk.sh   # APK firmato in dist/GymLog-<versione>.apk
```

## Rischi noti

- `supabase-kt` 3.1.4 è la prima versione compatibile con Kotlin 2.1.0; versioni 3.2+ richiedono
  Kotlin più recente e vanno verificate su tutto il progetto.
- Il client reale non ha test automatici: prima sincronizzazione, Realtime e immagini vanno
  osservati sul telefono.
- Se Realtime non è attivo sul progetto Supabase, l'app continua a funzionare ma si riallinea solo
  a login, avvio, salvataggio o **Sincronizza ora**.
- Il build completo richiede diversi minuti.
