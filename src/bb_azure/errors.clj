(ns bb-azure.errors
  "Azure error parsing and formatting.
   
   Azure CLI returns nested JSON error structures. This namespace provides
   tools to parse, extract, and format them for human readability."
  (:require
   [cheshire.core :as json]
   [clojure.pprint :as pp]
   [clojure.string :as str]))

;; -----------------------------------------------------------------------------
;; Error Parsing
;; -----------------------------------------------------------------------------

(defn parse-azure-error
  "Extract and parse Azure error JSON from stderr.
   
   Azure CLI often returns errors like:
     ERROR: {\"error\": {\"code\": \"...\", \"message\": \"...\"}}
   
   This function extracts the JSON portion and parses it.
   Returns nil if no valid JSON error found."
  [stderr]
  (when stderr
    (if-let [json-match (re-find #"ERROR:\s*(\{.*\})" stderr)]
      (try
        (json/parse-string (second json-match) true)
        (catch Exception _ nil))
      nil)))

;; -----------------------------------------------------------------------------
;; Error Extraction
;; -----------------------------------------------------------------------------

(defn extract-errors
  "Extract error chain from nested Azure deployment error with depth.
   
   Azure deployment errors can be deeply nested with :details arrays.
   This flattens them into a sequence of {:depth :code :message} maps."
  ([error] (extract-errors error 0))
  ([error depth]
   (let [current (when (:message error)
                   [{:depth depth
                     :code (:code error)
                     :message (:message error)}])
         children (mapcat #(extract-errors % (inc depth)) (:details error))]
     (concat current children))))

;; -----------------------------------------------------------------------------
;; Error Formatting
;; -----------------------------------------------------------------------------

(defn format-error
  "Format deployment error as a human-readable tree.
   
   Example output:
     InvalidTemplateDeployment: Deployment failed
        └─ ResourceDeploymentFailure: Resource operation failed
           └─ BadRequest: SKU not available"
  [error]
  (let [errors (extract-errors error)]
    (str/join "\n"
              (for [{:keys [depth code message]} errors]
                (str (apply str (repeat (* 3 depth) " "))
                     (if (pos? depth) "└─ " "")
                     code ": " message)))))

(defn save-error!
  "Save error to file in readable format. Returns filename.
   
   The saved file contains:
   1. Timestamp
   2. Human-readable formatted error (as comments)
   3. Full EDN structure for programmatic access"
  ([error] (save-error! error ".last-error.edn"))
  ([error filename]
   (let [formatted (format-error error)]
     (spit filename
           (str ";; Azure Deployment Error - " (java.time.LocalDateTime/now) "\n"
                ";; Human readable:\n"
                (str/join "\n" (map #(str ";; " %) (str/split-lines formatted)))
                "\n\n"
                ";; Full error structure:\n"
                (with-out-str (pp/pprint error))))
     filename)))

;; -----------------------------------------------------------------------------
;; ex-info Integration
;; -----------------------------------------------------------------------------

(defn error->ex-info
  "Convert an Azure error map to an ex-info exception.
   
   Useful for re-throwing parsed errors with full context."
  [error]
  (ex-info (format-error error)
           {:type :azure-error
            :error error}))
