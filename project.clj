(defproject chowder/falcon "0.1.0-SNAPSHOT"
  :description "Deployment utility"

  :plugins
  [[lein-cljsbuild "1.0.6"]]

  :dependencies
  [[org.clojure/clojure "1.7.0"]
   [org.clojure/clojurescript "1.7.145"]
   [org.clojure/core.async "0.2.371"]
   [org.clojure/tools.cli "0.3.3"]
   [prismatic/schema "1.0.1"]]

  :clean-targets [:target-path "bin/falcon.js"]

  :cljsbuild
  {:builds
   [{:id "client"
     :source-paths ["src"]
     :compiler
     {:output-to "bin/falcon.js"
      :target :nodejs
      :optimizations :simple
      :pretty-print true
      :main "falcon.core"}}]})
