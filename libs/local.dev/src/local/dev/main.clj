(ns local.dev.main
  (:require
   [cider.nrepl]
   [clj-commons.ansi :as ansi]
   [clojure.java.io :as io]
   [fipp.edn :as fipp]
   [nrepl.server :as nrepl]
   [rebel-readline.clojure.line-reader :as clj-line-reader]
   [rebel-readline.clojure.main :as rebel-main]
   [rebel-readline.core :as rebel-core]
   [rebel-readline.jline-api :as api]))

(defn ^:private syntax-highlight-fipp
  [x]
  (binding [*out* (.. api/*line-reader* getTerminal writer)]
    (print (api/->ansi
            (clj-line-reader/highlight-clj-str
             (with-out-str (fipp/pprint x)))))))

(defn ^:private start-nrepl
  [env-port]
  (let [server-map (nrepl/start-server
                    :port (or env-port 0)
                    :handler (apply nrepl.server/default-handler
                                    (map #'cider.nrepl/resolve-or-fail cider.nrepl/cider-middleware)))
        port (:port server-map)
        port-file (io/file ".nrepl-port")]
    (.deleteOnExit port-file)
    (spit port-file port)
    (println (ansi/compose [:green (format "nREPL client can be connected to port '%d'." port)]))))

(defn -main
  [& _]
  (start-nrepl (some-> (System/getenv "NREPL_PORT")
                       (Integer/parseInt)))
  (rebel-core/ensure-terminal
   (rebel-main/repl :print syntax-highlight-fipp))
  (println (ansi/compose [:green "Goodbye!"]))
  (System/exit 0))
