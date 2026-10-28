# Szervezetek a REMS-ben és az EPKEB-ben

## Mit jelent a szervezet a REMS-ben

Források: `docs/organizations.md`, `manual/owner.md` („Creating organisations”), `docs/glossary.md`, `docs/architecture/009-review-and-form-model.md`, `docs/architecture/019-catalogue-management-with-organization-delegation.md`, és a kód (`src/clj/rems/db/organizations.clj`, `src/clj/rems/middleware.clj`, `src/clj/rems/service/*`).

1. **Adminisztratív címke és jogosultsági egység.** Minden katalóguselem, űrlap, workflow, erőforrás és licenc egy szervezethez tartozik; legalább egy szervezetnek léteznie kell.
2. **Szervezet tulajdonosa (`organization-owner`).** A szervezet `:organization/owners` listáján szereplő felhasználó csak a saját szervezete elemeit hozhatja létre és szerkesztheti (űrlap, workflow, katalógus stb.). Az `owner` szerep minden szervezetét. Ez **adminisztrációs** jog, a kérvények intézéséhez nincs köze.
3. **Megosztás (ADR 019).** Az elemek lehetnek nyilvánosak vagy privátak a szervezeten belül; vegyes szervezetű katalóguselem is összerakható. **A kérvényt a workflow handlerei intézik**, nem a szervezet.
4. **A szervezet adatai:** azonosító, rövid név és teljes név (nyelvenként), tulajdonosok, **review-emails** (bírálói postafiókok listája). A review-emails mezőt a REMS tárolja, de a jelenlegi kód semmire nem használja: egy tervezett (ADR 009, „option 6”) szervezeti bírálói funkció maradványa.
5. **A felhasználó szervezete.** A felhasználónak lehet `:organizations` attribútuma, amely az identitásszolgáltatótól jön (OIDC-nél az `:oidc-extra-attributes` beállítással), vagy a users API-n állítható be. A dokumentáció szerint **ezt a REMS semmire nem használja**, csak megmutatja a handlernek a kérvényen.
6. **Amit a szervezet nem csinál:**
   - nem korlátozza, ki nyújthat be kérvényt (bárki, aki be tud lépni);
   - nem dönti el, ki intézi a kérvényt (azt a workflow handler-listája);
   - nem köti a felhasználót a kérvényhez.

**Összefoglalva:** a REMS-szervezet az adminisztráció felosztására való (ki szerkesztheti a katalógust), nem a kérelmezők intézményének kezelésére.

## Mit kell tudnia az EPKEB-nek az intézményekről

A [cel-folyamat.md](cel-folyamat.md) és az [eljárásrend](eljarasrend.md) alapján a tagintézményhez kötődik:

| Szabály | Mihez kell az intézmény |
|---|---|
| Kérelmet csak a kapcsolattartó hozhat létre | a felhasználó → intézmény → kapcsolattartó-e |
| Az engedélyen a kiállító intézmény neve szerepel | a kérvény → intézmény |
| A bíráló nem lehet a kérelem intézményéből | a bíráló intézménye ≠ a kérvény intézménye |
| Arányos bírálati terhelés | intézményenkénti kérelem- és bírálatszám |
| Illetékes társelnök | intézmény → társelnök |
| Bírálói kör | a felhasználó → intézmény, bírálói státusz |

Ezek egyikét sem tudja a gyári REMS-szervezet.

## Javaslat

### 1. Egy adminisztratív szervezet: `EPKEB`

Az űrlapok, workflow-k, katalóguselemek, erőforrások és licencek mind az `EPKEB` szervezethez tartoznak (ahogy most is a rems.analyst.hu-n). Tulajdonosa az elnökség (vagy egy admin). Így egy közös katalógus van, és nem kell intézményenként másolni semmit.

### 2. A tagintézmények is REMS-szervezetek

A 18 tagintézmény REMS-szervezetként jön létre (azonosító pl. `pte-btk-pi`, rövid név „PTE BTK PI”, teljes név magyarul és angolul). Ezek nem birtokolnak katalóguselemet. Arra valók, hogy az intézmény egyetlen helyen, a REMS admin felületén legyen nyilvántartva, és a meglévő szervezetkezelő felület (lista, szerkesztés, archiválás) újrahasznosítható legyen.

