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
    ;[playback.core :as playback]
    ;[portal.api :as p]
    ;[tab.api :as tab]
    [[puget.printer :as puget :refer [cprint]] (add-tap (bound-fn* (resolve 'puget/cprint)))]
    [dev]])

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

;; Useful functions to be loaded in for all projects

;#_{:clj-kondo/ignore [:unresolved-namespace]}
;(defn play
;  []
;  (playback/open-portal!)
;  (add-tap #'playback/portal-tap))
