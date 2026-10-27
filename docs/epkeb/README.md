# EPKEB testreszabás

Ez a mappa a REMS testreszabását dokumentálja az **Egyesített Pszichológiai Kutatásetikai Bizottság (EPKEB)** etikai engedélyezési folyamatához.

A cél: a kérelmek beküldése, a bírálók kijelölése, a bírálat, a módosítási körök és az engedély kiadása egy webes rendszerben történjen, a mostani e-mail + titkár + közös Excel-táblázat alapú folyamat helyett.

## Rendszerek

| Rendszer | Szerep | Szabály |
|---|---|---|
| **Google Drive** (sablonok, kérelmek, közös Excel-nyilvántartás) | **Éles működés** – a bizottság ma ezzel dolgozik | **Tilos módosítani.** Csak olvasni szabad, adatot onnan csak összesítve, személyes adatok nélkül veszünk át. |
| **rems.analyst.hu** | **Próbarendszer** – a REMS-alapú rendszer bemutatása az elnökségnek | Szabadon módosítható; csak teszt- és bemutatóadatok, valódi kérelem és valódi engedély nem kerül bele. |
| **engedely.epkeb.hu** | A jövőbeli éles rendszer | Az elnökség jóváhagyása után. |

## Tartalom

| Dokumentum | Miről szól |
|---|---|
| [cel-folyamat.md](cel-folyamat.md) | **A tervezett folyamat**: szerepek, lépések, állapotok, és mi fejlesztendő hozzá |
| [urlap-terv.md](urlap-terv.md) | A rems.analyst.hu jelenlegi beállításai, és az új kéreleműrlap terve duplikációk nélkül |
| [jogosultsagok.md](jogosultsagok.md) | Mit lehet a REMS tiltásaival és kapcsolóival lefedni, és mihez kell fejlesztés |
| [szervezetek.md](szervezetek.md) | Mit jelent a szervezet a REMS-ben, és hogyan kezeljük a tagintézményeket |
| [eljarasrend.md](eljarasrend.md) | Az EPKEB eljárási rendjének a rendszert érintő szabályai, és a 2026-os nyilvántartás (táblázat) elemzése |
| [kovetelmenyek.md](kovetelmenyek.md) | Az „EPKEBADMIN felhasználói követelményjegyzék” összefoglalása, a kommentekben felmerült álláspontokkal és döntésekkel |
| [jelenlegi-folyamat.md](jelenlegi-folyamat.md) | A mai, kézi folyamat a „Feladatok, lépések” leírás és a levélsablonok alapján: szerepek, határidők, levéltípusok |
| [sablonok.md](sablonok.md) | A használt sablonok szerkezete: kérelem űrlap, bírálati lap, engedély (normál/rövidített, magyar/angol), tagintézmények |
| [rems-lekepezes.md](rems-lekepezes.md) | Mi oldható meg a gyári REMS-szel konfigurációból, és mihez kell fejlesztés |
| [virtualis-engedely.md](virtualis-engedely.md) | A virtuális (linkkel ellenőrizhető) engedély terve |
| [telepites.md](telepites.md) | A rems.analyst.hu próbarendszer állapota, a talált hibák és a teendők |
| [utemterv.md](utemterv.md) | A fejlesztés javasolt sorrendje és a nyitott kérdések |

## Források

- EPKEBADMIN felhasználói követelményjegyzék (Google Docs, kommentekkel), 2024-05-19
- „sablonok/anyagok” Google Drive-mappa: kérelem, bírálati lap, engedélysablonok, levélsablonok, feladatleírás, tagintézmények listája
- A `/opt/analyst.hu/rems` alatti futó telepítés

A forrásdokumentumok belső anyagok. Ez a dokumentáció személyneveket és kitöltött kérelemadatokat szándékosan nem tartalmaz; ahol a sablon nevet ír, ott a szerepkör szerepel (pl. „társelnök”).

## Állapot (2026-10-27)

- Követelmények és sablonok feldolgozva.
- A fejlesztés az `epkeb` ágon folyik, amely a **v2.41** upstream kiadásról indul (lásd [telepites.md](telepites.md#upstream-verzió)). A `master` a fork gyári állapota marad.
- Az `epkeb` ágon már benne van: a magyar fordítás (`resources/translations/hu.edn`) és a szervezet-űrlap javítása.
- Döntés: az engedély **virtuális** legyen – egy link, amelyen külső fél is ellenőrizheti, hogy az engedély létezik és érvényes, és onnan PDF-ben letölthető.
