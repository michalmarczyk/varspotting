(ns varspotting.core
  "Spotting Clojure Vars for fun and profit!

  This namespace defines the spotter and defspotter macros, a
  collection of basic spotters and tools for calling spotters and
  printing spotting returns. See leiningen.varspotting for an example
  of usage."
  (:require [clojure.walk :as walk]
            [clojure.pprint :refer [print-table]])
  (:import (clojure.lang Var Var$Unbound)))

(def clojure-namespaces
  '[clojure.core
    clojure.data
    clojure.edn
    clojure.inspector
    clojure.instant
    clojure.java.browse
    clojure.java.javadoc
    clojure.java.io
    clojure.java.shell
    clojure.main
    clojure.pprint
    clojure.reflect
    clojure.repl
    clojure.set
    clojure.stacktrace
    clojure.string
    clojure.template
    clojure.test
    clojure.walk
    clojure.xml
    clojure.zip])

(apply require clojure-namespaces)

(defn unbound?
  "Checks whether x is a value marking a Var as unbound."
  [x]
  (instance? Var$Unbound x))

(defn proper-fn?
  "Checks whether x is a proper function (as created by fn, in
  contrast to callable values such as maps or vectors).

  NB. operates by excluding known \"improper\" function-like types."
  [x]
  (and (ifn? x)
       (not ((some-fn var? unbound? map? vector? set?) x))))

(defn lift
  "Lifts f to IDeref."
  [f]
  (comp f deref))

(defn ^:spotter public
  "Spotter for public Vars."
  [nss]
  (->> nss
       (map ns-publics)
       (map #(reduce-kv (fn [m k v]
                          (assoc m (symbol (name (.. v -ns -name)) (name k)) v))
                        {}
                        %))
       (apply merge)
       vals
       set))

(defn ^:private spotter-tail [name docstring? stages]
  (let [docstring (if (string? docstring?) docstring?)
        stages    (if docstring
                    stages
                    (cons docstring? stages))]
    `(~(with-meta name
         (if docstring
           {:spotter true :doc docstring}
           {:spotter true}))
      [~'nss]
      (->> ~'nss
           public
           ~@stages
           set))))

(defmacro spotter
  "Creates a spotter, that is a function taking a collection of
  namespaces or namespace-naming symbols and returning a set of public
  Vars from those namespaces."
  [name docstring? & stages]
  `(fn ~@(spotter-tail name docstring? stages)))

(defmacro defspotter
  "Defines a spotter, that is a function taking a collection of
  namespaces or namespace-naming symbols and returning a set of public
  Vars from those namespaces.

  Vars created with defspotter are marked with the :spotter metadata
  key."
  [name docstring? & stages]
  (let [docstring (if (string? docstring?) docstring?)
        stages (if docstring
                 stages
                 (cons docstring? stages))]
    `(defn ~@(spotter-tail name docstring? stages))))

(defspotter dynamic
  "Spotter for dynamic Vars."
  (filter #(-> % meta (contains? :dynamic))))

(defspotter unbound
  "Spotter for Vars which are initially unbound."
  (filter (lift unbound?)))

(defspotter ifns
  "Spotter for Vars holding arbitrary function-like values.
  NB. this includes Unbound!"
  (filter (lift ifn?)))

(defspotter non-ifns
  "Spotter for Vars holding non-function-like values."
  (remove (lift ifn?)))

(defspotter non-macro-fns
  "Spotter for non-macro Vars holding proper functions (as created by
  fn, in contrast to callable values such as maps or vectors).

  NB. operates by excluding known \"improper\" function-like types."
  (filter (lift proper-fn?))
  (remove #(.isMacro ^Var %)))

(defspotter macro-fns
  "Spotter for Vars registered as holding macro expanders."
  (filter #(.isMacro ^Var %)))

(defspotter maps
  "Spotter for Vars holding maps."
  (filter (lift map?)))

(defspotter vectors
  "Spotter for Vars holding vectors."
  (filter (lift vector?)))

(defspotter sets
  "Spotter for Vars holding sets."
  (filter (lift set?)))

(def ^:dynamic *spotters*
  "This Var should be bound to a collection of Vars holding spotters
  during calls to spot-all and print-spotting-report.

  Defaults to nil; see also with-default-spotters."
  nil)

(defn var-name [^Var v]
  (let [ns-name (.. v -ns -name)
        sym     (. v -sym)]
    (symbol (name ns-name) (name sym))))

(defn ^:private var-name* [^Var v]
  (if (identical? (.-ns v) (the-ns 'varspotting.core))
    (.-sym v)
    (var-name v)))

(defn spot-all
  "Returns a map of spotting results for the spotters currently bound
  to *spotters*. The keys are keywords naming the spotters; name only
  for spotters from varspotting.core, fully qualified otherwise."
  [nss]
  (zipmap (map (comp keyword var-name*) *spotters*)
          (map #(% nss) *spotters*)))

(defmacro with-default-spotters
  "Evaluates body with *spotters* dynamically bound to the default
  collection of spotters from varspotting.core."
  [& body]
  (let [spotters ((spotter spotters (filter #(-> % meta :spotter)))
                  '[varspotting.core])
        spotter-var-forms (mapv (comp #(list 'var %) var-name) spotters)]
    `(binding [*spotters* ~spotter-var-forms]
       ~@body)))

(defmacro print-spotting-report
  "Prints spotting report using the collection of spotters found in
  *spotters* in the current dynamic context.

  Clauses are pairs of a string naming the spotting result and an
  expression involving keywords naming spotters in which the keywords
  will be replaced by the sets of Vars returned from the corresponding
  spotters. Spotters from varspotting.core may be referred with
  non-namespace-qualified keywords (although they still must be
  present in *spotters* to be available).

  Example:

    (set/union :sets :maps)
    ;; will be replaced by
    ;; (set/union <actual-set-of-set-holding-vars>
    ;;            <actual-set-of-map-holding-vars>)

  For the final report, the counts of the resulting sets will be
  used."
  [nss & clauses]
  (let [results-sym (gensym "spotting_results__")
        clauses     (partition 2 clauses)]
    `(let [~results-sym (spot-all ~nss)]
       (print-table
        ["Spotter" "Var count"]
        ~(mapv (fn [[heading input]]
                 `(let [res# ~(walk/postwalk (fn [x]
                                               (if (keyword? x)
                                                 (list results-sym x)
                                                 x))
                                             input)]
                    {"Spotter"   ~heading
                     "Var count" (count res#)}))
               clauses)))))
