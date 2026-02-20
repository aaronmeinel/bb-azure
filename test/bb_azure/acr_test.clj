(ns bb-azure.acr-test
  (:require [clojure.test :refer [deftest is testing]]
            [bb-azure.core :as az]
            [bb-azure.acr :as acr]))

;; =============================================================================
;; Container Registry Operation Tests (using call capture)
;; =============================================================================

(deftest find-acr-test
  (testing "find-acr calls az acr list with resource group"
    (let [calls (az/with-capture
                  (acr/find-acr "rg-test"))]
      (is (= 1 (count calls)))
      (is (= :az (:fn (first calls))))
      (is (= ["acr" "list" "-g" "rg-test"]
             (:args (first calls)))))))

(deftest login-test
  (testing "login! calls az acr login with acr name"
    (let [calls (az/with-capture
                  (acr/login! "myregistry"))]
      (is (= 1 (count calls)))
      (is (= :az-interactive! (:fn (first calls))))
      (is (= ["acr" "login" "-n" "myregistry"]
             (:args (first calls)))))))

(deftest repos-test
  (testing "repos calls az acr repository list"
    (let [calls (az/with-capture
                  (acr/repos "myregistry"))]
      (is (= 1 (count calls)))
      (is (= :az (:fn (first calls))))
      (is (= ["acr" "repository" "list" "-n" "myregistry"]
             (:args (first calls)))))))

(deftest tags-test
  (testing "tags calls az acr repository show-tags with defaults"
    (let [calls (az/with-capture
                  (acr/tags "myregistry" "myrepo"))]
      (is (= 1 (count calls)))
      (is (= :az (:fn (first calls))))
      (is (= ["acr" "repository" "show-tags"
              "-n" "myregistry"
              "--repository" "myrepo"
              "--orderby" "time_desc"
              "--top" "5"]
             (:args (first calls))))))

  (testing "tags accepts custom limit"
    (let [calls (az/with-capture
                  (acr/tags "myregistry" "myrepo" 10))]
      (is (= 1 (count calls)))
      (is (= :az (:fn (first calls))))
      (is (= ["acr" "repository" "show-tags"
              "-n" "myregistry"
              "--repository" "myrepo"
              "--orderby" "time_desc"
              "--top" "10"]
             (:args (first calls)))))))
