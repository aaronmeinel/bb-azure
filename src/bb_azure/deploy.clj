(ns bb-azure.deploy
  "Bicep deployment operations"
  (:require [bb-azure.core :as az]
            [bb-azure.errors :as errors]
            [bb-azure.output :as out]
            [bb-azure.utils :as utils]
            [babashka.process :as p]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

(defn deploy!
  "Deploy Bicep template. Options:
   :location - Azure region
   :template - Bicep file path (default: main.bicep)
   :name     - Deployment name
   :params   - Map of parameters"
  [{:keys [location template name params]}]
  (let [bicep-args (utils/params->bicep-args params)
        result (apply p/sh "az" "deployment" "sub" "create"
                      "-l" location
                      "-f" (or template "main.bicep")
                      "-n" name
                      "-o" "json"
                      bicep-args)]
    (if (zero? (:exit result))
      {:ok true}
      (let [parsed (errors/parse-azure-error (:err result))
            error (or (:error parsed) parsed {:code "UnknownError" :message (:err result)})]
        {:error error}))))

(defn deploy-with-output!
  "Deploy with formatted output"
  [{:keys [location params] :as opts} on-success]
  (out/info "Location:" location)
  (out/info "Admin IP:" (utils/fetch-ip))
  (let [result (deploy! opts)]
    (if (:ok result)
      (do (out/success "Deployment complete!")
          (when on-success (on-success)))
      (do
        (println)
        (println (errors/format-error (:error result)))
        (println)
        (out/info "Full error saved to:" (errors/save-error! (:error result)))
        (out/die! "Deployment failed")))))

(defn whatif!
  "Run what-if analysis. Returns {:status :changes}"
  [{:keys [location template params]}]
  (let [bicep-args (utils/params->bicep-args params)]
    (apply az/az! "deployment" "sub" "what-if"
           "-l" location
           "-f" (or template "main.bicep")
           "--no-pretty-print"
           bicep-args)))

(defn whatif-show!
  "Run what-if with formatted output"
  [opts title]
  (let [result (whatif! opts)]
    (out/header title)
    (pp/print-table [:changeType :after]
                    (for [c (:changes result)]
                      {:changeType (:changeType c)
                       :after (str (-> c :after :type) " / " (-> c :after :name))}))
    (out/success "Status:" (:status result))))
