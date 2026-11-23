# Shapat: dizajn handoff za Claude Code

Pročitaj ovo prvo, pa `SPEC.md` (ekrani i komponente), pa `tokens.json` (boje, tipografija, razmaci). `reference/ShapatTheme.kt` je samo referenca, ne kopiraj ga slepo.

## Kontekst
- Shapat je messenger, fork SimpleX Chat (repo `broceta26/simplex-chat`, grana `mesindzer/rebrand`, app id `com.balkanbug.chat`). Urađen je samo Android; iOS nije počet.
- Ovaj folder je isporuka dizajna (`docs/design/`) i nije kod aplikacije.
- Provereno u kodu (23.11.2026): UI je Compose Multiplatform u `apps/multiplatform/common` (Android i desktop dele kod). Teme su u `ui/theme/Theme.kt` (`enum DefaultTheme { LIGHT, DARK, SIMPLEX, BLACK }`, po jedna `ColorPalette` i `ColorPaletteApp` za svaku, `ThemeColors`, `ThemeOverrides`), a primena u `ThemeManager.kt`. Test je `ThemesTest.kt`. Zato se „Ponoć i zlato" i „Nord tmux" dodaju kao nove `DefaultTheme` vrednosti sa sopstvenim paletama, ne kao paralelni sistem.
- Palete ne mogu da nose celu tmux temu: ona menja i raspored (nema balona, monospace linije, statusna traka). Prvo dodaj palete, pa tmux raspored kao poseban prikaz poruke koji se uključuje temom. Ne kopiraj ceo ekran razgovora.
- Pročitaj i `apps/multiplatform/CLAUDE.md` i `docs/CONTRIBUTING.md` pre prve izmene.

## Dizajn u jednoj rečenici
Struktura liste kao WhatsApp, baloni i reakcije kao iMessage, više uređaja kao Telegram. Dve teme: **Ponoć i zlato** (podrazumevana) i **Nord tmux** (terminalska, opciona, bira se u podešavanjima).

## Šta se NE menja
- Protokol, kriptografija, red poruka, direktorijum imena (`apps/sapat-directory`), so `sapat-wipe|` za PIN brisanja, interni nazivi u kodu (`SapatHomeView`, `scripts/sapat`, `sapat-test.keystore`). Ovo je samo koža i raspored.
- Skup reakcija ostaje onaj koji SimpleX već podržava; menja se samo prikaz i birač.
- Vidljivo ime je svuda „Shapat" (odluka od 22.11.2026).

## Redosled rada (svaka faza zasebna grana ili commit, sa screenshotom)
1. Tokeni i tema: ubaci „Ponoć i zlato" kao podrazumevanu temu, prekidač u podešavanjima.
2. Lista razgovora: red, čipovi, bedž, tab traka.
3. Razgovor: baloni sa repom, reakcije, birač (dugi pritisak), polje za unos.
4. Kartica „Uređaji" (veza sa postojećom funkcijom povezivanja desktopa; ne izmišljaj novu).
5. Tema „Nord tmux" (isti ekrani, druga koža, monospace, nema zaobljenja).
6. Desktop raspored sa tri kolone.
7. iOS (poseban zadatak, tek kad Android prođe pregled).

## Završeno znači
- Svaki ekran iz `SPEC.md` postoji u obe teme, sa screenshotom pored dizajna.
- Kontrast teksta ≥ 4.5:1 (vrednosti u `tokens.json` su već proverene, ne vraćaj stare).
- Mete dodira ≥ 48dp, svi ikonski dugmići imaju `contentDescription`, UI izdrži font 200% bez skraćivanja imena.
- Postojeći testovi i `android-emu` CI prolaze.
