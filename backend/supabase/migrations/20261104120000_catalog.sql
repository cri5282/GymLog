-- GymLog — catalogo esercizi condiviso (1.6.0)
--
-- Regole, in ordine di importanza:
--   * l'admin è chi accede con un'email presente in public.admin_emails. Non esiste una tabella
--     profiles né un ruolo salvato al primo accesso: aggiungere un'email qui basta subito.
--   * i campi condivisi li modifica il creatore o un admin.
--   * l'immagine la imposta chiunque se manca; la sostituisce solo un admin. È un UPDATE
--     condizionale: due utenti che caricano insieme producono un solo vincitore, senza tabelle
--     di lock.
--   * note personali, passo di carico, hevyName, storico, schede e foto locali non esistono qui.
--
-- Verificato da backend/test.sh su un Postgres vero (stub di auth/storage).

create domain public.muscle as text
    check (value in (
        'PETTO', 'DORSALI', 'TRAPEZI', 'DELTOIDE_ANTERIORE', 'DELTOIDE_LATERALE',
        'DELTOIDE_POSTERIORE', 'BICIPITI', 'TRICIPITI', 'AVAMBRACCI', 'ADDOME',
        'QUADRICIPITI', 'FEMORALI', 'GLUTEI', 'POLPACCI', 'ADDUTTORI', 'ABDUTTORI'
    ));

create domain public.length_bias as text
    check (value in ('NONE', 'LENGTHENED', 'SHORTENED'));

-- ---------------------------------------------------------------------------------------------
-- Admin
-- ---------------------------------------------------------------------------------------------

create table public.admin_emails (
    email text primary key check (email = lower(email))
);

insert into public.admin_emails (email) values ('nicolosimonini3@gmail.com');

alter table public.admin_emails enable row level security;
-- Nessuna policy: la tabella si legge solo tramite is_admin() (security definer).

create function public.is_admin() returns boolean
language sql stable security definer set search_path = ''
as $$
    select exists (
        select 1
        from public.admin_emails a
        join auth.users u on lower(u.email) = a.email
        where u.id = auth.uid()
          and u.email_confirmed_at is not null
    )
$$;

-- ---------------------------------------------------------------------------------------------
-- Catalogo
-- ---------------------------------------------------------------------------------------------

create table public.exercises (
    id                uuid primary key,
    name              text not null check (char_length(btrim(name)) between 2 and 80),
    name_key          text generated always as (lower(btrim(name))) stored,
    primary_muscle    public.muscle not null,
    secondary_muscles public.muscle[] not null default '{}',
    length_bias       public.length_bias not null default 'NONE',
    single            boolean not null default false,
    image_path        text,
    image_set_by      uuid references auth.users (id) on delete set null,
    image_set_at      timestamptz,
    created_by        uuid references auth.users (id) on delete set null,
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now(),
    deleted_at        timestamptz
);

-- Un nome può esistere una volta sola, ma un nome cancellato si può riusare.
create unique index exercises_name_key_uq on public.exercises (name_key) where deleted_at is null;

create function public.touch_updated_at() returns trigger
language plpgsql set search_path = ''
as $$
begin
    new.updated_at = now();
    return new;
end
$$;

create trigger exercises_touch_updated_at
    before update on public.exercises
    for each row execute function public.touch_updated_at();

alter table public.exercises enable row level security;

create policy exercises_select on public.exercises
    for select to authenticated
    using (deleted_at is null or (select public.is_admin()));

create policy exercises_insert on public.exercises
    for insert to authenticated
    with check (
        created_by = (select auth.uid())
        and image_path is null
        and deleted_at is null
    );

create policy exercises_update on public.exercises
    for update to authenticated
    using (
        deleted_at is null
        and ((select public.is_admin()) or created_by = (select auth.uid()))
    )
    with check (deleted_at is null or (select public.is_admin()));

-- Nessun permesso di DELETE: la cancellazione è un soft delete da funzione.
revoke all on table public.exercises from anon, authenticated;
grant select on table public.exercises to authenticated;
grant insert (id, name, primary_muscle, secondary_muscles, length_bias, single, created_by)
    on table public.exercises to authenticated;
