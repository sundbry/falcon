(ns falcon.cluster
  (:require 
    [cljs.pprint :refer [pprint]]
    [cljs.core.async :as async :refer [<!]]
    [cljs.tools.cli :as cli]
    [schema.core :as S]
    [falcon.config :as config]
    [falcon.core :as core]
    [falcon.schema :as schema]
    [falcon.shell :as shell])
  (:require-macros
    [cljs.core.async.macros :refer [go]]))

(def ^:private cli-options
  [["-e" "--environment <env>" "Environment"
    :default "local"]
   ["-x" "--cluster <name>" "Cluster name"
    :default "main"]])

(defn- vagrant-dir
  []
  (str (.cwd js/process) "/cloud/cluster/kubernetes-vagrant-coreos-cluster"))

(S/defn ^:private vagrant-options
  [ccfg :- schema/ClusterConfig]
  {:cwd (vagrant-dir) 
   :env
   {"NODES" (str (get ccfg "nodes"))
    "CHANNEL" (str (get ccfg "coreos-channel"))
    "MASTER_MEM" (str (get ccfg "master-mem-mb"))
    "MASTER_CPUS" (str (get ccfg "master-cpus"))
    "NODE_MEM" (str (get ccfg "node-mem-mb"))
    "NODE_CPUS" (str (get ccfg "node-cpus"))
    "USE_KUBE_UI" (str (get ccfg "kube-ui"))
    "BASE_IP_ADDR" (str get ccfg "base-ip")
    }})

(defn- vagrant-cmd
  [ccfg cmd]
  (shell/passthru (concat ["vagrant"] cmd) (vagrant-options ccfg)))

(S/defn create
  "Create a new cluster"
  [ccfg :- schema/ClusterConfig args]
  (println "Creating cluster with configuration:")
  (pprint ccfg)
  (vagrant-cmd ccfg ["up"]))

(S/defn down
  "Bring a cluster offline"
  [ccfg :- schema/ClusterConfig args]
  (println "Bringing cluster offline:")
  (pprint ccfg)
  (go (<! (core/safe-wait))
      (<! (vagrant-cmd ccfg ["halt"]))))

(S/defn destroy
  "Destroy a cluster"
  [ccfg :- schema/ClusterConfig args]
  (println "About to DESTROY cluster with configuration:")
  (pprint ccfg)
  (go (<! (core/safe-wait))
      (<! (vagrant-cmd ccfg ["destroy"]))))

(S/defn status
  "Print cluster status"
  [ccfg :- schema/ClusterConfig args]
  (vagrant-cmd ccfg ["status"]))

(S/defn command
  "Run a cluster command"
  [function
   config :- schema/Config
   {:keys [arguments]} :- schema/Command]
  (let [{:keys [options errors summary]} (cli/parse-opts arguments cli-options)
        {:keys [environment cluster]} options]
    (cond
      (some? errors) (println errors)
      (some? cluster) (function (get-in config [environment "clusters" cluster]) arguments)
      :default (println summary))))
