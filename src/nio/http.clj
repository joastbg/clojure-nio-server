(ns nio.http)

;; http code goes here

(defn get-date-str []
  (let [calendar (java.util.Calendar/getInstance)
        format   (java.text.SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss z")]
    (.format format (.getTime calendar))))

(defn http-str-reply [content]
  (let [content-length (.length content)]
  (clojure.string/join "\n"
                       ["HTTP/1.1 200 OK"
                        (str "Date: " (get-date-str))
                        "Server: Raptor/0.01 (Unix)"
                        (str "Content-length: " content-length)
                        "Content-type: text/html; charset=UTF-8"
                        ""
                        content])))
