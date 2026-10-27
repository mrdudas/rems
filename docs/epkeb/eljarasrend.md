# Eljárásrend és nyilvántartás

## Az EPKEB eljárási rendje

Forrás: *Az Egyesített Pszichológiai Kutatásetikai Bizottság (EPKEB) eljárási rendje* (`EPKEB_eljaras.pdf`). A rendszert érintő szabályok:

### Bizottság

- A bizottság általában elektronikus levelezéssel dolgozik; évente legalább egy ülés (januárban), ahol a soros elnököt vagy a társelnököket választják. Az ülés megváltoztathatja az ügyrendet és az előzetes etikai eljárás menetét.
- Az ülésről nyilvános jegyzőkönyv készül, de az egyedi kérelmek ügye nem kerül bele.

### Kérelem

- Kérelmet a részt vevő tanszékek bármely oktatója, kutatója, alkalmazottja, doktorandusza vagy diákja kezdeményezhet.
- Nyelve magyar vagy angol; **az eljárás a beadás nyelvén folytatódik.**
- Fokozat nélküli kérelmezőnél kötelező a kutatást felügyelő oktató vagy kutató (szupervizor) megnevezése és **előzetes hozzájárulása**: ezzel vállalja, hogy a kérelmet olvasta, és a tartalmáért felelős.
- A kérelmet elektronikusan kell beadni, a mellékletekkel, **a tanszék/intézet által kijelölt kapcsolattartón keresztül**. A kapcsolattartó formailag ellenőrzi, és ha rendben van, továbbítja a soros elnöknek vagy az általa kijelölt titkárnak; ha nem, hiánypótlásra visszaadja. **A kapcsolattartó továbbítás nélkül visszautasíthatja** azokat a kérelmeket, amelyek vizsgálatára a bizottság nem illetékes.
- Az elnök a kérelmet **sorszámmal** látja el; az engedélyre később ezzel lehet hivatkozni.

### Bírálat

- Alapesetben a bizottság **két tagja** bírálja el, az elnök felkérésére.
- **A bíráló nem lehet abból az intézményből, ahonnan a kérelem érkezett.**
- A bíráló **külső szakértőt** is bevonhat, erről az elnököt értesíteni kell.
- Mérce: a Magyar Pszichológiai Társaság *Pszichológusok Etikai Kódexe*, a bizottság állásfoglalásai és további kutatásetikai elvek.
- A két bíráló külön-külön dönt:
  1. **Jóváhagyás** – az etikai engedély megadható;
  2. **Kiegészítés kérése** – további információt kér, vagy kisebb változtatást javasol, amellyel a kérelem elfogadható;
  3. **Elutasítás** – indoklással arról, mely etikai elvet sértené a kutatás; akkor is, ha az EPKEB nem felhatalmazott a kérelem elbírálására.

### Döntés

- **Ha bármelyik bíráló elutasítást javasol**, a kérelem a teljes bizottság elé kerül, minden tag megkapja. Az elutasítás fenntartásáról **szótöbbséggel** dönt; **szavazategyenlőségnél a soros elnök szavazata dönt**.
- **Ha bármelyik bíráló kiegészítést kér**, a kérelmet vissza kell küldeni a kérelmezőnek. A módosított kérelem beküldési határideje **egységesen 30 nap**; elmulasztása az eljárás felfüggesztését vonja maga után. A kérelmező új eljárást is indíthat a módosított kérelemmel; ekkor új bírálókat kell kijelölni, akiknek ismét 20 napjuk van. **A kiegészített kérelem nem kap új sorszámot, és az eredeti bírálókhoz kerül vissza.**
- **Ha mindkét bíráló jóváhagyja**, az elnök e-mailben értesíti a kérelmezőt, aki ennek alapján elkezdheti a kutatást. Az elnök által aláírt engedély elektronikus változatát a kérelmező megkapja. Az eredeti dokumentumot az elnök őrzi, és megbízatása végén archiválásra átadja (a dokumentum a HUN-REN TTK Kognitív Idegtudományi és Pszichológiai Intézetét, akkor még MTA néven, jelöli meg).

### Titoktartás

