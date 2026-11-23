# Obavezan PIN i obavezno ime — dizajn (23.11.2026)

## Namera
U Shapat ne može da se uđe bez PIN-a, a svaki profil odmah bira svoje jedinstveno ime. Odlučio Nikola 23.11.2026.

## Ciljevi
- Nova instalacija: postavljanje PIN-a je prvi ekran, bez preskakanja.
- Postojeća instalacija bez PIN-a: isti ekran pri sledećem otvaranju.
- Zaključavanje ne može da se isključi u podešavanjima.
- Pogrešan PIN ne briše aplikaciju, osim ako korisnik to sam uključi (varijanta A).
- Ime profila je jedinstveno ime u imeniku i ujedno prikazno ime. Ako imenik nije dostupan, korisnik ulazi, a ime se traži pri sledećem otvaranju (meka blokada).

## Šta nije cilj
- Desktop aplikacija (nije proizvod; oba pravila važe samo na Androidu).
- iOS (nije početa).
- Izmene Haskell jezgra ili servisa imenika.
- PIN ne šifruje bazu: on je brava na ulazu u aplikaciju, baza ostaje šifrovana ključem iz Android Keystore-a.

## Pristupi
1. **Kapija u `MainScreen` (izabrano).** Jedno mesto odlučuje: bez PIN-a se prikazuje samo ekran za postavljanje. Svako stanje bez PIN-a (nova instalacija, stara instalacija, brisanje PIN-om za uništavanje) vodi na isti ekran, pa nema putanje koja ga zaobilazi.
2. Novi korak u `OnboardingStage`. Odbačeno: ne pokriva postojeće instalacije i stanje posle brisanja, a enum deli sa upstream kodom.
3. Proširiti postojeći opcioni dijalog („uključi zaključavanje“). Odbačeno: dijalog može da se odbije.

Za ime: provera dostupnosti pre pravljenja profila tražila bi novu operaciju u imeniku i adresu profila koja tada još ne postoji. Zato se ime preuzima odmah posle pravljenja profila, u pozadini; ekran se pokazuje samo ako je ime zauzeto ili ga profil nema.

## Ponašanje
- `AppLock.pinRequired`: Android i `performLA == false`.
- `SetAppPasscodeView(mandatory = true)`: bez „Otkaži“ na prvom unosu; „nazad“ sa potvrde vraća na prvi unos. PIN aplikacije ne može biti jednak PIN-u za uništavanje.
- Pogrešan PIN (`SapatWipe.afterWrongPin`): prva dva bez posledica, zatim pauza 30 s, 1 min, 5 min, pa 15 min za svaki sledeći. Preostale sekunde se čuvaju i odbrojavaju samo dok aplikacija radi, pa pomeranje sata i restart telefona ne skraćuju čekanje. PIN za uništavanje radi i tokom pauze.
- Opcija „Obriši posle 3 pogrešna PIN-a“ (podrazumevano isključena) vraća staro ponašanje, sa upozorenjem pre trećeg.
- Razlog neuspeha (pauza, poslednji pokušaj) sada se prikazuje korisniku; ranije je poruka bila opšta.
- Ime (`SapatName`): novi profil se pravi sa imenom po pravilima imenika (3–32 znaka, a–z, cifre, crtica, počinje slovom). Kad razgovor proradi, ime se preuzima u imeniku. Zauzeto ime ili profil bez izabranog imena → ekran „Izaberi svoje ime“ bez izlaza. Ako imenik nije dostupan, pojavljuje se „Nastavi bez imena“, a pitanje se ponavlja pri sledećem pokretanju.
- Privremeno ime koje imenik sam dodeli (reč i 5 cifara) ne računa se kao izabrano.

## Odluke koje sam doneo sam
- Skriveni profili i profili preko povezanog desktopa se ne pitaju za ime.
- Kod profila napravljenog pre ove izmene, izabrano ime zamenjuje prikazno ime (kontakti vide promenu).
- Raspored pauza i gornja granica od 15 minuta.
