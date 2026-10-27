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
- Levelezés: `smtp-relay:25`.
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
3. `cd /opt/analyst.hu && docker compose build rems` (kb. 15–25 perc, 2–3 GB memória).
4. `docker compose up -d rems`; a migrációk automatikusan lefutnak.
5. Ellenőrzés a böngészőben. Újraindításkor a REMS-munkamenetek elvesznek, újra be kell lépni.

## A próbarendszer beállításai (`config.edn`)

```clojure
:mail-from "rems@analyst.hu"
:languages [:en :hu]
:enable-organization-contacts true
:enable-processing-states true
:disable-commands [:application.command/change-resources
                   :application.command/assign-external-id
                   :application.command/add-licenses]
```

Bemutatóadat: az `EPKEB` szervezet kapcsolattartója és illetékes elnöke beállítva; a 2026/8-as kérelem a kapcsolattartós létrehozás próbája.

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
- Még nyitott: a resource/application fogalmak fordítása (lásd [rems-lekepezes.md](rems-lekepezes.md#fogalmak)).
- A felületen még sok helyen „Jelentkezés” áll (az eredeti „application” fordítása); ezt a fogalmi tisztázással együtt kell javítani.

## Hibák

| Hiba | Hatás | Javítás |
|---|---|---|
| A `config.edn` `:email-from` kulcsot használt, a REMS viszont `:mail-from` kulcsot olvas (`src/clj/rems/email/core.clj`) | Az e-mailek feladója nem volt beállítva | **Javítva** 2026-10-27 |
| A `sources/rems/dev-config.edn` titkosítatlan adatbázisjelszót tartalmaz | Jelszó a forrásfában | A jelszó környezeti változóba; ha a fájl valaha bárhová felkerült, a jelszót cserélni |