> A kutatásetikai vizsgálat kérelme bizalmasan kezelendő; tényéről és tartalmáról sem az elnök, sem a bizottsági tagok nem adhatnak információt a kérelmezőn kívül senkinek, **jóváhagyás esetén sem**. Külső szakértő bevonásakor őt is tájékoztatni kell a bizalmasságról. Az elutasítás és indoklása kizárólag a kérelmezőnek küldhető meg.

Ez a [virtuális engedély](virtualis-engedely.md#titoktartás) tervét érinti.

## A 2026-os nyilvántartás

Forrás: *Kérelmek_terhelés_KÖZÖS_2026.xlsx* (csak olvasva). Személyes adatokat ez a dokumentum nem tartalmaz.

### Munkalapok

| Munkalap | Tartalom |
|---|---|
| `2026_KÉRELMEK` | kérelmenként egy sor (lásd lent) |
| `2026_TERHELÉS` | intézményenként: illetékes társelnök, bírálók (státusz, kapcsolattartó-e, e-mail), kérelmek és bírálatok száma, utolsó felkérés, várható bírálat (+20 nap), bírálat/kérelem arány |
| `Intézményi terhelés számítás` | a 2025-ös kérelemszám alapján hány bíráló kellene intézményenként, hogy egy bírálóra kb. 6–7 kérelem jusson |
| `???` | szervezési teendők és döntések (pl. a bírálati terhelés: **arányos**) |
| `2025_kérelmek`, `2025_terhelés` | az előző év ugyanilyen bontásban |

### A kérelem-sor mezői (ezeket kell a rendszernek kiváltania)

| Szakasz | Mezők |
|---|---|
| Regisztráció (elnök) | státusz, illetékes társelnök, eljárásmód (normál/rövidített), kérelmező, kutatás címe, e-mail, intézmény, beérkezés, határidő (automatikus) |
| Bírálat (titkár) | bíráló 1 és 2, felkérés kiküldve, bírálatok dátuma, bírálatok eredménye (1. Elfogadva / 2. Módosítás / 3. Visszautasítva), első visszaküldés módosításra, utolsó módosítás elfogadása, módosítások elfogadva, visszajelzés a bírálóknak |
| Zárás (elnök) | jóváhagyás dátuma, engedélyszám (automatikus), kérelem zárása (megadva/megtagadva), megjegyzés |

Státuszok: 1. Bírálók kijelölve (elnök) → 2. Bírálók felkérve (titkár) → 3. Módosítás alatt (titkár) → 5. Kérelem lezárva (elnök).

### Számok (2026, a felmérés napjáig)

- **146 kérelem**: 132 normál, 14 rövidített. 103 lezárva, 26 bírálat alatt, 13 módosítás alatt.
- Bírálati eredmények: kb. 57% elfogadva, 29% módosítást kért, 1–2% visszautasítás, a többi még folyamatban.
- A legtöbb kérelem néhány intézményből jön: PTE BTK PI 43, DE BTK PI 17, ELTE TTK ET 14, SZTE BTK PI 13, HUN-REN TTK KPI 11.
- **Utolsó kiadott engedélyszám a táblázatban: 2026-228.**
- 18 tagintézmény, mindegyiknek **egy kapcsolattartója**, összesen kb. 36 bírálóval. A bírálói státuszok: *Bírál*, *Bírál\**, *Átmenetileg nem*, *Nem*.
- **Két társelnök**; minden tagintézmény az egyikhez tartozik. A legtöbb intézmény az egyik társelnökhöz tartozik, a két legnagyobb (PTE BTK PI, SZTE BTK PI) a másikhoz; ez a terhelést és az összeférhetetlenséget (a társelnök saját intézménye) is kezeli.

### Következmények a rendszerre

- A bírálói körhöz státusz kell (aktív / átmenetileg nem / nem).
- A bírálókiosztás tájékoztató adatai: idei bírálatok, folyamatban lévők, utolsó felkérés, intézményi arány (cél: a bírálatok száma arányos az intézmény kérelmeinek számával).
- Az illetékes társelnök tagintézményenként beállítható, és a saját intézmény kizárása rá is vonatkozik.
- A kezdő engedélyszám az élesítéskor a táblázat utolsó száma + 1.
