# Valódi ügyek és a mostani nyilvántartás

A bizottság néhány 2026-os ügyének iratai és a közös Excel-nyilvántartás („Kérelmek_terhelés_KÖZÖS_2026”) alapján: mi történt, hogyan követik ma, és mit kezel ebből a rendszer. Az iratok a repó `assets/` mappájában vannak, amely nem kerül a gitbe (`.gitignore`). Ez a leírás szándékosan nem tartalmaz személyneveket, a sorszámokkal hivatkozik.

## Három ügy

### 2026-052: a sima eset

| Mikor | Mi történt |
|---|---|
| 02.05. | Angol nyelvű kérelem (v4.0 sablon), csecsemőkkel végzett vizsgálat, aláírt PDF-ben; a kérelmező doktorandusz, témavezetővel |
| 02.10. | Beérkezett, sorszámot kapott |
| 02.11. | Mindkét bírálat megjött: „elfogadásra javaslom”. Az egyik bíráló Word-, a másik PDF-bírálati lapon; a javaslatot aláhúzással jelölik a négy lehetőség közül |
| 02.12. | Engedély (Word-sablonból, PDF) |

Tanulságok:

- A bírálati lapon a javaslat **aláhúzással** van jelölve; szövegként kinyerve elveszik, és a táblázatba kézzel kell átírni. A rendszerben a javaslat választólista (`:enable-review-recommendations`), így nem tévedhet el.
- Az engedélyen a dátum **éve hibás** (a sablonban maradt a tavalyi év); a sorszám év része is eltér a kérelem évétől. A rendszerben az engedélyt a rendszer állítja elő a jóváhagyás adataiból (tervezett [virtuális engedély](virtualis-engedely.md)).
- Az ügy 7 nap alatt lezárult: ezt a mostani rendszer is tudja, a rendszerben ugyanígy menne, csak a dokumentumok mozgatása nélkül.

### 2026-060: módosító kérelem egy korábbi engedélyhez

| Mikor | Mi történt |
|---|---|
| 2024.10. | Eredeti kérelem (2024-102): MR-vizsgálat gyermekekkel és fiatal felnőttekkel; 15 MB-os PDF, elektronikus aláírással (AVDH), sorszámozott mellékletekkel |
| 2025.03. | 1. módosító kérelem: rövidített eljárás, hivatkozik a 2024-102-re, csak a változó pontokat tölti ki, és új mellékleteket csatol (15–17.) |
| 2026.02. | 2. módosító kérelem: a hivatkozott sorszám **üres**, a változások a 18–22. mellékletben |
| 02.19.–02.27. | Új sorszám (2026-060), rövidített eljárásként; két „bírálat”, engedély |

Tanulságok:

- Egy kutatás **évekig él** és több módosításon megy át; a mellékletek számozása az összes kérelmen át folytatódik (1–22.). A mostani rendszerben ez csak úgy követhető, ha valaki tudja, melyik mappában van az eredeti.
- A módosító kérelemből kimaradhat az eredeti sorszám, és ilyenkor csak emlékezetből lehet összekötni őket.
- A rendszerben: a módosítás a **jóváhagyott kérelem másolata** (`copy-as-new`), így az eredeti válaszok és mellékletek megvannak, és a kérelem oldalán látszik a kapcsolat („Másolva innen: …”). (Egy kérelem újra benyújtott változatainál a REMS a változott válaszokat különbségként is mutatja; a másolatnál ez nincs meg.) A rendszer előtti engedélyekhez a rövidített űrlap kéri az eredeti engedély számát és az eredeti kérelmet mellékletként. A rövidített űrlapon (2026-10-28 óta) csak ezek és az eltérések leírása kötelező, a többi kérdés csak akkor, ha a válasz változott – ahogy a papíros sablon is mondja.

### 2026-069: rövidített eljárásból normál

