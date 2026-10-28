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
| A „Cím” oszlop mindenhol üres | „A kutatás címe” mezőt az űrlapon címmezővé (description) állítani |
| Az állapotsáv „Kérelem → Jóváhagyás → Jóváhagyva” | EPKEB-lépések: Kitöltés → Benyújtva → Bírálat → Döntés → Engedély |
| A bíráló és az elnök is a kérelmezőnek szóló bevezetőt látja („Töltse ki és nyújtsa be…”), benne a REMS-es „hozzáférés megszerzése” | szerepenként más bevezető, a „hozzáférés” szóhasználat kigyomlálása (a feltételeknél is: „az eljárások eléréséhez”) |
| Az elnöknél kilenc gomb, köztük a nem használt „Döntés kérése” | a nem használt műveletek tiltása a munkafolyamatban; a fő művelet kiemelése |
| A katalógus a nem kapcsolattartóknak üres („Nincsenek sorok”) | a menüpont elrejtése, akinek nem kell |
| „Üdvözlettel: REMS” a levelekben, „Analyst Platform – EPKEB” a láblécben | EPKEB-aláírás és -lábléc |
| Kézi mentés a hosszú űrlapon | automatikus mentés (`:enable-autosave`) |

### 2. Szerepenkénti teendőlista

Belépés után mindenki a saját teendőit lássa:

- kapcsolattartó: a kérelmei állapottal és a következő lépéssel, „Új kérelem” gomb (a katalógus mint külön lépés elmarad);
- kutató: kitöltendő kérelmek, megválaszolandó kérdések;
- bíráló: „Bírálataim” határidővel és a hátralévő napokkal; a kérelem oldalán határidősáv;
- elnök: bírálóra váró kérelmek, késő bírálatok, döntésre váró kérelmek, a bírálói naptár.

### 3. A kérelemoldal

- „Bírálatok” kártya az elnöknek: ki bírál, állapot, határidő, javaslat; itt a figyelmeztetés, ha a bírálói vélemény nem egységes.
- „Kérdések és válaszok” szál az eseménynapló helyett.
- Az űrlap szakaszokra bontása a kitöltöttség jelzésével (az új kéreleműrlappal együtt).
