# GymLog 1.6.0 — Account Google e catalogo esercizi condiviso

> Piano di lavoro e architettura. Stato: **bozza da approvare** (nessuna riga di codice scritta).
> Data: 2026-11-04 · Base di partenza: GymLog 1.5.0 (`versionCode` 8), Room v3.

---

## 0. Sintesi in 12 righe

- **Obiettivo.** Utenti con login Google; gli esercizi creati da un utente compaiono a tutti gli altri
  con la loro immagine. L'immagine è modificabile solo da **admin** e dal **proprietario**
  (`nicolosimonini3@gmail.com`); gli altri possono solo **aggiungerla se manca**.
- **Backend consigliato: Supabase** (Postgres + Auth Google + Storage + Row Level Security).
  Le regole "chi può cambiare l'immagine" diventano SQL verificabile (policy + una funzione atomica),
  non codice sparso nell'app.
- **L'app resta offline-first.** Room è sempre la fonte di verità per la UI; il cloud è solo un
  catalogo condiviso che si sincronizza in background. Allenamenti, serie, misure corporee, schede
  e foto personali **non lasciano mai il telefono**.
- **Il login è facoltativo.** Senza account l'app funziona esattamente come oggi.
- **Una sola immagine condivisa per esercizio**, scritta con un `UPDATE … WHERE image_path IS NULL`
  atomico (compare-and-set): niente race condition, niente tabella in più.
- **Migrazione Room 3→4 solo `ADD COLUMN`** (vincolo data-safety), nessuna tabella nuova, nessun indice nuovo.
- **Sync** con WorkManager: push (nuovi/modificati) → push immagini → pull incrementale per cursore.
- **Stima:** ~14–20 giorni di lavoro in 8 fasi (§10). Costo infrastruttura iniziale: 0 € (free tier, vedi §11).

---

## 1. Situazione attuale del progetto

| Aspetto | Stato |
| --- | --- |
| App | Android, Kotlin 2.1.0, Compose (BOM 2024.12.01), Material3, minSdk 26 / target 35 |
| Versione | **1.5.0** (`versionCode` 8), APK in `dist/GymLog-1.5.0.apk` (3 nov 2026) |
| Codice | ~9.000 righe in `main`, ~2.700 in `test`, **190 test** JVM (Robolectric + Room in-memory) |
| Persistenza | Room **v3** (`gymlog.db`), schemi esportati `app/schemas/{1,2,3}.json`, DataStore per le impostazioni |
| Migrazioni | `MIGRATION_1_2`, `MIGRATION_2_3`, solo `ADD COLUMN` / `CREATE TABLE` di tabelle nuove, coperte da `MigrationTest` |
| DI | Manuale: `AppContainer` crea `db`, `settings`, `photos`, `exercises`, `programs`, `workouts`, `body`, `restTimer`, `backup`… |
| Architettura | `data/{db,repo,backup,hevy,settings}` → `domain` → `ui/*` (ViewModel + StateFlow), `appViewModel` factory |
| Esercizi | `Exercise` (id Long autogenerato, `name` **unique**, `hevyName` unique, muscoli, `lengthBias`, `executionNotes`, `loadStepKg`, `single`, `archived`, `needsReview`) + `ExerciseSecondaryMuscle` + `ExercisePhoto` (file locali in `filesDir/photos`) |
| Riferimenti | `SetEntry.exerciseId` e `RoutineExercise.exerciseId` puntano agli id locali (FK senza cascade) → **gli id locali non devono mai cambiare** |
| Foto | `PhotoStore` copia/crea JPEG a piena risoluzione in `filesDir/photos`; il backup ZIP include `photos/` |
| Rete | **Nessuna**: il manifest non dichiara `INTERNET`; il README dichiara "nessun account, nessun server" |
| Seed | `SeedData`: 31 esercizi + scheda "Ipertrofia – ottobre 2026" al primo avvio. Alcune `executionNotes` sono **personali** (es. "Gamba infortunata…") |
| Ultimi lavori | ✓ che si deseleziona, −15 s nel recupero, "Modifica esercizio" dalla schermata allenamento (dialog a tutto schermo su `ExerciseEditScreen`), Corpo nella bottom bar |
| Non c'è git | La cartella non è un repo: prima di iniziare conviene `git init` + primo commit (rete di sicurezza per un lavoro di questa portata) |

Vincoli permanenti (memoria *gymlog-data-safety*): mai `pm clear` / `uninstall`, mai
`fallbackToDestructiveMigration()`, aggiornare solo con `adb install -r`, migrazioni solo `ADD COLUMN`,
stessa chiave di firma, test di migrazione sulla copia del backup reale (`~/Backup-GymLog/…zip`).
Gradle va eseguito con JBR 21 (non cambiare la versione di Kotlin).

---

## 2. Requisiti e interpretazione

| # | Requisito dell'utente | Interpretazione adottata |
| --- | --- | --- |
| R1 | Registrazione utenti con accesso Google | Google Sign-In (Credential Manager) → sessione Supabase. Niente email/password. |
| R2 | Database che gestisce gli utenti | Tabella `profiles` collegata a `auth.users`, con campo `role` (`user` / `admin`). |
| R3 | Un esercizio aggiunto da un utente appare a tutti | Catalogo globale `exercises`; sync bidirezionale verso la tabella locale `Exercise`. |
| R4 | Con l'immagine | Un'immagine condivisa per esercizio (bucket Storage pubblico + colonna `image_path`). |
| R5 | Immagine modificabile solo da admin e da `nicolosimonini3@gmail.com` | Entrambi hanno `role = 'admin'` (il tuo account è nella tabella `admin_emails`). Sostituzione/rimozione: solo admin, imposta dal server, non dal client. |
| R6 | Gli altri possono solo aggiungere l'immagine se non c'è | Funzione `set_exercise_image()` con `UPDATE … WHERE image_path IS NULL OR is_admin()`: chi arriva secondo riceve `false`. |

