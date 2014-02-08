;   Copyright (c) Johan Astborg. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns nioserver.request)

(defn parse-method [str]
  (keyword str))

; todo: support upgrade
(defn parse-proto [str]
  (if (= "HTTP/1.1" str)
    :HTTP11
    :Unsupported))

(defn parse-conn [str]
  (if (= "keep-alive" str)
    :Keep-Alive
    :Unsupported))

(defn parse-first [lines]
  (let [[method path proto] lines]
  {:method method :path path :proto proto}))

(defn make-keyword [str]
  (-> str
      clojure.string/trim
      clojure.string/lower-case
      keyword))

(defn into-request-map [lines]
  (into {}
    (cons (parse-first (clojure.string/split (first lines) #"\s"))
      (map #(hash-map (make-keyword (first %1)) (second %1))
        (map #(clojure.string/split %1 #": ") (next lines))))))

;; TODO: use macro to generate assoc for rmap
(defn do-stuff [a]
  `(~a (parse-method (~a rmap))))

`(assoc rmap ~@(do-stuff :apa) ~@(do-stuff :proto))

`(:method (parse-moethod (:method rmap)))
`(assoc apa ~@(map #(list %1 `(parse-method (~%1 apa))) [:method :proto :connection]))

`(1 2 3 (1 2 3) ())

`(+ ~(list 1 2 3))

`(+ ~@(list 1 2 3))

(defn parse-request [req-str]
  (let [lines (clojure.string/split req-str #"\r\n")]
    (let [rmap (into-request-map lines)]
      (assoc rmap
        :method (parse-method (:method rmap))
        :proto (parse-proto (:proto rmap))
        :connection (parse-conn (:connection rmap))))))
