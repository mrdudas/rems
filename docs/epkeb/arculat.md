# Arculat és felhasználói élmény

## EPKEB-téma

A REMS megjelenése témával szabható testre: színek és logók (`theme.edn`), valamint egy kiegészítő stíluslap. Az EPKEB-téma a repó `epkeb-theme/` mappájában van, és a Docker-képbe is bekerül (`/rems/epkeb-theme`).

| Fájl | Mi van benne |
|---|---|
| `epkeb-theme/theme.edn` | színek, gombok, táblázatok, állapotsáv, logók |
| `epkeb-theme/public/css/epkeb.css` | ami változóval nem állítható: világos kártyák kék szegéllyel, lekerekített gombok és mezők, menüsor, lábléc |
| `epkeb-theme/public/img/epkeb-logo.svg`, `epkeb-logo-en.svg` | a teljes logó felirattal (magyar, angol) |
| `epkeb-theme/public/img/epkeb-mark.svg` | a menüsor kis logója |

Bekapcsolás a `config.edn`-ben:

```clojure
:theme-path "/rems/epkeb-theme/theme.edn"
:extra-stylesheets {:root "/rems/epkeb-theme/public" :files ["/css/epkeb.css"]}
```

A próbarendszeren (2026-10-28) a téma a `rems-docker-compose.yml`-ben külön is be van csatolva (`./epkeb-theme:/rems/epkeb-theme:ro`), hogy képújraépítés nélkül lehessen finomítani. A mappát ilyenkor nem szabad lecserélni (törlés és újramásolás), csak a tartalmát felülírni, különben a konténer a régi, törölt mappát látja, és újra kell indítani. A `theme.edn` változása újraindítást kíván, a CSS-é és a képeké nem (a böngésző gyorsítótárát frissíteni kell).

### Színek

A színek az EPKEB-jelből (tengerészkék, kék) és az epkeb.ttk.hu oldalról (meggypiros) származnak.

| Szerep | Szín |
|---|---|
| Sötét alapszín (címek, lábléc) | `#16233A` |
| Kiemelés (gombok, linkek, aktív lépés) | `#1F6FD1`, sötétebb árnyalat `#14509A` |
| Világos kék háttér | `#EAF2FC`, `#EEF3FA` |
| Figyelmeztetés, hiba | `#CD214F` |
| Oldalháttér, vonalak | `#F4F6FA`, `#E3E8EF` |

Betűtípus: a rendszer saját betűkészlete (macOS, Windows, Android), külső betűtípus-szolgáltatás nélkül.

### Logó

- A régi EPKEB-jel (négyzetekből álló spirál, középen kék iránytűnyíl) csak alacsony felbontású képként van meg (a sablonokban). Ennek **vektoros, letisztított újrarajzolása** került a témába, a jel motívumát és színeit megtartva.
- A TTK-logó nem használható.
- Nyitott kérdés az elnökség felé: elfogadható-e az újrarajzolt jel, vagy van eredeti, vektoros változat.

## A felhasználói élmény javítása

Az áttekintés (2026-10-28) a kapcsolattartó, az elnök és a bíráló nézetén készült. Javasolt csomagok fontossági sorrendben:

### 1. Gyors javítások

| Mit láttunk | Javítás |
|---|---|
| Fekete dobozfejlécek, egyforma fekete gombok, üres sáv az oldal tetején | **kész:** EPKEB-téma |
| A „Cím” oszlop mindenhol üres | **kész** (próbarendszer): új űrlap (#5) címmezővel, a két eljárás erre állítva; a régi kérelmeknél az elnök figyelmeztetést lát, hogy a régi eljárás kivezetett |
| Az állapotsáv „Kérelem → Jóváhagyás → Jóváhagyva” | **kész**: Kitöltés → Bírálat és döntés → Engedély |
| A bíráló és az elnök is a kérelmezőnek szóló bevezetőt látja („Töltse ki és nyújtsa be…”), benne a REMS-es „hozzáférés megszerzése” | **kész**: külön bevezető az elnököknek és bírálóknak; „felhasználási feltételek” helyett „nyilatkozatok”, a „hozzáférés” helyett „engedély” |
| Az elnöknél kilenc gomb, köztük a nem használt „Döntés kérése” | **kész**: a „Döntés kérése” tiltva (`:disable-commands`); szerepenként a fő művelet kék gomb (kutató: beadható jelzés, felügyelő: jóváhagyás, bíráló: bírálat, elnök: bírálat kérése, jóváhagyás vagy elutasítás) |
| A katalógus a nem kapcsolattartóknak üres („Nincsenek sorok”) | **kész**: csak kapcsolattartónak és adminnak látszik; a többiek belépés után a kérelmeikre érkeznek |
| „Üdvözlettel: REMS” a levelekben, „Analyst Platform – EPKEB” a láblécben | **kész** |
| Kézi mentés a hosszú űrlapon | **kész** (`:enable-autosave true`) |

### 2. Szerepenkénti teendőlista

**Kész (2026-10-28):** „Teendőim” oldal (`/my-tasks`, API: `GET /api/applications/my-tasks`). Belépés után mindenki ide érkezik (`:enable-organization-contacts` mellett). A teendők szerepenként csoportosítva, sürgősség szerint színezve (piros: késés, kék: most a felhasználón múlik, szürke: másra vár):

| Szerep | Teendők |
|---|---|
| Kapcsolattartó | benyújtható; a felügyelőre vár; kitöltés alatt; kiegészítésre visszaküldve; kérdés érkezett. „Új kérelem” gomb. |
| Kutató | kitöltendő (még nincs beadhatónak jelölve); kiegészítésre visszaküldve; kérdés érkezett |
| Felügyelő | jóváhagyásra vár (kiemelve, ha már beadhatónak jelölték) |
| Bíráló | bírálat a határidővel és a hátralévő napokkal, illetve a késéssel |
| Elnök | bírálat késik; bírálókra vár (kevesebb mint két bíráló felkérve); bírálat folyamatban; döntésre vár (minden bírálat beérkezett, vagy rövidített eljárás). Link a bírálói naptárra. |

„Kérdés érkezett”: a bírálók vagy az elnökök legutóbbi nyilvános megjegyzésére még nem válaszolt senki a kérelmező oldalról. A kérelem oldalán a bíráló a bírálata határidejét is látja (piros, ha késik).

A teendők minden elnöknél megjelennek, nem csak az illetékesnél.

Belépés után mindenki a saját teendőit lássa:

- kapcsolattartó: a kérelmei állapottal és a következő lépéssel, „Új kérelem” gomb (a katalógus mint külön lépés elmarad);
- kutató: kitöltendő kérelmek, megválaszolandó kérdések;
- bíráló: „Bírálataim” határidővel és a hátralévő napokkal; a kérelem oldalán határidősáv;
- elnök: bírálóra váró kérelmek, késő bírálatok, döntésre váró kérelmek, a bírálói naptár.

### 3. A kérelemoldal

- „Bírálatok” kártya az elnöknek: ki bírál, állapot, határidő, javaslat; itt a figyelmeztetés, ha a bírálói vélemény nem egységes.
- „Kérdések és válaszok” szál az eseménynapló helyett.
- Az űrlap szakaszokra bontása a kitöltöttség jelzésével (az új kéreleműrlappal együtt).
