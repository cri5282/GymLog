-- Verifica delle regole del catalogo. Gira in una transazione e fa rollback: nessun dato resta.
-- Eseguito da backend/test.sh su un Postgres con gli stub di auth/storage.
--
-- Le asserzioni sono in SQL puro (niente pgTAP): l'unica dipendenza è psql.

begin;

insert into auth.users (id, email, email_confirmed_at) values
    ('00000000-0000-0000-0000-000000000001', 'nicolosimonini3@gmail.com', now()),
    ('00000000-0000-0000-0000-000000000002', 'amico@example.com', now()),
    ('00000000-0000-0000-0000-000000000003', 'altro@example.com', now()),
    ('00000000-0000-0000-0000-000000000004', 'terzo@example.com', now());

create function public.t_ok(cond boolean, msg text) returns void
language plpgsql as $$
begin
    if not coalesce(cond, false) then
        raise exception 'FAIL: %', msg;
    end if;
end
$$;
grant execute on function public.t_ok(boolean, text) to authenticated, anon;

-- =============================================================================================
-- 1. Un utente normale crea un esercizio
-- =============================================================================================
set local role authenticated;
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000002', true);

insert into public.exercises (id, name, primary_muscle, secondary_muscles, created_by)
values ('11111111-1111-1111-1111-111111111111', 'Panca piana', 'PETTO',
        '{TRICIPITI,DELTOIDE_ANTERIORE}', '00000000-0000-0000-0000-000000000002');

select public.t_ok(
    (select created_by = '00000000-0000-0000-0000-000000000002'
            and name_key = 'panca piana'
            and updated_at is not null
     from public.exercises where id = '11111111-1111-1111-1111-111111111111'),
    'creazione con creatore e name_key');

-- created_by di un altro utente: rifiutato
do $$
begin
    begin
        insert into public.exercises (id, name, primary_muscle, created_by)
        values ('33333333-3333-3333-3333-333333333333', 'Spoof', 'PETTO',
                '00000000-0000-0000-0000-000000000001');
        raise exception 'FAIL: accettato created_by altrui';
    exception when insufficient_privilege then
        null;
    end;
end
$$;

-- nome duplicato anche con maiuscole e spazi: rifiutato
do $$
begin
    begin
        insert into public.exercises (id, name, primary_muscle, created_by)
        values ('44444444-4444-4444-4444-444444444444', '  PANCA PIANA  ', 'PETTO',
                '00000000-0000-0000-0000-000000000002');
        raise exception 'FAIL: accettato un nome duplicato';
    exception when unique_violation then
        null;
    end;
end
$$;

-- muscolo fuori elenco: rifiutato
do $$
begin
    begin
        insert into public.exercises (id, name, primary_muscle, created_by)
        values ('55555555-5555-5555-5555-555555555555', 'Esercizio strano', 'SPALLE',
                '00000000-0000-0000-0000-000000000002');
        raise exception 'FAIL: accettato un muscolo inesistente';
    exception when check_violation then
        null;
    end;
end
$$;

-- =============================================================================================
-- 2. Modifica dei campi condivisi: creatore o admin
-- =============================================================================================
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000003', true);

do $$
declare n int;
begin
    update public.exercises set name = 'Panca rubata'
     where id = '11111111-1111-1111-1111-111111111111';
    get diagnostics n = row_count;
    if n <> 0 then raise exception 'FAIL: un altro utente ha rinominato l''esercizio'; end if;
end
$$;

-- image_path non è scrivibile direttamente da nessun client
do $$
begin
    begin
        update public.exercises set image_path = 'x.jpg'
         where id = '11111111-1111-1111-1111-111111111111';
        raise exception 'FAIL: image_path scrivibile dal client';
    exception when insufficient_privilege then
        null;
    end;
end
$$;

-- l'admin aggiorna l'esercizio di un altro
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000001', true);
select public.t_ok(public.is_admin(), 'prima email admin riconosciuta');
update public.exercises set name = 'Panca piana (bilanciere)'
 where id = '11111111-1111-1111-1111-111111111111';

select public.t_ok(
    (select name = 'Panca piana (bilanciere)' and updated_at >= created_at
     from public.exercises where id = '11111111-1111-1111-1111-111111111111'),
    'admin aggiorna e updated_at resta valorizzato');

-- un secondo admin vale subito, senza riregistrarsi
reset role;
insert into public.admin_emails (email) values ('altro@example.com');
set local role authenticated;
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000003', true);
select public.t_ok(public.is_admin(), 'nuova email admin attiva senza riregistrazione');

-- admin_emails non si legge dal client
do $$
begin
    begin
        perform 1 from public.admin_emails;
        raise exception 'FAIL: admin_emails leggibile dal client';
    exception when insufficient_privilege then
        null;
    end;
end
$$;

