# Jogosultságok és tiltások

Források: `docs/application-permissions.md` (a teljes szerep × állapot × parancs táblázat), `resources/config-defaults.edn` (`:disable-commands`), `CHANGELOG.md`, és a kód (`src/clj/rems/application/model.clj`, `master_workflow.clj`).

## Hogyan működik

1. **A master workflow** meghatározza, melyik szerep melyik állapotban mit tehet. Ez a felső határ. A `default` és a `decider` workflow-típus ennek egy részhalmaza; a különbség, hogy jóváhagyni és elutasítani a handler (default) vagy a decider (decider) tud.
2. **Tiltások** ezt szűkítik, kétféleképpen:
   - **globálisan** a `config.edn`-ben: `:disable-commands [:application.command/...]` – minden workflow-ra, minden szerepre;
   - **workflow-nként** az admin felületen (Workflow → *Disable commands*), szabályokkal: `{:command ... :when/state [...] :when/role [...]}`. Az állapot és a szerep elhagyható; ilyenkor mindenre vonatkozik.
3. **Tiltással csak elvenni lehet**, hozzáadni nem. Ami a master workflow-ban nincs meg egy szerepnek, azt tiltásokkal nem lehet megadni.

További beépített kapcsolók:

| Kapcsoló | Hatás |
|---|---|
| Workflow → *anonymize handling* | A kérelmező és a tagok a handler, a bíráló és a decider neve helyett csak „Ügyintéző” feliratot látnak az eseményekben, a mellékleteknél és az e-mailekben. |
| Űrlapmező → *privát* | A mezőt a bírálók nem látják (a handler igen). |
| Workflow → *voting* (`:enable-voting`) | `:reviewers-vote` vagy `:handlers-vote`; értékei: elfogad / elutasít / üres (kísérleti). |
| `:enable-processing-states` | Szabadon definiált állapotcímkék (pl. „Bírálók felkérve”), nyilvános vagy belső. |
| `:application-deadline-days` | Döntési határidő az első benyújtástól (csak jelzés). |

## Mit fed le ez az EPKEB-folyamatból kód nélkül

| Igény ([cel-folyamat.md](cel-folyamat.md)) | Megoldás tiltással vagy kapcsolóval |
|---|---|
| **A bíráló névtelen a kérelmező felé** (eljárásrend) | *anonymize handling* a workflow-n. Ez a bírálóra is vonatkozik, mert a bíráló „handling” szerep. |
| **A bíráló kérdez a kérelmezőtől** | A bíráló `remark` parancsa nyilvánosra állítva megjelenik a kérelmezőnek, névtelenítve. A válaszhoz a `:enable-applicant-remarks` kapcsoló kell (lásd lent). |
| **Teljes bizottsági szavazás elutasítási javaslatnál** | `request-decision` a bizottsági tagoknak (decider), mindenki `decide`-dal szavaz (elfogad/elutasít, indoklással). A handler a többség szerint dönt. A szavazatszámlálás kézi. |
| **Rövidített eljárás bírálók nélkül** | Külön workflow, ahol tiltva: `request-review`, `invite-reviewer`, `request-decision`, `invite-decider`, `vote`. |
| **Elutasítás után újra beadható, de csak a kapcsolattartó másolhat** | `copy-as-new` tiltva a `member` szerepre (a kérelmező), az `applicant` (kapcsolattartó) megtartja. |
| **A kapcsolattartó ne cserélhesse a katalóguselemet** | `change-resources` globálisan tiltva. |
| **Felesleges admin funkciók elrejtése** | Globálisan tiltva: `assign-external-id`, `add-licenses`; a handlernél `change-applicant`. |
| **A bíráló ne redaktáljon mellékletet** | `redact-attachments` tiltva a `reviewer` és `past-reviewer` szerepre. |
| **Állapotok a táblázat szerint** | Processing states: „Bírálók kijelölve”, „Bírálók felkérve”, „Módosítás alatt”, „Bizottsági szavazás”. |

