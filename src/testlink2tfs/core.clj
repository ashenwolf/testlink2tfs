(ns testlink2tfs.core
  (:require [testlink2tfs.testlink :as tl ]
            [testlink2tfs.tfs      :as tfs])
  (:gen-class))


(defn test-migration [settings cases-num]
  (let [project       (tfs/connect-to-tfs-project settings)
        tl-test-cases (take cases-num (tl/get-test-cases settings))]
    (map #(tfs/add-test-case project %) tl-test-cases)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (println "Hello, World!"))


(test-migration "tl2tfs.conf.yaml" 5)
