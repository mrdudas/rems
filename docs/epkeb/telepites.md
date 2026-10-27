# Telepítés: rems.analyst.hu (próbarendszer)

A rems.analyst.hu **próbarendszer**: arra szolgál, hogy a REMS-alapú folyamatot bemutassuk az elnökségnek. A bizottság éles munkája ma a Google Drive-on folyik (sablonok, közös Excel-nyilvántartás), azt **tilos módosítani** (lásd [README.md](README.md#rendszerek)). A próbarendszerbe csak teszt- és bemutatóadat kerül.

Az állapot 2026-10-27-i felmérése. A beállításokat (űrlapok, workflow-k, katalógus, licencek) az admin API-n olvastuk ki, lásd [urlap-terv.md](urlap-terv.md#a-remsanalysthu-jelenlegi-beállításai).

## Felépítés

- Docker-konténer `rems` néven, a `/opt/analyst.hu/rems/rems-docker-compose.yml` szerint (a fő `docker-compose.yml` húzza be), Traefik mögött, `rems.${DOMAIN}` hoszton, a 3000-es porton.
- Build: `/opt/analyst.hu/rems/Dockerfile.local`, többlépcsős (Clojure/Leiningen + Node build, majd `eclipse-temurin:25-jre-alpine` futtatókörnyezet). A forrás: `/opt/analyst.hu/sources/rems`.
- Adatbázis: a közös `supabase-db` PostgreSQL, `rems` adatbázis.
- Becsatolt fájlok: `config.edn` (csak olvasható), `translations/` mappa, keresőindex-volume.
- Azonosítás: OIDC a Keycloakon keresztül (`auth.analyst.hu`, `analyst` realm, `rems-client` kliens).
- Levelezés: `smtp-relay:25`.
- Nyelvek: `[:en :hu]`, a fordítás a `translations/` mappából.

## Forráskód

- A konténer a `/opt/analyst.hu/sources/rems` mappából épül. Ez a CSCfi upstream egy 2026 februári állapota, két nem commitolt módosítással:
  - `src/cljs/rems/administration/create_organization.cljs`: a szervezet létrehozásakor és szerkesztésekor elég egy nyelven kitölteni a nevet és a rövid nevet (eredetileg minden beállított nyelven kötelező volt);
  - `dev-config.edn`: fejlesztői adatbázis-elérés és `public-url` a test-rems.analyst.hu-hoz.
- A `~/rems` (fork: `github.com/mrdudas/rems`) frissebb upstream állapot, de ezek a módosítások nincsenek benne.

**Teendő:** a fork `epkeb` ága legyen az egyetlen forrás, és a konténer abból épüljön. A szervezet-űrlap javítása és a magyar fordítás már átkerült az `epkeb` ágra.

## Upstream verzió

Állapot 2026-10-27-én (`upstream` = `github.com/CSCfi/rems`):

| | Verzió |
|---|---|
| Próbarendszer (rems.analyst.hu) | v2.39 + 2 commit (2026-01) |
| Fork `master` | v2.39.1 és v2.40 közötti fejlesztői állapot |
| Upstream `master` | v2.40 + néhány commit |
| Legújabb kiadás | **v2.41** (2026-10-09), a `release-v2.41` ágon |

A v2.41 visszavonja a v2.40 változásait (katalógushierarchia, új keresési mezők, `:external-frontend-url`, PostgreSQL 18 támogatás), mert a katalógusoldal lelassult tőlük; funkcionálisan azonos a v2.39.1-gyel. Az upstream `master` viszont továbbra is a v2.40-es kódot viszi.

Döntés: az `epkeb` ág a **v2.41** tagről indul. A próbarendszeren futó v2.39-hez képest ez csak a v2.39.1 javításait hozza, így nincs kockázatos migráció. Ha a CSC a v2.40 funkcióit javítva újra kiadja, arra át lehet állni.

Frissítés menete: `git fetch upstream --tags`, majd az új kiadási tag beolvasztása az `epkeb` ágba.

## Magyar fordítás

- A konténer a `/opt/analyst.hu/rems/translations/hu.edn` fájlt használja (2026-09). Ez az EPKEB-re szabott változat („Kérelem”, „Kérelmező”, „Kutató”).
- A forrásfában (`sources/rems/resources/translations/hu.edn`, 2026-02) egy régebbi, általános változat van.
- Az `epkeb` ágon a `resources/translations/hu.edn` a telepített változat, javítva: a „Kérlemező” elírás javítva, a hiányzó `:t.actions/pre-actions-info` kulcs pótolva. A `test/clj/rems/test_locales.clj` a magyart is ellenőrzi (kulcsegyezés az angollal, és hogy a forráskódban használt kulcsok mind megvannak).
- Még nyitott: a resource/application fogalmak fordítása (lásd [rems-lekepezes.md](rems-lekepezes.md#fogalmak)).
- Amíg a próbarendszer konténere nem a forkból épül, a becsatolt `translations/hu.edn` felülírja a beépítettet; átálláskor a becsatolást meg kell szüntetni vagy a repóbeli fájllal szinkronban tartani.

## Hibák

| Hiba | Hatás | Javítás |
|---|---|---|
| A `config.edn` `:email-from` kulcsot használ, a REMS viszont `:mail-from` kulcsot olvas (`src/clj/rems/email/core.clj`) | Az e-mailek feladója nincs beállítva; a levelek valószínűleg nem mennek ki vagy elutasítják őket | `:email-from` → `:mail-from` a `config.edn`-ben |
| A `sources/rems/dev-config.edn` titkosítatlan adatbázisjelszót tartalmaz | Jelszó a forrásfában | A jelszó környezeti változóba; ha a fájl valaha bárhová felkerült, a jelszót cserélni |
