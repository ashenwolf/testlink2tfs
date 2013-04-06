(ns testlink2tfs.core
  (:require [testlink2tfs.testlink :as tl ]
            [testlink2tfs.tfs      :as tfs])
  (:gen-class))


(defn test-migration
  ([settings] (test-migration settings nil))
  ([settings cases-num]
     (let [project       (tfs/connect-to-tfs-project settings)
           all-cases     (tl/get-test-cases settings)
           tl-test-cases (if (nil? cases-num) all-cases (take (Integer. cases-num) all-cases))]
     (def cases-num (count (map #(tfs/add-test-case project %) tl-test-cases)))
     (println "Migration completed...")
     (println cases-num "test case(s) migrated"))))

(defn -main
  "tfs2testlink <config file> <case limit>"
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  
  (if (<= 1 (count args) 2)
      (apply test-migration args)
      (println "tfs2testlink <config file> <case limit>")))
