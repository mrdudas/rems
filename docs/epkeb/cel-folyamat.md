# A tervezett folyamat

Ez a dokumentum a rendszer **cél-folyamatát** írja le. Nem a mostani PDF-űrlapot és e-mail-folyamatot másolja le, hanem újragondolja azt, a 2026-10-27-i egyeztetés alapján és az [eljárásrend](eljarasrend.md) szabályaival összhangban.

## Alapelvek

1. **A munkát a kérelmező végzi, nem a bizottság.** Kevés a bíráló és az adminisztrátor, ezért minden adatot a kérelmező visz be, egyszer, strukturáltan. A bizottság tagjai csak döntenek.
2. **Egy adat egyszer.** Ami a rendszerből tudható (a kérelmező neve és e-mail-címe, az intézmény, a beadás dátuma, a kérelem sorszáma), azt nem kérjük be. A mostani PDF-űrlap duplikációi megszűnnek.
3. **Kapuőr a kapcsolattartó.** Kérelmet csak a tagintézmények kijelölt kapcsolattartói hozhatnak létre és nyújthatnak be. A formai ellenőrzés az ő feladata.
4. **A rendszer követi az állapotot.** A közös Excel-táblázat (státusz, határidők, terhelés, engedélyszám) helyét a rendszer veszi át.

## Szerepek

| Szerep | Kik | Mit tehet | REMS |
|---|---|---|---|
| **Kapcsolattartó** | tagintézményenként 1–2 fő | kérelmet hoz létre, meghívja a kérelmezőt, formailag ellenőriz, benyújt; visszautasíthatja, ha nem az EPKEB illetékes | applicant; a létrehozás joga **új korlátozás** |
| **Kérelmező (kutató)** | a kapcsolattartó hívja meg | kitölti az űrlapot és a mellékleteket, jelzi, hogy a kérelem beadható; válaszol a bírálók kérdéseire; módosítja a kérelmet | member, **bővített jogokkal** |
| **Felügyelő (szupervizor)** | fokozat nélküli kérelmezőnél kötelező | előzetesen hozzájárul (vállalja a felelősséget a tartalomért) | member + **új jóváhagyó lépés** |
| **Elnök / társelnök** | több is lehet | mindent lát; bírálókat jelöl ki; dönt; engedélyt ad ki | handler |
| **Elnök nevében eljáró** (titkár) | | az elnök adminisztratív lépéseit végzi | handler |
| **Bíráló** | bírálói körből | letölti/olvassa a kérelmet, kérdez, kiegészítést kér, javaslatot tesz | reviewer |
| **Bizottsági tag** | minden tag | elutasítási javaslatnál szavaz | **új** (szavazás) |

## Lépések

### 1. Létrehozás és kitöltés

1. A kapcsolattartó létrehozza a kérelmet (normál vagy rövidített eljárás).
2. Meghívja a kérelmezőt e-mailben (REMS `invite-member`). Ha a kérelmezőnek nincs tudományos fokozata, a felügyelőt is.
3. A kérelmező kitölti az űrlapot és feltölti a mellékleteket. Mentheti, folytathatja.
4. A felügyelő a rendszerben jóváhagyja a kérelmet (aláírás helyett).
5. A kérelmező jelzi, hogy a kérelem beadható. A kapcsolattartó értesítést kap.

### 2. Formai ellenőrzés és benyújtás

6. A kapcsolattartó formailag ellenőrzi:
   - ha hiányos: megjegyzéssel visszaadja a kérelmezőnek (a kérelem piszkozat marad);
   - ha nem az EPKEB illetékessége: visszautasítja, továbbítás nélkül;
   - ha rendben: **benyújtja**. Ekkor kap sorszámot, és innen indul a bírálati folyamat.

### 3. Bírálók kijelölése

7. Az illetékes társelnök (a tagintézmény alapján) értesítést kap; bármelyik elnök eljárhat.
8. Bírálókijelölés:
   - **jelentkezés**: a bírálói kör tagjai látják a bírálóra váró kérelmeket (címmel, eljárásmóddal), és jelentkezhetnek;
   - **kiosztás**: az elnök listából választ. A listán csak aktív bírálók szerepelnek, **a kérelmező intézményéből senki** (eljárásrend), és minden név mellett ott van az idei bírálatok száma, a folyamatban lévők száma és az intézmény terhelési aránya;
   - normál eljárásban **két bíráló**; rövidített eljárásban nincs bíráló, az illetékes társelnök egy személyben dönt.
9. A bírálók felkérést kapnak, 20 napos határidővel.

### 4. Bírálat

10. A bíráló a kérelem oldalán:
    - **kérdezhet / információt kérhet** a kérelmezőtől. A kérdés névtelenül jelenik meg, a kérelmező a rendszerben válaszol; a kérelem közben nem kerül vissza;
    - **külső szakértőt** vonhat be; erről az elnök értesítést kap;
    - leadja a **bírálatot** a bírálati lap szerint: javaslat (*Jóváhagyás* / *Kiegészítés kérése* / *Elutasítás*, vagy *az EPKEB nem illetékes*), indoklás, a kérelmezőnek szóló észrevételek, az elnöknek szóló megjegyzések.
