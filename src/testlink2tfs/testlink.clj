(ns testlink2tfs.testlink
  (:require [necessary-evil.core    :as xmlrpc]
            [clj-yaml.core          :as yaml  ]
            [net.cgrand.enlive-html :as html  ])
  (:import java.io.StringReader))


(defrecord TestCase [tcid tcname tcsummary tcsteps tcexp tcimgs])

(defn load-settings [path]
  (yaml/parse-string (slurp path)))

(defn pull-test-cases [settings-path]
  (def settings (load-settings settings-path))
  (xmlrpc/call (-> settings :tl :url) :tl.getTestCasesForTestSuite
                 { :devKey       (get-in settings [:tl :devkey]) 
                   :testsuiteid  (get-in settings [:tl :tsid])
                   :details      "full" }))

(defn extract-images [prefix html-text]
  (if (not-empty html-text)
    (map #(str prefix (-> % :attrs :src)) (-> html-text java.io.StringReader. html/html-resource (html/select [:img])))))

(defn to-test-case [settings
                    {tlid      :id,
                     tlname    :name,
                     tlsummary :summary,
                     tlsteps   :steps,
                     tlexp     :expected_results}]
  (TestCase. tlid tlname tlsummary tlsteps tlexp (extract-images (-> settings :tl :www-prefix) tlsummary)))  ; attachments are not used in our testlink cases at the moment...

(defn get-test-cases [settings-path]
  (let [tl-cases (pull-test-cases settings-path)
        settings (load-settings settings-path)
        tsid (str (get-in settings [:tl :tsid]))
        non-recursive (get-in settings [:tl :norec])]
    (map #(to-test-case settings %)
         (if non-recursive (filter #(= (:parent_id %) tsid) tl-cases) tl-cases))))
