-- Gestione admin dei collaboratori immagini e pubblicazione eventi catalogo.
create table public.exercise_image_editors (
    user_id uuid primary key references auth.users (id) on delete cascade,
    granted_by uuid references auth.users (id) on delete set null,
    granted_at timestamptz not null default now()
);
alter table public.exercise_image_editors enable row level security;
revoke all on public.exercise_image_editors from anon, authenticated;

create function public.can_manage_exercise_images() returns boolean
language sql stable security definer set search_path = '' as $$
    select public.is_admin() or exists (
        select 1 from public.exercise_image_editors e where e.user_id = auth.uid()
    )
$$;

create function public.set_exercise_image_editor(p_email text, p_enabled boolean) returns void
language plpgsql security definer set search_path = '' as $$
declare target uuid;
begin
    if not public.is_admin() then
        raise exception 'solo un admin può assegnare i permessi immagini' using errcode = '42501';
    end if;
    select id into target from auth.users
     where lower(email) = lower(btrim(p_email)) and email_confirmed_at is not null;
    if target is null then
        raise exception 'account verificato non trovato' using errcode = '22023';
    end if;
    if p_enabled then
        insert into public.exercise_image_editors (user_id, granted_by)
        values (target, auth.uid()) on conflict (user_id) do nothing;
    else
        delete from public.exercise_image_editors where user_id = target;
    end if;
end
$$;

create function public.list_exercise_image_editors() returns setof text
language plpgsql stable security definer set search_path = '' as $$
begin
    if not public.is_admin() then
        raise exception 'solo un admin può vedere i permessi immagini' using errcode = '42501';
    end if;
    return query
        select u.email::text from public.exercise_image_editors e
        join auth.users u on u.id = e.user_id order by lower(u.email);
end
$$;

revoke all on function public.can_manage_exercise_images() from public;
revoke all on function public.set_exercise_image_editor(text, boolean) from public;
revoke all on function public.list_exercise_image_editors() from public;
grant execute on function public.can_manage_exercise_images() to authenticated;
grant execute on function public.set_exercise_image_editor(text, boolean) to authenticated;
grant execute on function public.list_exercise_image_editors() to authenticated;

drop policy exercise_images_insert on storage.objects;
create policy exercise_images_insert on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'exercise-images'
        and name like (select auth.uid())::text || '/%'
        and (select public.can_manage_exercise_images())
    );

create or replace function public.set_exercise_image(p_exercise uuid, p_path text) returns boolean
language plpgsql security definer set search_path = '' as $$
declare n int;
begin
    if not public.can_manage_exercise_images() then
        raise exception 'permesso immagini richiesto' using errcode = '42501';
    end if;
    if p_path is null
       or p_path !~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.jpg$' then
        raise exception 'percorso immagine non valido' using errcode = '22023';
    end if;
    if not (public.is_admin() or split_part(p_path, '/', 1) = auth.uid()::text) then
        raise exception 'percorso di un altro utente' using errcode = '42501';
    end if;
    if not exists (select 1 from storage.objects o where o.bucket_id = 'exercise-images' and o.name = p_path) then
        raise exception 'file non caricato' using errcode = '22023';
    end if;
    update public.exercises set image_path = p_path, image_set_by = auth.uid(), image_set_at = now()
     where id = p_exercise and deleted_at is null;
    get diagnostics n = row_count;
    return n = 1;
end
$$;
revoke all on function public.set_exercise_image(uuid, text) from public;
grant execute on function public.set_exercise_image(uuid, text) to authenticated;

create or replace function public.clear_exercise_image(p_exercise uuid) returns void
language plpgsql security definer set search_path = '' as $$
begin
    if not public.can_manage_exercise_images() then
        raise exception 'permesso immagini richiesto' using errcode = '42501';
    end if;
    update public.exercises
       set image_path = null, image_set_by = auth.uid(), image_set_at = now()
     where id = p_exercise and deleted_at is null;
end
$$;
revoke all on function public.clear_exercise_image(uuid) from public;
grant execute on function public.clear_exercise_image(uuid) to authenticated;

do $$
begin
    if exists (select 1 from pg_publication where pubname = 'supabase_realtime')
       and not exists (
           select 1 from pg_publication_tables
            where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'exercises'
       ) then
        alter publication supabase_realtime add table public.exercises;
    end if;
end
$$;
