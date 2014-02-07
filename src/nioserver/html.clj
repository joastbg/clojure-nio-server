;   Copyright (c) Johan Astborg. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns nioserver.html
  (:require [clojure.zip :as zip])
  (:require [clojure.walk :as walk]))

; html generation

; site with pages as tree structure
; build using zippers
(comment
(def test-site '(site (page "Start page" (page "About") (page "Contact"))))

(def root-loc (zip/seq-zip test-site))

(zip/node (zip/down root-loc))

(-> root-loc zip/down zip/right zip/down zip/right zip/right zip/right zip/down zip/right zip/node)
(-> root-loc zip/down zip/right zip/down zip/end?)

(def data [[1 :foo] [2 [3 [4 "abc"]] 5]])
(defn f [x] (do (println "visiting:" x) x))

(walk/prewalk f data)

;; html
(defn f2 [x] (do (println (type x))))
(walk/prewalk f2 test-site)
)