| Mikor | Mi történt |
|---|---|
| 03.03. | Online kérdőíves kutatás (angol). Az 1. változat **rövidített eljárást kér**, egy 2023-as engedélyre hivatkozva, amely ugyanannak a tanszéknek egy **másik kutatójának más kutatása** (kutyák, nem macskák); a hivatkozott 2023-as kérelem is csatolva |
| 03.04. | Beérkezett, normál eljárásként |
| 03.13. | 2. változat: a hivatkozás kikerült, az „IGEN / NEM” kérdés kitöltetlen maradt (mindkét szó ott áll) |
| 03.13.–03.19. | Két „elfogadva” bírálat (az egyik dátuma hiányzik a táblázatból), engedély |

Tanulságok:

- A kérelmező félreértette, mikor jár rövidített eljárás; a bizottság **normál eljárásra tette át**, és ehhez új változatot kért. A papíros űrlapon az „IGEN / NEM” kérdés kitöltetlen maradhat.
- Az engedélyen a „Supervisor” sorban a kutató saját neve áll, pedig fokozata van – a sablon kitöltési hibája.
- A rendszerben: az eljárásmód a **katalóguselem** (normál vagy rövidített), nem egy kitölthető mező. Ha a bizottság szerint nem jár rövidített eljárás, a kérelmet visszaküldi, a kapcsolattartó pedig a normál eljárásban újat indít belőle (a válaszok másolhatók). A felügyelő csak akkor szerepel, ha valóban van (fokozat nélkül kötelező). Az engedélyt a rendszer állítja elő, így az adatai nem térhetnek el a kérelemtől.

## A mostani nyilvántartás

### Lapok

| Lap | Mire szolgál |
|---|---|
| `2026_KÉRELMEK` | Soronként egy kérelem, három szakaszban: regisztráció (elnök), bírálat (titkár), zárás (elnök); plusz egy „Megjegyzés” oszlop, ahová a szabad szöveges állapot kerül |
| `2026_TERHELÉS` | Intézményenként az illetékes társelnök, a bírálók (státusz, kapcsolattartó-e, e-mail), az intézmény kérelmei és bírálatai, utolsó felkérés, várható bírálat, bírálat/kérelem arány |
| `Intézményi terhelés számítás` | Hány bíráló kellene intézményenként, hogy egy bírálóra kb. 7 kérelem jusson |
| `2025_kérelmek`, `2025_terhelés` | Az előző év, kicsit más oszlopokkal |
| `Munkalap7` | Teendők és döntések listája (engedélysablon, tárhely, kapcsolattartók, arányos terhelés, szoftverfejlesztés) és elérhetőségek |

### Mit mutatnak a számok (2026, 2026-10-28-i állapot)

- 145 kitöltött sor és 34 előre lefoglalt, üres sorszám; 131 normál és 14 rövidített eljárás; 102 lezárva, 24 bírálat alatt, 14 módosítás alatt.
- Beérkezéstől jóváhagyásig: **medián 35 nap**, a leghosszabb 180 nap.
- A bírálatok **35%-a** (162-ből 56) a 20 napos határidőn túl érkezett; a medián 17 nap.
- 39 kérelem ment vissza módosításra; a visszaküldéstől az elfogadásig medián 20 nap, a leghosszabb 129 nap.
- 26 ügyben a két bíráló **eltérő** javaslatot adott, 3-szor volt elutasító javaslat, 1 kérelmet tagadtak meg.
- A „Megjegyzés” oszlopban kézzel vezetik: „figyelmeztetés kiküldve”, „emlékeztető kiküldve”, „harmadik kör megy”, „módosítás kiküldve a kérelmezőknek / a bírálóknak”, „3. bíráló bevonása”, „ezt a sorszámot átugrottam”.
- Adatminőség: hiányzó bírálati dátum, a beérkezés előtti bírálati dátum (–66 nap), kitöltetlen eljárásmód vagy státusz – a kézi vezetés természetes hibái.
- 39 bíráló: 30 „Bírál”, 4 „Bírál\*”, 4 „Nem”, 1 „Átmenetileg nem”; 18 intézmény. Egy bírálóra jellemzően 6–9 bírálat jutott, néhányra 14–34.

### Fontos eltérés a szabályoktól: rövidített eljárás

