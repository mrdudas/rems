# Telepítés: rems.analyst.hu (próbarendszer)

A rems.analyst.hu **próbarendszer**: arra szolgál, hogy a REMS-alapú folyamatot bemutassuk az elnökségnek. A bizottság éles munkája ma a Google Drive-on folyik (sablonok, közös Excel-nyilvántartás), azt **tilos módosítani** (lásd [README.md](README.md#rendszerek)). A próbarendszerbe csak teszt- és bemutatóadat kerül.

Az állapot 2026-10-27-i felmérése. A beállításokat (űrlapok, workflow-k, katalógus, licencek) az admin API-n olvastuk ki, lásd [urlap-terv.md](urlap-terv.md#a-remsanalysthu-jelenlegi-beállításai).

## Felépítés

- Docker-konténer `rems` néven, a `/opt/analyst.hu/rems/rems-docker-compose.yml` szerint (a fő `docker-compose.yml` húzza be), Traefik mögött, `rems.${DOMAIN}` hoszton, a 3000-es porton.
- Build (2026-10-27 óta): a repó `Dockerfile.epkeb` fájlja, build-kontextus a `/home/dudas.zsolt/rems` munkakönyvtár (az `epkeb` ág). Többlépcsős: Leiningen + npm build, majd `eclipse-temurin:25-jre-alpine` futtatókörnyezet. A `Dockerfile.epkeb.dockerignore` a `.git` mappát is beengedi, mert a `lein uberjar` a `git describe`-ból írja a verziót.
- Indításkor `COMMANDS="migrate run"`: előbb lefutnak az adatbázis-migrációk.
- Adatbázis: a közös `supabase-db` PostgreSQL, `rems` adatbázis.
- Becsatolt fájlok: `config.edn` (csak olvasható), keresőindex-volume. A `translations/` becsatolását megszüntettük; a fordítás a jar-ból jön.
- Azonosítás: OIDC a Keycloakon keresztül (`auth.analyst.hu`, `analyst` realm, `rems-client` kliens).
- Levelezés (2026-10-28 óta): **a próbarendszer nem küld valódi e-mailt.** Minden levél a `rems-mailpit` levélelfogóba megy (`:smtp-host "rems-mailpit"`, `:smtp-port 1025`; a szolgáltatás a `rems-docker-compose.yml`-ben van). A Mailpit csak a belső hálózaton érhető el; a levelek (például a meghívó linkek) a webes felületén (8025-ös port) vagy az API-n (`/api/v1/messages`) olvashatók, legfeljebb 2000 levél marad meg. Korábban a közös `smtp-relay:25`-ön keresztül valódi címekre ment a levél. Éles rendszerben a valódi SMTP-t kell beállítani.
- Helyi tesztek: nem küldenek levelet, mert a `test-config.edn`-ben nincs SMTP-szerver (a REMS ilyenkor csak naplózza a levelet), és a tesztkonténer csak a teszt-adatbázist éri el.
- Nyelvek: `[:en :hu]`.

## Forráskód

- A konténer a `/opt/analyst.hu/sources/rems` mappából épül. Ez a CSCfi upstream egy 2026 februári állapota, két nem commitolt módosítással:
  - `src/cljs/rems/administration/create_organization.cljs`: a szervezet létrehozásakor és szerkesztésekor elég egy nyelven kitölteni a nevet és a rövid nevet (eredetileg minden beállított nyelven kötelező volt);
  - `dev-config.edn`: fejlesztői adatbázis-elérés és `public-url` a test-rems.analyst.hu-hoz.
- A `~/rems` (fork: `github.com/mrdudas/rems`) frissebb upstream állapot, de ezek a módosítások nincsenek benne.

**Kész (2026-10-27):** a konténer a fork `epkeb` ágából épül; a szervezet-űrlap javítása és a magyar fordítás átkerült az ágra. A `/opt/analyst.hu/sources/rems` már nem használt. A módosított telepítési fájlokról mentés készült (`*.bak-20261027-1901`).

## Frissítés menete

1. Változtatás és commit az `epkeb` ágon (`~/rems`).
2. Unit tesztek (helyben nincs Java, ezért konténerben):
   `docker run --rm -m 3g -v $PWD:/w -w /w clojure:temurin-21-lein-2.11.2 lein kaocha unit`
   Integrációs tesztek: egyszerre a teljes készlet nem fér el a szerver memóriájában, ezért névterenként, 4-es csoportokban futnak egy eldobható PostgreSQL 13 konténer mellett (`--network container:rems-test-pg`, `lein kaocha integration --focus <névtér> ...`, `JVM_OPTS=-Xmx900m`, konténer `-m 1400m`). Ehhez kb. 2 GB szabad memória kell. A GitHub Actions is futtatja őket (`.github/workflows/test.yml`), ha a forkon engedélyezve van.
3. `git push`. A GitHub Actions (`.github/workflows/docker.yml`) megépíti a képet a `Dockerfile.epkeb`-ből, és feltölti: `ghcr.io/mrdudas/rems:epkeb` (és commitonként `:epkeb-<sha>`). Az építés a GitHub gépén fut, a szerver memóriáját nem terheli. Az eredmény a repó *Actions* fülén látszik.
4. `cd /opt/analyst.hu && docker compose pull rems && docker compose up -d rems`; a migrációk automatikusan lefutnak.
   Helyi építés (ha a GitHub nem elérhető): a `rems-docker-compose.yml`-ben az `image:` helyett a `build:` blokk, majd `docker compose build rems` (kb. 15–25 perc, 2–3 GB memória).
5. Ellenőrzés a böngészőben. Újraindításkor a REMS-munkamenetek elvesznek, újra be kell lépni.

## Azonosítás és tesztbelépés

**Probléma (2026-10-28):** a REMS az `analyst` realm `rems-client` kliensét használja, a többi analyst.hu-szolgáltatással közös Keycloak-munkamenettel. A REMS kilépése a Keycloak-munkamenetet nem zárja le, ezért a következő bejelentkezés ugyanazzal a fiókkal történik; más fiókra váltani így nem lehet, a Keycloak-munkamenet lezárása pedig a többi szolgáltatásból is kiléptetne.

**Tesztelésre (jelenlegi állapot):**

- A REMS előtt Keycloak-belépés van (oauth2-proxy, mint a többi szolgáltatásnál), saját `rems-oauth2-proxy` forwardauth köztes réteggel (`rems-docker-compose.yml`). A közös `oauth2-proxy@docker` nem használható, mert az `authResponseHeadersRegex=^X-` beállítása a kérésből minden `X-` fejlécet töröl, így a REMS `X-CSRF-Token` fejlécét is, és minden mentés 401-et kap.
- A REMS-ben a beépített tesztbelépés van bekapcsolva (`:authentication :fake`, `:fake-authentication-data :test`): a `/fake-login` oldalon jelszó nélkül lehet a beépített tesztfelhasználók (alice, elsa, frank, handler, carl, owner, ...) nevében belépni, és egy kattintással váltani. Csak az oauth2-proxy mögött szabad bekapcsolni.
- Tesztszerepek: `alice` az EPKEB szervezet kapcsolattartója, `elsa` kutató, `frank` felügyelő, `handler` a „próba munkafolyamat” elnöke (handlere), `carl` bíráló, `owner` admin (`grant-role owner owner`; a szerepet a futó REMS újraindítás után látja).
- A Keycloakos fiókok (az eredeti admin is) ilyenkor nem tudnak belépni.

**Visszaállítás Keycloakos belépésre:** a `config.edn`-ben `:authentication :oidc` (a `:fake-authentication-data` sor törlése), majd `docker compose up -d rems`. A REMS-kilépés valódi Keycloak-kilépéssé tehető az `:oidc-end-session-on-logout true` beállítással (RP-initiated logout), de ez a közös realm miatt a többi szolgáltatásból is kiléptet; a fiókváltáshoz a `:oidc-additional-authorization-parameters "&prompt=login"` beállítás (mindig jelszót kér) elég, ez most is be van állítva.

## A próbarendszer beállításai (`config.edn`)

```clojure
:mail-from "rems@analyst.hu"
:smtp-host "rems-mailpit"
:smtp-port 1025
:languages [:en :hu]
:default-language :hu
:enable-cart false
:enable-organization-contacts true
:enable-member-editing true
:enable-supervisor-approval true
:enable-applicant-remarks true
:enable-processing-states true
:enable-autosave true
:enable-review-recommendations true
:reviewer-pool-organization "EPKEB"
:restrict-organization-owners true
:theme-path "/rems/epkeb-theme/theme.edn"
:extra-stylesheets {:root "/rems/epkeb-theme/public" :files ["/css/epkeb.css"]}
:disable-commands [:application.command/change-resources
                   :application.command/assign-external-id
                   :application.command/add-licenses]
```

### Bemutatóadat

| Elem | Beállítás |
|---|---|
| Szervezet `EPKEB` | kapcsolattartó és illetékes elnök beállítva |
| Workflow #3 „próba munkafolyamat” (normál eljárás) | *anonymize handling* be; tiltások: `approve` a bírálónak, `copy-as-new` a tagnak, `change-applicant`, `redact-attachments` a bírálónak; állapotcímkék: Bírálók kijelölve, Bírálók felkérve, Módosítás alatt, Bizottsági szavazás |
| Workflow #4 „Rövidített eljárás” | *anonymize handling* be; tiltások: `copy-as-new` a tagnak, `change-applicant`, `request-review`, `invite-reviewer`, `request-decision`, `invite-decider`, `vote` |
| Űrlap #5 | az #3 másolata, „A kutatás címe” címmezőként (description), így a listákban a kérelem címe látszik. A használatban lévő űrlap nem szerkeszthető, ezért kellett másolat. |
| Workflow #5 „próba munkafolyamat” és #6 „Rövidített eljárás” | a #3 és #4 másolata (elnökök, nyilatkozatok, tiltások, névtelenítés, állapotcímkék) az #5 űrlappal. A munkafolyamathoz csatolt űrlap utólag nem cserélhető, és a katalóguselem űrlapja a munkafolyamaté mellé kerül (két űrlap), ezért kellett új munkafolyamat. A #3 és #4 neve „(régi)”; a korábbi kérelmek azokon maradnak. |
| Katalóguselem #6 „Normál eljárás” és #7 „Rövidített eljárás” | workflow #5 és #6, saját űrlap nélkül (kivezetve) |
| Űrlap #6 és #7, workflow #7 és #8, katalóguselem #8 „Normál eljárás” és #9 „Rövidített eljárás” (2026-10-28 óta) | az új űrlap ([urlap-terv.md](urlap-terv.md#az-új-űrlap-2026-10-28)); a #5 és #6 workflow neve „(régi, #5 űrlap)” |
| Szervezet `DE-PI` „Debreceni Egyetem Pszichológiai Intézet” | bemutató tagintézmény: kapcsolattartó `alice`, illetékes elnök az EPKEB-é; a bírálói körben `malice` innen delegált (így a debreceni kérelmeknél nem kérhető fel) |
| Űrlap #8, workflow #9, katalóguselem #10 „Rövidített eljárás” (2026-10-28 óta) | javított rövidített űrlap: csak a nyelv, az eredeti engedély száma, az eltérések és az eredeti kérelem kötelező; a #8 workflow neve „(régi, #7 űrlap)” |
| Kérelem 2026/15 | álnevesített valódi kérelem az új űrlapon, benyújtva |
| Kérelem 2026/8 | a kapcsolattartós létrehozás próbája |

## Upstream verzió

Állapot 2026-10-27-én (`upstream` = `github.com/CSCfi/rems`):

| | Verzió |
|---|---|
| Próbarendszer (rems.analyst.hu) | v2.41 + `epkeb` ág (2026-10-27 óta; előtte v2.39 + 2 commit) |
| Fork `master` | v2.39.1 és v2.40 közötti fejlesztői állapot |
| Upstream `master` | v2.40 + néhány commit |
| Legújabb kiadás | **v2.41** (2026-10-09), a `release-v2.41` ágon |

A v2.41 visszavonja a v2.40 változásait (katalógushierarchia, új keresési mezők, `:external-frontend-url`, PostgreSQL 18 támogatás), mert a katalógusoldal lelassult tőlük; funkcionálisan azonos a v2.39.1-gyel. Az upstream `master` viszont továbbra is a v2.40-es kódot viszi.

Döntés: az `epkeb` ág a **v2.41** tagről indul. A próbarendszeren futó v2.39-hez képest ez csak a v2.39.1 javításait hozza, így nincs kockázatos migráció. Ha a CSC a v2.40 funkcióit javítva újra kiadja, arra át lehet állni.

Frissítés menete: `git fetch upstream --tags`, majd az új kiadási tag beolvasztása az `epkeb` ágba.

## Magyar fordítás

- Kiindulás: a `/opt/analyst.hu/rems/translations/hu.edn` (2026-09), az EPKEB-re szabott változat („Kérelem”, „Kérelmező”, „Kutató”).
- Az `epkeb` ágon a `resources/translations/hu.edn` a telepített változat, javítva: a „Kérlemező” elírás javítva, a hiányzó `:t.actions/pre-actions-info` kulcs pótolva. A `test/clj/rems/test_locales.clj` a magyart is ellenőrzi (kulcsegyezés az angollal, és hogy a forráskódban használt kulcsok mind megvannak).
- Szóhasználat (2026-10-28): a [szojegyzek.md](szojegyzek.md) szerint. A „Jelentkezés” helyett „Kérelem”, a „Kezelő” helyett „Elnök”, a „Véleményező” helyett „Bíráló”, a resource/katalógustétel „Eljárás”. A névtelenített elnök és bíráló a kérelmező oldalon „Bizottság” néven jelenik meg (angolul „Committee”). Az angol felületen a szerepnevek is EPKEB-esek (Contact person, Researcher, Chair).

## Hibák

| Hiba | Hatás | Javítás |
|---|---|---|
| A `config.edn` `:email-from` kulcsot használt, a REMS viszont `:mail-from` kulcsot olvas (`src/clj/rems/email/core.clj`) | Az e-mailek feladója nem volt beállítva | **Javítva** 2026-10-27 |
| A `sources/rems/dev-config.edn` titkosítatlan adatbázisjelszót tartalmaz | Jelszó a forrásfában | A jelszó környezeti változóba; ha a fájl valaha bárhová felkerült, a jelszót cserélni |
