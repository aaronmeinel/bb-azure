(ns bb-azure.acr
  "Azure Container Registry operations"
  (:require [bb-azure.core :as az]
            [bb-azure.output :as out]
            [clojure.pprint :as pp]))

(defn find-acr
  "Find ACR in resource group, returns name or nil"
  [rg]
  (-> (az/az "acr" "list" "-g" rg) first :name))

(defn login!
  "Login to ACR (interactive)"
  [acr-name]
  (az/az-interactive! "acr" "login" "-n" acr-name))

(defn repos
  "List repositories in ACR"
  [acr-name]
  (az/az "acr" "repository" "list" "-n" acr-name))

(defn tags
  "List tags for a repository (newest first)"
  ([acr-name repo] (tags acr-name repo 5))
  ([acr-name repo n]
   (az/az "acr" "repository" "show-tags"
          "-n" acr-name
          "--repository" repo
          "--orderby" "time_desc"
          "--top" (str n))))

(defn repos-show!
  "Show ACR repos and tags as table"
  [rg]
  (if-let [acr-name (find-acr rg)]
    (do
      (out/header (str "ACR: " acr-name))
      (pp/print-table [:repo :tag]
                      (for [repo (repos acr-name)
                            tag (tags acr-name repo)]
                        {:repo repo :tag tag})))
    (out/die! (str "No ACR found in " rg))))
