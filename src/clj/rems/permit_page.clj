(ns rems.permit-page
  "EPKEB: the public verification page and the PDF of a permit, available
  without logging in to anyone who has the link. See
  docs/epkeb/virtualis-engedely.md"
  (:require [clj-pdf.core :refer [pdf]]
            [clj-time.core :as time]
            [clojure.string :as str]
            [compojure.core :refer [GET routes]]
            [hiccup2.core :as h]
            [rems.config :refer [env]]
            [rems.db.permits]
            [rems.service.permits :as permits]
            [ring.util.http-response :refer [content-type header not-found ok]])
  (:import [com.google.zxing BarcodeFormat EncodeHintType]
           [com.google.zxing.qrcode QRCodeWriter]
           [java.awt.image BufferedImage]
           [java.io ByteArrayOutputStream]))

(def ^:private texts
  {:hu {:committee "EGYESÍTETT PSZICHOLÓGIAI KUTATÁSETIKAI BIZOTTSÁG"
        :title "Etikai engedély"
        :title-abbreviated "Etikai engedély (rövidített eljárás)"
        :intro "Az Egyesített Pszichológiai Kutatásetikai Bizottság (EPKEB) az alábbi kutatási tervet etikai szempontból értékelte, és annak végrehajtásához %s megbízásából hozzájárul."
        :researcher "Kutató"
        :supervisor "A kutatás felügyelője"
        :project-title "Kutatás címe"
        :number "Referenciaszám"
        :original "eredeti engedély: %s"
        :date "Engedélyezés dátuma"
        :approver "EPKEB társelnök"
        :valid "ÉRVÉNYES"
        :revoked "VISSZAVONVA"
        :revoked-on "Visszavonva: %s"
        :verify "Az engedély érvényessége ellenőrizhető:"
        :download "PDF letöltése"
        :other-language "English"
        :not-found "Nincs ilyen engedély. Ellenőrizze a linket."
        :months ["január" "február" "március" "április" "május" "június" "július" "augusztus" "szeptember" "október" "november" "december"]}
   :en {:committee "UNITED ETHICAL REVIEW COMMITTEE FOR RESEARCH IN PSYCHOLOGY (EPKEB)"
        :title "Ethics Approval"
        :title-abbreviated "Ethics Approval (expedited procedure)"
        :intro "The United Ethical Review Committee for Research in Psychology (EPKEB) has evaluated the ethical aspects of the following research proposal, and has approved it on behalf of %s."
        :researcher "Researcher"
        :supervisor "Supervisor"
        :project-title "Project title"
        :number "Reference number"
        :original "original approval: %s"
        :date "Date of approval"
        :approver "Co-chair of EPKEB"
        :valid "VALID"
        :revoked "REVOKED"
        :revoked-on "Revoked: %s"
        :verify "The validity of the approval can be verified at:"
        :download "Download PDF"
        :other-language "Magyar"
        :not-found "There is no such approval. Please check the link."
        :months ["January" "February" "March" "April" "May" "June" "July" "August" "September" "October" "November" "December"]}})

