"""Generates the EPKEB application forms (normal and abbreviated procedure)
for the REMS form API. See docs/epkeb/urlap-terv.md.

    python3 urlap.py normal > normal.json
    python3 urlap.py abbreviated > abbreviated.json
"""
import json
import sys

def t(hu, en):
    return {"hu": hu, "en": en}

def opts(*pairs):
    return [{"key": k, "label": t(hu, en)} for k, hu, en in pairs]

def only_if(field_id, *values):
    return {"visibility/type": "only-if",
            "visibility/field": {"field/id": field_id},
            "visibility/values": list(values)}

def field(fid, ftype, title, optional=False, **extra):
    f = {"field/id": fid, "field/type": ftype, "field/title": title, "field/optional": optional}
    for k, v in extra.items():
        f["field/" + k.replace("_", "-")] = v
    return f

def header(fid, hu, en):
    return {"field/id": fid, "field/type": "header", "field/title": t(hu, en), "field/optional": True}

RISKS = [("a", "Kellemetlen ingerekkel vagy kellemetlen helyzetekkel", "unpleasant stimuli or unpleasant situations"),
         ("b", "Invazív eljárással vagy fájdalomkeltéssel", "invasive procedures or eliciting pain"),
         ("c", "Víz, élelem, alvás stb. megvonásával", "deprivation of water, food, sleep, etc."),
         ("d", "Gyógyszer adásával", "drug administration"),
         ("e", "Bármilyen eljárással, amely a résztvevőnek kárt okozhat", "any procedure that could harm the participant"),
         ("f", "Mentális betegek vagy veszélyeztetett személyek vizsgálatával", "research on mental patients or at-risk individuals"),
         ("g", "A résztvevők félrevezetésével vagy megtévesztésével", "misleading or deceiving the participants"),
         ("h", "A kutatás természetére vonatkozó információ visszatartásával", "withholding information about the nature of the research"),
         ("none", "Egyikkel sem", "None of the above")]

