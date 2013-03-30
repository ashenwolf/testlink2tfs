(ns testlink2tfs.tfs
  (:require [clj-yaml.core :as yaml])
  (:import testlink2tfs.testlink.TestCase
           com.microsoft.tfs.core.TFSTeamProjectCollection
           com.microsoft.tfs.core.clients.workitem.WorkItem
           com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames
           com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials
           com.microsoft.tfs.core.util.URIUtils))


(defn load-settings [path]
  (yaml/parse-string (slurp path)))


(defn connect-to-tfs-project [settings-path]
  (let [settings (load-settings settings-path)
        tfs      (TFSTeamProjectCollection.
                    (-> settings :tfs :url URIUtils/newURI)
                    (UsernamePasswordCredentials. (-> settings :tfs :login) (-> settings :tfs :password))
                  )]
    (-> tfs .getWorkItemClient .getProjects (.get (-> settings :tfs :project)))
  ))

(defn add-test-case [project tl-test-case]
  (let [tc-wit (-> project .getWorkItemTypes (.get "Test Case"))
        tfs-test-case (-> project .getWorkItemClient (.newWorkItem tc-wit))]
          ; set name
          (-> tfs-test-case (.setTitle (:tcname tl-test-case)))
          ; set description
          (-> tfs-test-case .getFields (.getField CoreFieldReferenceNames/DESCRIPTION) (.setValue (:tcsummary tl-test-case)))
          ; save test case
          (-> tfs-test-case .save)
          ; return id
          (-> tfs-test-case .getID)))
