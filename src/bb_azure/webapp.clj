(ns bb-azure.webapp
  "App Service operations - generic tasks for any Azure web app"
  (:require [bb-azure.core :as az]
            [bb-azure.output :as out]
            [clojure.pprint :as pp]))

(defn restart!
  "Restart App Service"
  [rg app]
  (out/info "Restarting...")
  (az/az-run! "webapp" "restart" "-g" rg "-n" app)
  (out/success "Restarted"))

(defn stop!
  "Stop App Service"
  [rg app]
  (out/info "Stopping...")
  (az/az-run! "webapp" "stop" "-g" rg "-n" app)
  (out/success "Stopped"))

(defn start!
  "Start App Service"
  [rg app]
  (out/info "Starting...")
  (az/az-run! "webapp" "start" "-g" rg "-n" app)
  (out/success "Started"))

(defn logs!
  "Stream App Service logs (interactive)"
  [rg app]
  (out/info "Streaming logs (Ctrl+C to stop)...")
  (az/az-interactive! "webapp" "log" "tail" "-g" rg "-n" app))

(defn ssh!
  "SSH into App Service container (interactive)"
  [rg app]
  (az/az-interactive! "webapp" "ssh" "-g" rg "-n" app))

(defn config
  "Get app settings as list"
  [rg app]
  (->> (az/az "webapp" "config" "appsettings" "list" "-g" rg "-n" app)
       (sort-by :name)))

(defn config-show!
  "Show app settings as table"
  [rg app]
  (out/header "App Service Configuration")
  (pp/print-table [:name :value] (config rg app)))

(defn config-set!
  "Set app settings"
  [rg app settings]
  (out/info "Setting configuration...")
  (apply az/az-run! "webapp" "config" "appsettings" "set"
         "-g" rg "-n" app "--settings" settings)
  (out/success "Configuration updated"))
