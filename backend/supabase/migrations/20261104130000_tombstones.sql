-- I tombstone devono arrivare anche agli utenti normali: senza la riga cancellata il client non
-- sa di dover archiviare l'esercizio. I dati cancellati restano leggibili (il catalogo è pubblico
-- per gli utenti autenticati) e le scritture restano limitate dalle altre policy.
drop policy exercises_select on public.exercises;

create policy exercises_select on public.exercises
    for select to authenticated
    using (true);
