(ns bb-azure.postgres-test
  (:require [clojure.test :refer [deftest is testing]]
            [bb-azure.core :as az]
            [bb-azure.postgres :as postgres]))

;; =============================================================================
;; PostgreSQL Operation Tests (using call capture)
;; =============================================================================

(deftest find-server-test
  (testing "find-server calls az postgres flexible-server list"
    (let [calls (az/with-capture
                  (postgres/find-server "rg-test"))]
      (is (= 1 (count calls)))
      (is (= :az (:fn (first calls))))
      (is (= ["postgres" "flexible-server" "list" "-g" "rg-test"]
             (:args (first calls)))))))

(deftest connect-test
  (testing "connect! calls az postgres flexible-server connect with correct args"
    (let [calls (az/with-capture
                  (postgres/connect! "myserver" "admin" "mydb"))]
      (is (= 1 (count calls)))
      (is (= :az-interactive! (:fn (first calls))))
      (is (= ["postgres" "flexible-server" "connect"
              "-n" "myserver" "-u" "admin" "-d" "mydb" "--interactive"]
             (:args (first calls)))))))