(defn- language [permit requested]
  (or (#{:hu :en} (some-> requested keyword))
      (#{:hu :en} (some-> (:permit/language permit) keyword))
      :hu))

(defn- format-date [lang t]
  (when t
    (let [t (time/to-time-zone t (time/time-zone-for-id (:deadline-reminders-time-zone env "Europe/Budapest")))
          month (get-in texts [lang :months (dec (time/month t))])]
      (case lang
        :hu (str (time/year t) ". " month " " (time/day t) ".")
        :en (str (time/day t) " " month " " (time/year t))))))

(defn- localized [m lang]
  (or (get m lang) (get m :hu) (get m :en) (first (vals m))))

(defn- institution [permit lang]
  (->> [(localized (:permit/institution permit) lang) (:permit/department permit)]
       (remove str/blank?)
       (str/join ", ")))

(defn- fields
  "The rows of the permit: label and value."
  [permit lang]
  (let [t (texts lang)]
    (remove (comp str/blank? second)
            [[(:researcher t) (permits/researchers-text permit)]
             [(:supervisor t) (:permit/supervisor permit)]
             [(:project-title t) (:permit/title permit)]
             [(:number t) (str (:permit/number permit)
                               (when-some [original (:permit/original-approvals permit)]
                                 (str " (" (format (:original t) original) ")")))]
             [(:date t) (format-date lang (:permit/issued permit))]])))

(defn- title [permit lang]
  (if (= "abbreviated" (:permit/type permit))
    (get-in texts [lang :title-abbreviated])
    (get-in texts [lang :title])))

;;; QR code

(defn- qr-image ^BufferedImage [^String url size]
  (let [matrix (.encode (QRCodeWriter.) url BarcodeFormat/QR_CODE size size {EncodeHintType/MARGIN (int 1)})
        image (BufferedImage. size size BufferedImage/TYPE_INT_RGB)]
    (doseq [x (range size)
            y (range size)]
      (.setRGB image x y (if (.get matrix x y) 0x000000 0xFFFFFF)))
    image))

;;; PDF

(defn- pdf-font []
  (when-some [font (:permit-pdf-font env)]
    {:encoding :unicode :ttf-name font}))

(defn permit-pdf-bytes [permit lang]
  (let [t (texts lang)
        url (permits/permit-url permit)
        font (pdf-font)
        out (ByteArrayOutputStream.)]
    (pdf (into [] (remove nil?) [(cond-> {:title (str (:permit/number permit) " – " (title permit lang))
                   :size :a4
                   :left-margin 60
                   :right-margin 60
                   :top-margin 60
                   :bottom-margin 60}
            font (assoc :font font))
          [:paragraph {:align :center :style :bold :size 13} (:committee t)]
          [:spacer 2]
          [:paragraph {:align :center :style :bold :size 18} (title permit lang)]
          [:spacer 1]
          (when (:permit/revoked permit)
            [:paragraph {:align :center :style :bold :size 16 :color [205 33 79]}
             (str (:revoked t) " – " (format (:revoked-on t) (format-date lang (:permit/revoked permit))))])
          [:spacer 1]
          [:paragraph {:size 11 :leading 16} (format (:intro t) (institution permit lang))]
          [:spacer 1]
          (into [:table {:widths [30 70] :border false :cell-border false :padding 4}]
                (for [[label value] (fields permit lang)]
                  [[:cell [:phrase {:style :bold :size 11} label]]
                   [:cell [:phrase {:size 11} value]]]))
          [:spacer 2]
          [:paragraph {:size 11} (str (:approver t) " – " (:permit/approver permit))]
          [:spacer 2]
          [:paragraph {:size 9 :color [91 107 128]} (:verify t)]
          [:paragraph {:size 9 :color [91 107 128]} url]
          [:image {:width 90 :height 90} (qr-image url 300)]])
         out)
    (.toByteArray out)))

;;; HTML

(defn- page [lang title-text body]
  (str
   "<!DOCTYPE html>"
   (h/html
    [:html {:lang (name lang)}
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:meta {:name "robots" :content "noindex, nofollow"}]
      [:title title-text]
      [:style "body{margin:0;background:#F4F6FA;color:#16233A;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif}
.wrap{max-width:46rem;margin:0 auto;padding:2rem 1rem}
.card{background:#fff;border:1px solid #E3E8EF;border-radius:12px;padding:2rem;box-shadow:0 4px 16px rgba(22,35,58,.06)}
.committee{font-size:.8rem;letter-spacing:.06em;font-weight:700;color:#5B6B80;text-align:center}
h1{text-align:center;font-size:1.8rem;margin:.6rem 0 1.2rem}
.status{text-align:center;font-weight:800;font-size:1.3rem;padding:.6rem;border-radius:10px;margin:0 0 1.5rem}
.valid{background:#E5F4EA;color:#1E5E3A}.revoked{background:#FBE9EE;color:#8E1236}
dl{display:grid;grid-template-columns:12rem 1fr;gap:.6rem 1rem;margin:1.5rem 0}
dt{font-weight:600;color:#5B6B80}dd{margin:0}
.actions{display:flex;justify-content:space-between;align-items:center;margin-top:1.5rem;gap:1rem;flex-wrap:wrap}
.btn{display:inline-block;background:#1F6FD1;color:#fff;text-decoration:none;padding:.55rem 1.1rem;border-radius:8px;font-weight:600}
a{color:#1F6FD1}
@media (max-width:600px){dl{grid-template-columns:1fr}}"]]
     [:body [:div.wrap body]]])))

(defn- permit-page [permit lang]
  (let [t (texts lang)
        token (:permit/token permit)
        other (if (= lang :hu) :en :hu)]
    (page lang
          (str (:permit/number permit) " – " (title permit lang))
          [:div.card
           [:div.committee (:committee t)]
           [:h1 (title permit lang)]
           (if (:permit/revoked permit)
             [:div.status.revoked (:revoked t) " – " (format (:revoked-on t) (format-date lang (:permit/revoked permit)))]
             [:div.status.valid (:valid t)])
           [:p (format (:intro t) (institution permit lang))]
           (into [:dl] (mapcat (fn [[label value]] [[:dt label] [:dd value]]) (fields permit lang)))
           [:p (:approver t) " – " (:permit/approver permit)]
           [:div.actions
            [:a.btn {:href (str "/permit/" token "/pdf?lang=" (name lang))} (:download t)]
            [:a {:href (str "/permit/" token "?lang=" (name other))} (get-in texts [lang :other-language])]]])))

(defn- not-found-page [lang]
  (-> (not-found (page lang "EPKEB" [:div.card [:p (get-in texts [lang :not-found])]]))
      (content-type "text/html; charset=utf-8")))

(defn permit-routes []
  (routes
   (GET "/permit/:token" [token lang]
     (if-some [permit (when (:enable-permits env) (rems.db.permits/get-permit-by-token token))]
       (-> (ok (permit-page permit (language permit lang)))
           (content-type "text/html; charset=utf-8")
           (header "X-Robots-Tag" "noindex, nofollow"))
       (not-found-page (language nil lang))))
   (GET "/permit/:token/pdf" [token lang]
     (if-some [permit (when (:enable-permits env) (rems.db.permits/get-permit-by-token token))]
       (-> (ok (java.io.ByteArrayInputStream. (permit-pdf-bytes permit (language permit lang))))
           (content-type "application/pdf")
           (header "Content-Disposition" (str "inline; filename=\"EPKEB-" (:permit/number permit) ".pdf\""))
           (header "X-Robots-Tag" "noindex, nofollow"))
       (not-found-page (language nil lang))))))
