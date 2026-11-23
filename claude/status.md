# Shapat — stanje

Poslednja izmena: 23.11.2026.

## Gde smo
- Grana `mesindzer/rebrand`, samo Android. Pregled svega urađenog do 22.11: projektni dokument `claude/shapat-pregled.md`.
- 23.11.2026: obavezan PIN i obavezno jedinstveno ime (dizajn: `docs/superpowers/specs/2026-11-23-obavezan-pin-i-ime-design.md`).

## Šta važi u aplikaciji (Android)
- Bez PIN-a nema ulaska: prvi ekran nove instalacije postavlja PIN; zaključavanje ne može da se isključi.
- Pogrešan PIN: pauza koja raste (30 s, 1 min, 5 min, 15 min). Brisanje posle 3 greške samo ako ga korisnik uključi. PIN-ovi za uništavanje rade uvek.
- Ime profila = jedinstveno ime u imeniku = prikazno ime. Ako imenik nije dostupan, traži se pri sledećem otvaranju.

## Otvoreno
1. Adresa i otisak sertifikata imenika su upisani u `SapatDirectory.kt` u javnom repou, suprotno pravilu projekta da adrese servera ne ulaze u repo. Treba odluka: domen umesto IP adrese, ili privatni repo.
2. CI (`android-emu`) registruje probne profile i imena (`ci-<vreme>`) u pravom imeniku; treba probni imenik za CI.
3. `NameAlertsTest` (20 testova) pada i pre ove izmene: očekuje `@ime.simplex`, a Shapat prikazuje `@ime`.
4. Promena prikaznog imena u podešavanjima profila ne menja ime u imeniku (i obrnuto, osim pri prvom izboru).
5. Stavke iz `claude/shapat-pregled.md`, odeljak „Otvoreno“ (iOS, produkcijski ključ, preimenovanje repoa…).
