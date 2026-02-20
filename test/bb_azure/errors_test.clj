(ns bb-azure.errors-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [bb-azure.errors :as errors]))

;; =============================================================================
;; Sample Error Data
;; =============================================================================

(def sample-stderr
  "ERROR: {\"error\":{\"code\":\"InvalidTemplateDeployment\",\"message\":\"Deployment failed\",\"details\":[{\"code\":\"ResourceDeploymentFailure\",\"message\":\"Resource failed\"}]}}")

(def nested-error
  {:code "InvalidTemplateDeployment"
   :message "Deployment failed"
   :details [{:code "ResourceDeploymentFailure"
              :message "Resource operation failed"
              :details [{:code "BadRequest"
                         :message "SKU not available in region"}]}]})

;; =============================================================================
;; parse-azure-error Tests
;; =============================================================================

(deftest parse-azure-error-test
  (testing "parses JSON error from stderr"
    (let [result (errors/parse-azure-error sample-stderr)]
      (is (map? result))
      (is (= "InvalidTemplateDeployment" (get-in result [:error :code])))))
  
  (testing "returns nil for non-error stderr"
    (is (nil? (errors/parse-azure-error "Some other output"))))
  
  (testing "returns nil for nil input"
    (is (nil? (errors/parse-azure-error nil))))
  
  (testing "returns nil for malformed JSON"
    (is (nil? (errors/parse-azure-error "ERROR: {not valid json}")))))

;; =============================================================================
;; extract-errors Tests
;; =============================================================================

(deftest extract-errors-test
  (testing "extracts flat error"
    (let [simple {:code "Error" :message "Failed"}
          result (errors/extract-errors simple)]
      (is (= 1 (count result)))
      (is (= 0 (:depth (first result))))
      (is (= "Error" (:code (first result))))))
  
  (testing "extracts nested errors with depth"
    (let [result (errors/extract-errors nested-error)]
      (is (= 3 (count result)))
      (is (= [0 1 2] (map :depth result)))
      (is (= ["InvalidTemplateDeployment" "ResourceDeploymentFailure" "BadRequest"]
             (map :code result))))))

;; =============================================================================
;; format-error Tests
;; =============================================================================

(deftest format-error-test
  (testing "formats single error"
    (let [result (errors/format-error {:code "Error" :message "Failed"})]
      (is (str/includes? result "Error: Failed"))))
  
  (testing "formats nested errors as tree"
    (let [result (errors/format-error nested-error)]
      (is (str/includes? result "InvalidTemplateDeployment"))
      (is (str/includes? result "└─"))
      (is (str/includes? result "BadRequest")))))

;; =============================================================================
;; save-error! Tests
;; =============================================================================

(deftest save-error-test
  (testing "saves error to file"
    (let [filename (str "/tmp/test-error-" (System/currentTimeMillis) ".edn")
          result (errors/save-error! nested-error filename)]
      (is (= filename result))
      (is (.exists (java.io.File. filename)))
      (let [content (slurp filename)]
        (is (str/includes? content "InvalidTemplateDeployment"))
        (is (str/includes? content ";; Human readable")))
      ;; Cleanup
      (.delete (java.io.File. filename)))))
