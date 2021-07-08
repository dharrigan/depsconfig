(ns local.dev.main
  (:require
   [cider.nrepl]
   [clojure.java.io :as io]
   [fipp.edn :as fipp]
   [io.aviso.ansi :as ansi]
   [nrepl.server :as nrepl]
   [rebel-readline.clojure.line-reader :as clj-line-reader]
   [rebel-readline.jline-api :as api]
   [refactor-nrepl.middleware]
   hashp.core
   rebel-readline.clojure.main
   rebel-readline.core))

(defn ^:private syntax-highlight-fipp
  [x]
  (binding [*out* (.. api/*line-reader* getTerminal writer)]
    (try
     (print (api/->ansi
             (clj-line-reader/highlight-clj-str
              (with-out-str (fipp/pprint x)))))
     (catch java.lang.StackOverflowError _
       (try
        (fipp/pprint x)
        ;; Just in case of https://github.com/brandonbloom/fipp/issues/28
        (catch java.lang.StackOverflowError _
          (prn x)))))))

(defn ^:private install-reveal-extras
  "Returns a Reveal view object that tracks each tap>'d value and
  displays its metadata and class type, and its value in a table.

  In order for this to take effect, this function needs to be
  called and its result sent to Reveal, after Reveal is up and
  running. This dev.clj file achieves this by executing the
  following code when starting Reveal:

  (future (Thread/sleep 6000)
          (tap> (install-reveal-extras)))

  The six second delay should be enough for Reveal to initialize
  and display its initial view."
  []
  (try
    (let [last-tap (atom nil)
          rx-stream-as-is (requiring-resolve 'vlaaad.reveal.ext/stream-as-is)
          rx-obs-view     @(requiring-resolve 'vlaaad.reveal.ext/observable-view)
          rx-value-view   @(requiring-resolve 'vlaaad.reveal.ext/value-view)
          rx-table-view   @(requiring-resolve 'vlaaad.reveal.ext/table-view)
          rx-as           (requiring-resolve 'vlaaad.reveal.ext/as)
          rx-raw-string   (requiring-resolve 'vlaaad.reveal.ext/raw-string)]
      (add-tap #(reset! last-tap %))
      (rx-stream-as-is
       (rx-as
        {:fx/type rx-obs-view
         :ref last-tap
         :fn (fn [x]
               (let [x' (if (var? x) (deref x) x) ; get Var's value
                     c  (class x') ; class of underlying value
                     m  (meta x)   ; original metadata
                     m' (when (var? x) (meta x')) ; underlying Var metadata (if any)
                     ;; if the underlying value is a function
                     ;; and it has a docstring, use that; if
                     ;; the underlying value is a namespace,
                     ;; run ns-publics and display that map:
                     x' (cond
                          (and (fn? x') (or (:doc m) (:doc m')))
                          (or (:doc m) (:doc m'))
                          (= clojure.lang.Namespace c)
                          (ns-publics x')
                          :else
                          x')]
                 {:fx/type :v-box
                  :children
                  ;; in the top box, display metadata
                  [{:fx/type rx-value-view
                    :v-box/vgrow :always
                    :value (cond-> (assoc m :_class c) m' (assoc :_meta m'))}
                   (cond
                     ;; display a string in raw form for easier reading:
                     (string? x')
                     {:fx/type rx-value-view
                      :v-box/vgrow :always
                      :value (rx-stream-as-is (rx-as x' (rx-raw-string x' {:fill :string})))}
                     ;; automatically display URLs using the internal browser:
                     (instance? java.net.URL x')
                     {:fx/type :web-view
                      :url (str x')}
                     ;; else display simple values as a single item in a table:
                     (or (nil? x') (not (seqable? x')))
                     {:fx/type rx-table-view
                      :items [x']
                      :v-box/vgrow :always
                      :columns [{:fn identity :header 'value}
                                {:fn str :header 'string}]}
                     :else ; display the value in a reasonable table form:
                     (let [head (first x')]
                       {:fx/type rx-table-view
                        :items x'
                        :v-box/vgrow :always
                        :columns (cond
                                   (map? head) (for [k (keys head)] {:header k :fn #(get % k)})
                                   (map-entry? head) [{:header 'key :fn key} {:header 'val :fn val}]
                                   (indexed? head) (for [i (range (bounded-count 1024 head))] {:header i :fn #(nth % i)})
                                   :else [{:header 'item :fn identity}])}))]}))}
        (rx-raw-string "right-click > view" {:fill :object}))))
    (catch Throwable t
      (println "Unable to install Reveal extras!")
      (println (ex-message t)))))

(defn start-nrepl
  [env-port]
  (let [server-map (nrepl/start-server
                    :port (or env-port 0)
                    :handler (apply nrepl.server/default-handler
                                    (-> (map #'cider.nrepl/resolve-or-fail cider.nrepl/cider-middleware)
                                        (conj #'refactor-nrepl.middleware/wrap-refactor))))
        port (:port server-map)
        port-file (io/file ".nrepl-port")]
    (.deleteOnExit port-file)
    (spit port-file port)
    (println (ansi/green (str "nREPL client can be connected to port " port)))))

(defn -main
  [& _]
  (start-nrepl (some-> (System/getenv "NREPL_PORT")
                       (Integer/parseInt)))
  ;(tap> (install-reveal-extras))
  (rebel-readline.core/ensure-terminal
   (rebel-readline.clojure.main/repl :print syntax-highlight-fipp))
  (println (ansi/green "Goodbye!"))
  (System/exit 0))