### Javasolt beállítás

`config.edn`:

```clojure
:disable-commands [:application.command/change-resources
                   :application.command/assign-external-id
                   :application.command/add-licenses]
:enable-processing-states true
```

Normál eljárás workflow (`default` típus), *anonymize handling* bekapcsolva, tiltások:

```clojure
[{:command :application.command/copy-as-new :when/role [:member]}
 {:command :application.command/change-applicant}
 {:command :application.command/redact-attachments :when/role [:reviewer :past-reviewer]}]
```

Rövidített eljárás workflow (`default` típus), *anonymize handling* bekapcsolva, tiltások:

```clojure
[{:command :application.command/copy-as-new :when/role [:member]}
 {:command :application.command/change-applicant}
 {:command :application.command/request-review}
 {:command :application.command/invite-reviewer}
 {:command :application.command/request-decision}
 {:command :application.command/invite-decider}
 {:command :application.command/vote}]
```

## Amit tiltással nem lehet megoldani

Ezekhez a master workflow-t vagy új parancsot kell fejleszteni:

| Igény | Miért nem megy | Teendő |
|---|---|---|
| Bírálati lap három javaslattal | A `review` szabad szöveg; a `vote` csak elfogad/elutasít | Strukturált bírálat |
| Bírálói kör, saját intézmény kizárása, terhelés | A `request-review` bármely felhasználót elfogad | Szűrés és statisztika |
| Kiegészítés után automatikus újrafelkérés ugyanazoknak | Nincs ilyen automatizmus | Folyamatkezelő (process manager) |
| Határidők, emlékeztetők, felfüggesztés 30 nap után | Csak az `:application-deadline-days` jelzés van | Ütemezett feladat |
| A szavazatok automatikus összesítése | A handler kézzel dönt | Opcionális |

## Ki oszthat ki szerepeket

Két adminisztrátori szerep van: a **rendszergazda** (`owner`) és a **szervezettulajdonos** (a szervezet „Tulajdonosok” listáján szereplők).

| Szerep | Ki osztja ki | Hol |
|---|---|---|
| Rendszergazda (`owner`) | csak a szerveren, parancssorból (`grant-role owner <felhasználó>`), utána a REMS újraindítása kell | szerver |
| Szervezettulajdonos | csak a rendszergazda (`:restrict-organization-owners true` mellett) | Adminisztráció → Szervezetek → „Tulajdonosok” |
| Kapcsolattartó, illetékes elnök | a rendszergazda vagy a szervezet tulajdonosa | a szervezet „Kapcsolattartók” és „Illetékes elnökök” mezője |
| Bírálói kör tagja és státusza | a rendszergazda vagy az **EPKEB** szervezet tulajdonosa | az EPKEB szervezet „Bírálói kör” listája (más szervezetnél nem jelenik meg) |
| Elnök / titkár (handler) | a rendszergazda vagy a munkafolyamat szervezetének tulajdonosa | Adminisztráció → Munkafolyamatok → „Elnökök” |
| Kutató | a kapcsolattartó (meghívó) | a kérelem oldalán |
| Felügyelő | a kapcsolattartó vagy a kutató (meghívó vagy kijelölés) | a kérelem oldalán |
| Bíráló egy kérelemhez | az elnök vagy a titkár, a bírálói körből | a kérelem oldalán vagy a bírálói naptárban |

Döntés (2026-10-28):

- Az **EPKEB szervezet tulajdonosai az elnökök és a titkár**. Így az elnökség maga kezeli a kapcsolattartókat, az illetékes elnököket, a bírálói kört és a munkafolyamatok elnöki listáját.
- A **rendszergazda** csak a technikai üzemeltető.
- A **tulajdonosi listát csak a rendszergazda módosíthatja** (`:restrict-organization-owners true`). A gyári REMS-ben egy szervezettulajdonos más tulajdonost is felvehet vagy törölhet; a kapcsolóval ezt a szerver tiltja (403), és a felületen a mező nem szerkeszthető.
- A **tagintézményi szervezeteknek nincs tulajdonosa**: a kapcsolattartóikat és az illetékes elnöküket az EPKEB adminjai állítják be. Ha egy tagintézmény tulajdonost kapna, a saját kapcsolattartóit és illetékes elnökét átírhatná.

