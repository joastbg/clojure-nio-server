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
