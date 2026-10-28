# Leképezés a REMS-re

Mi oldható meg a gyári REMS-szel konfigurációból, és mihez kell fejlesztés.

## Fogalmak

| EPKEB | REMS |
|---|---|
| Kérelem | application (kérvény) |
| Kapcsolattartó | applicant (kérelmező) |
| Kutató, felügyelő | applicant / member, vagy csak űrlapmező |
| Elnök, társelnök | handler (ügyintéző) a workflow-ban |
| Bíráló | reviewer (`request-review` paranccsal felkért) |
| Tagintézmény | organization |
| Normál és rövidített eljárás | két workflow, két katalóguselem |
| Kérelemsablon | form (űrlap) |
| Engedély | jóváhagyás (`approve`) + új engedélyentitás, lásd [virtualis-engedely.md](virtualis-engedely.md) |
| Archív postafiók (elnok@epkeb.hu) | e-mail-másolat vagy eseményértesítés (fejlesztendő) |

Megjegyzés a fordításhoz: a telepített `hu.edn` a REMS „resource” fogalmát fordítja „Kérelem”-nek. Az EPKEB-ben a beküldött kérvény a kérelem, ezért a resource-ot inkább „Eljárás”-nak vagy „Eljárástípus”-nak kellene hívni, az application-t „Kérelem”-nek.

## Konfigurációval megoldható

| Követelmény | Megoldás |
|---|---|
| Beküldés űrlapon, mellékletekkel | form `attachment` mezőkkel (lásd [sablonok.md](sablonok.md#kérelem-űrlap)) |
| Formai hiba esetén visszaküldés, újraküldés | `return` + `submit` |
| Nem az EPKEB illetékessége | `reject` vagy `close` megjegyzéssel |
| Bármelyik elnök intézheti | minden elnök handler a workflow-ban |
| Bírálók kézi kijelölése | `request-review` |
| Módosítási körök, többször is | `return` → `submit` → új `request-review` |
| Rövidített eljárás bírálók nélkül | külön workflow és katalóguselem, kötelező „korábbi engedélyszám” mezővel |
| Magyar és angol felület | `:languages [:hu :en]`, `hu.edn` |
| Kérelemsablon letöltése hu/en, aláírási útmutató | link a katalóguselem leírásában vagy az űrlap info mezőjében |
| Visszavonás | `revoke` |

## Fejlesztést igényel

| # | Funkció | Miért |
|---|---|---|
| 1 | **Virtuális engedély** – számkiosztás, nyilvános ellenőrző oldal, PDF, visszavonás | A REMS-nek nincs engedélyfogalma, csak a kérvény teljes PDF-exportja. Lásd [virtualis-engedely.md](virtualis-engedely.md). |
| 2 | **Bírálati lap mint űrlap**: négy kötelező döntés, indoklás, kérelmezőnek szóló és csak elnöknek szóló rész | A REMS `review` csak szabad szöveg és melléklet. |
| 3 | **Névtelen továbbítás**: a bírálók kérelmezőnek szóló észrevételei bíráló neve nélkül jussanak el a kérelmezőhöz | A REMS-ben a bírálat a kérelmezőnek nem látszik. Ideiglenes megoldás: az elnök bemásolja a `return` megjegyzésébe. |
| 4 | **Figyelmeztetés nem egységes bírálatnál** (normál eljárásnál) | Az elnök felülbírálhatja a bírálókat, ezért nem tiltás, hanem figyelmeztetés jóváhagyáskor és elutasításkor, ha a két bírálat nem egyezik vagy hiányzik. |
| 5 | **Illetékes társelnök tagintézményenként**: értesítés csak neki, az engedélyen az ő neve | A REMS minden handlert értesít. Workflow-nként egy intézmény is megoldaná, de az ~20 workflow-t jelent. |
| 6 | **Archív másolat** az elnok@epkeb.hu címre, csatolmánnyal | A gyári e-mail-értesítő nem tud másolatot és csatolmányt. Alternatíva: `:event-notification-targets` egy kis archiváló szolgáltatásra. |
| 7 | **Határidők és emlékeztetők**: 20/10/14/30 nap, előtte 1 héttel és 3 nappal, késéskor | A REMS-ben nincs bírálati határidő. |
| 8 | **Bírálóválasztó statisztikával**: eddigi bírálatok száma, saját intézmény beküldött kérelmei | Kézi kiosztás támogatása ([i][j], [o][p]). |
| 9 | **Kérelem sorszáma** az e-mailek tárgyában („001-es EPKEB kérelem”) | A REMS external id formátuma beállítható, de az értesítők szövegét testre kell szabni. |
| 10 | **Nyelv a kérelemhez kötve**: angol kérelemhez angol bírálat, értesítő és engedély | A REMS a felhasználó nyelvét használja, nem a kérelemét. |

## Felhasználható REMS-mechanizmusok

- `docs/event-notification.md` – HTTP eseményértesítés (archívumhoz, külső integrációhoz).
- `docs/hooks.md`, `docs/plugins.md` – bővítési pontok.
- `:application-id-column`, external id – a kérelem sorszámához.
- Parancsok mellékletei (`:attachments`) és a `:public` jelző – megjegyzések és mellékletek láthatósága a kérelmező felé.
