(ns bb-azure.core
  "Azure CLI wrapper for Babashka.
   
   Core functions for interacting with Azure CLI:
   - `az`   - Execute and parse JSON output (returns nil on failure)
   - `az!`  - Execute and parse JSON output (throws on failure)
   - `az-run!` - Execute for side effects only (throws on failure)
   - `az-interactive!` - Execute with inherited stdin/stdout
   
   Control behavior with dynamic vars:
   - `*explain-mode*` - Print commands before executing
   - `*dry-run*` - Print commands without executing"
  (:require
   [babashka.process :as p]
   [cheshire.core :as json]
   [clojure.string :as str]))

;; -----------------------------------------------------------------------------
;; Dynamic Configuration
;; -----------------------------------------------------------------------------

(def ^:dynamic *explain-mode*
  "When true, print commands before executing"
  false)

(def ^:dynamic *dry-run*
  "When true, print commands but don't execute"
  false)

(def ^:dynamic *capture-calls*
  "When bound to an atom, captures [fn-name args] instead of executing.
   Useful for testing argument construction without Azure connection."
  nil)

;; -----------------------------------------------------------------------------
;; Internal Helpers
;; -----------------------------------------------------------------------------

(defn- az-cmd
  "Build the az command vector"
  [args]
  (into ["az"] args))

(defn- print-cmd
  "Print command for explain/dry-run modes"
  [args]
  (println (str "$ " (str/join " " (az-cmd args)))))

(defn- should-execute?
  "Check if we should actually execute (not in dry-run mode)"
  []
  (not *dry-run*))

(defn- capture!
  "Capture a call if *capture-calls* is bound, returns true if captured"
  [fn-name args]
  (when *capture-calls*
    (swap! *capture-calls* conj {:fn fn-name :args (vec args)})
    true))

(defn- explain
  "Print command if in explain mode"
  [args]
  (when *explain-mode*
    (print-cmd args)))

;; -----------------------------------------------------------------------------
;; Public API
;; -----------------------------------------------------------------------------

(defn az
  "Execute az CLI and return parsed JSON. Returns nil on failure.
   
   Usage:
     (az \"account\" \"show\")
     (az \"webapp\" \"list\" \"-g\" \"my-rg\")"
  [& args]
  (explain args)
  (cond
    (capture! :az args) nil
    (should-execute?)
    (let [result (p/sh (az-cmd (concat args ["-o" "json"])))]
      (when (zero? (:exit result))
        (json/parse-string (:out result) true)))))

(defn az!
  "Execute az CLI and return parsed JSON. Throws on failure.
   
   Usage:
     (az! \"account\" \"show\")
     (az! \"webapp\" \"list\" \"-g\" \"my-rg\")"
  [& args]
  (explain args)
  (cond
    (capture! :az! args) {}
    (should-execute?)
    (let [result (p/sh (az-cmd (concat args ["-o" "json"])))]
      (if (zero? (:exit result))
        (json/parse-string (:out result) true)
        (throw (ex-info (str "az command failed: " (:err result))
                        {:args args :exit (:exit result) :stderr (:err result)}))))
    :else {}))

(defn az-run!
  "Execute az CLI for side effects. Returns nil. Throws on failure.
   
   Usage:
     (az-run! \"webapp\" \"restart\" \"-g\" \"my-rg\" \"-n\" \"my-app\")"
  [& args]
  (explain args)
  (when-not (capture! :az-run! args)
    (when (should-execute?)
      (let [result (p/sh (az-cmd (concat args ["-o" "none"])))]
        (when-not (zero? (:exit result))
          (throw (ex-info (str "az command failed: " (:err result))
                          {:args args :exit (:exit result) :stderr (:err result)})))))))

(defn az-interactive!
  "Execute az CLI with inherited stdin/stdout. For interactive commands.
   
   Usage:
     (az-interactive! \"webapp\" \"ssh\" \"-g\" \"my-rg\" \"-n\" \"my-app\")"
  [& args]
  (explain args)
  (when-not (capture! :az-interactive! args)
    (when (should-execute?)
      (p/shell {:inherit true} (az-cmd args)))))

;; -----------------------------------------------------------------------------
;; Test Helpers
;; -----------------------------------------------------------------------------

(defmacro with-capture
  "Execute body with call capture enabled. Returns vector of captured calls.
   Each call is a map with :fn and :args keys.
   
   Usage:
     (with-capture
       (az \"webapp\" \"list\")
       (az-run! \"webapp\" \"restart\" \"-g\" \"rg\" \"-n\" \"app\"))
     => [{:fn :az :args [\"webapp\" \"list\"]}
         {:fn :az-run! :args [\"webapp\" \"restart\" \"-g\" \"rg\" \"-n\" \"app\"]}]"
  [& body]
  `(let [calls# (atom [])]
     (binding [*capture-calls* calls#]
       ~@body)
     @calls#))
