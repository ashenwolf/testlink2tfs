(ns testlink2tfs.tfs
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [net.cgrand.enlive-html :as html])
  (:import testlink2tfs.testlink.TestCase
           java.io.File
           com.microsoft.tfs.core.TFSTeamProjectCollection
           com.microsoft.tfs.core.clients.workitem.WorkItem
           com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames
           com.microsoft.tfs.core.clients.workitem.files.AttachmentFactory
           com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials
           com.microsoft.tfs.core.util.URIUtils))


(defn load-settings [path]
  (yaml/parse-string (slurp path)))

(defn connect-to-tfs-project [settings-path]
  (let [settings (load-settings settings-path)
        tfs      (TFSTeamProjectCollection.
                    (-> settings :tfs :url URIUtils/newURI)
                    (UsernamePasswordCredentials. (-> settings :tfs :login) (-> settings :tfs :password)))]
    (-> tfs .getWorkItemClient .getProjects (.get (-> settings :tfs :project)))))

(defn move-attachment [tfs-case tl-img]
  (let [settings (load-settings "tl2tfs.conf.yaml")
        filename-short (last (clojure.string/split tl-img #"/"))
        filename (clojure.string/join ["tmp/", filename-short])]
    (with-open [output (io/output-stream filename)]
      (io/copy (io/input-stream (clojure.string/join [(-> settings :tl :www-prefix) tl-img])) output))
    (def attachment (AttachmentFactory/newAttachment (File. filename) filename-short))
    (-> tfs-case .getAttachments (.add attachment))))

(defn update-summary [project wi-id summary]
  (let [wi (-> project .getWorkItemClient (.getWorkItemByID wi-id))
        attachments (map #(-> % .getURL .toString) (-> wi .getAttachments seq))]
    (def html-text (-> summary java.io.StringReader. html/html-resource))
    (defn match-url [url]
      (let [fname (last (clojure.string/split url #"/"))
            rx    (re-pattern (clojure.string/join ["^.*" fname "$"]))]
        (some #(re-find rx %) attachments)))
    (def tfs-summary (html/sniptest summary
       [:img] (fn [node] (update-in node [:attrs :src] #(match-url %)))))
    (-> wi .getFields (.getField CoreFieldReferenceNames/DESCRIPTION) (.setValue tfs-summary))
    (-> wi .save)
    ;(reduce #(apply clojure.string/replace %1 %2) summary (map vector tl-images attachments))
    ))


(defn add-test-case [project tl-test-case]
  (let [tc-wit (-> project .getWorkItemTypes (.get "Test Case"))
        tfs-test-case (-> project .getWorkItemClient (.newWorkItem tc-wit))]
    ; set name
    (-> tfs-test-case (.setTitle (:tcname tl-test-case)))
    ; save test case
    ; upload images
    (doseq [img (:tcimgs tl-test-case)] (move-attachment tfs-test-case img))
    ; set description
    ;(-> tfs-test-case .getFields (.getField CoreFieldReferenceNames/DESCRIPTION) (.setValue (:tcsummary tl-test-case)))
    ; save test case
    (-> tfs-test-case .save)
    ; update summary
    (update-summary project (-> tfs-test-case .getID ) (:tcsummary tl-test-case))
    ; return id
    (-> tfs-test-case .getID)))
