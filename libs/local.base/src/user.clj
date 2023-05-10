(def safe-requires
  "List of namespaces to require and refer when inside user ns at load time.
   Can be given an initialization body to execute after having been required.
   To do so, wrap the lib spec in a vector, and all elements after the lib
   spec vector will be evaled after the lib spec has been required."
  '[[clojure.java.javadoc :as javadoc :refer [javadoc]]
    [clojure.pprint :as pprint :refer [pp pprint]]
    [clojure.repl :as repl :refer [source apropos dir pst doc find-doc]]
    [clojure.set :as set]
    [clojure.stacktrace :as stacktrace :refer [e]]
    [clojure.string :as str]
    [flow-storm.api :as fs-api]
    [portal.api :as p]
    [kaocha.repl :as k]
    [tab.api :as tab]
    [[puget.printer :as puget :refer (cprint)] (add-tap (bound-fn* (resolve 'puget/cprint)))]
    [dev]])

(def lazy-safe-requires
  "List of namespaces to require lazily, by creating a function which
   can be called inside user namespace to cause the require.
   All requiring-fn will be prepended with req- for easy auto-complete.
   Can group requires within a single requiring-fn by listing multiple
   requires. Lib spec can be a safe-require with an init body if wrapped
   in a vector themselves."
  '[[async [clojure.core.async :as async]]
    [edn [clojure.edn :as edn]]
    [inspector [clojure.inspector :as inspector]]
    [logic [clojure.core.logic :as logic]]
    [reducers [clojure.core.reducers :as reducers]]
    [reflect [clojure.reflect :as reflect]]
    [spec
     [clojure.spec.alpha :as s]
     [clojure.spec.gen.alpha :as sgen]
     [clojure.spec.test.alpha :as stest]]
    [walk [clojure.walk :as walk]]
    [fast
     [clj-memory-meter.core :as fast-memory-meter :refer [measure]]
     [clj-java-decompiler.core :as fast-decompiler :refer [decompile]]]])

(defn safe-require-init
  [req]
  (let [init? (-> req first vector?)
        lib (if init? (first req) req)
        init (when init? (rest req))]
    `(try
      (require '~lib)
      ~@init
      (catch Throwable t#
        (println ~(str "Error loading " lib ":")
                 (or (.getMessage t#)
                     (type t#)))))))

(defmacro safe-require-inits
  []
  `(do
    ~@(for [req safe-requires]
        (safe-require-init req))))

(safe-require-inits)

(defmacro def-lazy-requiring-fns
  []
  `(do
    ~@(for [req-group lazy-safe-requires]
        (let [requiring-fn-name (first req-group)
              requires (rest req-group)]
          `(defn ~(symbol (str "req-" requiring-fn-name)) []
             ~@(for [req requires]
                 (safe-require-init req)))))))

(def-lazy-requiring-fns)

;; Useful functions to be loaded in for all projects

;;
;; Bridge/route all JUL log records to the SLF4J API.
;;
;(try
; (Class/forName "org.slf4j.bridge.SLF4JBridgeHandler")
; (eval
;  `(do
;    (org.slf4j.bridge.SLF4JBridgeHandler/removeHandlersForRootLogger)
;    (org.slf4j.bridge.SLF4JBridgeHandler/install)))
; (catch Throwable _))
