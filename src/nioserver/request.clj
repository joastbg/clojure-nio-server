;   Copyright (c) Johan Astborg. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns nioserver.request)

(defn parse-method [lines]
  (let [[method path proto] lines]
  {:method method :path path :proto proto}))

(defn parse-request [req-str]
  (let [lines (clojure.string/split req-str #"\s")]
    (parse-method lines)))

;; tests
;(parse-method "GET /favicon.ico HTTP/1.1")
;(parse-method "GET / HTTP/1.1")
