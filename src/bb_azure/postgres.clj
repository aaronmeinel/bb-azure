(ns bb-azure.postgres
  "PostgreSQL Flexible Server operations"
  (:require [bb-azure.core :as az]
            [bb-azure.output :as out]))

(defn find-server
  "Find PostgreSQL server in resource group, returns name or nil"
  [rg]
  (-> (az/az "postgres" "flexible-server" "list" "-g" rg) first :name))

(defn connect!
  "Connect to PostgreSQL interactively"
  [server user database]
  (out/info "Connecting to PostgreSQL...")
  (az/az-interactive! "postgres" "flexible-server" "connect"
                      "-n" server "-u" user "-d" database "--interactive"))
