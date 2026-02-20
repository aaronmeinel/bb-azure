(ns bb-azure.deploy-test
  (:require [clojure.test :refer [deftest is testing]]
            [bb-azure.core :as az]
            [bb-azure.deploy :as deploy]
            [bb-azure.utils :as utils]))

;; =============================================================================
;; Deployment Utility Tests
;; =============================================================================

(deftest params->bicep-args-test
  (testing "empty params returns empty vector"
    (is (= [] (utils/params->bicep-args {}))))

  (testing "single param produces correct bicep arg"
    (is (= ["-p" "name=dev"]
           (utils/params->bicep-args {:name "dev"}))))

  (testing "multiple params produce multiple -p flags"
    (let [args (utils/params->bicep-args {:name "dev" :env "production" :count 5})]
      (is (= 6 (count args))) ;; 3 params * 2 items each
      (is (every? #(= "-p" %) (take-nth 2 args)))
      ;; Check that all param key=value pairs are present
      (let [values (set (take-nth 2 (rest args)))]
        (is (contains? values "name=dev"))
        (is (contains? values "env=production"))
        (is (contains? values "count=5")))))

  (testing "handles special characters in values"
    (is (= ["-p" "password=abc123!@#"]
           (utils/params->bicep-args {:password "abc123!@#"})))))

;; =============================================================================
;; whatif! Tests (uses az! which we can capture)
;; =============================================================================

(deftest whatif-test
  (testing "whatif! calls deployment sub what-if with correct args"
    (let [calls (az/with-capture
                  (deploy/whatif! {:location "westeurope"
                                   :template "main.bicep"
                                   :params {:name "test" :env "dev"}}))]
      (is (= 1 (count calls)))
      (is (= :az! (:fn (first calls))))
      (let [args (:args (first calls))]
        (is (= "deployment" (nth args 0)))
        (is (= "sub" (nth args 1)))
        (is (= "what-if" (nth args 2)))
        (is (= "-l" (nth args 3)))
        (is (= "westeurope" (nth args 4)))
        (is (= "-f" (nth args 5)))
        (is (= "main.bicep" (nth args 6)))
        (is (= "--no-pretty-print" (nth args 7)))
        ;; Rest should be bicep params
        (is (some #(= "-p" %) (drop 8 args))))))

  (testing "whatif! uses default template when not specified"
    (let [calls (az/with-capture
                  (deploy/whatif! {:location "eastus"
                                   :params {}}))]
      (let [args (:args (first calls))]
        (is (= "main.bicep" (nth args 6)))))))

;; =============================================================================
;; deploy! Tests (uses babashka.process directly, harder to capture)
;; Note: deploy! doesn't use az/az! so we test the helper functions instead
;; =============================================================================

(deftest deploy-params-construction-test
  (testing "deploy params are properly constructed"
    ;; We test that params->bicep-args works correctly
    ;; since deploy! uses it internally
    (let [params {:baseName "primos"
                  :environment "dev"
                  :adminIp "1.2.3.4"
                  :postgresPassword "secret123"}
          args (utils/params->bicep-args params)]
      ;; Should have 4 params, each with -p prefix
      (is (= 8 (count args)))
      (is (= 4 (count (filter #(= "-p" %) args)))))))