## Elkészült fejlesztések

Mind kapcsolóval bekapcsolható; alapból ki vannak kapcsolva, így a gyári működés nem változik.

| Igény | Megoldás | Kapcsoló |
|---|---|---|
| A meghívott kérelmező (member) kitöltheti az űrlapot | `save-draft` a `member` szerepnek piszkozat és visszaküldött állapotban (mellékletfeltöltéssel együtt). Benyújtani továbbra is csak a kapcsolattartó tud. | `:enable-member-editing` |
| „Beadható” jelzés | Új parancs: `mark-ready` (a tagnak, piszkozat és visszaküldött állapotban), esemény: `marked-ready`. A kérelem oldalán „Beadhatónak jelölve” és az időpont látszik. Értesítés: ha van felügyelő, aki még nem hagyta jóvá a kérelmet, ő kap e-mailt („A kérelem a jóváhagyására vár”); különben a kapcsolattartó („A kérelem beadható”). | `:enable-member-editing` |
| A kérelmező válaszol a bíráló kérdésére | Benyújtott (bírálat alatti) kérelemnél a kapcsolattartó (`applicant`) és a kutatók (`member`) is írhatnak megjegyzést (`remark`, a felületen „Válasz vagy megjegyzés a bizottságnak”). Ez **mindig nyilvános**; az elnökök (handlerek) és a bírálók – a bírálatot már leadottak is – e-mailt kapnak róla. A bírálók és az elnökök nyilvános megjegyzéséről (kérdéséről) ilyenkor a kutatók is e-mailt kapnak, nem csak a kapcsolattartó. Névtelenítés mellett a kérelmező oldal továbbra sem látja a kérdező nevét. Piszkozatban, visszaküldött és lezárt kérelemnél nem elérhető. | `:enable-applicant-remarks` |
| Felügyelői jóváhagyás | Új szerep: `supervisor`. A felügyelőt a kapcsolattartó vagy a kutató **e-mailben meghívja** (`invite-supervisor`, „Felügyelő meghívása”): a meghívott a csatlakozáskor egyből felügyelő lesz. Egy már csatlakozott kutató is kijelölhető (`designate-supervisor`, a kutató dobozában „Kijelölés felügyelőnek”); saját magát senki nem jelölheti ki. A felügyelő jóváhagyja a kérelmet (`approve-supervision`, „Jóváhagyom felügyelőként”). Ha van felügyelő, a `submit` csak az ő jóváhagyása után sikerül (`t.actions.errors/supervision-not-approved`). Jóváhagyáskor, ha a kérelem már beadhatónak van jelölve, a kapcsolattartó kap e-mailt („A kérelem benyújtható”); ha még nincs, a kutatók. | `:enable-supervisor-approval` |
| Bírálói kör | A bírálói kör az `EPKEB` szervezet része: a szervezet szerkesztésekor a „Bírálói kör” listában adható meg a bíráló, a delegáló tagintézménye és a státusza (*Bírál* / *Bírál\** / *Átmenetileg nem* / *Nem*). Csak az EPKEB szervezet tulajdonosai és az adminok szerkeszthetik. Bírálatot (`request-review`) csak *Bírál* vagy *Bírál\** státuszú körtagtól lehet kérni, és a kérelmező tagintézményének delegáltjától nem (`t.actions.errors/reviewer-not-in-pool`, `reviewer-from-applicant-organization`). E-mailes bírálómeghívás (`invite-reviewer`) nincs. A „Bírálat kérése” ablak csak a kérhető bírálókat mutatja, a legkevésbé terheltekkel kezdve, a delegáló intézménnyel és a terheléssel: idei bírálatok, folyamatban lévők, utolsó felkérés, valamint az intézmény idei kérelmei és bírálatai. | `:reviewer-pool-organization "EPKEB"` |
| Bírálói naptár | Új oldal: „Bírálói naptár” (`/reviewer-calendar`), csak az elnököknek (handlereknek, a titkár is az), ha van bírálói kör. Függőlegesen a kör aktív tagjai (és akinek még van folyamatban lévő bírálata), vízszintesen az idő két héttel visszafelé és hat héttel előre, a mai nappal jelölve. Minden folyamatban lévő bírálat egy sáv a felkéréstől a határidőig (a felkéréstől 20 nap; ha a kérelmet előtte kiegészítésre visszaküldték, 10 nap). A határidőn túli rész piros, a késés napjaival; a bíráló neve is piros, ha késik. A sávra kattintva megnyílik a kérelem. Egy bírálónak több sávja lehet. Felül a bírálóra váró benyújtott kérelmek listája (csak azoké, amelyeket az elnök kezel, és amelyekben bírálatot lehet kérni; rövidített eljárásban nem); egy kérelmet kiválasztva minden bírálónál „Felkérés” gomb jelenik meg, vagy „Felkérve”, vagy „Nem kérhető” (nem aktív, vagy a kérelmező tagintézményének delegáltja). | `:reviewer-pool-organization`, `:review-deadline-days` (20), `:re-review-deadline-days` (10) |
| Illetékes elnök értesítése | Ha a kérelem szervezetének (a kapcsolattartó tagintézményének) vannak illetékes elnökei, és közülük legalább egy elnöke (handlere) a munkafolyamatnak, akkor az elnököknek szóló e-maileket (benyújtás, válasz a bírálói kérdésre, bírálat stb.) csak ők kapják. Különben minden elnök. A kérelmet a többi elnök is látja, és eljárhat benne. | `:enable-organization-contacts` |