grant update (name, primary_muscle, secondary_muscles, length_bias, single)
    on table public.exercises to authenticated;

-- ---------------------------------------------------------------------------------------------
-- Immagine condivisa: una sola per esercizio, compare-and-set
-- ---------------------------------------------------------------------------------------------

create function public.set_exercise_image(p_exercise uuid, p_path text) returns boolean
language plpgsql security definer set search_path = ''
as $$
declare
    n int;
begin
    if auth.uid() is null then
        raise exception 'utente non autenticato' using errcode = '42501';
    end if;

    -- <uid>/<uuid>.jpg: un solo livello, nessun percorso relativo.
    if p_path is null
       or p_path !~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.jpg$' then
        raise exception 'percorso immagine non valido' using errcode = '22023';
    end if;

    if not (public.is_admin() or split_part(p_path, '/', 1) = auth.uid()::text) then
        raise exception 'percorso di un altro utente' using errcode = '42501';
    end if;

    if not exists (
        select 1 from storage.objects o
        where o.bucket_id = 'exercise-images' and o.name = p_path
    ) then
        raise exception 'file non caricato' using errcode = '22023';
    end if;

    -- Atomicità: se due utenti arrivano insieme, Postgres serializza sulla riga; il secondo
    -- rivaluta il WHERE, trova image_path valorizzato e aggiorna 0 righe -> false.
    update public.exercises
       set image_path = p_path,
           image_set_by = auth.uid(),
           image_set_at = now()
     where id = p_exercise
       and deleted_at is null
       and (image_path is null or public.is_admin());

    get diagnostics n = row_count;
    return n = 1;
end
$$;

create function public.clear_exercise_image(p_exercise uuid) returns void
language plpgsql security definer set search_path = ''
as $$
begin
    if not public.is_admin() then
        raise exception 'solo un admin può rimuovere l''immagine' using errcode = '42501';
    end if;

    update public.exercises
       set image_path = null,
           image_set_by = auth.uid(),
           image_set_at = now()
     where id = p_exercise and deleted_at is null;
end
$$;

-- ---------------------------------------------------------------------------------------------
-- Amministrazione
-- ---------------------------------------------------------------------------------------------

create function public.admin_delete_exercise(p_exercise uuid) returns void
language plpgsql security definer set search_path = ''
as $$
begin
    if not public.is_admin() then
        raise exception 'solo un admin può eliminare un esercizio' using errcode = '42501';
    end if;

    update public.exercises set deleted_at = now()
     where id = p_exercise and deleted_at is null;
end
$$;

create function public.delete_my_account() returns void
language plpgsql security definer set search_path = ''
as $$
begin
    if auth.uid() is null then
        raise exception 'utente non autenticato' using errcode = '42501';
    end if;

    -- Le FK on delete set null anonimizzano i contributi: gli esercizi restano nel catalogo.
    delete from auth.users where id = auth.uid();
end
$$;

revoke all on function public.is_admin() from public;
revoke all on function public.set_exercise_image(uuid, text) from public;
revoke all on function public.clear_exercise_image(uuid) from public;
revoke all on function public.admin_delete_exercise(uuid) from public;
revoke all on function public.delete_my_account() from public;

grant execute on function public.is_admin() to authenticated;
grant execute on function public.set_exercise_image(uuid, text) to authenticated;
grant execute on function public.clear_exercise_image(uuid) to authenticated;
grant execute on function public.admin_delete_exercise(uuid) to authenticated;
grant execute on function public.delete_my_account() to authenticated;

-- ---------------------------------------------------------------------------------------------
-- Storage: bucket pubblico in lettura, scrittura solo nella propria cartella
-- ---------------------------------------------------------------------------------------------

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('exercise-images', 'exercise-images', true, 1048576, array['image/jpeg'])
on conflict (id) do nothing;

create policy exercise_images_insert on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'exercise-images'
        and name like (select auth.uid())::text || '/%'
    );

create policy exercise_images_admin on storage.objects
    for all to authenticated
    using (bucket_id = 'exercise-images' and (select public.is_admin()))
    with check (bucket_id = 'exercise-images' and (select public.is_admin()));
