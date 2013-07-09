(ns leiningen.varspotting
  "Spot Clojure Vars with or without a project."
  (:require [varspotting.core
             :refer [clojure-namespaces
                     with-default-spotters
                     print-spotting-report]]
            [clojure.set :as set]))

(defn run-report
  ([heading nss]
     (println (str "Varspotting report for " heading ":"))
     (println (apply str (repeat (min 80 (+ (count heading) 24)) \= )) "")
     (run-report nss)
     (println))
  ([nss]
     (with-default-spotters
       (print-spotting-report nss
         "Public"           :public
         "Unbound"          :unbound
         "Dynamic"          :dynamic
         "Proper functions" :non-macro-fns
         "Macros"           :macro-fns
         "Non-fn IFns"      (set/union :maps :vectors :sets)
         "Dynamic proper functions"
         (set/intersection :dynamic :non-macro-fns)))))

(defn ^:no-project-needed varspotting
  ([project]
     (run-report "clojure.core" '[clojure.core])
     (run-report "built-in namespaces" clojure-namespaces))
  ([project & nss]
     (let [syms (map symbol nss)]
       (doseq [sym syms]
         (require sym))
       (run-report (apply str (interpose ", " nss)) syms))))