A szervezet adatai **EPKEB-mezőkkel** bővülnek. A szervezet a `organization` tábla `data` JSON-oszlopában tárolódik, így adatbázis-migráció nem kell. A séma viszont zárt (`OrganizationRaw` a `src/clj/rems/db/organizations.clj`-ben, és a betöltés csak az ismert kulcsokat veszi át), ezért a sémát, a betöltést, az API-t és az admin felületet bővíteni kell:

| Mező | Jelentés |
|---|---|
| `:epkeb/member-institution?` | tagintézmény-e (megkülönbözteti az `EPKEB` admin szervezettől) |
| `:epkeb/contacts` | kapcsolattartók (felhasználóazonosítók), 1–2 fő |
| `:epkeb/chair` | illetékes társelnök (felhasználóazonosító) |
| `:epkeb/reviewers` | bírálók és státuszuk (aktív / átmenetileg nem / nem) |
| `:epkeb/permit-name` | az engedélyen szereplő név (hu/en), ha eltér a teljes névtől |

A szervezeti tulajdonos (`owners`) szerepet **nem** használjuk a kapcsolattartóra, mert az admin jogot adna (űrlapot, workflow-t szerkeszthetne).

### 3. A felhasználó intézménye

Két lehetőség:

- **(a) Az EPKEB-mezőkből:** aki egy intézmény kapcsolattartó- vagy bírálólistáján szerepel, az ahhoz az intézményhez tartozik. A kérelmező intézménye a kérvényé, vagyis a kapcsolattartóé, aki létrehozta. **Javasolt**, mert nem kell hozzá a Keycloak módosítása.
- (b) Keycloak-attribútumból (`organizations` claim, `:oidc-extra-attributes`): az intézményt a felhasználó fiókjában kell beállítani. Több munka a fiókok kezelésében.

### 4. A kérvény intézménye

A kérvény létrehozásakor a rendszer rögzíti a kapcsolattartó intézményét (új esemény-mező vagy a létrehozó intézménye alapján számolva), és a kérvényen meg is jeleníti. Az űrlapon nincs intézmény-mező.

### Elvetett lehetőségek

- **Intézményenként külön katalóguselem és workflow** (18 × normál/rövidített): ez a REMS „szervezeti” modellje lenne, de a közös űrlap és folyamat változásait 36 helyen kellene karbantartani, és a bírálói szabályokat (más intézményből, arányos terhelés) így sem oldja meg.
- **Az intézmény mint űrlapmező** (a mostani rems.analyst.hu-n): bárki bármelyik intézményt kiválaszthatja, és a kapcsolattartói jogosultság nem köthető hozzá.

## Megvalósítás (1. lépés)

Elkészült az `epkeb` ágon:

- **Új szervezeti mezők:** `:organization/contacts` (kapcsolattartók) és `:organization/chairs` (illetékes elnökök). Az admin felületen a szervezet szerkesztésekor és megtekintésekor látszanak, ha a funkció be van kapcsolva. Az API-ban (`/api/organizations/create`, `/edit`) `[{:userid ...}]` alakban adhatók meg.
- **Új szerepkör:** `:organization-contact` – annak a felhasználónak, aki valamelyik szervezet kapcsolattartója.
- **Új beállítás:** `:enable-organization-contacts` (alapból `false`). Bekapcsolva:
  - kérelmet csak engedélyezett, nem archivált szervezet kapcsolattartója hozhat létre (másolat készítésére is vonatkozik); másnak a `t.actions.errors/not-organization-contact` hibát adja;
  - a létrehozási esemény rögzíti a kapcsolattartó szervezetét (`:application/organization`); ha valaki több szervezet kapcsolattartója, az azonosító szerinti első;
  - a kérelem oldalán megjelenik az intézmény;
  - a katalógusban a „Jelentkezés” (kérelem indítása) gomb csak kapcsolattartónak látszik.

A tagintézmény itt még nem különül el az `EPKEB` admin szervezettől: bármely szervezet kaphat kapcsolattartót. Az illetékes elnök mező alapján kapják az elnökök az e-maileket: ha a kérelem szervezetének van illetékes elnöke a munkafolyamat elnökei között, csak ő kap értesítést (lásd [jogosultsagok.md](jogosultsagok.md#elkészült-fejlesztések)). A bírálói szabályok későbbi lépésben használják.

Bekapcsolás a próbarendszeren (`/opt/analyst.hu/rems/config.edn`):

```clojure
:enable-organization-contacts true
```
