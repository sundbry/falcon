(ns falcon.cmd.deploy
  (:require
    [clojure.string :as string]
    [cljs.core.async :as async :refer [<!]]
    [cljs.pprint :refer [pprint]]
    [cljs.tools.cli :as cli]
    [schema.core :as S]
    [falcon.config :as config]
    [falcon.core :as core]
    [falcon.schema :as schema]
    [falcon.cmd.container :as container-ns]
    [falcon.cmd.service :as service-ns])
  (:require-macros
    [falcon.core :refer [require-arguments]]
    [cljs.core.async.macros :refer [go]]))

(S/defn create
  "Build and push container, then deploy replication controller."
  [{:keys [container-tag]:as opts} args]
  (require-arguments
    args
    (fn [service]
      (let [params {:service service}]
        (core/print-summary "Create deployment:" opts params)
        (go
          (let [container-tag (or container-tag (<! (container-ns/build opts [service])))]
            (<! (container-ns/push (assoc opts :container-tag container-tag) [service]))
            (<! (service-ns/create-rc opts [service container-tag]))))))))

(S/defn roll
  "Build and push container, then rolling deploy it's replication controller."
  [{:keys [container-tag] :as opts} args]
  (require-arguments
    args
    (fn [service old-controller-tag]
      (let [params {:service service
                    :old-controller-tag old-controller-tag}]
        (core/print-summary "Rolling deployment:" opts params)
        (go
          (let [container-tag (or container-tag (<! (container-ns/build opts [service])))
                opts (assoc opts :container-tag container-tag)]
            (<! (container-ns/push opts [service]))
            (<! (service-ns/rolling-update opts [service old-controller-tag container-tag]))))))))

(def cli
  {:doc "High-level deployment commands"
   :options [["-x" "--cluster <name>" "Cluster name"
              :default config/default-cluster]
             ["-t" "--git-tag <tag>" "Git tag"
              :default "master"]
             ["-c" "--container-tag <tag>" "Container tag"]
             ["-n" "--no-cache" "Disable docker cache"
              :default false]]
   :commands {"create" create
              "roll" roll}})