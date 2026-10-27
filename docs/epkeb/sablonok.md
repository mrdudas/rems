# Sablonok

A „sablonok/anyagok” Drive-mappa tartalma és az egyes sablonok szerkezete.

## A mappa tartalma

| Fájl | Mi ez |
|---|---|
| `EPKEB_kerelem_v4.doc`, `EPKEB_application_v2.doc` | Kérelemsablon magyarul és angolul |
| `BÍRÁLATI LAP_sablon_2025.docx` | Bírálati lap |
| `EPKEB_engedély_2026.docx` (2 változat), `EPKEB_approval_2026_eng.docx` | Engedélysablon magyarul és angolul |
| `EPKEB_engedély_2024_RÖVIDÍTETTHEZ.docx`, `EPKEB_approval_2024_eng_EXPEDITED.docx` | Rövidített eljárás engedélysablonja |
| `EPKEB_engedély_2026_0NN.docx` | Kiadott engedélyek (mintának) |
| `EPKEB_feladatok_lepesek_2024.docx` | A kézi folyamat leírása → [jelenlegi-folyamat.md](jelenlegi-folyamat.md) |
| `EPKEB levél sablonok_TITKÁROKNAK_angol+magyar bővített.docx` | Levélsablonok → [jelenlegi-folyamat.md](jelenlegi-folyamat.md#levéltípusok) |
| `Résztvevő intézmények listája.docx` | Tagintézmények és képviselőik |
| `levelezolista.docx` | Levelezőlista |
| `Felosztás_2025.xlsx` | Kiosztás 2025 (még nem feldolgozva) |
| `EPKEB_alapito.pdf`, `EPKEB_eljaras.pdf` | Alapító okirat, eljárásrend (még nem feldolgozva) |
| `EPKEB elnökség 2026/`, `Javaslat (Kérelem szövegezés - 2026.07.28)/` | Almappák (még nem feldolgozva) |

## Kérelem űrlap

A REMS-űrlap mezői a `EPKEB_kerelem_v4` alapján. A magyar és az angol sablon azonos szerkezetű.

| # | Mező | REMS mezőtípus (javaslat) |
|---|---|---|
| – | Rövidített etikai eljárást kezdeményez? | `option` (igen/nem) – a rövidített eljárás külön katalóguselem is lehet |
| – | A hivatkozott korábbi kérelem sorszáma / engedélyszáma (több is lehet) | `text`, csak rövidített eljárásnál látható és kötelező |
| 1 | Intézmény (egyetem) | a REMS-szervezetből vagy `option` a tagintézmények listájából |
| 2 | Tanszék | `text` |
| 3 | A kutatás végzőjének neve (kérelmező) | `text` |
| 4 | Beosztása (diák, doktorandusz, oktató, kutató stb.) | `option` + egyéb |
| 5 | Tudományos fokozat | `text` |
| 6 | A kutatás felügyelője (ha a kérelmezőnek nincs fokozata) | `text` |
| 7 | A kérelmező postacíme | `text` |
| 8 | A kérelmező e-mail címe | `email` |
| 9 | A kérelem beadásának dátuma | automatikus (beküldés ideje) |
| 10 | A kutatás kezdetének várható időpontja | `date` |
| 11 | A kutatás címe | `text` – ez lesz a kérelem címe is |
| 12 | A kutatás célja (legfeljebb 30 szó) | `texta` |
| 13 | A vizsgált személyek életkora | `text` |
| 14 | A vizsgált személyek kiválasztásának módja | `texta` |
| 15 | Toborzás módja (+ dokumentáció: hirdetés, levél stb.) | `texta` + `attachment` |
| 16 | Ösztönzés vagy fizetség a részvételért | `texta` |
| 17 | A vizsgálat helyszíne | `text` |
| 18 | A vizsgálat rövid leírása (legfeljebb egy oldal) | `texta` |
| 19 | Berendezések és eszközök (+ dokumentáció) | `texta` + `attachment` |
| 20 | Kérdőívek, tesztek, interjútechnika (+ másolat, ha nem sztenderd) | `texta` + `attachment` |
| 21 | Tájékozott beleegyezés módja életkor szerint (3 év alatt; 3–14; 14–18, passzív beleegyezés lehetősége; felnőtt) + információs dokumentum és beleegyező ív | `option` (korcsoport) + `attachment` |
| 22 | Adatok bizalmas kezelése, azonosíthatatlanság; személyes adatgyűjtésnél GDPR-hivatkozás | `texta` |
| 23 | Jár-e a vizsgálat: (1) kellemetlen ingerekkel/helyzetekkel, (2) fájdalommal, (3) víz/élelem/alvás megvonásával, (4) gyógyszerrel/pszichoaktív szerrel, (5) testi sérülés veszélyével, (6) mentális betegek vagy veszélyeztetettek részvételével, (7) tudatos félrevezetéssel, (8) a kutatás céljának eltitkolásával? | 8 db `option` (igen/nem) |
| 24 | Ha bármelyikre IGEN: hogyan előzi meg a testi és lelki kárt? | `texta`, csak IGEN esetén látható |
| 25 | Hogyan csökkenti az akaratlanul keltett szorongást? | `texta` – a sablonverzió ellenőrzésére is szolgált |
| 26 | További etikai vonatkozások | `texta` |
| – | Kérelmezői nyilatkozat („összhangban van a pszichológiai kutatásetikai normákkal”) | `option` vagy licenc (elfogadandó feltétel) |
| – | Felügyelői nyilatkozat („olvastam, nem vet fel további etikai kérdéseket”) | `option` vagy `attachment` (aláírt nyilatkozat) |

Az aláírások: elektronikus űrlapnál a kérelmezői nyilatkozat elfogadása a bejelentkezett kapcsolattartó nevéhez kötődik. A felügyelő jóváhagyásáról döntés kell (aláírt PDF-melléklet, vagy a felügyelő meghívása tagként a REMS-ben).

## Bírálati lap

A `BÍRÁLATI LAP_sablon_2025` alapján a bírálat szerkezete:

| Mező | Ki látja |
|---|---|
| A kutatás címe, a kérvényező neve | automatikus |
| **Bírálati vélemény** – egyet kell választani: (1) etikai szempontból rendben, változatlan formában elfogadásra javaslom; (2) módosításra, kiegészítésre szorul; (3) nem javaslom elfogadásra, mert megsérti a Pszichológusok Szakmai Etikai Kódexét vagy más etikai szabályt; (4) elbírálására az EPKEB nem felhatalmazott | elnök |
| Indoklás | elnök |
| **A kutatónak szóló észrevételek**, javaslatok, javítandó pontok (a kérelem nyelvén) | elnök + **kérelmező, névtelenül** |
| **Megjegyzések a bizottság elnökének** (a szerző nem kapja meg) | csak elnök |
| Dátum, bíráló | automatikus |

Lábjegyzet a sablonon: a módosított kérelmek beküldési határideje egységesen 30 nap; elmulasztása az eljárás felfüggesztését vonja maga után.

## Engedély

Közös szerkezet (normál, magyar):

> EGYESÍTETT PSZICHOLÓGIAI KUTATÁSETIKAI BIZOTTSÁG
>
> **Etikai engedély**
>
> Az Egyesített Pszichológiai Kutatásetikai Bizottság (EPKEB) az alábbi kutatási tervet etikai szempontból értékelte, és annak végrehajtásához a(z) *{intézmény/tanszék}* megbízásából hozzájárul.
>
> Kutató: *{kutató}*
> A kutatás felügyelője: *{felügyelő}*
> Kutatás címe: *{cím}*
> Referencia szám: *{ÉÉÉÉ-NNN}*
> Engedélyezés dátuma: *{ÉÉÉÉ. hónap NN.}*
>
> EPKEB társelnök – *{társelnök neve}* (a sablonon „vagy” elválasztással mindkét társelnök neve szerepelhet)

Angol változat (*Ethics Approval*): „The United Ethical Review Committee for Research in Psychology (EPKEB) has evaluated the ethical aspects of the following research proposal, and has approved it on behalf of *{institution}*”; mezők: Researcher, Supervisor, Project title, Reference number, Date of approval; aláíró: Co-chair of EPKEB.

Változatok:

| Változat | Különbség |
|---|---|
| Magyar normál | – |
| Angol normál | angol szöveg; angol nyelvű kérelemhez |
| Rövidített (hu/en, *EXPEDITED*) | a referenciaszám mellett zárójelben az eredeti engedély(ek) száma |

Megfigyelések:

- A referenciaszám formátuma `ÉÉÉÉ-NNN`, évente újrainduló háromjegyű sorszám (pl. 2026-076).
- A sablonok fejléce és aláírása kép (a szövegkinyerés „automatically generated description” képleírást ad).
- Az angol minta dátuma hibás formátumú („02 12, 2026”) – a generált engedélynél a dátumot a nyelvnek megfelelően kell formázni.

## Tagintézmények

A *Résztvevő intézmények listája* szerint (zárójelben a csatlakozás éve, ahol ismert):

- Debreceni Egyetem Pszichológiai Intézet
- Debreceni Egyetem Egészségtudományi Kar, Szociális és Társadalomtudományi Intézet, Pszichológiai Tanszék
- Közép-európai Egyetem (CEU) Kognitív Tudományi Tanszék
- Pécsi Tudományegyetem Pszichológia Intézet
- Szegedi Tudományegyetem Pszichológiai Intézet
- Szegedi Tudományegyetem Általános Orvosi Kar, Magatartástudományi Intézet
- (HUN-REN) TTK Kognitív Idegtudományi és Pszichológiai Intézet
- BME TTK Kognitív Tudományi Tanszék (2012)
- Pázmány Péter Katolikus Egyetem Pszichológiai Intézet (2014)
- ELTE TTK Biológiai Intézet, Etológia Tanszék (2015)
- HUN-REN TTK Agyi Képalkotó Központ (2015)
- BME GPK Mechatronika, Optika és Gépészeti Informatika Tanszék (2015)
- BME GTK Ergonómia és Pszichológia Tanszék (2016)
- Nyíregyházi Egyetem Alkalmazott Pedagógia és Pszichológia Intézet (2017)
- Eszterházy Károly Katolikus Egyetem Pszichológia Intézet (2022)
- Pécsi Tudományegyetem Magatartástudományi Intézet (2023)
- Moholy-Nagy Művészeti Egyetem (2023)
- Pannon Egyetem Humántudományi Kar, Pszichológiai és Mentálhigiéné Intézet (2024)

Intézményenként 1–5 képviselő van; ők alkotják a bírálói kört. Minden tagintézményhez egy illetékes társelnök tartozik.
