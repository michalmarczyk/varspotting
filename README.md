# Varspotting

Spotting Clojure Vars for fun and profit!

Inspired by the
[Roughly how many functions are in the Clojure core libraries?](http://stackoverflow.com/questions/17524906/roughly-how-many-functions-are-in-the-clojure-core-libraries)
Stack Overflow question.

## Usage

Varspotting reports counts of Clojure Vars meeting certain criteria.

Here's the default report on built-in Clojure Vars (for Clojure
1.5.1):

```
Varspotting report for clojure.core:
====================================

|                  Spotter | Var count |
|--------------------------+-----------|
|                   Public |       591 |
|                  Unbound |         2 |
|                  Dynamic |        11 |
|         Proper functions |       475 |
|                   Macros |        76 |
|              Non-fn IFns |         6 |
| Dynamic proper functions |         1 |

Varspotting report for built-in namespaces:
===========================================

|                  Spotter | Var count |
|--------------------------+-----------|
|                   Public |       844 |
|                  Unbound |         6 |
|                  Dynamic |        39 |
|         Proper functions |       670 |
|                   Macros |        99 |
|              Non-fn IFns |        17 |
| Dynamic proper functions |         6 |
```


Varspotting can be used as a library or a Leiningen plugin. The
Leiningen dependency specifier is

```clj
[varspotting "0.0.2"]
```

Including this dependency in the `:user` profile will make Varspotting
available in all Leiningen projects:

```clj
;; in ~/.lein/profiles.clj
{:user {:plugins [[varspotting "0.0.2"]]}}
```

Note that Varspotting currently has no dependencies besides Clojure.

With the above `profiles.clj` entry in place, the following will print
the default report for Clojure namespaces specified above:

```
lein varspotting
```

Alternatively, you can say

```
lein varspotting <namespaces-to-report-on>
```

to get a report on the specified namespaces. In this way, Varspotting
can be used to report on arbitrary Clojure libraries. Here's the
default report on Varspotting 0.0.2:

```
Varspotting report for varspotting.core, leiningen.varspotting:
===============================================================

|                  Spotter | Var count |
|--------------------------+-----------|
|                   Public |        23 |
|                  Unbound |         0 |
|                  Dynamic |         1 |
|         Proper functions |        17 |
|                   Macros |         4 |
|              Non-fn IFns |         2 |
| Dynamic proper functions |         0 |
```
    
The `varspotting.core` namespace includes docstrings describing all
the building blocks behind the final report. Of particular interest
are the `spotter`, `defspotter` and `print-spotting-report` macros and
the `*spotters*` dynamically-rebindable Var; these can be used to
prepare custom reports. For now, to use custom spotters alongside the
full suite of built-in spotters, one can use

```clj
(with-default-spotters
  (binding [*spotters* (into *spotters* custom-spotters)]
    (print-spotting-report namespaces
      ...)))
```

See `leiningen.varspotting` for the actual function handling user
interaction.

## Licence

Copyright © 2013 Michał Marczyk

Distributed under the Eclipse Public License, the same as Clojure.
