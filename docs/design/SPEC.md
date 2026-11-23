# Shapat: specifikacija ekrana i komponenti

Vrednosti boja, fontova i razmaka su u `tokens.json`. Ovde je samo struktura i ponašanje. Dimenzije u dp, tekst u sp.

## Navigacija
Donja tab traka sa tri kartice: **Razgovori**, **Pozivi**, **Uređaji**. Aktivna kartica je u boji `accent`, ostale `muted`. Traka je sakrivena unutar razgovora (umesto nje je polje za unos). Dugme „nazad" uvek vraća na listu.

## 1. Razgovori (lista)
```
Razgovori                     [olovka]
[Svi] [Neproč.] [Grupe]
(O) Milica               21:14
    Jesi li poslala fajl?   (3)
(O) Porodica             18:45
    Mira: Kad stižete?     (12)
(O) Stefan               19:02
    Dogovoreno, vidimo se sutra.
[Razgovori]  [Pozivi]  [Uređaji]
```
- Red: avatar 48, ime (strong, 1 red), pregled (muted, 1 red sa "...", u grupi prefiks „ime: "), vreme gore desno.
- Nepročitano: vreme prelazi u `accent`, bedž pilula (`accent` pozadina, `onAccent` tekst, min 20 širine). Samo broj, bez „99+" glomaznosti: preko 99 prikaži „99+".
- Razdelnik 0.5dp `divider` ispod reda. Min visina 64, ceo red je meta dodira.
- Čipovi filtriraju listu; aktivan je pun `accent`, ostali sa 0.5dp ivicom.
- Prazno stanje: naslov „Počnite prvi razgovor", jedna rečenica, dugme „Novi razgovor".

## 2. Razgovor
```
<-  (O) Milica
        na mreži
        [dolazna] Jesi li poslala fajl?
                       <3 2
   Šaljem odmah  [odlazna]
   sada · Dostavljeno
[ Poruka ............ ] [pošalji]
```
- Dolazni balon levo (`incomingBubble`), odlazni desno (`outgoingBubble`, tekst `onOutgoing`). Max širina 78% ekrana.
- Oblik: radijus 18, „rep" je ugao sa radijusom 4 (donji levi za dolazni, donji desni za odlazni). Grupisane uzastopne poruke istog pošiljaoca imaju rep samo na poslednjoj.
- Vreme i status ispod balona, `meta` veličina, `muted`. Statusi: „Šalje se", „Poslato", „Dostavljeno", „Pročitano". Greška je u `danger` sa ikonom, ne samo bojom.
- Header: dugme nazad, avatar 32, ime, ispod „na mreži" ili „pre X min".
- Polje za unos: pilula na `surface`, dugme za slanje je krug `accent`. Prazan unos se ne šalje (dugme je neaktivno, bez poruke greške).

## 3. Reakcije
- **Dugi pritisak** na balon otvara birač (ne običan klik). Birač je pilula iznad balona, 6 reakcija, svaka meta ≥ 48.
- Izabrana reakcija se prikazuje kao pilula na uglu balona (`reactionPill`, ivica `reactionPillBorder` 1.5dp, preklapa balon ~6dp). Više reakcija: ikone jedna do druge + ukupan broj.
- Ponovni izbor iste reakcije je skida. Reakcija nikad nije samo boja: uvek ikona ili znak.
- Skup reakcija: ono što SimpleX već podržava. Ne menjati protokol.
- Pristupačnost: svaka reakcija ima opis („srce, 2"), birač se zatvara dugmetom nazad.

## 4. Pozivi
Lista: ime, tip (dolazni, odlazni, propušten) i vreme. Propušten je `danger` plus tekst „propušten". Klik poziva ponovo. Prazno stanje: „Još nema poziva".

## 5. Uređaji (Telegram ideja)
- Kartica po uređaju: ikona (telefon, laptop, terminal), naziv, podnaslov („Beograd, pre 2 min"). Ovaj telefon je označen „aktivan sada" ivicom `accent`.
- Ostali uređaji imaju „Odjavi"; traži potvrdu („Odjaviti Laptop?"). Najviše 5 uređaja.
- Dugme na dnu „Poveži uređaj" (isprekidana ivica `accent`) pokreće postojeći tok povezivanja desktopa. Ne izmišljaj novi.
- Ne prikazuj ništa što aplikacija ne zna o uređaju; podnaslov dolazi iz stvarnih podataka.

## 6. Desktop (tri kolone, kasnije)
Lista razgovora (320) | razgovor (fleksibilno) | podaci o kontaktu (320, može da se sakrije). Aktivan razgovor ima levu liniju 3dp `accent` (u gold temi). Ispod 900dp ekrana prelazi u mobilni raspored.

## Tema „Nord tmux"
Isti ekrani i isto ponašanje, druga koža. Font monospace 13, radijusi 0, nema avatara.
```
nika@phone:~$ chats
01 milica     21:14  [3]
   Jesi li poslala fajl?
02 porodica   18:45  [12]
...
[0] 0:chat*  1:call  2:dev      <- statusna traka (zelena)
```
- Naslov ekrana je „prompt" linija u `accent`. Redovi su numerisani; broj nepročitanih je invertovan (`onBg` pozadina, `bg` tekst).
- Razgovor: `[21:14] <milica> tekst`. Svoje poruke `<nika>` u `ownNick` boji, status `✓✓` na kraju. Nema balona.
- Reakcija je linija ispod poruke: `   └ <3 +1`. Birač je linija `react: [1]<3 [2]+1 ...` (svaka stavka meta ≥ 48 visine, širi se po potrebi).
- Tab traka je statusna linija (`statusBar` pozadina, `onStatusBar` tekst); aktivna kartica ima `*`.
- Pri velikom fontu redovi se prelamaju u više linija, nikad ne seku ime. Nemoj čuvati ugrađene razmake padEnd kao rešenje na dugme ili listu; koristi kolone sa težinom.
- Ovo je opciona tema; podrazumevana ostaje „Ponoć i zlato".

## Ekrani koji već postoje (samo nova koža, ponašanje se ne menja)
Shapat ima pravila koja dizajn ne sme da pokvari (izvor: `claude/status.md`):
- **PIN pri ulasku**: obavezan, ne može da se isključi; pogrešan PIN daje pauzu koja raste (30 s, 1, 5, 15 min). Ekran dobija temu, tastaturu i poruke ostavi kakve jesu.
- **Povezivanje po imenu ili kodu**: nema QR kodova ni pozivnih linkova za dodavanje kontakta. Početni ekran i „Dodaj kontakt" pokazuju sopstveno ime sa „Kopiraj" i „Podeli". Ne vraćaj QR u ovim tokovima. QR ostaje samo za prelazak na drugi uređaj, bezbednosni kod i adresu servera.
- Ime profila je jedinstveno ime u imeniku; prikazuje se kao `@ime`.
- Zato je u kartici „Uređaji" dugme „Poveži uređaj" ono što aplikacija već ima za desktop; QR tu sme da bude samo ako taj tok već koristi QR.

## Stanja koja moraju postojati
Učitavanje (skelet redova), prazno, greška mreže (traka na vrhu, „Nema veze. Pokušavamo ponovo."), poruka se šalje/poslata/neuspela, tastatura otvorena, rotacija ekrana.
