(ns bb-azure.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [bb-azure.core :as az]))

;; =============================================================================
;; Dynamic Vars Tests
;; =============================================================================

(deftest explain-mode-test
  (testing "explain mode is off by default"
    (is (false? az/*explain-mode*)))
  
  (testing "explain mode can be bound"
    (binding [az/*explain-mode* true]
      (is (true? az/*explain-mode*)))))

(deftest dry-run-test
  (testing "dry-run is off by default"
    (is (false? az/*dry-run*)))
  
  (testing "dry-run prevents execution"
    (binding [az/*dry-run* true]
      ;; az returns nil without executing
      (is (nil? (az/az "this-would-fail-if-executed")))
      ;; az! returns empty map without executing
      (is (= {} (az/az! "this-would-fail-if-executed")))
      ;; az-run! returns nil without executing
      (is (nil? (az/az-run! "this-would-fail-if-executed"))))))

(deftest capture-calls-test
  (testing "capture-calls is nil by default"
    (is (nil? az/*capture-calls*)))
  
  (testing "with-capture captures az calls"
    (let [calls (az/with-capture
                  (az/az "webapp" "list" "-g" "my-rg"))]
      (is (= 1 (count calls)))
      (is (= :az (:fn (first calls))))
      (is (= ["webapp" "list" "-g" "my-rg"] (:args (first calls))))))
  
  (testing "with-capture captures az! calls"
    (let [calls (az/with-capture
                  (az/az! "group" "show" "-n" "rg-test"))]
      (is (= 1 (count calls)))
      (is (= :az! (:fn (first calls))))
      (is (= ["group" "show" "-n" "rg-test"] (:args (first calls))))))
  
  (testing "with-capture captures az-run! calls"
    (let [calls (az/with-capture
                  (az/az-run! "webapp" "restart" "-g" "rg" "-n" "app"))]
      (is (= 1 (count calls)))
      (is (= :az-run! (:fn (first calls))))
      (is (= ["webapp" "restart" "-g" "rg" "-n" "app"] (:args (first calls))))))
  
  (testing "with-capture captures az-interactive! calls"
    (let [calls (az/with-capture
                  (az/az-interactive! "webapp" "ssh" "-g" "rg" "-n" "app"))]
      (is (= 1 (count calls)))
      (is (= :az-interactive! (:fn (first calls))))
      (is (= ["webapp" "ssh" "-g" "rg" "-n" "app"] (:args (first calls))))))
  
  (testing "with-capture captures multiple calls"
    (let [calls (az/with-capture
                  (az/az "webapp" "list")
                  (az/az-run! "webapp" "restart" "-g" "rg" "-n" "app")
                  (az/az! "group" "show" "-n" "test"))]
      (is (= 3 (count calls)))
      (is (= [:az :az-run! :az!] (mapv :fn calls))))))

;; =============================================================================
;; Integration Tests (require Azure CLI)
;; =============================================================================

(deftest az-account-show-test
  (testing "az account show returns subscription info"
    (when-let [result (az/az "account" "show")]
      (is (contains? result :id))
      (is (contains? result :name))
      (is (contains? result :tenantId)))))

(deftest az-bang-throws-on-invalid-test
  (testing "az! throws on invalid command"
    (is (thrown? clojure.lang.ExceptionInfo
                 (az/az! "invalid-resource-type" "list")))))
