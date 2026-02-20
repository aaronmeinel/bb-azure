(ns bb-azure.webapp-test
  (:require [clojure.test :refer [deftest is testing]]
            [bb-azure.core :as az]
            [bb-azure.webapp :as webapp]))

;; =============================================================================
;; App Service Operation Tests (using call capture)
;; =============================================================================

(deftest restart-test
  (testing "restart! calls az webapp restart with correct args"
    (let [calls (az/with-capture
                  (webapp/restart! "rg-test" "app-test"))]
      (is (= 1 (count calls)))
      (is (= :az-run! (:fn (first calls))))
      (is (= ["webapp" "restart" "-g" "rg-test" "-n" "app-test"]
             (:args (first calls)))))))

(deftest stop-test
  (testing "stop! calls az webapp stop with correct args"
    (let [calls (az/with-capture
                  (webapp/stop! "rg-prod" "my-app"))]
      (is (= 1 (count calls)))
      (is (= :az-run! (:fn (first calls))))
      (is (= ["webapp" "stop" "-g" "rg-prod" "-n" "my-app"]
             (:args (first calls)))))))

(deftest start-test
  (testing "start! calls az webapp start with correct args"
    (let [calls (az/with-capture
                  (webapp/start! "rg-staging" "staging-app"))]
      (is (= 1 (count calls)))
      (is (= :az-run! (:fn (first calls))))
      (is (= ["webapp" "start" "-g" "rg-staging" "-n" "staging-app"]
             (:args (first calls)))))))

(deftest logs-test
  (testing "logs! calls az webapp log tail with correct args"
    (let [calls (az/with-capture
                  (webapp/logs! "rg-dev" "dev-app"))]
      (is (= 1 (count calls)))
      (is (= :az-interactive! (:fn (first calls))))
      (is (= ["webapp" "log" "tail" "-g" "rg-dev" "-n" "dev-app"]
             (:args (first calls)))))))

(deftest ssh-test
  (testing "ssh! calls az webapp ssh with correct args"
    (let [calls (az/with-capture
                  (webapp/ssh! "my-rg" "my-app"))]
      (is (= 1 (count calls)))
      (is (= :az-interactive! (:fn (first calls))))
      (is (= ["webapp" "ssh" "-g" "my-rg" "-n" "my-app"]
             (:args (first calls)))))))

(deftest config-test
  (testing "config calls az webapp config appsettings list"
    (let [calls (az/with-capture
                  (webapp/config "rg-test" "test-app"))]
      (is (= 1 (count calls)))
      (is (= :az (:fn (first calls))))
      (is (= ["webapp" "config" "appsettings" "list" "-g" "rg-test" "-n" "test-app"]
             (:args (first calls)))))))

(deftest config-set-test
  (testing "config-set! calls az webapp config appsettings set with settings"
    (let [calls (az/with-capture
                  (webapp/config-set! "rg-test" "test-app" ["KEY1=value1" "KEY2=value2"]))]
      (is (= 1 (count calls)))
      (is (= :az-run! (:fn (first calls))))
      (let [args (:args (first calls))]
        (is (= "webapp" (nth args 0)))
        (is (= "config" (nth args 1)))
        (is (= "appsettings" (nth args 2)))
        (is (= "set" (nth args 3)))
        (is (= "-g" (nth args 4)))
        (is (= "rg-test" (nth args 5)))
        (is (= "-n" (nth args 6)))
        (is (= "test-app" (nth args 7)))
        (is (= "--settings" (nth args 8)))
        (is (= "KEY1=value1" (nth args 9)))
        (is (= "KEY2=value2" (nth args 10)))))))
