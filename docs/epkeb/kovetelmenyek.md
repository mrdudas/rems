# Követelmények

Forrás: *EPKEBADMIN felhasználói követelményjegyzék* (2024-05-19) és a hozzá fűzött kommentek. A kommenteket betűjelükkel ([a]–[t]) hivatkozzuk.

## Szerepek

| Szerep | Jogosultság |
|---|---|
| **Kapcsolattartó** (intézményi) | Feltölti vagy újra feltölti a saját kérvényeit, letölti a kiadott engedélyt. |
| **Elnök / társelnök** (lehet több) | Minden információhoz hozzáfér a weboldalon. |
| **Bíráló** | Letölti a neki kiosztott kérvényt, és űrlapon benyújtja a bírálatát. |

Kommentek:

- [a] Felmerült, hogy a rendszer a **titkár szerepét** szünteti meg, és ez az elnöki feladatot ijesztőbbé teheti („e-mail cunami”).
- [b] Válasz: az elnök ugyanazt végzi, mint eddig, csak weboldalon; az e-mail-terhelés szabályozható, akár nullára is, az elnök a weboldalon követi az ügyeket. Az elnok@epkeb.hu cím csak archívum.
- [c] Legyen letölthető az éppen érvényes **kérelemsablon magyarul és angolul**.
- [d] Az egész rendszer legyen **magyar és angol nyelvű**, nyelvváltással.

## Folyamat

1. **Beküldés.** A kapcsolattartó űrlapon beküldi a kérelmet, PDF-et csatol. Az illetékes elnök tájékoztató levelet kap (több elnöknél beállítható, melyik tagintézmény kérvényeiről melyik elnök kap értesítést). Az elnok@epkeb.hu fiók a melléklettel együtt archív másolatot kap.
   - JAVASLAT a dokumentumban: szkennelt, kézzel aláírt PDF helyett tisztán elektronikus kitöltés és AVDH-hitelesítés.
   - [e][f] Kommentek: AVDH helyett inkább útmutató link arról, hogyan lehet PDF-be aláírást illeszteni (Acrobat Reader, lefotózott vagy egérrel rajzolt aláírás). Ezt a szerző is támogatja.
2. **Formai ellenőrzés és bírálókijelölés.** Az elnök (bármelyik elnök) ellenőrzi a kérvényt, majd jóváhagyja a rendszer által javasolt bírálókat vagy újakat jelöl ki. A bírálók és az archív fiók értesítést kapnak. Formai hiba esetén az elnök azonnal visszaküldi a kapcsolattartónak, aki újra beküldheti.
   - [g][h] A formai ellenőrzés **a kapcsolattartó feladata és kötelessége**; ő az előszűrő, nem az elnök.
   - [i][j] Automatikus bírálókiosztó algoritmus helyett **kézi kiválasztás** (leterheltség, sürgősség, összeférhetetlenség). Segítségként a nevek mellett látszódjon a bíráló eddigi bírálatainak száma és az intézménye által beküldött kérvények száma.
   - [k][l] A bírálói felkérés értesítője inkább az érintett elnök címére menjen; cél, hogy mindenről legyen archívum, de senkit ne terheljen felesleges levél.
   - [m][n] Az elnöknek kell egy opció arra is, ha a kérelem **nem az EPKEB illetékessége** (ilyenkor nincs javítási lehetőség).
   - [o][p] Nyitott kérdés: egyenlő arányú vagy a tagintézmény által benyújtott kérvények számával súlyozott kiosztás. Korábbi megállapodás: minden bíráló havi egy bírálatot vállal. Kézi kiválasztásnál ezt az elnök mérlegeli, nem a rendszer.
3. **Bírálat.** A bíráló letölti a kérvényt, űrlapon leadja a bírálatot. Az elnök és az archív fiók értesítést kap (az archív fiók a pozitív bírálatot csatolva is).
4. **Mindkét bírálat után:**
   - **Mindkettő pozitív:** az elnök a weboldalon elkészíti az engedélyt (automatikus engedélyszámmal), hitelesíti, és a kapcsolattartó letölti.
     - [q][r] Az engedélyszámok nem akadhatnak össze; megoldás: kezdő sorszám megadása, onnan automatikus számozás.
     - [s][t] A teljesen automatikus aláírás feltörés esetén hamis engedélyekhez vezethet; a kommentelők a kézi, visszatöltős változatot tartották szimpatikusabbnak. **Felülírva:** 2026-10-27-i döntés szerint az engedély virtuális, linkkel ellenőrizhető (lásd [virtualis-engedely.md](virtualis-engedely.md)).
   - **Módosítást kér valamelyik bíráló:** a bírálati szöveg **névtelenül** a kapcsolattartóhoz kerül, aki a változásokat kiemelve újra feltölti; a folyamat a 3. ponttól ismétlődik, akár többször. Minden érintett értesítést kap.
   - **A bíráló a bizottság véleményét kérné:** a weboldaltól függetlenül fordul az elnökhöz vagy a bizottsághoz.
5. **Rövidített eljárás.** Nincs bírálókijelölés, az elnök egy személyben dönt. Eltérő engedélysablon, amelyen a korábbi kérvény engedélyszáma is szerepel.
