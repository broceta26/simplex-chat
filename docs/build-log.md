# Shapat — dnevnik izmena i buildova

## 23.11.2026 — obavezan PIN i obavezno ime
- `AppLock.pinRequired` i ekran `MandatoryPinView` u `App.kt`; `SetAppPasscodeView(mandatory)`; prekidač za zaključavanje uklonjen na Androidu.
- `SapatWipe`: pauza posle pogrešnih PIN-ova, opcija `SapatWipeOnWrongPin`; razlog neuspeha se prikazuje.
- `SapatDirectory`: oznaka izabranog imena, pravila imena, jedna registracija u isto vreme. `SapatNameGate.kt`: preuzimanje imena i ekran „Izaberi svoje ime“.
- `WelcomeView`: ime profila po pravilima imenika, mala slova.
- CI: `smoke-test.sh` postavlja PIN (1379) i koristi ime `ci-<vreme>`; `ui relaunch` unosi PIN; `ui pin`; `prefs.py` prima `LocalAuthenticationLockDelay`.
- Testovi: `SapatGatesTest` (8), prolaze lokalno. Lokalno prolaze i `:common:compileKotlinDesktop` i `:common:compileDebugKotlinAndroid`.
- Lokalni build traži Android SDK (`platforms;android-36`) i `git submodule update --init --recursive`.
