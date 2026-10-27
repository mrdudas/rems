# Kérelem űrlap: jelenlegi állapot és új terv

## A rems.analyst.hu jelenlegi beállításai

Felmérve 2026-10-27-én, a REMS admin API-n keresztül, csak olvasva.

| Elem | Állapot |
|---|---|
| Szervezet | 1 db: `EPKEB` |
| Űrlapok | #1 „Etikai Kérelem” (archivált), #2 „asdfads” (archivált), **#3 és #4 „Etikai Kérelem űrlap”** (aktív) |
| Workflow-k | #1 „EPKEB” és #2 „első wf” (archiváltak), **#3 „próba munkafolyamat”** (default típus, 2 handler, űrlap: #3) |
| Katalóguselemek | „etikai kutatás” (archivált), **„Epkeb kutatási engedély”** (workflow #3, **űrlap #3**) |
| Erőforrás | „etikai engedény” [sic], 1 licenccel |
| Licencek | „Etikai Nyilatkozat” (valódi szöveg, az erőforráshoz kötve); „Nyilatkozat sérülékeny csoportok érintettségéről”, „Adatkezelési hozzájárulás”, „Etikai jóváhagyás” (helyőrző szövegek); 2 archivált |
| Kérvények | 6 db, teszt (draft, closed, approved, revoked, rejected) |

Az #4-es űrlap a #3 javított változata: kikerült belőle a beadás dátuma, a kutatás kezdete opcionális lett, és a kockázati kérdésekhez bekerült az „Egyikkel sem” válasz. **A katalóguselem viszont még a #3-at használja.**

### A #4-es űrlap mezői

| Mező | Típus | Kötelező | Megjegyzés |
|---|---|---|---|
| Rövidített etikai eljárást kezdeményez? | option I/N | igen | |
| A hivatkozott korábbi kérelem sorszáma | text | igen, ha rövidített | |
| Intézmény (egyetem) | option, 18 intézmény | igen | |
| A kutatás végzőjének neve | text | igen | |
| Beosztása | text | igen | |
| A kutatás felügyelője | text | **igen** | a PDF-en csak fokozat nélküli kérelmezőnél kell |
| A kérelmező postacíme | text | igen | |
| A kérelmező e-mail-címe | email | igen | |
| A kutatás kezdetének várható időpontja | date | nem | |
| A kutatás címe | text | igen | |
| A kutatás célja (legfeljebb 30 szó) | text | igen | |
| A vizsgált személyek életkora | text | igen | |
| Kiválasztás módja | text | igen | |
| Toborzás módja | text | igen | |
| Ösztönzés, fizetség | option igen/nem | igen | részletek nélkül |
| Helyszín | text | igen | |
| Rövid leírás | texta | igen | |
| Berendezések, eszközök | text | igen | |
| Kérdőívek, tesztek | table (név, URL) | igen | |
| Kockázatok (A–H, „Egyikkel sem”) | multiselect | igen | |
| Ha bármelyik igen: kármegelőzés | texta | nem | csak „igen” esetén látszik, de akkor sem kötelező |
| Akaratlanul keltett szorongás csökkentése | texta | igen | |
| További etikai vonatkozások | texta | nem | |
| Tájékoztatott beleegyező nyilatkozat | attachment | igen | |
| Kérdőívek másolata | attachment | nem | |
| Eszközök dokumentációja | attachment | nem | |

**Hiányzik** a PDF-hez képest: a tanszék, a tudományos fokozat (5.), a beleegyezés módja korcsoportonként (21.) és az adatkezelés (22.).

## Az új űrlap terve

Elvek: a kérelmező tölt ki mindent; ami a rendszerből tudható, azt nem kérdezzük; a feltételes mezők csak akkor jelennek meg (és akkor kötelezőek), ha kellenek; egy adat egyszer.

### Ami kikerül, mert a rendszer tudja

| Mező | Honnan jön |
|---|---|
| Rövidített eljárás? | **Külön katalóguselem** („Rövidített eljárás”), saját workflow-val, mert ott nincs bíráló |
| Intézmény | a kapcsolattartó szervezete (a kérelmet ő hozza létre) |
| A kérelmező neve és e-mail-címe | a meghívott kérelmező felhasználói fiókja |
| A beadás dátuma | a benyújtás időpontja |
| Postacím | nem kell: a kapcsolattartás a rendszerben és e-mailben folyik (ha mégis kell, az elnökség jelezze) |

### Mezők

**1. A kérelem**

| Mező | Típus | Szabály |
|---|---|---|
| A kérelem nyelve | option: magyar / angol | kötelező; az eljárás, a bírálat és az engedély ezen a nyelven folyik (eljárásrend) |
| Tanszék / kutatócsoport | text | kötelező; az engedélyen az intézmény mellett szerepel |
| *(csak rövidített eljárásnál)* Az eredeti engedély(ek) száma | text, `ÉÉÉÉ-NNN` formátum, vesszővel több is | kötelező |
| *(csak rövidített eljárásnál)* Miben tér el az eredeti kutatástól? | texta | kötelező; a többi mező az eredeti kérelemből másolható |

**2. Kérelmező és felügyelő**

| Mező | Típus | Szabály |
|---|---|---|
| Beosztás | option: hallgató (BA/MA) / doktorandusz / oktató, kutató / egyéb | kötelező |
| Tudományos fokozat | option: nincs / PhD vagy DLA / DSc / egyéb | kötelező |
| A felügyelő neve és e-mail-címe | text + email | kötelező, ha nincs fokozat; a felügyelőt a rendszer meghívja jóváhagyásra |

**3. A kutatás**

| Mező | Típus | Szabály |
|---|---|---|
| A kutatás címe | text | kötelező; ez lesz a kérvény címe és az engedélyen is ez szerepel |
| A kutatás célja | texta, legfeljebb kb. 250 karakter (30 szó) | kötelező |
| A kutatás várható kezdete | date | opcionális |
| A vizsgálat helyszíne | text | kötelező |
| A vizsgálat rövid leírása | texta, legfeljebb kb. 4000 karakter (egy oldal) | kötelező |

**4. Résztvevők**

| Mező | Típus | Szabály |
|---|---|---|
| Korcsoport(ok) | multiselect: 3 év alatt / 3–14 év / 14–18 év / felnőtt | kötelező; ebből következik, milyen beleegyezési dokumentum kell (6. szakasz) |
| Életkor pontosan | text (pl. „4–8 év”) | kötelező |
| Sérülékeny csoport (mentális betegek, mentálisan veszélyeztetettek) | option igen/nem | kötelező |
| Kiválasztás módja | texta | kötelező |
| Toborzás módja | texta | kötelező |
| Toborzó anyagok (hirdetés, levél) | attachment | opcionális |
| Ösztönzés, fizetség | option igen/nem | kötelező |
| Az ösztönzés leírása | texta | kötelező, ha igen |

**5. Módszerek és kockázatok**

| Mező | Típus | Szabály |
|---|---|---|
| Berendezések és eszközök | texta | kötelező |
| Eszközök dokumentációja | attachment | opcionális (nem kell, ha korábbi eljárásban jóváhagyták) |
| Kérdőívek, tesztek, interjútechnikák | table: név / sztenderd vagy saját / link | kötelező |
| A nem sztenderd eszközök másolata | attachment | kötelező, ha van saját eszköz – a táblázat alapján a REMS-ben nem feltételezhető, ezért szövegben kérjük |
| Jár-e a vizsgálat … (A–H) | multiselect + „Egyikkel sem” | kötelező |
| Kármegelőzés | texta | **kötelező**, ha bármelyik A–H |
| Akaratlanul keltett szorongás csökkentése | texta | kötelező |

**6. Beleegyezés és adatkezelés**

| Mező | Típus | Szabály |
|---|---|---|
| Szülői tájékoztató és beleegyező ív | attachment | kötelező, ha 18 év alatti korcsoport |
| A gyermeknek szóló (szóbeli vagy írásbeli) tájékoztatás | attachment | kötelező, ha 3–14 vagy 14–18 év |
| Passzív szülői beleegyezés | option igen/nem | csak 14–18 évnél; igen esetén indoklás (a gyermek neve nem kerül felvételre) |
| Felnőtt résztvevők tájékoztatója és beleegyező nyilatkozata | attachment | kötelező, ha felnőtt |
| Az adatok bizalmas kezelése, anonimitás | texta | kötelező |
| Gyűjt-e személyes adatot (e-mail-cím, vagy legalább két személyes adat)? | option igen/nem | kötelező; igen esetén a tájékoztatónak hivatkoznia kell az intézményi adatkezelési szabályzatra (GDPR) – ezt a mező súgója mondja el |

**7. Egyéb**

| Mező | Típus | Szabály |
|---|---|---|
| További etikai vonatkozások | texta | opcionális |

**Nyilatkozatok**: az „Etikai Nyilatkozat” licenc marad (a kérelmező fogadja el benyújtás előtt). A felügyelő hozzájárulása a rendszerben történik (lásd [cel-folyamat.md](cel-folyamat.md)). A helyőrző licenceket („sérülékeny csoportok”, „adatkezelés”, „etikai jóváhagyás”) archiválni kell, vagy valódi szöveggel feltölteni.

### Megoldandó kérdések a REMS-ben

- **Feltételes mezők multiselect alapján:** a REMS `only-if` láthatósága option és multiselect mezőre is működik, így a korcsoport alapú mellékletek beállíthatók.
- **Kétnyelvűség:** az űrlap minden címkéje magyar és angol; a kérelem nyelvét a felhasználó választja ki.
- **Rövidített eljárás:** az eredeti kérelem mezőinek átmásolása a REMS `copy-as-new` funkciójára építhető, ha az eredeti kérelem már a rendszerben van.
- **Szöveghossz-korlát:** a REMS text és texta mezőnek van `max-length` beállítása.
