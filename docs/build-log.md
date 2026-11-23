# Shapat — dnevnik izmena i buildova

## 23.11.2026 — obavezan PIN i obavezno ime
- `AppLock.pinRequired` i ekran `MandatoryPinView` u `App.kt`; `SetAppPasscodeView(mandatory)`; prekidač za zaključavanje uklonjen na Androidu.
- `SapatWipe`: pauza posle pogrešnih PIN-ova, opcija `SapatWipeOnWrongPin`; razlog neuspeha se prikazuje.
- `SapatDirectory`: oznaka izabranog imena, pravila imena, jedna registracija u isto vreme. `SapatNameGate.kt`: preuzimanje imena i ekran „Izaberi svoje ime“.
- `WelcomeView`: ime profila po pravilima imenika, mala slova.
- CI: `smoke-test.sh` postavlja PIN (1379) i koristi ime `ci-<vreme>`; `ui relaunch` unosi PIN; `ui pin`; `prefs.py` prima `LocalAuthenticationLockDelay`.
- Testovi: `SapatGatesTest` (8), prolaze lokalno. Lokalno prolaze i `:common:compileKotlinDesktop` i `:common:compileDebugKotlinAndroid`.
- Lokalni build traži Android SDK (`platforms;android-36`) i `git submodule update --init --recursive`.

## 23.11.2026 (veče) — povezivanje u jednom koraku
Povod: od svih koji su instalirali aplikaciju, Nikola se povezao samo sa dvoje. Simptomi: „zahtev poslat, ništa dalje“ i „ime/kod nije pronađen“.

Uzroci nađeni u kodu:
1. Dugme „Pošalji zahtev“ nije slalo zahtev: otvaralo je pripremljen razgovor, a zahtev je išao tek na „Poveži se“ u njemu. Zatim je druga strana morala ručno da prihvati.
2. Kod se prikazuje u grupama („123-456-789“ ili „123 456 789“), a unos je tražio 9 spojenih cifara: sa crticama „nije pronađen“, sa razmacima je dugme bilo isključeno.
3. Pre jutrošnje izmene prikazno ime nije bilo ime u imeniku (imenik je dodeljivao privremeno, npr. `sova-48213`), pa ime koje ljudi vide jedni kod drugih nije moglo da se pronađe.

Registracija imena nije uslov: profil se sam upisuje u imenik čim se otvori početni ekran (potvrđeno u logu emulatora: adresa i upis za oko 2 s).

Izmene:
- `AddByNameView`: `sapatConnect` priprema kontakt i odmah šalje zahtev; `SapatMyName` kartica (ime, kod, Kopiraj, Podeli).
- `SapatDirectory`: `connectTarget` prima kod sa crticama i razmacima; `acceptSettings`/`applyAccept` uključuju automatsko prihvatanje na adresi; prekidač `SapatManualAccept` u „Ime i kod“.
- `SapatHomeView`: uklonjen „Pošalji pozivni link“.
- `QRCode.kt`: `sapatNoConnectionQR` — bez QR koda kontakta/grupe i bez skenera na Androidu.
- Testovi: `SapatGatesTest` sada 11.
