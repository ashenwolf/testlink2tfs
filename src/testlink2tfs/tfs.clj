(ns testlink2tfs.tfs
  (:require [clj-yaml.core          :as yaml]
            [clojure.java.io        :as io]
            [net.cgrand.enlive-html :as html]
            [clojure.data.xml       :as xml]
            [clojure.java.io        :as io])
  (:import testlink2tfs.testlink.TestCase
           java.io.File
           com.microsoft.tfs.core.TFSTeamProjectCollection
           com.microsoft.tfs.core.clients.workitem.WorkItem
           com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames
           com.microsoft.tfs.core.clients.workitem.files.AttachmentFactory
           com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials
           com.microsoft.tfs.core.util.URIUtils
           java.net.URLDecoder java.util.regex.Pattern))


(defn load-settings [path]
  (yaml/parse-string (slurp path)))

(defn connect-to-tfs-project [settings-path]
  (let [settings (load-settings settings-path)
        tfs      (TFSTeamProjectCollection.
                    (-> settings :tfs :url URIUtils/newURI)
                    (UsernamePasswordCredentials. (-> settings :tfs :login) (-> settings :tfs :password)))]
    (-> tfs .getWorkItemClient .getProjects (.get (-> settings :tfs :project)))))

(defn move-attachment [tfs-case tl-img]
  (let [filename-short (URLDecoder/decode (last (clojure.string/split tl-img #"/")))
        filename (str (System/getProperty "java.io.tmpdir") filename-short)]
    (with-open [output (io/output-stream filename)]
      (io/copy (io/input-stream tl-img) output))
    (def attachment (AttachmentFactory/newAttachment (File. filename) filename-short))
    (-> tfs-case .getAttachments (.add attachment))
    filename))
    

(defn update-summary [project wi-id summary]
  (let [wi (-> project .getWorkItemClient (.getWorkItemByID wi-id))
        attachments (map #(-> % .getURL .toString) (-> wi .getAttachments seq))]
    (def html-text (-> summary java.io.StringReader. html/html-resource))
    (defn match-url [url]
      (let [fname (URLDecoder/decode (last (clojure.string/split url #"/")))
            rx    (re-pattern (str "^.*" (Pattern/quote fname) "$"))]
        (some #(re-find rx (URLDecoder/decode %)) attachments)))
    (def tfs-summary (html/sniptest summary
       [:img] (fn [node] (update-in node [:attrs :src] #(match-url %)))))
    (-> wi .getFields (.getField CoreFieldReferenceNames/DESCRIPTION) (.setValue tfs-summary))
    (-> wi .save)))

(defn extract-steps [tlsteps tlexp]
  (let [html-steps (-> (if (nil? tlsteps) "" tlsteps) java.io.StringReader. html/html-resource)
        html-exp   (-> (if (nil? tlexp) "" tlexp) java.io.StringReader. html/html-resource)]
    (defn extractor [content]
      (map #(apply str (-> % (html/at [:*] html/unwrap))) (-> content (html/select #{[:li] [:p]}))))
    (concat (extractor html-steps) (extractor html-exp))))

(defn to-xml [steps]
  (defn make-xml-steps [steps]
    (let [id (atom 0)]
      (map (fn [step]
           (swap! id inc)
           (xml/element :step {:type "ActionStep" :id @id} 
             (xml/element :parameterizedString {} 
                          (xml/element :text {} step))
             (xml/element :parameterizedString {})
             (xml/element :description {}))) steps)))    
  
  (xml/emit-str (xml/element :steps {:last (count steps) :id 0} (make-xml-steps steps))))

(defn add-test-case [project tl-test-case]
  (let [tc-wit (-> project .getWorkItemTypes (.get "Test Case"))
        tfs-test-case (-> project .getWorkItemClient (.newWorkItem tc-wit))]
    ; set name
    (println "Processing test case #" (:tcid tl-test-case) (:tcname tl-test-case))
    (-> tfs-test-case (.setTitle (:tcname tl-test-case)))
    ; upload images
    (def temp-files (doall (map #(move-attachment tfs-test-case %) (:tcimgs tl-test-case))))
    ; add steps
    (-> tfs-test-case .getFields (.getField "Steps")
        (.setValue (to-xml (remove #(clojure.string/blank? %) (extract-steps (:tcsteps tl-test-case) (:tcexp tl-test-case))))))
    ; save test case
    (-> tfs-test-case .save)
    ; cleanup
    (doseq [file temp-files] (io/delete-file file))
    ; update summary
    (if-not (clojure.string/blank? (:tcsummary tl-test-case))
      (update-summary project (-> tfs-test-case .getID ) (:tcsummary tl-test-case)))
    ; return id
    (-> tfs-test-case .getID)))
