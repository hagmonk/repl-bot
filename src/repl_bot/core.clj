(ns repl-bot.core
  (:require [repl-bot.discord :as discord]
            [environ.core :refer [env]]
            [clojure.tools.nrepl :as repl])
  (:gen-class))

(defonce repl-agent (agent (let [{:keys [nrepl-host nrepl-port]
                                  :or   {nrepl-host "localhost" nrepl-port 9876}} env]
                             (-> (repl/connect :host nrepl-host :port nrepl-port)
                                 (repl/client 5000)))))

(defn log-event [type data]
  (println "\nReceived: " type " -> " data))

(defn repl-send-and-receive! [message]
  (if (await-for 5000 repl-agent)
    (-> @repl-agent
        (repl/message {:op "eval" :code message})
        repl/response-values
        pr-str)
    "Message timed out"))

(defn repl-command [type data]
  (let [command (get data "content")
        channel (get data "channel_id")
        user (get-in data ["author" "username"])]
    (when (and (= (some-> channel str Long/parseLong) 315213511120912386)
               (not (re-find #"^\s*=>" command))
               (not (= user "repl-bot")))
      (discord/answer data (repl-send-and-receive! command)))))

; channel-id 315213511120912386
(defn -main [& args]
  (let [{:keys [token nrepl-host nrepl-port] :or {nrepl-host "localhost"
                                                  nrepl-port 9876}} env]

    (discord/connect token
                     {
                      "MESSAGE_CREATE" [repl-command]
                      "ALL_OTHER"      [log-event]}
                     true)))