-- =============================================================================================
-- 3. Immagine: primo che arriva vince, solo l'admin sostituisce
-- =============================================================================================
reset role;
insert into storage.objects (bucket_id, name) values
    ('exercise-images', '00000000-0000-0000-0000-000000000002/aaaaaaaa-0000-0000-0000-000000000001.jpg'),
    ('exercise-images', '00000000-0000-0000-0000-000000000004/bbbbbbbb-0000-0000-0000-000000000002.jpg');

set local role authenticated;
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000002', true);

select public.t_ok(
    public.set_exercise_image(
        '11111111-1111-1111-1111-111111111111',
        '00000000-0000-0000-0000-000000000002/aaaaaaaa-0000-0000-0000-000000000001.jpg'),
    'il primo che carica imposta l''immagine');

-- il secondo arriva dopo: false, non eccezione
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000004', true);
select public.t_ok(
    not public.set_exercise_image(
        '11111111-1111-1111-1111-111111111111',
        '00000000-0000-0000-0000-000000000004/bbbbbbbb-0000-0000-0000-000000000002.jpg'),
    'il secondo non sostituisce l''immagine');

-- percorso di un altro utente: rifiutato
do $$
begin
    begin
        perform public.set_exercise_image(
            '11111111-1111-1111-1111-111111111111',
            '00000000-0000-0000-0000-000000000002/aaaaaaaa-0000-0000-0000-000000000001.jpg');
        raise exception 'FAIL: accettato il percorso di un altro utente';
    exception when insufficient_privilege then
        null;
    end;
end
$$;

-- file inesistente: rifiutato
do $$
begin
    begin
        perform public.set_exercise_image(
            '11111111-1111-1111-1111-111111111111',
            '00000000-0000-0000-0000-000000000004/cccccccc-0000-0000-0000-000000000003.jpg');
        raise exception 'FAIL: accettato un file inesistente';
    exception when invalid_parameter_value then
        null;
    end;
end
$$;

-- utente normale non può rimuovere l'immagine
do $$
begin
    begin
        perform public.clear_exercise_image('11111111-1111-1111-1111-111111111111');
        raise exception 'FAIL: un utente normale ha rimosso l''immagine';
    exception when insufficient_privilege then
        null;
    end;
end
$$;

-- l'admin sostituisce e poi rimuove
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000001', true);
select public.t_ok(
    public.set_exercise_image(
        '11111111-1111-1111-1111-111111111111',
        '00000000-0000-0000-0000-000000000004/bbbbbbbb-0000-0000-0000-000000000002.jpg'),
    'l''admin sostituisce l''immagine');

select public.clear_exercise_image('11111111-1111-1111-1111-111111111111');
select public.t_ok(
    (select image_path is null from public.exercises where id = '11111111-1111-1111-1111-111111111111'),
    'l''admin rimuove l''immagine');

-- =============================================================================================
-- 4. Soft delete e visibilità
-- =============================================================================================
select public.admin_delete_exercise('11111111-1111-1111-1111-111111111111');

select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000002', true);
select public.t_ok(
    exists (select 1 from public.exercises
            where id = '11111111-1111-1111-1111-111111111111' and deleted_at is not null),
    'l''utente normale vede il tombstone e può archiviare l''esercizio');

select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000001', true);
select public.t_ok(
    exists (select 1 from public.exercises where id = '11111111-1111-1111-1111-111111111111' and deleted_at is not null),
    'l''admin vede la riga cancellata (tombstone)');

-- il nome torna disponibile: il vecchio nome appartiene a una riga cancellata
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000002', true);
insert into public.exercises (id, name, primary_muscle, created_by)
values ('77777777-7777-7777-7777-777777777777', 'Panca piana (bilanciere)', 'PETTO',
        '00000000-0000-0000-0000-000000000002');
select public.t_ok(true, 'nome riutilizzabile dopo il soft delete');

-- =============================================================================================
-- 5. anon non legge niente
-- =============================================================================================
reset role;
set local role anon;
do $$
begin
    begin
        perform 1 from public.exercises;
        raise exception 'FAIL: anon legge il catalogo';
    exception when insufficient_privilege then
        null;
    end;
end
$$;

-- =============================================================================================
-- 6. Eliminazione account: i contributi restano ma vengono anonimizzati
-- =============================================================================================
reset role;
set local role authenticated;
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000004', true);

insert into public.exercises (id, name, primary_muscle, created_by)
values ('66666666-6666-6666-6666-666666666666', 'Rematore', 'DORSALI',
        '00000000-0000-0000-0000-000000000004');

select public.delete_my_account();

select public.t_ok(
    not exists (select 1 from auth.users where id = '00000000-0000-0000-0000-000000000004'),
    'account eliminato');

select public.t_ok(
    (select created_by is null from public.exercises where id = '66666666-6666-6666-6666-666666666666'),
    'esercizio conservato con creatore anonimizzato');

\echo 'catalogo: tutte le verifiche superate'
rollback;
