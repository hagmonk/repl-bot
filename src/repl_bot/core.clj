(ns repl-bot.core
  (:require [repl-bot.discord :as discord]
            [environ.core :refer [env]]
            [clojure.tools.nrepl :as repl]
            [clojail.testers :refer [secure-tester-without-def blanket]]
            [clojure.stacktrace :refer [root-cause]]
            [clojail.core :refer [sandbox]])
  (:gen-class)
  (:import (java.io StringWriter)
           (java.util.concurrent TimeoutException)))

(defn eval-form [form sbox]
  (with-open [out (StringWriter.)]
    (let [result (sbox form {#'*out* out})]
      {:expr form
       :result [out result]})))

(defonce session (atom {:sb nil}))

(defn eval-string [expr sbox]
  (let [form (binding [*read-eval* false] (read-string expr))]
    (eval-form form sbox)))

(def try-clojure-tester
  (conj secure-tester-without-def (blanket "tryclojure" "noir")))

(defn make-sandbox []
  (sandbox try-clojure-tester
           :timeout 2000
           :init '(do (require '[clojure.repl :refer [doc source]])
                      (future (Thread/sleep 600000)
                              (-> *ns* .getName remove-ns)))))

(defn find-sb [old]
  (if-let [sb (get old "sb")]
    old
    (assoc old "sb" (make-sandbox))))

(defn eval-request [expr]
  (try
    (eval-string expr (get (swap! session find-sb) "sb"))
    (catch TimeoutException _
      {:error true :message "Execution Timed Out!"})
    (catch Exception e
      {:error true :message (str (root-cause e))})))


(defn log-event [type data]
  (println "\nReceived: " type " -> " data))

(defn repl-command [type data]
  (let [command (get data "content")
        channel (get data "channel_id")
        user (get-in data ["author" "username"])]
    (when (and (= (some-> channel str Long/parseLong) 315213511120912386)
               (not (re-find #"^\s*=>" command))
               (not (= user "repl-bot")))
      (discord/answer data (some-> command eval-request :result last)))))

; channel-id 315213511120912386
(defn -main [& args]
  (let [{:keys [token nrepl-host nrepl-port] :or {nrepl-host "localhost"
                                                  nrepl-port 9876}} env]

    (discord/connect token
                     {
                      "MESSAGE_CREATE" [repl-command]
                      "ALL_OTHER"      [log-event]}
                     true)))
