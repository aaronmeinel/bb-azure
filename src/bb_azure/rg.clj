(ns bb-azure.rg
  "Resource group operations"
  (:require [bb-azure.core :as az]
            [bb-azure.output :as out]
            [clojure.pprint :as pp]))

(defn exists?
  "Check if resource group exists"
  [rg]
  (some? (az/az "group" "show" "-n" rg)))

(defn resources
  "List resources in resource group"
  [rg]
  (az/az "resource" "list" "-g" rg))

(defn delete!
  "Delete resource group (async, with confirmation)"
  [rg confirm-value]
  (when-not (exists? rg)
    (out/die! (str "Resource group " rg " does not exist")))
  (when-not (out/confirm-typed (str "Delete " rg "?") confirm-value)
    (out/die! "Aborted"))
  (az/az-run! "group" "delete" "-n" rg "--yes" "--no-wait")
  (out/success "Deletion initiated"))

(defn show!
  "Show resource group status as table"
  [rg]
  (if (exists? rg)
    (do
      (out/info "Resource Group:" rg)
      (println)
      (out/info "Resources:")
      (pp/print-table [:name :type] (resources rg)))
    (out/warn (str "Resource group " rg " does not exist"))))