Szabályok:

- A „beadható” jelzés és a felügyelői jóváhagyás a **válaszok akkori állapotára** vonatkozik: ha később bárki megváltoztatja a válaszokat, mindkettő érvényét veszti. Ugyanazon válaszok újramentése nem érinti.
- A benyújtás törli a „beadható” jelzést; a felügyelői jóváhagyás megmarad, amíg a válaszok nem változnak (így visszaküldés után módosítás nélkül újra benyújtható).
- A felügyelő **csak jóváhagy**: amíg felügyelő, nem kutató szerepű, így nem menthet és nem jelölheti beadhatónak a kérelmet. Benyújtás után a bírálói kérdésekre válaszolhat (`remark`, `:enable-applicant-remarks` mellett).
- Ha a jóváhagyás után módosulnak a válaszok, a kérelem oldalán „A kérelem a jóváhagyás után módosult, új jóváhagyás kell” látszik. A felügyelő a következő „beadható” jelzéskor e-mailt kap.
- Ha a felügyelőt eltávolítják a tagok közül, a feltétel megszűnik. Új felügyelő kijelölése a korábbi jóváhagyást törli, a korábbi felügyelő újra kutató lesz.
- Felügyelőt csak akkor kell kijelölni, ha a kérelmezőnek nincs fokozata; ezt a rendszer nem ellenőrzi, a kapcsolattartó dönti el.
- A felügyelő szerep a workflow tiltásaiban is választható (*Disable commands* → szerep). A jogok szerepenként összeadódnak, ezért egy tiltás egy másik szerep jogát nem veszi el.

A [cel-folyamat.md](cel-folyamat.md) fejlesztési listája ennek megfelelően rövidül: a névtelenség, a bizottsági szavazás (kézi számlálással), a rövidített eljárás és a felesleges funkciók elrejtése konfigurációval megoldható.