def form(abbreviated):
    fields = [header("h1", "1. A kérelem", "1. The application"),
              field("language", "option", t("A kérelem nyelve", "Language of the application"),
                    options=opts(("hu", "magyar", "Hungarian"), ("en", "angol", "English")),
                    info_text=t("Az eljárás, a bírálat és az engedély ezen a nyelven folyik.",
                                "The procedure, the review and the approval are in this language.")),
              field("department", "text", t("Tanszék / kutatócsoport", "Department / research group"), max_length=200)]
    if abbreviated:
        fields += [field("original-approval", "text", t("Az eredeti engedély(ek) száma", "Reference number(s) of the original approval(s)"),
                         max_length=100, placeholder=t("pl. 2025-052", "e.g. 2025-052")),
                   field("changes", "texta", t("Miben tér el a kutatás az eredetitől?", "How does the research differ from the original?"),
                         max_length=4000)]
    fields += [header("h2", "2. Kérelmező és felügyelő", "2. Applicant and supervisor"),
               field("position", "option", t("A kérelmező beosztása", "Position of the applicant"),
                     options=opts(("student", "hallgató (BA/MA)", "student (BA/MA)"),
                                  ("phd-student", "doktorandusz", "doctoral student"),
                                  ("faculty", "oktató, kutató", "faculty member, researcher"),
                                  ("other", "egyéb", "other"))),
               field("degree", "option", t("A kérelmező tudományos fokozata", "Degree of the applicant"),
                     options=opts(("none", "nincs (pl. MA)", "none (e.g. MA)"),
                                  ("phd", "PhD vagy DLA", "PhD or DLA"),
                                  ("dsc", "DSc", "DSc"),
                                  ("other", "egyéb", "other"))),
               field("supervisor-note", "label",
                     t("Fokozat nélküli kérelmezőnél felügyelő szükséges. Hívja meg a „Kérelmezők” dobozban a „Felügyelő meghívása” gombbal: a kérelem csak az ő jóváhagyása után nyújtható be.",
                       "An applicant without a degree needs a supervisor. Invite the supervisor with \"Invite supervisor\" in the Applicants box: the application can be submitted only after the supervisor's approval."),
                     optional=True, visibility=only_if("degree", "none")),
               header("h3", "3. A kutatás", "3. The research"),
               field("title", "description", t("A kutatás címe", "Title of the research"), max_length=300,
                     info_text=t("Ez lesz a kérelem címe, és ez szerepel az engedélyen is.", "This is the title of the application and of the approval.")),
               field("aim", "texta", t("A kutatás célja (legfeljebb 30 szó)", "Aims of the research (30 words max)"), max_length=300),
               field("start", "text", t("A kutatás várható kezdete", "Expected commencement of the research"), optional=True,
                     max_length=100, placeholder=t("pl. 2026. november", "e.g. November 2026")),
               field("location", "text", t("A vizsgálat helyszíne", "Location of the research"), max_length=300),
               field("summary", "texta", t("A vizsgálat rövid leírása (legfeljebb egy oldal)", "Brief description of the research (one page max)"),
                     max_length=6000, info_text=t("Háttér, kutatási kérdések, hipotézisek, elrendezés.", "Background, research questions, hypotheses, design.")),
               field("preregistration", "text", t("Előregisztráció linkje", "Pre-registration link"), optional=True, max_length=500),
               header("h4", "4. Résztvevők", "4. Participants"),
               field("age-groups", "multiselect", t("A résztvevők korcsoportja", "Age groups of the participants"),
                     options=opts(("under-3", "3 év alatti", "below 3 years"),
                                  ("3-14", "3–14 év", "3 to 14 years"),
                                  ("14-18", "14–18 év", "14 to 18 years"),
                                  ("adult", "felnőtt", "adult")),
                     info_text=t("Ebből következik, milyen tájékoztató és beleegyező kell (6. szakasz).",
                                 "This determines the information sheets and consent forms needed (section 6).")),
               field("age", "text", t("A résztvevők életkora", "Age range of the participants"), max_length=200,
                     placeholder=t("pl. 4–8 év", "e.g. 4 to 8 years")),
               field("selection", "texta", t("A résztvevők kiválasztásának módja", "Participant selection criteria"), max_length=2000),
               field("recruitment", "texta", t("Hogyan toborozza a résztvevőket?", "How do you recruit the participants?"), max_length=2000),
               field("recruitment-material", "attachment", t("Toborzó anyagok (hirdetés, levél stb.)", "Recruitment material (flyers, letters, etc.)"), optional=True),
               field("incentive", "option", t("Ad-e ösztönzést vagy fizetséget a részvételért?", "Do you offer any incentives for participation?"),
                     options=opts(("yes", "igen", "yes"), ("no", "nem", "no"))),
               field("incentive-description", "texta", t("Az ösztönzés leírása", "Description of the incentives"), max_length=2000,
                     visibility=only_if("incentive", "yes")),
               header("h5", "5. Módszerek és kockázatok", "5. Methods and risks"),
               field("equipment", "texta", t("Milyen berendezéseket és eszközöket alkalmaz?", "What kind of equipment do you use?"), max_length=2000),
               field("equipment-documentation", "attachment", t("Az eszközök dokumentációja", "Specification of the equipment"), optional=True,
                     info_text=t("Nem kell, ha egy korábbi kérelemben már jóváhagyták.", "Not needed if approved in an earlier application.")),
               field("instruments", "table", t("Kérdőívek, tesztek, interjútechnikák", "Questionnaires, tests, interviews"),
                     columns=[{"key": "name", "label": t("Megnevezés", "Name")},
                              {"key": "kind", "label": t("Sztenderd vagy saját", "Standard or own")},
                              {"key": "link", "label": t("Hivatkozás (DOI, link)", "Reference (DOI, link)")}]),
               field("instruments-description", "texta", t("Az eszközök leírása (pontozás, alskálák, hivatkozások)", "Description of the instruments (scoring, subscales, references)"),
                     optional=True, max_length=6000),
               field("instruments-copy", "attachment", t("A kérdőívek, tesztek másolata", "Copy of the questionnaires and tests"), optional=True,
                     info_text=t("Saját, még nem jóváhagyott eszköznél kötelező.", "Required for own instruments not yet approved by the committee.")),
               field("risks", "multiselect", t("Jár-e a vizsgálat…", "Does the procedure involve…"), options=opts(*RISKS),
                     info_text=t("Jelölje meg, ami igaz; ha egyik sem, az „Egyikkel sem” lehetőséget. Az igen válasz nem zárja ki a jóváhagyást.",
                                 "Mark what applies, or \"None of the above\". Answering yes does not rule out the approval.")),
               field("harm-prevention", "texta", t("Hogyan biztosítja a résztvevők biztonságát és jóllétét?", "How do you ensure the participants' safety and well-being?"),
                     max_length=4000, visibility=only_if("risks", *[k for k, _, _ in RISKS if k != "none"])),
               field("anxiety", "texta", t("Hogyan csökkenti a vizsgálat által akaratlanul keltett szorongást?", "How do you reduce the anxiety inadvertently generated by the research?"),
                     max_length=2000),
               header("h6", "6. Beleegyezés és adatkezelés", "6. Consent and data protection"),
               field("consent-parent", "attachment", t("Szülői tájékoztató és beleegyező nyilatkozat", "Information sheet and consent form for the parents"),
                     visibility=only_if("age-groups", "under-3", "3-14", "14-18")),
               field("consent-child", "attachment", t("A gyermeknek szóló tájékoztatás", "Information for the child"),
                     visibility=only_if("age-groups", "3-14", "14-18"),
                     info_text=t("3–14 évesnél a szóbeli vagy írásbeli tájékoztatás szövege, 14–18 évesnél a tájékoztató és a beleegyező nyilatkozat.",
                                 "For 3 to 14 years the content of the verbal or written information, for 14 to 18 years the information sheet and the consent form.")),
               field("opt-out", "option", t("Passzív (opt-out) szülői beleegyezéssel dolgozik?", "Do you use an opt-out procedure with the parents?"),
                     options=opts(("yes", "igen", "yes"), ("no", "nem", "no")),
                     visibility=only_if("age-groups", "14-18"),
                     info_text=t("Csak akkor lehetséges, ha a gyermek neve nem kerül rögzítésre.", "Only possible if the child's name is not recorded.")),
               field("consent-adult", "attachment", t("Tájékoztató és beleegyező nyilatkozat a felnőtt résztvevőknek", "Information sheet and consent form for adult participants"),
                     visibility=only_if("age-groups", "adult")),
               field("confidentiality", "texta", t("Hogyan biztosítja az adatok bizalmas kezelését, és hogy a résztvevők ne legyenek azonosíthatók?",
                                                   "How do you ensure that the data remain confidential and the participants are not identifiable?"),
                     max_length=4000),
               field("personal-data", "option", t("Gyűjt-e személyes adatot (e-mail-cím, vagy legalább két személyes adat, pl. név, születési idő)?",
                                                  "Do you collect personal data (an e-mail address, or at least two personal data, e.g. name, date of birth)?"),
                     options=opts(("yes", "igen", "yes"), ("no", "nem", "no")),
                     info_text=t("Ha igen, sorolja fel őket az előző válaszban, és a tájékoztatóban hivatkozzon az intézménye adatkezelési szabályzatára (GDPR).",
                                 "If yes, list them in the previous answer, and state in the information sheet that you follow the data protection regulations of your institution (GDPR).")),
               header("h7", "7. Egyéb", "7. Other"),
               field("other-ethics", "texta", t("Van-e a kutatásnak más, etikai szempontból fontos vonatkozása?", "Is there any further aspect of the research important for ethical consideration?"),
                     optional=True, max_length=4000)]
    return fields

if __name__ == "__main__":
    abbreviated = sys.argv[1:] == ["abbreviated"]
    name = "EPKEB kérelem – rövidített eljárás" if abbreviated else "EPKEB kérelem – normál eljárás"
    print(json.dumps({"organization": {"organization/id": "EPKEB"},
                      "form/internal-name": name,
                      "form/external-title": t("Etikai kérelem", "Application for ethical approval"),
                      "form/fields": form(abbreviated)},
                     ensure_ascii=False, indent=1))
