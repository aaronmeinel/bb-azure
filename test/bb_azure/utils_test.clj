(ns bb-azure.utils-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [bb-azure.utils :as utils]))

;; =============================================================================
;; params->bicep-args Tests
;; =============================================================================

(deftest params->bicep-args-test
  (testing "converts empty map to empty list"
    (is (= [] (utils/params->bicep-args {}))))

  (testing "converts single param"
    (is (= ["-p" "name=value"]
           (utils/params->bicep-args {:name "value"}))))

  (testing "converts multiple params"
    (let [result (utils/params->bicep-args {:env "dev" :sku "B1"})]
      (is (= 4 (count result)))
      (is (every? #(or (= "-p" %) (str/includes? % "=")) result))))

  (testing "handles non-string values"
    (is (= ["-p" "count=42"]
           (utils/params->bicep-args {:count 42})))
    (is (= ["-p" "enabled=true"]
           (utils/params->bicep-args {:enabled true})))))

;; =============================================================================
;; generate-secret Tests
;; =============================================================================

(deftest generate-secret-test
  (testing "generates secret of requested length"
    (let [secret (utils/generate-secret 24)]
      (is (= 24 (count secret)))))

  (testing "generates different secrets each time"
    (let [s1 (utils/generate-secret 16)
          s2 (utils/generate-secret 16)]
      (is (not= s1 s2))))

  (testing "caps at 64 characters"
    (let [secret (utils/generate-secret 100)]
      (is (<= (count secret) 64)))))

;; =============================================================================
;; fetch-ip Tests
;; =============================================================================

(deftest fetch-ip-test
  (testing "returns valid IP address"
    (let [ip (utils/fetch-ip)]
      (is (string? ip))
      (is (re-matches #"\d+\.\d+\.\d+\.\d+" ip)))))

;; =============================================================================
;; EDN File Helpers Tests
;; =============================================================================

(deftest load-edn-test
  (testing "returns nil for non-existent file"
    (is (nil? (utils/load-edn "/tmp/does-not-exist-12345.edn"))))

  (testing "loads existing EDN file"
    (let [filename (str "/tmp/test-load-" (System/currentTimeMillis) ".edn")]
      (spit filename "{:key \"value\"}")
      (is (= {:key "value"} (utils/load-edn filename)))
      (.delete (java.io.File. filename)))))

(deftest save-edn-test
  (testing "saves data to file"
    (let [filename (str "/tmp/test-save-" (System/currentTimeMillis) ".edn")
          data {:foo "bar" :num 42}]
      (utils/save-edn! filename data)
      (is (.exists (java.io.File. filename)))
      (is (= data (utils/load-edn filename)))
      (.delete (java.io.File. filename))))

  (testing "includes header comment"
    (let [filename (str "/tmp/test-header-" (System/currentTimeMillis) ".edn")]
      (utils/save-edn! filename {:x 1} ";; My Header")
      (is (str/starts-with? (slurp filename) ";; My Header"))
      (.delete (java.io.File. filename)))))
