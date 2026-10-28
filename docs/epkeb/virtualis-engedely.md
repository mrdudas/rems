# Virtuális engedély

Döntés (2026-10-27): az engedély elsősorban **nem PDF, hanem egy link**. A linken bárki – külső szereplő is (folyóirat, finanszírozó, együttműködő intézmény) – ellenőrizheti, hogy az engedély létezik és érvényes, és onnan PDF-ben le is töltheti.

Állapot (2026-10-28): **elkészült**, a `:enable-permits` kapcsolóval.

| Rész | Megvalósítás |
|---|---|
| Tábla | `permit` (migráció `20261028170000-epkeb-permits`): token, év, sorszám, kérelem, kiállítás, visszavonás, adatok (JSON pillanatkép); egyedi (év, sorszám) |
| Számkiosztás | `rems.db.permits/issue-permit!`: az év legnagyobb sorszáma + 1, a parancsok közös zárolása alatt (nincs ütközés); `:permit-first-number {:year 2026 :sequence 229}` a kézi számozás folytatásához |
| Kiállítás | `rems.service.permits/handle-events`: a jóváhagyás eseményére, a levelek előtt; az adatok a kérelemből: kutató(k) (a felügyelőn kívüli kutatók, ha nincsenek, a kapcsolattartó), felügyelő, cím, a kapcsolattartó szervezete és a tanszék mező, nyelv, rövidített eljárásnál az eredeti engedély száma, a jóváhagyó elnök |
| Visszavonás | a `revoke` eseményre; az oldal és a PDF „VISSZAVONVA” állapotot mutat a dátummal |
| Ellenőrző oldal | `/permit/<token>` (`rems.permit-page`), bejelentkezés nélkül, `noindex`, magyar/angol váltással, „PDF letöltése” gombbal |
| PDF | `/permit/<token>/pdf`, menet közben generálva, QR-kóddal és a linkkel; a betűtípus `:permit-pdf-font` (a témában: DejaVu Sans, a magyar ékezetekhez) |
| A kérelem oldalán | „Engedély” kártya: szám, állapot, a megosztható link, PDF (`GET /api/applications/:id/permit`) |
| Levél | a jóváhagyó levélben az engedély száma és linkje |

A próbarendszeren a Keycloak-belépés minden útvonal előtt ott van; a `/permit/` útvonalat a Traefikben külön szabály engedi bejelentkezés nélkül (`rems-docker-compose.yml`, `rems-permit` router).

## Életciklus

1. **Kiállítás.**
   - Normál eljárás: az illetékes elnök jóváhagyja a kérelmet (`approve`), miután mindkét bírálat pozitív.
   - Rövidített eljárás: a társelnök egy személyben jóváhagyja.

   A jóváhagyással a rendszer létrehoz egy engedélyrekordot és kiosztja az engedélyszámot.
2. **Közzététel.** A kapcsolattartó a kérelme oldalán és az értesítő e-mailben megkapja az ellenőrző linket.
3. **Ellenőrzés.** A link bejelentkezés nélkül megnyitható; az oldal mutatja az engedély adatait és állapotát.
4. **Visszavonás.** A REMS `revoke` parancsa az engedélyt „visszavont” állapotba teszi; a link ezután is működik, de VISSZAVONVA állapotot mutat, dátummal.

## Engedélyszám

- Formátum: `ÉÉÉÉ-NNN` (pl. `2026-077`), évente 001-től újrainduló sorszám, ahogy a mostani engedélyeken.
- A kiosztás adatbázis-tranzakcióban történik (évenkénti számláló sor zárolásával, vagy egyedi indexszel és újrapróbálással), így két egyidejű jóváhagyás sem kaphat azonos számot ([q][r]).
- Beállítható kezdő sorszám, hogy a rendszer a kézi számozás folytatásaként induljon (pl. az utolsó kiadott szám + 1).
- Rövidített eljárásnál az engedély tárolja a hivatkozott korábbi engedélyszámo(ka)t is, és ezek zárójelben megjelennek a referenciaszám mellett.

## Adatok

Engedélyrekord (új táblában, a kérvényhez kötve):

| Mező | Forrás |
|---|---|
| azonosító token | véletlen, nem kitalálható (pl. 128 bites, URL-biztos) |
| engedélyszám | számkiosztás |
| kérvény azonosító | REMS application id |
| típus | normál / rövidített |
| nyelv | a kérelem nyelve (hu/en) |
| intézmény/tanszék | űrlap 1–2. pont |
| kutató, felügyelő, kutatás címe | űrlap 3., 6., 11. pont |
| hivatkozott engedély(ek) | rövidített eljárásnál |
| engedélyezés dátuma | a jóváhagyás ideje |
| kiállító társelnök | a jóváhagyó felhasználó |
| állapot | érvényes / visszavont, visszavonás ideje és oka |

