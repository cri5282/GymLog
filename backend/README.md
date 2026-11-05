# Backend GymLog

Catalogo esercizi condiviso: **Supabase** (Postgres + Auth Google + Storage). L'app resta
offline-first: questo backend serve solo a scambiare esercizi, immagini e profilo.

## Contenuto

```
supabase/migrations/20261104120000_catalog.sql      schema, RLS, funzioni, bucket
supabase/migrations/20261104130000_tombstones.sql   tombstone leggibili da tutti gli utenti
supabase/migrations/20261104140000_image_editors_realtime.sql permessi immagini e realtime
supabase/tests/00_stub.sql                       stub di auth/storage (solo per i test locali)
supabase/tests/10_catalog.sql                    verifiche delle regole
test.sh                                          esegue tutto su un Postgres usa-e-getta
```

## Regole applicate

- **Admin**: chi accede con un'email in `public.admin_emails` (oggi `nicolosimonini3@gmail.com`).
  Non c'è una tabella `profiles` né un ruolo salvato alla registrazione: aggiungere un'email
  rende admin dal sync successivo, senza riregistrarsi.
- **Campi condivisi** (nome, muscoli, `lengthBias`, `single`): li modifica il creatore o un admin.
- **Immagine**: una predefinita sincronizzata per esercizio. Admin e utenti autorizzati possono
  impostarla, sostituirla o rimuoverla. L'admin assegna/revoca il permesso dalla schermata Account.
- Le modifiche al catalogo arrivano ai client autenticati tramite Supabase Realtime.
- **Mai sul server**: note di esecuzione, passo di carico, `hevyName`, storico, schede, misure,
  foto personali.
- Nessun `DELETE` dal client: solo soft delete da funzione admin.

## Catalogo iniziale

Non c'è un seed SQL: i 31 esercizi preinstallati sono già nel database locale di ogni telefono e
vengono caricati al primo accesso (con la schermata di consenso). Chi arriva dopo fa il
collegamento per nome (`name_key` unico), quindi non nascono doppioni.

## Applicare a un progetto Supabase

```bash
supabase link --project-ref <ref>
supabase db push
```

Poi in *Auth → Providers → Google* incollare client ID e secret del client **Web** di Google Cloud.

## Verificare in locale

```bash
./backend/test.sh
```

Avvia `postgres:16-alpine` con `--rm`, applica stub e migrazioni e lancia le verifiche: RLS,
creatore/admin, nome duplicato, muscolo non valido, gara sull'immagine, soft delete, anon senza
accesso, eliminazione account. Non richiede Supabase CLI né Docker Compose.
