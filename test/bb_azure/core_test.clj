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
