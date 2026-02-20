(ns bb-azure.output-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [bb-azure.output :as out]))

;; =============================================================================
;; Colors Test
;; =============================================================================

(deftest colors-test
  (testing "colors map has required keys"
    (is (contains? out/colors :reset))
    (is (contains? out/colors :red))
    (is (contains? out/colors :green))
    (is (contains? out/colors :yellow))
    (is (contains? out/colors :cyan))))

;; =============================================================================
;; colorize Tests
;; =============================================================================

(deftest colorize-test
  (testing "wraps text with color codes"
    (let [result (out/colorize :red "error")]
      (is (str/includes? result "error"))
      (is (str/includes? result (:red out/colors)))
      (is (str/includes? result (:reset out/colors)))))
  
  (testing "handles unknown color gracefully"
    (let [result (out/colorize :unknown "text")]
      (is (str/includes? result "text"))
      (is (str/includes? result (:reset out/colors))))))

;; =============================================================================
;; Output Functions Tests (capture stdout)
;; =============================================================================

(deftest info-test
  (testing "info prints message"
    (let [output (with-out-str (out/info "test message"))]
      (is (str/includes? output "test message")))))

(deftest warn-test
  (testing "warn prints warning symbol"
    (let [output (with-out-str (out/warn "warning"))]
      (is (str/includes? output "⚠"))
      (is (str/includes? output "warning")))))

(deftest success-test
  (testing "success prints checkmark"
    (let [output (with-out-str (out/success "done"))]
      (is (str/includes? output "✓"))
      (is (str/includes? output "done")))))

(deftest error-test
  (testing "error prints x mark"
    (let [output (with-out-str (out/error "failed"))]
      (is (str/includes? output "✗"))
      (is (str/includes? output "failed")))))

(deftest header-test
  (testing "header prints separator"
    (let [output (with-out-str (out/header "Section"))]
      (is (str/includes? output "Section"))
      (is (str/includes? output "─")))))

;; =============================================================================
;; get-env Tests
;; =============================================================================

(deftest get-env-test
  (testing "returns nil for non-existent var"
    (is (nil? (out/get-env "DEFINITELY_NOT_SET_VAR_12345"))))
  
  (testing "returns default for non-existent var"
    (is (= "default" (out/get-env "DEFINITELY_NOT_SET_VAR_12345" "default"))))
  
  (testing "returns value for existing var"
    (is (some? (out/get-env "HOME")))))
