-- Stub minimo di auth e storage per provare le migrazioni su un Postgres normale.
-- Su Supabase questi schemi esistono già: questo file si usa solo da backend/test.sh.

create schema if not exists auth;
create schema if not exists storage;

create table if not exists auth.users (
    id                 uuid primary key,
    email              text,
    email_confirmed_at timestamptz
);

create or replace function auth.uid() returns uuid
language sql stable
as $$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;

create table if not exists storage.buckets (
    id                 text primary key,
    name               text not null,
    public             boolean not null default false,
    file_size_limit    bigint,
    allowed_mime_types text[]
);

create table if not exists storage.objects (
    id        uuid primary key default gen_random_uuid(),
    bucket_id text not null,
    name      text not null,
    owner     uuid,
    unique (bucket_id, name)
);

alter table storage.objects enable row level security;

do $$
begin
    if not exists (select 1 from pg_roles where rolname = 'anon') then
        create role anon nologin;
    end if;
    if not exists (select 1 from pg_roles where rolname = 'authenticated') then
        create role authenticated nologin;
    end if;
end
$$;

grant usage on schema public, auth, storage to anon, authenticated;
grant execute on function auth.uid() to anon, authenticated;
grant select, insert on storage.objects to authenticated;
grant select on storage.buckets to authenticated;
grant select on auth.users to authenticated;
