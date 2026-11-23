# Shapat — arhitektura (kratko)

Fork SimpleX Chat-a. Haskell jezgro se ne menja; Shapat živi u Kotlin sloju (`apps/multiplatform/common`), u servisu imenika (`apps/sapat-directory`) i u CI skriptama (`scripts/ci`, `scripts/sapat`).

## Ulazak u aplikaciju (`App.kt`, `MainScreen`)
Redosled odluka: migracija → greška baze → **PIN nije postavljen: `MandatoryPinView`** → prvi ulazak (onboarding) ili početni ekran → **`SapatNameGate`** preko početnog ekrana. Zaključana aplikacija prikazuje samo ekran za PIN (`LocalAuthView`).

## PIN (`AppLock.kt`, `views/localauth`)
- PIN se čuva u Android Keystore-u (`ksAppPassword`); režim je uvek PIN, ne sistemsko otključavanje.
- `SapatWipe`: PIN-ovi za uništavanje (samo SHA-256), brojač pogrešnih PIN-ova, pauza, opciono brisanje.
- PIN je brava na ulazu; bazu šifruje poseban ključ iz Keystore-a.

## Ime i kod (`model/SapatDirectory.kt`, `views/chatlist/SapatNameGate.kt`)
- Imenik čuva: kod (9 cifara), ime, adresu za kontakt. Aplikacija čuva token po profilu.
- Novi profil: prikazno ime je ime za imenik; preuzima se kad razgovor proradi (`SapatName.checkUser`).
- Pravila imena su ista u aplikaciji i u `directory.py` (`NAME_RE`).

## Testovi i build
- `:common:desktopTest` (JVM); Shapat testovi: `SapatGatesTest`, `SapatShotsTest`.
- `android-emu.yml` na svaki push u `mesindzer/**`: build APK-a i `scripts/ci/android/smoke-test.sh` u emulatoru.