Az adatok a kiállításkor rögzülnek (pillanatkép), hogy az engedély tartalma a kérvény későbbi változásától független legyen.

## Nyilvános ellenőrző oldal

- URL: `<public-url>/permit/<token>`; nem kell hozzá bejelentkezés.
- Tartalma: a sablon szövege kitöltve ([sablonok.md](sablonok.md#engedély)), nagy állapotjelzés (**ÉRVÉNYES** / **VISSZAVONVA**), nyelvváltás hu/en, „PDF letöltése” gomb.
- Csak az engedélyen amúgy is szereplő adatokat mutatja; a kérvény többi része és a bírálatok nem láthatók.
- Az oldal `noindex`, hogy keresők ne gyűjtsék.

### Miért token, és nem engedélyszám a linkben

Az engedélyszámok sorszámok; ha a link ezekből állna, végigpróbálgatással bárki listázhatná a kutatók nevét és a kutatások címét. A nem kitalálható token miatt csak az ellenőrizheti az engedélyt, aki a linket vagy a QR-kódot megkapta.

## PDF

- Kérésre, menet közben generálva (a projektben már szereplő `clj-pdf` könyvtárral); nem tárolt fájl.
- A sablon szerinti tartalom, magyar vagy angol nyelven; a dátum a nyelvnek megfelelően formázva.
- QR-kód és rövid link az ellenőrző oldalra, „Az engedély érvényessége ellenőrizhető: …” szöveggel.
- Visszavont engedélynél a PDF is jelzi a visszavonást.

## Hitelesség

A bizottsági kommentek ([s][t]) a kézi aláírást támogatták, mert az automatikus aláírás feltörés esetén hamis engedélyekhez vezethet. A virtuális engedélynél nincs aláírt dokumentum; a hitelességet ez adja:

- engedélyt csak bejelentkezett, handler szerepű elnök állíthat ki; nem egységes bírálatok mellett is kiadhatja (felülbírálás), de a rendszer erre figyelmeztet, és a döntés naplózva van;
- minden kiállítás és visszavonás naplózott REMS-esemény (ki, mikor);
- az érvényesség mindig a hivatalos oldalon ellenőrizhető, nem a PDF-en – egy hamisított PDF linkje nem létező vagy más engedélyre mutat.

## Titoktartás

Az [eljárásrend](eljarasrend.md#titoktartás) szerint a kérelem tényéről és tartalmáról a bizottság senkinek nem adhat információt a kérelmezőn kívül, **jóváhagyás esetén sem**. A nyilvánosan ellenőrizhető engedély ezzel csak így fér össze:

- a linket a rendszer csak a kérelmezőnek (és a kapcsolattartónak) adja ki; azt ő osztja meg, ha akarja. Ez a kérelmező döntése, nem a bizottság közlése;
- engedélyszám vagy név alapján nem lehet keresni; az oldal nem indexelhető;
- az oldal csak az engedélyen amúgy is szereplő adatokat mutatja;
- elutasított kérelemről nincs link és nincs nyilvános adat.

**Ezt a bizottságnak jóvá kell hagynia**, és érdemes az eljárásrendbe is beírni (pl. „a jóváhagyott kérelemről kiadott engedély érvényessége a kérelmező által megosztott linken ellenőrizhető”).

## Domain

Döntés (2026-10-27): fejlesztés és tesztelés alatt a rendszer a **rems.analyst.hu** alatt fut; élesítéskor átkerül az **engedely.epkeb.hu** domainre, mert a külső fél az EPKEB saját domainjét fogadja el hitelesnek.

Következmény: a link nem tartalmazhat beégetett domaint. Az ellenőrző URL a `:public-url` beállításból áll össze, és a PDF-be, QR-kódba, e-mailbe is ebből kerül. A domainváltás előtt kiadott engedélyek linkjei a régi domainen maradnak, ezért élesítéskor a rems.analyst.hu `/permit/...` útvonalait át kell irányítani (HTTP 301) az engedely.epkeb.hu-ra. A legegyszerűbb, ha valódi engedély csak a domainváltás után készül; a rems.analyst.hu-n csak bemutató engedélyek készülnek.

## Nyitott kérdések

- A nyilvános ellenőrzés jóváhagyása a titoktartási szabály miatt (lásd fent).
- Mi a következő kiadandó engedélyszám? A 2026-os táblázat utolsó száma 2026-228; élesítéskor az akkori utolsó szám + 1.
- Kerüljön-e a PDF-re a társelnök aláírásának képe, vagy elég a név?
- Mindkét társelnök neve szerepeljen (ahogy a mostani sablonon), vagy csak a kiállítóé?
- A régi, papíralapú engedélyeket be kell-e vinni a rendszerbe (ellenőrizhetőség miatt)?