**Ambiguità risolte con un default** (tutte riaperte in §12, nessuna blocca l'inizio):

1. Quali campi sono "condivisi"? → `name`, muscolo primario, muscoli secondari, `lengthBias`, `single`, immagine.
   **Restano personali** (mai caricati): `executionNotes`, `loadStepKg`, `hevyName`, `archived`, `needsReview`,
   foto locali, e tutto lo storico. Motivo: le note del seed contengono dati personali e il passo di carico è una preferenza.
2. Chi può modificare i campi condivisi (non l'immagine)? → il **creatore** e gli **admin**; gli altri vedono i campi bloccati.
   Senza questa regola chiunque potrebbe rinominare un esercizio per tutti.
3. Chi può eliminare un esercizio dal catalogo? → solo admin, come *soft delete* (`deleted_at`); sui client diventa `archived`.

---

## 3. Scelta dell'architettura

### 3.1 Confronto

| Criterio | **Supabase** (consigliato) | Firebase (Auth + Firestore + Storage) | Backend proprio (Ktor/FastAPI + Postgres + MinIO) |
| --- | --- | --- | --- |
| Login Google | Nativo (ID token da Credential Manager) | Nativo | Da implementare (verifica JWT Google) |
| Modello dati | **Relazionale** (come Room): vincoli, unique, trigger | Documenti NoSQL, niente unique su campi | Libero |
| Regola "immagine solo se vuota" | 1 `UPDATE` atomico + permessi per colonna | Regole Firestore + regole Storage separate, non atomiche tra loro | Codice server |
| Autorizzazioni | RLS in SQL, testabili con pgTAP | Security Rules (linguaggio proprio) | Codice server |
| Manutenzione | Nessun server da gestire | Nessun server | **Alta** (uptime, TLS, backup, aggiornamenti) |
| Costo iniziale | Free tier senza carta | Storage richiede il piano a consumo (carta) per i nuovi progetti — *da verificare* | Costo/hosting |
| Lock-in | Basso: è Postgres, self-hostable (hai già Docker) | Alto | Nessuno |
| Contro | Il free tier **mette in pausa** il progetto dopo ~1 settimana senza traffico (mitigazione in §11) | Query/ordinamenti limitati, regole difficili da testare | Tutto a tuo carico |

**Decisione: Supabase.** Il backend proprio è l'unica alternativa che avrebbe senso se volessi ospitare
tutto sul tuo PC; è rimandabile: Supabase si può migrare su Postgres self-hosted senza cambiare il client.

### 3.2 Principi

1. **Offline-first invariato**: la UI legge solo da Room. Il cloud non è mai nel percorso critico dell'allenamento.
2. **Il server è l'autorità sui permessi**; la UI li replica solo per disabilitare i controlli.
3. **Minimizzazione dei dati**: sul cloud solo catalogo, profilo e immagini macchinari.
4. **Idempotenza ovunque**: id degli esercizi generati dal client (UUID), ritentare un push non crea duplicati.
5. **Funzione spenta se non configurata**: se `SUPABASE_URL` è vuoto (build di test/CI) la sezione account non compare.
6. **Meno tabelle possibile**: nessuna *outbox* separata, nessuna tabella immagini, nessun nuovo indice locale.

### 3.3 Diagramma

```
┌────────────────────────── Telefono ──────────────────────────┐          ┌────────── Supabase (EU) ───────────┐
│ UI Compose ──► ViewModel ──► Repository ──► Room (fonte di verità)│      │ Auth (Google)                       │
│                                   │                 ▲            │      │   └─ trigger → profiles(role)        │
│  Settings ► "Account e condivisione"                │            │      │ Postgres (RLS attiva ovunque)        │
│        │                          ▼                 │            │      │   ├─ exercises                          │
│  AuthRepository ◄─ Credential Manager (Google)      │            │      │   ├─ profiles / admin_emails            │
│        │ JWT                                        │            │ HTTPS│   └─ RPC set_exercise_image() (CAS)      │
│        ▼                                            │            │◄────►│ Storage: bucket exercise-images         │
│  CatalogRemote (supabase-kt) ◄── CatalogSync ───────┘            │      │   └─ <uid>/<uuid>.jpg (pubblico, 1 MB)  │
│        ▲                         ▲                               │      └─────────────────────────────────────┘
│  WorkManager (SyncWorker)  SharedImageProcessor (resize, no EXIF)│
│  Coil (cache disco) ◄── URL pubblico dell'immagine condivisa     │
└──────────────────────────────────────────────────────────────────┘
```

---

## 4. Backend: schema, sicurezza, funzioni

Cartella nuova nel repo: `backend/supabase/{migrations,tests,seed}` (Supabase CLI, stack locale con Docker).

### 4.1 Ruoli e profili

```sql
create table public.profiles (
  id           uuid primary key references auth.users(id) on delete cascade,
  email        text not null,
  display_name text,
  role         text not null default 'user' check (role in ('user','admin')),
  created_at   timestamptz not null default now()
);
create table public.admin_emails (email text primary key);       -- le tue email "admin"
insert into public.admin_emails values ('nicolosimonini3@gmail.com');  -- + eventuale account admin dedicato

-- alla prima registrazione crea il profilo; il ruolo admin si decide SOLO qui, mai dal client
create function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  insert into public.profiles(id, email, display_name, role)
  values (new.id, lower(new.email), new.raw_user_meta_data->>'name',
          case when exists (select 1 from public.admin_emails a where a.email = lower(new.email))
               then 'admin' else 'user' end);
  return new;
end $$;
create trigger on_auth_user_created after insert on auth.users
  for each row execute function public.handle_new_user();

create function public.is_admin() returns boolean
language sql stable security definer set search_path = '' as
$$ select exists (select 1 from public.profiles where id = auth.uid() and role = 'admin') $$;
```

`profiles`: RLS attiva, `select` solo sulla propria riga (e admin su tutte), **nessuna policy di update su `role`**
(grant per colonna: solo `display_name`).

### 4.2 Catalogo esercizi

```sql
create table public.exercises (
  id                uuid primary key,                         -- generato dal client
  name              text not null check (char_length(btrim(name)) between 2 and 80),
  name_key          text generated always as (lower(btrim(name))) stored,
  primary_muscle    text not null,                            -- nome dell'enum Kotlin (PETTO, DORSALI…)
  secondary_muscles text[] not null default '{}',
  length_bias       text not null default 'NONE',
  single            boolean not null default false,
  image_path        text,                                     -- chiave nel bucket, null = nessuna immagine
  image_set_by      uuid references public.profiles(id) on delete set null,
  image_set_at      timestamptz,
  created_by        uuid references public.profiles(id) on delete set null,   -- null = seed di sistema
  created_at        timestamptz not null default now(),
  updated_at        timestamptz not null default now(),       -- trigger BEFORE UPDATE
  deleted_at        timestamptz
);
create unique index exercises_name_key_uq on public.exercises(name_key) where deleted_at is null;
create index exercises_cursor_idx on public.exercises(updated_at, id);
```

**Policy (RLS attiva, ruolo `authenticated`; `anon` non ha accesso a nulla):**

| Operazione | Regola |
| --- | --- |
| `select` | righe non cancellate (gli admin vedono anche le cancellate: servono ai client come *tombstone*) |
| `insert` | `created_by = auth.uid()`, `image_path is null`, solo colonne `id,name,primary_muscle,secondary_muscles,length_bias,single,created_by` |
| `update` | `is_admin() OR created_by = auth.uid()`, **solo** colonne `name,primary_muscle,secondary_muscles,length_bias,single` |
| `delete` | nessuna policy (solo soft delete tramite `admin_delete_exercise()` per admin) |
| `image_path/image_set_*`, `deleted_at`, `created_by` | **non scrivibili dal client** (revoke + grant per colonna): si cambiano solo via funzioni `SECURITY DEFINER` |

Trigger di quota (anti-spam): max 30 nuovi esercizi per utente in 24 h (gli admin esenti), nome ≤ 80 caratteri.

### 4.3 Immagine: la regola centrale

```sql
create function public.set_exercise_image(p_exercise uuid, p_path text) returns boolean
language plpgsql security definer set search_path = '' as $$
declare n int;
begin
  if auth.uid() is null then raise exception 'not authenticated'; end if;
  -- il file deve stare nella cartella dell'utente (gli admin possono usare qualsiasi file) ed esistere davvero
  if not (public.is_admin() or (storage.foldername(p_path))[1] = auth.uid()::text) then
    raise exception 'invalid path'; end if;
  if not exists (select 1 from storage.objects where bucket_id = 'exercise-images' and name = p_path) then
    raise exception 'file not uploaded'; end if;

  update public.exercises
     set image_path = p_path, image_set_by = auth.uid(), image_set_at = now()
   where id = p_exercise and deleted_at is null
     and (image_path is null or public.is_admin());          -- ← R5 + R6 in una riga
  get diagnostics n = row_count;
  return n = 1;                                              -- false = qualcuno è arrivato prima
end $$;
```

- **Atomicità:** se due utenti inviano insieme, Postgres serializza sulla riga; il secondo `UPDATE` rivaluta
  il `WHERE`, trova `image_path` già valorizzato e aggiorna 0 righe → `false`.
- `clear_exercise_image(p_exercise)` e `admin_delete_exercise(p_exercise)`: controllano `is_admin()`.
- `delete_my_account()`: cancella la riga in `auth.users` (le FK `on delete set null` anonimizzano i contributi).
- `revoke all … from public; grant execute … to authenticated`.

### 4.4 Storage

```sql
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('exercise-images', 'exercise-images', true, 1048576, array['image/jpeg']);

-- upload: solo nella propria cartella <uid>/…, mai sovrascrivere/cancellare (niente update/delete per gli utenti)
create policy img_insert on storage.objects for insert to authenticated
  with check (bucket_id = 'exercise-images' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy img_admin  on storage.objects for all to authenticated
  using (bucket_id = 'exercise-images' and (select public.is_admin()));
```

- Nomi file = UUID non indovinabili; bucket **pubblico in lettura** (immagini di macchinari, basso rischio)
  → l'app usa l'URL diretto e Coil lo mette in cache. Le immagini non cambiano mai di path: `Cache-Control` di 1 anno.
- **File orfani** (upload riuscito ma `set_exercise_image` perso la gara): job settimanale (Edge Function o
  script con `service_role`, **mai** nell'app) che elimina via API Storage i file non referenziati più vecchi di 24 h.

### 4.5 Seed del catalogo

Script `backend/seed/generate-seed.kts` (o test JVM) che legge `SeedData.exercises` e produce
`insert … on conflict do nothing` con `created_by = null`. Escludere `executionNotes`. Così un utente nuovo che installa
l'app (con i 31 esercizi locali del seed) fa il *link per nome* al primo sync senza duplicati.

### 4.6 Configurazione Auth

- Provider **Google** attivo; **email signup disabilitato**; **anonymous sign-ins disabilitato**.
- Redirect/consent non servono per il flusso nativo con ID token.
- Regione progetto: **UE** (Francoforte).
- Ruoli: nessun custom claim nel JWT (si usa `is_admin()` con lookup su `profiles`; in RLS scritto come
  `(select public.is_admin())` per farlo valutare una volta per query).

---

## 5. Login Google

### 5.1 Setup Google Cloud (una tantum)

1. Progetto Google Cloud → *OAuth consent screen* (External, stato Testing finché sei in beta: max 100 tester; poi *In production*).
2. Credenziali OAuth:
   - **Web client** → il suo *client ID* è il `serverClientId` che l'app passa a Google e che Supabase accetta come audience.
   - **Android client** per ciascuna impronta SHA-1: **debug**, **release** (`keystore/` del progetto — la chiave deve restare quella) e, se mai userai il Play Store, quella di Play App Signing.
3. In Supabase → Auth → Google: incollare client ID/secret del Web client.

### 5.2 Flusso nell'app

```
tap "Accedi con Google"
 └─ AuthRepository.signIn(activity)
     1. rawNonce = random; hashed = sha256(rawNonce)
     2. Credential Manager: GetGoogleIdOption(serverClientId, nonce = hashed, filterByAuthorizedAccounts = false)
     3. GoogleIdTokenCredential → idToken
     4. supabase.auth.signInWith(IDToken) { idToken; provider = Google; nonce = rawNonce }
     5. sessione salvata da supabase-kt (refresh automatico)
     6. profiles(role) letto → AuthState.SignedIn(user, role)
```

- `AuthState`: `SignedOut | Loading | SignedIn(uid, email, name, role) | Error(msg)` esposto come `StateFlow`.
- Il ruolo è letto dal server a ogni avvio/refresh; la UI lo usa solo per mostrare/nascondere azioni.
- **Session storage** non incluso nei backup: aggiungere `dataExtractionRules`/`fullBackupContent` che escludano
  le SharedPreferences di sessione (il manifest ha `allowBackup="true"`). Il backup ZIP dell'app non contiene
  la sessione (si ripristinano solo `gymlog.db`, `photos/`, `datastore/`) → dopo un ripristino si rifà il login.
- Logout: `auth.signOut()`; i dati locali (incluso il catalogo scaricato) **restano** (offline-first).

---

## 6. Client Android

### 6.1 Dipendenze (da fissare in Fase 2 verificando compatibilità con Kotlin 2.1.0 / AGP 8.7.3 / compileSdk 35)

| Libreria | Perché |
| --- | --- |
| plugin `kotlin-serialization` (stessa versione di Kotlin) | DTO di PostgREST |
| `supabase-kt`: `auth-kt`, `postgrest-kt`, `storage-kt` + `ktor-client-okhttp` | sessione + refresh token + REST + upload |
| `androidx.credentials`, `credentials-play-services-auth`, `googleid` | login Google nativo |
| `androidx.work:work-runtime-ktx` | sync in background con vincoli di rete e backoff |
| `androidx.exifinterface` | rotazione corretta prima di togliere l'EXIF |
| Coil (già presente) | mostra l'immagine condivisa da URL, con cache su disco |

Scelta di riserva se `supabase-kt` non fosse compatibile con Kotlin 2.1.0: OkHttp (già transitivo di Coil) +
`org.json` per ~200 righe di REST/auth manuali. Si decide in Fase 0 con un *spike* di mezza giornata; **non si alza la versione di Kotlin**.

Segreti: `SUPABASE_URL` e `SUPABASE_ANON_KEY` in `local.properties` → `BuildConfig` (la anon key è pubblica per
progetto, protetta da RLS; la `service_role` **non entra mai** nel repo né nell'APK). Più `GOOGLE_WEB_CLIENT_ID`.

Manifest: `INTERNET`, `ACCESS_NETWORK_STATE`.

### 6.2 Migrazione Room 3 → 4 (solo `ADD COLUMN`)

Colonne nuove su `Exercise` (tutte nullable o con default, nessun enum/TypeConverter):

| Colonna | Tipo | Significato |
| --- | --- | --- |
| `remoteId` | `TEXT` null | UUID nel catalogo; null = mai sincronizzato |
| `remoteCreatedBy` | `TEXT` null | uid del creatore (per i permessi nella UI) |
| `remoteUpdatedAt` | `INTEGER` null | `updated_at` visto l'ultima volta (rilevare conflitti) |
| `dirty` | `INTEGER NOT NULL DEFAULT 0` | modifiche locali da inviare |
| `sharedImageKey` | `TEXT` null | chiave dell'immagine condivisa (URL = base + key) |
| `pendingImageFile` | `TEXT` null | file locale elaborato in attesa di essere proposto come immagine condivisa |

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Exercise ADD COLUMN remoteId TEXT")
        db.execSQL("ALTER TABLE Exercise ADD COLUMN remoteCreatedBy TEXT")
        db.execSQL("ALTER TABLE Exercise ADD COLUMN remoteUpdatedAt INTEGER")
        db.execSQL("ALTER TABLE Exercise ADD COLUMN dirty INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE Exercise ADD COLUMN sharedImageKey TEXT")
        db.execSQL("ALTER TABLE Exercise ADD COLUMN pendingImageFile TEXT")
    }
}
```

- **Niente indice unico su `remoteId`**: sarebbe l'unica istruzione non-`ADD COLUMN`. L'unicità è garantita dal
  sync (un solo worker alla volta, `Mutex`) e controllata nel merge.
- `4.json` esportato e committato; `MigrationTest` esteso con 3→4 (righe esistenti intatte, default corretti)
  e catena 1→4.
- Cursore del pull e uid dell'ultimo account sincronizzato vanno in **DataStore** (`Settings`), non in Room.
- Procedura obbligatoria prima di toccare il telefono: **Impostazioni → Esporta backup**, poi test della
  migrazione sulla copia `~/Backup-GymLog/…`, poi `adb install -r`.

### 6.3 Nuovi componenti (pacchetti e file)

```
data/remote/
  SupabaseProvider.kt        crea SupabaseClient da BuildConfig (null se non configurato)
  AuthRepository.kt          AuthState, signIn/signOut/deleteAccount, ruolo
  CatalogRemote.kt           interfaccia: pull(cursor), insert, update, uploadImage, setImage, clearImage, delete
  SupabaseCatalogRemote.kt   implementazione con postgrest/storage + DTO @Serializable
data/sync/
  CatalogSync.kt             logica pura di merge/push/pull (testabile con FakeCatalogRemote)
  SharedImageProcessor.kt    decode con inSampleSize, rotazione EXIF, max 1024 px, JPEG q80, senza EXIF
  SyncWorker.kt              CoroutineWorker; SyncScheduler.kt (unique work, vincolo rete, backoff)
  SyncStatus.kt              stato osservabile: idle/running/lastSyncAt/ultimo errore
domain/
  ExercisePermissions.kt     canEditShared, canSetSharedImage, canReplaceSharedImage, canDeleteShared
```

`AppContainer` aggiunge `auth`, `catalogSync`, `syncScheduler` (nulli se la funzione è spenta). I repository
esistenti restano l'unica porta d'accesso a Room.

### 6.4 Algoritmo di sincronizzazione

Eseguito sotto `Mutex`, solo se `SignedIn`. Triggers: dopo il login, apertura app (foreground), dopo ogni
salvataggio di un esercizio condiviso (debounce 5 s), pulsante "Sincronizza ora", periodico ogni 12 h (WorkManager `KEEP`).

```
syncOnce():
  1. pushNew()      per ogni Exercise con remoteId = null, !needsReview, !archived:
                      remoteId = UUID locale (salvato SUBITO in Room → i retry riusano lo stesso id)
                      POST ignora-duplicati
                        ok            → remoteCreatedBy = me, remoteUpdatedAt = server, dirty = 0
                        23505 (nome)  → GET per name_key → collega a quello esistente (remoteId = esistente)
                                        e adotta i campi remoti (il tuo "Squat" era già nel catalogo)
  2. pushDirty()    per ogni Exercise con remoteId != null && dirty = 1 && permessi:
                      PATCH … WHERE id = :id AND updated_at = :remoteUpdatedAt
                        0 righe → conflitto: vince il server, dirty = 0, snackbar "modifica non applicata"
  3. pushImages()   per ogni Exercise con pendingImageFile != null && remoteId != null:
                      upload <uid>/<uuid>.jpg → rpc set_exercise_image
                        true  → sharedImageKey = path, pendingImageFile = null
                        false → pendingImageFile = null, notifica "qualcuno ha già aggiunto l'immagine"
  4. pull()         loop: GET exercises WHERE updated_at >= cursor ORDER BY updated_at, id LIMIT 500
                      (>= + upsert idempotente: nessuna riga persa se due hanno lo stesso timestamp)
                      applica ogni riga (vedi merge) in una transazione Room, poi avanza il cursore
```

**Merge di una riga remota `r`** (mai toccati: `executionNotes`, `loadStepKg`, `hevyName`, `needsReview`):

| Caso | Azione |
| --- | --- |
| Esiste `Exercise.remoteId == r.id` | Aggiorna campi condivisi e `sharedImageKey` (se `dirty = 0`; se `dirty = 1` il push viene prima, quindi il caso non si verifica) |
| Nessun remoteId ma `nome` uguale (NOCASE, trim) | **Collega** (`remoteId = r.id`), campi remoti vincono. Vale per il seed e per gli esercizi già presenti |
| Nessuna corrispondenza | **Inserisci** nuovo `Exercise` locale (id locale nuovo) |
| `r.deleted_at != null` | `archived = true` (le righe non si cancellano mai: lo storico resta valido) |
| Il nome remoto collide con un esercizio **solo locale** | rinomina quello locale in `"<nome> (mio)"` ed evita il crash sull'indice unico |
| Due locali ("Squat"/"squat") puntano allo stesso remoto | il primo si collega, il secondo resta locale e viene segnalato |
| Valore di enum sconosciuto (app vecchia, server nuovo) | salta la riga e mostra "aggiorna l'app per vedere N esercizi" |

**Primo login di un utente esistente (tu):** schermata di riepilogo *"Condividerai N esercizi: nome, muscoli, tipo.
Le tue note, i tuoi allenamenti e le tue foto restano sul telefono"* con conferma; gli esercizi `needsReview`
(import Hevy da completare) e `archived` non vengono caricati finché non sono sistemati.

**Cambio account / backup ripristinato:** `Settings.syncUid` diverso dall'uid corrente → il catalogo è globale,
quindi si mantengono i `remoteId`; i `dirty` che l'utente non può più inviare vengono scartati con avviso.

### 6.5 Immagini

- **Creazione/consenso.** Nel salvataggio di un nuovo esercizio la prima foto scattata/scelta può essere
  condivisa (checkbox *"Mostra questa foto a tutti gli utenti"*, preselezionata, con testo esplicito: sarà pubblica).
- **Esercizio senza immagine condivisa** (`sharedImageKey = null`): su ogni foto locale il pulsante
  **"Usa come immagine condivisa"** (conferma). Disponibile per tutti gli utenti loggati; è così che anche tu
  promuovi le tue 3 foto esistenti.
- **Esercizio con immagine condivisa:** utente normale → badge 🔒 "Immagine condivisa", nessun pulsante di modifica;
  **admin** → "Sostituisci" (stesso flusso, `set_exercise_image` lo consente) e "Rimuovi" (`clear_exercise_image`).
- **Elaborazione** (`SharedImageProcessor`): applica la rotazione EXIF, ridimensiona a ≤ 1024 px sul lato lungo,
  JPEG qualità 80 (~100–200 KB), **senza metadati** (via GPS e data di scatto). La foto locale originale resta intatta.
- **Offline:** l'immagine resta in `pendingImageFile` finché c'è rete; se nel frattempo qualcuno l'ha impostata,
  la tua viene scartata e vedi la sua.
- **Visualizzazione:** `PhotoCarousel` mostra prima l'immagine condivisa (URL → Coil, cache disco 100 MB),
  poi le foto locali. Prefetch (Wi-Fi) per gli esercizi della scheda attiva, così l'allenamento in palestra
  senza campo mostra comunque le immagini già viste. Le immagini condivise **non** entrano nel backup ZIP
  (si riscaricano).

### 6.6 Modifiche alla UI

| Schermata | Modifica |
| --- | --- |
| **Impostazioni** | Nuova sezione *Account e catalogo condiviso*: stato, "Accedi con Google", avatar/nome/email, badge **Admin**, "Sincronizza ora", ultimo sync/errore, "Esci", "Elimina account", link privacy |
| **Libreria esercizi** | Icona ☁ per gli esercizi condivisi, pill "da sincronizzare" (`AccentPill` già esistente), filtro Tutti / Miei |
| **Modifica esercizio** | Campi condivisi **disabilitati con spiegazione** se `!canEditShared`; sezione immagine come in §6.5; campi personali (note, passo, foto locali) sempre modificabili |
| **Allenamento ⋮ → Modifica esercizio** | Nessuna modifica: riusa `ExerciseEditScreen`, quindi eredita permessi e immagine condivisa; `refreshExercise` già ricarica al ritorno |
| **Home / Esercizi (prima volta dopo l'update)** | Banner non bloccante "Condividi il catalogo esercizi con gli altri utenti" → apre Impostazioni |

I permessi lato UI vivono in `ExercisePermissions` (funzioni pure → test JVM): `canEditShared = remoteId == null || admin || remoteCreatedBy == me`,
`canSetSharedImage = remoteId != null && (admin || sharedImageKey == null)`.

---

## 7. Sicurezza e privacy

| Tema | Misura |
| --- | --- |
| Autorizzazioni | Solo RLS + funzioni `SECURITY DEFINER` con `search_path = ''`; nessun permesso deciso dal client |
| Escalation | `role` non scrivibile dal client; admin deciso da `admin_emails` + email verificata da Google |
| Chiavi | Nell'APK solo URL + anon key; `service_role` solo in ambiente admin |
| Abuso | Quote di creazione, limite 1 MB e solo `image/jpeg` sul bucket, niente update/delete file per utenti, signup email/anonimo disattivati |
| Contenuti illeciti | Bucket scrivibile solo da loggati; admin può rimuovere; (Fase 6) pulsante **Segnala** → tabella `reports` |
| Privacy dati | EXIF rimosso; note personali non caricate; sessione esclusa dai backup di sistema |
| GDPR | Base giuridica: consenso esplicito al primo login; **eliminazione account** in-app; regione UE; pagina informativa (titolare, dati trattati: email, nome, uid, esercizi/immagini caricati) |
| README | Aggiornare *Privacy e permessi*: ora c'è `INTERNET`, esiste un account **facoltativo**, cosa viene e non viene inviato |
| Play Store (se mai) | Richiede privacy policy pubblica, eliminazione account in-app *e* via web, dichiarazione Data Safety |

---

## 8. Strategia di test

**Backend (SQL, stack locale con `supabase start`, pgTAP):**
1. Utente normale: `set_exercise_image` su esercizio senza immagine → `true`; seconda chiamata di un altro utente → `false`.
2. Utente normale non può sostituire/rimuovere; admin sì.
3. Percorso di un altro utente (`<altro-uid>/x.jpg`) rifiutato; file inesistente rifiutato.
4. **Gara:** due transazioni concorrenti → esattamente un `true`.
5. Non-creatore non può `UPDATE` di `name`; creatore sì; nessuno scrive `image_path`/`role`/`deleted_at` direttamente.
6. Duplicato di nome (anche `" squat "` vs `"Squat"`) rifiutato; quota 30/24 h; `anon` non legge nulla.
7. Trigger `handle_new_user`: email in `admin_emails` → `admin`, altre → `user`.

**Client (JVM, Robolectric + Room in-memory, stile dei test esistenti con `TestDb`):**
- `MigrationTest` 3→4 e 1→4; test sul DB reale ripristinato dal backup `~/Backup-GymLog`.
- `CatalogSyncTest` con `FakeCatalogRemote`: link per nome, inserimento, tombstone→archived, collisione nome locale,
  retry idempotente del push (stesso UUID), `23505`→collegamento, conflitto `updated_at`, cursore con timestamp uguali,
  enum sconosciuto, cambio account.
- `SharedImageProcessorTest`: dimensione massima, rotazione, **assenza di EXIF/GPS**, peso < 300 KB.
- `ExercisePermissionsTest` (tabella di verità), `AuthRepository` (mappatura ruolo, errori), ViewModel di modifica (campi bloccati).
- Regressione: tutti i 190 test esistenti devono restare verdi con la funzione **spenta** (`SUPABASE_URL` vuoto).

**Manuale a due/tre dispositivi (checklist di rilascio):** utente A crea esercizio → B lo vede entro un sync;
B aggiunge immagine mancante; A non può sostituirla; admin la sostituisce; aereo: crea esercizio offline e
sincronizza dopo; due utenti caricano insieme l'immagine; logout/login; elimina account; ripristino backup.

---

## 9. Compatibilità con le funzioni esistenti

| Funzione | Impatto |
| --- | --- |
| Backup / ripristino ZIP | `gymlog.db` include le nuove colonne; restore di un backup v3 → migra da solo; `photos/` invariato; la sessione non è nel backup |
| Import Hevy | Invariato; gli esercizi creati sono `needsReview` e non vengono caricati finché non completati |
| Seed | Resta, ma al primo sync si collega al catalogo per nome (nessun duplicato) |
| Eliminazione esercizio | Locale: come oggi (elimina o archivia se usato). Condiviso: l'utente normale lo **archivia solo sul suo telefono**; l'eliminazione globale è solo admin |
| Statistiche / allenamenti | Nessuna modifica: usano gli id locali, mai cambiati |
| Widget, timer, Corpo | Nessuna modifica |

---

## 10. Piano di lavoro per fasi

Stime in giornate di lavoro effettivo (sviluppo + test), indicative.

### Fase 0 — Decisioni, account e spike (0,5–1 g)
- Conferma delle decisioni aperte (§12). `git init` + commit della 1.5.0.
- Crea progetto Supabase (UE); crea progetto Google Cloud, consent screen, client Web + Android (debug e release).
- Spike: `supabase-kt` compila con Kotlin 2.1.0/AGP 8.7.3 e fa login con un token reale → altrimenti piano B (OkHttp).
- **Uscita:** una app di prova stampa l'uid dopo il login Google.

### Fase 1 — Backend (2–3 g)
- Migrazioni SQL (§4), seed generato da `SeedData`, bucket e policy, funzioni, quota, `admin_emails`.
- Test pgTAP (§8) in locale con Docker; script `backend/README.md` per applicare le migrazioni.
- Job di pulizia file orfani + ping settimanale anti-pausa.
- **Uscita:** tutti i test SQL verdi; i due account admin hanno `role = 'admin'`.

### Fase 2 — Fondamenta client (1–2 g)
- Dipendenze, plugin serialization, `BuildConfig`, permessi `INTERNET`/`ACCESS_NETWORK_STATE`, `dataExtractionRules`.
- `Exercise` + `MIGRATION_3_4` + `4.json` + `MigrationTest`; test sulla copia del backup reale.
- Interfacce `CatalogRemote`, `FakeCatalogRemote`, `ExercisePermissions`.
- **Uscita:** `scripts/test.sh` verde (190+ test), APK installabile con `adb install -r` senza perdita di dati.

### Fase 3 — Autenticazione (2 g)
- `SupabaseProvider`, `AuthRepository`, Credential Manager con nonce, lettura ruolo.
- Sezione Impostazioni (login, logout, stato, badge admin, elimina account), schermata di consenso.
- **Uscita:** login/logout funzionanti su telefono; il tuo account mostra **Admin**, un altro account no.

### Fase 4 — Sincronizzazione dei metadati (3–4 g)
- `SupabaseCatalogRemote`, `CatalogSync` (push new/dirty, pull a cursore, merge), `SyncWorker`/`SyncScheduler`, `SyncStatus`.
- Marcatura `dirty` in `ExerciseRepository.save`; pill "da sincronizzare"; icona ☁; filtro Miei.
- **Uscita:** `CatalogSyncTest` verde; due telefoni (A e B) scambiano esercizi; offline→online recupera.

### Fase 5 — Immagini condivise (2–3 g)
- `SharedImageProcessor`, upload + `set_exercise_image`, `pendingImageFile`, gestione "persa la gara".
- UI: blocchi per ruolo, "Usa come immagine condivisa", consenso, Sostituisci/Rimuovi per admin; `PhotoCarousel` con URL + Coil; prefetch Wi-Fi.
- **Uscita:** matrice permessi del §2 verificata end-to-end con utente normale, creatore e admin.

### Fase 6 — Amministrazione e moderazione (1–2 g, parzialmente opzionale)
- Azioni admin: eliminazione globale (soft), modifica metadati di qualsiasi esercizio.
- (Opz.) Tabella `reports` + pulsante "Segnala immagine/esercizio"; vista admin delle segnalazioni.
- **Uscita:** un admin può togliere un'immagine inappropriata da telefono.

### Fase 7 — QA e rilascio 1.6.0 (2 g)
- Checklist manuale (§8), test di regressione completo, aggiornamento README/INSTALLA.md (privacy, permessi, account).
- `versionCode` 9 / `versionName` "1.6.0"; build con JBR 21; **backup dal telefono prima di installare**;
  `adb install -r` (mai `pm clear`/`uninstall`); verifica che esercizi, serie, pesi e foto esistenti siano ancora lì.
- **Uscita:** APK in `dist/GymLog-1.6.0.apk`.

**Ordine consigliato e punti di stop:** le Fasi 0–2 non cambiano il comportamento dell'app (si possono rilasciare
da sole come 1.5.1 con la migrazione già pronta). Le Fasi 3–5 sono dietro il flag di configurazione; si può testare
tutto in debug contro uno stack locale prima di toccare il progetto Supabase reale.

---

## 11. Costi e limiti operativi

- **Supabase free (da verificare al momento della creazione):** ~500 MB database, ~1 GB storage, ~5 GB egress/mese,
  50.000 utenti attivi/mese. Con immagini da ~150 KB servono ~6.500 immagini per riempire 1 GB.
- **Pausa per inattività:** i progetti free vengono messi in pausa dopo circa una settimana senza richieste →
  ping settimanale (cron sul tuo PC/server Docker o GitHub Action) oppure piano Pro (~25 $/mese).
- **Egress:** immagini lazy + cache Coil + `Cache-Control` lungo; niente download di massa all'avvio.
- **Backup del database cloud:** `pg_dump` periodico (nel free tier i backup gestiti sono limitati) verso il tuo disco.

## 12. Rischi

| Rischio | Prob. | Impatto | Mitigazione |
| --- | --- | --- | --- |
| Migrazione Room che perde dati | Bassa | **Critico** | Solo `ADD COLUMN`, `MigrationTest`, prova sulla copia del backup, backup dal telefono prima di installare |
| `supabase-kt` incompatibile con Kotlin 2.1.0 | Media | Medio | Spike in Fase 0; piano B OkHttp; non aggiornare Kotlin |
| SHA-1 sbagliato → login Google fallisce in release | Media | Medio | Registrare debug + release subito; stessa keystore; test su APK firmato |
| Doppioni di esercizi (nomi quasi uguali: "Squat" / "Squat (Bilanciere)") | Alta | Basso | `name_key` unico lato server; fusione manuale da admin; futuro: suggerimento "esiste già" mentre si digita |
| Contenuti inappropriati nelle immagini | Media | Medio | Solo loggati, 1 MB, moderazione admin, segnalazioni (Fase 6) |
| Progetto free in pausa | Alta | Basso | Ping settimanale / piano Pro; l'app resta usabile offline |
| Cambio di nome remoto che collide con esercizio locale | Media | Basso | Rinomina del locale con suffisso, mai errore |
| Utente rinomina esercizi per tutti | — | — | Evitato: solo creatore/admin modificano i campi condivisi |
| Complessità del sync | Media | Medio | Logica isolata in `CatalogSync` + fake remote + test; nessuna tabella outbox |

## 13. Decisioni aperte (con il default che userò se non dici altro)

1. **Login facoltativo** (default) o obbligatorio? Obbligatorio romperebbe l'uso offline al primo avvio.
2. **Backend:** Supabase (default) o Firebase?
3. **Chi è "admin"?** Default: ruolo `admin` assegnato via `admin_emails`; il tuo account incluso. Servono altre email?
4. **Metadati condivisi modificabili da** creatore + admin (default). Alternativa: solo admin.
5. **Una sola immagine** per esercizio (default, coerente con "l'immagine"); le foto locali multiple restano personali.
6. **Note di esecuzione condivise?** Default **no** (restano personali).
7. **Caricamento automatico degli esercizi esistenti** al primo login, con schermata di consenso (default sì).
8. **Distribuzione:** APK diretti (default) o Play Store? Cambia SHA-1, privacy policy e obblighi di eliminazione account.
9. **Moderazione (Fase 6):** solo azioni admin (default) o anche segnalazioni degli utenti?
10. **Versione:** 1.6.0 unica o 1.5.1 con solo migrazione/fondamenta prima?

---

## 14. Definizione di "fatto"

- [ ] Un utente nuovo accede con Google, vede il catalogo e può usarlo offline.
- [ ] Un esercizio creato da A compare a B (e viceversa) dopo la sincronizzazione.
- [ ] Immagine: B può aggiungerla se manca, **non** può cambiarla se esiste; admin e `nicolosimonini3@gmail.com` possono sostituirla/rimuoverla; due upload simultanei → un solo vincitore.
- [ ] Allenamenti, serie, misure, note e foto personali non compaiono mai sul server.
- [ ] Nessun dato locale perso nell'aggiornamento da 1.5.0 (verificato su backup reale e su telefono).
- [ ] Tutti i test (esistenti + nuovi JVM + pgTAP) verdi; README e privacy aggiornati.
- [ ] **Export della chat eseguito** prima di esaurire i token (§15).

---

## 15. Export della chat prima della fine dei token

**Regola:** prima che i token della sessione finiscano, va fatto un export della conversazione, così decisioni,
motivazioni e stato del lavoro non si perdono con la compattazione del contesto.

**Quando.** Non si aspetta l'ultimo momento: l'export è un punto di controllo obbligatorio
- a fine di **ogni fase** del §10 (insieme al commit);
- appena il contesto si avvicina al limite (circa 80% o al primo avviso di compattazione automatica);
- prima di qualsiasi operazione rischiosa (installazione sul telefono, migrazione di produzione, `supabase db push`).

**Come.**
1. Export leggibile: comando `/export` di Claude Code (lo lancia l'utente) → file Markdown/testo.
2. Copia integrale grezza: il transcript JSONL della sessione in
   `~/.claude/projects/-home-cri5282/<id-sessione>.jsonl`, copiato con `cp` (nessun comando distruttivo).
3. Destinazione: `docs/chat-export/AAAA-MM-GG-fase-N.md` (e `.jsonl`) dentro il progetto, versionati con git.
4. Nel file di export, in testa: fase raggiunta, decisioni prese (§13), prossimo passo, comandi di build/test usati.

**Attenzione ai segreti.** Prima di committare un export va fatta una scansione e una redazione di: `service_role` key,
password della keystore (`keystore.properties`), client secret OAuth, token/JWT, `SUPABASE_ANON_KEY` se non voluta nel
repo. Se la scansione trova qualcosa, il file non si committa finché non è ripulito. `docs/chat-export/` va
aggiunto a `.gitignore` se il repo diventa pubblico.

**Verifica.** Il punto di controllo è completo solo se il file esiste, non è vuoto, contiene l'ultimo messaggio della sessione
e ha superato la scansione dei segreti. Se non c'è più spazio per eseguire l'export, ha priorità su qualsiasi altra attività.
