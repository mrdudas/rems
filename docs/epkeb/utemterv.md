# Ütemterv és nyitott kérdések

## Javasolt sorrend

### 1. Alapok

- `hu.edn` átvétele a repóba, javításokkal, hiányzó kulcsok pótlása; `:hu` a nyelvek közé.
- A szervezet-űrlap javításának átvétele.
- `config.edn`: `:mail-from` javítás.
- A konténer a forkból épüljön.
- Seed/teszt adatok: tagintézmények mint szervezetek, normál és rövidített workflow, kérelem űrlap ([sablonok.md](sablonok.md#kérelem-űrlap)), két katalóguselem.

### 2. Virtuális engedély

[virtualis-engedely.md](virtualis-engedely.md): adatbázis-migráció, számkiosztás, kiállítás jóváhagyáskor, nyilvános ellenőrző oldal, PDF QR-kóddal, visszavonás, tesztek.

### 3. Bírálat

- Bírálati lap űrlapként, négy döntéssel és kétféle láthatóságú szöveggel.
- Névtelen továbbítás a kérelmezőnek.
- Az elnök felülbírálhatja a bírálókat: jóváhagyáskor és elutasításkor figyelmeztetés, ha a bírálatok nem egységesek vagy hiányoznak.

### 4. Értesítések és határidők

- Illetékes társelnök tagintézményenként.
- Archív másolat az elnok@epkeb.hu címre.
- Határidők (20/10/14/30 nap) és emlékeztetők.
- Kérelem sorszáma a levelek tárgyában; a levél nyelve a kérelem nyelve.

### 5. Kényelmi funkciók

- Bírálóválasztó statisztikával.
- Kérelemsablonok letöltése, aláírási útmutató.

### 6. Bemutató az elnökségnek

- A rems.analyst.hu próbarendszeren, bemutatóadatokkal: tagintézmények, kapcsolattartók, bírálók, néhány kérelem különböző állapotokban, egy kiadott bemutató engedély.
- Az elnökség jóváhagyása kell a nyitott kérdésekhez (lent) és az éles bevezetéshez.

### 7. Élesítés

- Az éles munka addig a Google Drive-on folyik; azt a rendszer nem módosítja. Átálláskor a folyamatban lévő kérelmek kezelését és a kezdő engedélyszámot külön kell egyeztetni.
- Átköltözés az **engedely.epkeb.hu** domainre (DNS, Traefik-szabály és TLS-tanúsítvány, `:public-url`, a Keycloak-kliens átirányítási címei).
- Ha a rems.analyst.hu alatt már készült valódi engedély: a `/permit/...` útvonalak 301-es átirányítása az új domainre.

## Eldöntött kérdések

| Kérdés | Döntés |
|---|---|
| Engedély formája | virtuális: link, nyilvános ellenőrzés, PDF letölthető |
| Alapverzió | upstream v2.41, `epkeb` ág |
| Domain | fejlesztés alatt rems.analyst.hu, élesben engedely.epkeb.hu |
| Rendszerek | éles: Google Drive (tilos módosítani); rems.analyst.hu: próbarendszer a bemutatóhoz |
| Ki dolgozik | a kérelmező tölt ki mindent; a bizottság csak dönt ([cel-folyamat.md](cel-folyamat.md)) |
| Ki hozhat létre kérelmet | csak a kapcsolattartó (intézményenként 1–2 fő), aki meghívja a kérelmezőt és benyújtja a kérelmet |
| Űrlap | nem a PDF másolata; újragondolva, duplikációk nélkül |
| Bírálókiosztás | egyelőre kézi, listából választva; jelentkezés is lehetséges; terhelés arányos |
| Elutasított kérelem | később újra beadható (új eljárás) |
| Eltérő bírálatok | az elnök felülbírálhatja a bírálókat (engedélyt ad vagy elutasít); a rendszer figyelmeztet, ha a bírálói vélemény nem egységes |

## Nyitott kérdések

| Kérdés | Kinek |
|---|---|
| Következő kiadandó engedélyszám | elnökség |
| Aláíráskép a PDF-en; egy vagy két társelnök neve | elnökség |
| Régi engedélyek bevitele | elnökség |
| Nyilvános engedély-ellenőrzés vs. titoktartási szabály ([virtualis-engedely.md](virtualis-engedely.md#titoktartás)) | bizottság |
| A „Bírál\*” bírálói státusz jelentése | elnökség |
| Felügyelői jóváhagyás: a javaslat a rendszerbeli jóváhagyás aláírt melléklet helyett ([cel-folyamat.md](cel-folyamat.md)) – elfogadható-e? | elnökség |
| Az új űrlap terve ([urlap-terv.md](urlap-terv.md)): postacím kell-e, mezőlista jóváhagyása | elnökség |
| Még fel nem dolgozott anyagok: „Felosztás_2025.xlsx”, alapító okirat, a két almappa | – |
