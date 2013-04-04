(defproject testlink2tfs "0.1.0-SNAPSHOT"
  :description "A tool to migrate test cases from Testlink to TFS"
  :url "https://github.com/ashenwolf/testlink2tfs"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-yaml "0.4.0"]
                 [necessary-evil "2.0.0"]
                 [org.clojure/data.xml "0.0.7"]
                 [enlive "1.1.1"]
                 [com.microsoft.tfs.sdk "11.0.0"]
                 [commons-lang "2.4"]]
  :jvm-opts ["-Dcom.microsoft.tfs.jni.native.base-directory=redist\\native"] 
  :main testlink2tfs.core)