A követelményjegyzék szerint rövidített eljárásban nincs bíráló, az elnök egy személyben dönt. A táblázat szerint 2026-ban **mind a 14 rövidített ügyben két „bíráló” szerepel, és ez szinte mindig a két társelnök** volt (néhány nap alatt). A gyakorlat tehát: a két társelnök hagyja jóvá.

A rendszerben most a rövidített munkafolyamat tiltja a bírálatkérést, és bármelyik elnök egy lépésben jóváhagyhat. **Nyitott kérdés az elnökség felé:** egy társelnök dönt (a szabály), vagy mindkettő jóváhagyása kell (a gyakorlat)? Ha mindkettőé, a rövidített eljárásban a két társelnök „bírálóként” jelezné az egyetértését, és a „Bírálatok” kártya mutatná, ha még hiányzik az egyik.

### A táblázat oszlopai a rendszerben

A táblázat mindent kézzel követ; a rendszerben ezek az adatok az eseményekből jönnek, és nem kell őket vezetni.

| Táblázat | A rendszerben |
|---|---|
| Státusz (1–5.), szakaszok | az állapot és az állapotsáv (Kitöltés → Bírálat és döntés → Engedély); a finomabb állapotok a processing state címkék (Bírálók kijelölve, Bírálók felkérve, Módosítás alatt, Bizottsági szavazás) |
| Társelnök | a kérelmező tagintézményének illetékes elnöke (a szervezeten) |
| Eljárásmód | a katalóguselem (normál / rövidített) |
| Kérelmező neve, e-mail, intézmény, cím | a felhasználói fiókok, a kapcsolattartó szervezete, a kérelem címmezője |
| Beérkezett, határidő (auto.) | a benyújtás időpontja; a bírálati határidő a felkéréstől számít (20 nap, újrabírálatnál 10) |
| Bíráló #1, #2 | bírálatkérés a bírálói körből; a „Bírálatok” kártya és a bírálói naptár |
| Felkérés kiküldve | a felkérés eseménye és levele |
| Bírálat dátuma, eredménye | a bírálat eseménye a javaslattal |
| Első módosításra visszaküldve, utolsó módosítás elfogadása, módosítások elfogadva | a visszaküldés és az újbóli benyújtás eseményei, tetszőleges számú körrel |
| Visszajelzés a bírálóknak | az újra benyújtott kérelemhez az elnök újra felkéri a bírálókat (az automatikus újrafelkérés tervezett) |
| Jóváhagyás dátuma, engedélyszám (auto.) | a jóváhagyás eseménye; az engedélyszám a virtuális engedéllyel jön |
| Kérelem zárása (megadva / megtagadva) | jóváhagyott vagy elutasított állapot |
| Megjegyzés („figyelmeztetés kiküldve”, „harmadik kör”) | az eseménynapló; a figyelmeztetéseket a rendszer küldi (emlékeztetők, tervezett) |
| Terhelés lap (bírálatok száma, utolsó felkérés, várható bírálat, arány) | a bírálóválasztó statisztikája és a bírálói naptár |
| Intézményi terhelés számítás | a bírálói kör tagintézmény szerint; az arány számítható |
| Sorszám lefoglalása, átugrása | a sorszámot a rendszer osztja ki benyújtáskor; nincs lyuk és nincs ütközés |

A mostani gyakorlat szerint valószínűleg ugyanezt a táblázatot szeretnék látni a rendszerben is. Ezt nem másoljuk le: a teendők, a bírálói naptár és a kérelem oldala mutatja ugyanezt, mindig naprakészen. Ha mégis kell egy áttekintő lista, a REMS kérelemlistája és az exportja (CSV) adja; egy szűrhető „nyilvántartás” nézet később készülhet hozzá.

### Ami a táblázatból még hiányzik a rendszerből

- **Emlékeztetők** a bírálói határidő előtt és késéskor, és figyelmeztetés a kérelmezőnek a 30 napos módosítási határidőnél (a táblázat szerint ezeket ma kézzel küldik).
- **Harmadik bíráló** bevonása (a REMS-ben lehetséges, de a „Bírálatok” kártya és a szabályok két bírálóval számolnak).
- **Lefoglalt sorszámok**: a táblázatban előre kiosztott üres sorok vannak; a rendszerben erre nincs szükség.
