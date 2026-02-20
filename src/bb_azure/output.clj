(ns bb-azure.output
  "Terminal output helpers for CLI applications.
   
   Provides colorized output functions for building user-friendly CLIs."
  (:require [clojure.string :as str]))

;; -----------------------------------------------------------------------------
;; ANSI Colors
;; -----------------------------------------------------------------------------

(def colors
  "ANSI color codes"
  {:reset   "\u001b[0m"
   :red     "\u001b[31m"
   :green   "\u001b[32m"
   :yellow  "\u001b[33m"
   :blue    "\u001b[34m"
   :magenta "\u001b[35m"
   :cyan    "\u001b[36m"
   :bold    "\u001b[1m"
   :dim     "\u001b[2m"})

(defn colorize
  "Wrap text in ANSI color codes"
  [color text]
  (str (get colors color "") text (:reset colors)))

;; -----------------------------------------------------------------------------
;; Output Functions
;; -----------------------------------------------------------------------------

(defn info
  "Print info message (cyan)"
  [& args]
  (println (colorize :cyan (str/join " " args))))

(defn warn
  "Print warning message (yellow)"
  [& args]
  (println (colorize :yellow (str "⚠ " (str/join " " args)))))

(defn error
  "Print error message (red)"
  [& args]
  (println (colorize :red (str "✗ " (str/join " " args)))))

(defn success
  "Print success message (green)"
  [& args]
  (println (colorize :green (str "✓ " (str/join " " args)))))

(defn header
  "Print section header (bold cyan)"
  [& args]
  (println)
  (println (str (:bold colors) (:cyan colors) (str/join " " args) (:reset colors)))
  (println (colorize :dim (apply str (repeat 60 "─")))))

(defn die!
  "Print error message and exit with code 1"
  [& args]
  (apply error args)
  (System/exit 1))

;; -----------------------------------------------------------------------------
;; User Interaction
;; -----------------------------------------------------------------------------

(defn confirm
  "Ask for confirmation, returns true if user confirms"
  [prompt]
  (print (str prompt " [y/N] "))
  (flush)
  (= "y" (str/lower-case (or (read-line) ""))))

;; -----------------------------------------------------------------------------
;; Environment
;; -----------------------------------------------------------------------------

(defn get-env
  "Get environment variable with optional default"
  ([name] (get-env name nil))
  ([name default]
   (or (System/getenv name) default)))
