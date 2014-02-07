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

(defn parse-request [req-str]
  (let [lines (clojure.string/split req-str #"\r\n")]
    (let [rmap (into-request-map lines)]
      (assoc rmap
        :method (parse-method (:method rmap))
        :proto (parse-proto (:proto rmap))
        :connection (parse-conn (:connection rmap))
      ))))

(comment

(def test-req "GET /index.html HTTP/1.1\r\n
Host: localhost:8888\r\n
Connection: keep-alive\r\n
Cache-Control: max-age=0\r\n
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n
User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/32.0.1700.102 Chrome/32.0.1700.102 Safari/537.36\r\n
Accept-Encoding: gzip,deflate,sdch\r\n
Accept-Language: en-US,en;q=0.8\r\n")

;; tests

(parse-request test-req)

)
