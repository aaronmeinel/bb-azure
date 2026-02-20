#!/usr/bin/env bb
;; Example: Simple deployment script using bb-azure
;;
;; This demonstrates common patterns for Azure automation with bb-azure.
;;
;; Usage:
;;   bb deploy.clj dev
;;   bb deploy.clj prod --dry-run

(require '[bb-azure.core :as az]
         '[bb-azure.errors :as errors]
         '[bb-azure.output :as out]
         '[babashka.cli :as cli])

;; -----------------------------------------------------------------------------
;; Configuration
;; -----------------------------------------------------------------------------

(def environments
  {:dev  {:resource-group "rg-myapp-dev"
          :location "westeurope"
          :sku "B1"}
   :prod {:resource-group "rg-myapp-prod"
          :location "westeurope"
          :sku "P1v2"}})

;; -----------------------------------------------------------------------------
;; Deployment Functions
;; -----------------------------------------------------------------------------

(defn ensure-resource-group
  "Create resource group if it doesn't exist"
  [{:keys [resource-group location]}]
  (out/info "Checking resource group" resource-group)
  (if (az/az "group" "show" "-n" resource-group)
    (out/success "Resource group exists")
    (do
      (out/info "Creating resource group...")
      (az/az-run! "group" "create" "-n" resource-group "-l" location)
      (out/success "Resource group created"))))

(defn deploy-webapp
  "Deploy or update the web app"
  [{:keys [resource-group sku]}]
  (out/info "Deploying web app with SKU" sku)
  (let [result (az/az "webapp" "list" "-g" resource-group)]
    (if (seq result)
      (out/info "Updating existing web app...")
      (out/info "Creating new web app..."))
    ;; Add your deployment logic here
    (out/success "Web app deployed")))

(defn show-status
  "Show current deployment status"
  [{:keys [resource-group]}]
  (out/header "Deployment Status")
  (if-let [webapps (az/az "webapp" "list" "-g" resource-group)]
    (doseq [app webapps]
      (println (str "  " (:name app) " - " (:state app))))
    (out/warn "No web apps found")))

;; -----------------------------------------------------------------------------
;; Main
;; -----------------------------------------------------------------------------

(defn deploy
  "Main deployment function"
  [env-name opts]
  (let [env-key (keyword env-name)
        config (get environments env-key)]
    (when-not config
      (out/die! "Unknown environment:" env-name))

    (out/header (str "Deploying to " (name env-key)))

    ;; Use dry-run mode if requested
    (binding [az/*explain-mode* true
              az/*dry-run* (:dry-run opts false)]
      (when (:dry-run opts)
        (out/warn "DRY RUN MODE - no changes will be made"))

      (try
        (ensure-resource-group config)
        (deploy-webapp config)
        (show-status config)
        (out/success "Deployment complete!")
        (catch Exception e
          (let [err-data (ex-data e)]
            (when-let [parsed (errors/parse-azure-error (:stderr err-data))]
              (errors/save-error! parsed)
              (println (errors/format-error parsed)))
            (out/die! "Deployment failed")))))))

;; CLI entry point
(let [{:keys [args opts]} (cli/parse-args *command-line-args*
                                          {:coerce {:dry-run :boolean}})]
  (if (empty? args)
    (out/die! "Usage: deploy.clj <environment> [--dry-run]")
    (deploy (first args) opts)))
