(ns testlink2tfs.testlink
  (:require [necessary-evil.core    :as xmlrpc]
            [clj-yaml.core          :as yaml  ]
            [net.cgrand.enlive-html :as html  ])
  (:import java.io.StringReader))


(defrecord TestCase [tcid tcname tcsummary tcsteps tcexp tcimgs tcatts])

(defn load-settings [path]
  (yaml/parse-string (slurp path)))

(defn pull-test-cases [settings-path]
  (def settings (load-settings settings-path))
  (xmlrpc/call (get-in settings [:tl :url]) :tl.getTestCasesForTestSuite
                 { :devKey       (get-in settings [:tl :devkey]) 
                   :testsuiteid  (get-in settings [:tl :tsid])
                   :details      "full" }))

(defn extract-images [html-text]
  (if (not-empty html-text)
    (map #(-> % :attrs :src) (-> html-text java.io.StringReader. html/html-resource (html/select [:img])))))

(defn to-test-case [{tlid      :id,
                     tlname    :name,
                     tlsummary :summary,
                     tlsteps   :steps,
                     tlexp     :expected_results}]
  (TestCase. tlid tlname tlsummary tlsteps tlexp (extract-images tlsummary) []))  ; attachments are not used in our testlink cases at the moment...

(defn get-test-cases [settings]
  (map to-test-case (pull-test-cases settings)))