11. Emlékeztető a határidő előtt 1 héttel és 3 nappal, késés esetén az illetékes elnöknek is.

### 5. Döntés

12. Amikor mindkét bírálat beérkezett:
    - **mindkettő Jóváhagyás** → az elnök kiadja a [virtuális engedélyt](virtualis-engedely.md);
    - **valamelyik Kiegészítés kérése** (és egyik sem elutasítás) → a kérelem visszakerül a kérelmezőhöz, a bírálók kérelmezőnek szóló észrevételeivel, **névtelenül**. 30 nap határidő. A kiegészített kérelem **ugyanazzal a sorszámmal** visszakerül **ugyanahhoz a két bírálóhoz**, 10 napos határidővel. Ez többször is ismétlődhet;
    - **valamelyik Elutasítás** → a kérelem a **teljes bizottság** elé kerül, minden tag szavaz. Egyszerű többség dönt, szavazategyenlőség esetén a soros elnök szavazata dönt;
    - a bíráló a teljes bizottság véleményét kéri → ugyanez a szavazás.

    Ez a szokásos menet, de **az elnök felülbírálhatja a bírálókat**: eltérő vagy nem egyező bírálatok mellett is kiadhatja az engedélyt vagy elutasíthatja a kérelmet. A rendszer ezt nem tiltja, de jóváhagyáskor és elutasításkor **figyelmeztet**, ha a bírálói vélemény nem egységes (vagy ha egy bírálat még hiányzik).
13. Ha a kérelmező 30 napon belül nem nyújtja be a kiegészítést, az eljárás felfüggesztődik (a kérelem lezárul).

### 6. Elutasítás után

14. Az elutasított vagy lezárt kérelem **később újra beadható**: a kapcsolattartó másolatot készít belőle (REMS `copy-as-new`). Ez új eljárás, új sorszámmal és új bírálókkal. Az eljárásrend szerint ugyanez a helyzet, ha a kérelmező a 30 nap után új eljárást indít.

## Állapotok

A táblázat státuszai és a rendszerbeli megfelelőjük:

| Táblázat | Rendszer |
|---|---|
| – | Piszkozat (kitöltés alatt) → Beadható (jelezve) |
| 1. Bírálók kijelölve (elnök) | Benyújtva, bírálókra vár |
| 2. Bírálók felkérve (titkár) | Bírálat alatt |
| 3. Módosítás alatt (titkár) | Visszaküldve kiegészítésre |
| – | Bizottsági szavazás |
| 5. Kérelem lezárva – megadva | Jóváhagyva, engedély kiadva |
| 5. Kérelem lezárva – megtagadva | Elutasítva |

A REMS processing state funkciója (`:enable-processing-states`) a finomabb állapotokra használható.

## Mit tud ebből a REMS, és mi fejlesztendő

| Funkció | REMS | Teendő |
|---|---|---|
| Kérelmező meghívása | `invite-member` | – |
| Létrehozás csak kapcsolattartónak | nincs | **kész**: `:enable-organization-contacts`, kapcsolattartók és illetékes elnökök a szervezeten (lásd [szervezetek.md](szervezetek.md#megvalósítás-1-lépés)) |
| A meghívott tag szerkesztheti az űrlapot | csak az applicant szerkeszthet | **kész**: `:enable-member-editing` |
| „Beadható” jelzés | nincs | **kész**: `mark-ready` parancs, értesítés a kapcsolattartónak (lásd [jogosultsagok.md](jogosultsagok.md#elkészült-fejlesztések)) |
| Felügyelői jóváhagyás | nincs | **kész**: `designate-supervisor` és `approve-supervision`, a benyújtás feltétele |
| Benyújtás, visszaadás, másolat elutasítás után | `submit`, `return`, `copy-as-new` | – |
| Bírálói kör, jelentkezés, terhelés, saját intézmény kizárása | `request-review` bármely felhasználóra | **fejlesztés** |
| Bírálói kérdés és kérelmezői válasz | a bíráló `remark`-ja lehet nyilvános, de a kérelmező nem tud válaszolni | **fejlesztés**: kérdés–válasz, névtelenül |
| Strukturált bírálat javaslattal | `review` (szabad szöveg) + kísérleti `vote` (elfogad/elutasít, `:reviewers-vote`) | **fejlesztés**: bírálati lap űrlap, három javaslattal |
| Kiegészítés után vissza ugyanahhoz a két bírálóhoz | kézzel `request-review` | **fejlesztés**: automatikus újrafelkérés |
| Bizottsági szavazás | `vote` csak handlernek vagy bírálónak | **fejlesztés**: a bizottsági tagok meghívása szavazásra, többségi szabály, elnöki döntés egyenlőségnél |
| Határidők, emlékeztetők, felfüggesztés 30 nap után | nincs | **fejlesztés** |
| Illetékes társelnök | minden handler kap értesítést | **fejlesztés** |
