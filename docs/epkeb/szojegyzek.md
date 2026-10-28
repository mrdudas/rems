# Szójegyzék

A felület szóhasználata. A fordításokban (`resources/translations/*.edn`) ezek a kötelező kifejezések; a REMS-fogalmak megfeleltetése a [rems-lekepezes.md](rems-lekepezes.md#fogalmak)-ben van.

| REMS-fogalom | Magyar | Angol | Megjegyzés |
|---|---|---|---|
| application | **kérelem** | application | nem „jelentkezés”, nem „kérvény” |
| applicant (szerep) | **kapcsolattartó** | contact person | a tagintézmény kapcsolattartója, aki létrehozza és benyújtja a kérelmet |
| member | **kutató** | researcher | a kapcsolattartó által meghívott tag |
| applicant + members (együtt) | **kérelmezők**, kérelmező oldal | applicants | |
| invited member | meghívott kutató | invited researcher | |
| supervisor | **felügyelő** | supervisor | fokozat nélküli kutatónál kötelező |
| handler | **elnök** | chair | elnök, társelnök és az elnök nevében eljáró titkár |
| anonymized handler (`:t.roles/anonymous-handler`) | **Bizottság** | Committee | így látja a kérelmező a névtelenített elnököt és bírálót |
| reviewer | **bíráló** | reviewer | nem „véleményező” |
| review | **bírálat** | review | |
| decider | döntéshozó | decider | az EPKEB-ben nem használt |
| remark | megjegyzés | remark | |
| catalogue item, resource | **eljárás** (normál, rövidített) | procedure | nem „kérelem” |
| form | űrlap | form | |
| workflow | munkafolyamat | workflow | |
| organization | tagintézmény (a felületen: szervezet) | organization | |
| submit | benyújtás | submit | |
| return | visszaküldés kiegészítésre | return | |
| approve | jóváhagyás (engedély kiadása) | approve | |
| reject | elutasítás | reject | |

## Nyelvek

- A próbarendszeren és élesben a magyar és az angol felület használt (`:languages [:en :hu]`); ezekben a fenti szóhasználat kötelező.
- A finn, svéd és dán fordítás a gyári REMS-é. Az EPKEB-fejlesztések új kulcsai ezekben is megvannak (a `test-locales` teszt megköveteli), de a szóhasználatuk nincs EPKEB-re igazítva.
