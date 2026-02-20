(ns bb-azure.utils
  "Generic utilities for Azure CLI automation"
  (:require [babashka.process :as p]
            [clojure.string :as str]
            [clojure.pprint :as pp]))

;; -----------------------------------------------------------------------------
;; IP Utilities
;; -----------------------------------------------------------------------------

(defn fetch-ip
  "Get public IP address via ipify"
  []
  (str/trim (:out (p/sh "curl" "-s" "https://api.ipify.org"))))

;; -----------------------------------------------------------------------------
;; Secret Generation
;; -----------------------------------------------------------------------------

(defn generate-secret
  "Generate a random secret using openssl"
  [length]
  (-> (p/sh "openssl" "rand" "-base64" (str (* length 2)))
      :out
      str/trim
      (subs 0 (min length 64))))

;; -----------------------------------------------------------------------------
;; Bicep Helpers
;; -----------------------------------------------------------------------------

(defn params->bicep-args
  "Convert params map to Bicep -p arguments"
  [params]
  (mapcat (fn [[k v]]
            ["-p" (str (name k) "=" v)])
          params))

;; -----------------------------------------------------------------------------
;; Config File Helpers
;; -----------------------------------------------------------------------------

(defn load-edn
  "Load EDN from file, returns nil if file doesn't exist"
  [file]
  (when (.exists (java.io.File. file))
    (clojure.edn/read-string (slurp file))))

(defn save-edn!
  "Save data to EDN file with optional header comment"
  ([file data] (save-edn! file data nil))
  ([file data header]
   (spit file
         (str (when header (str header "\n\n"))
              (with-out-str (pp/pprint data))))
   file))
