;   Copyright (c) Johan Astborg. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns nioserver.http)

;; http code goes here

(defn get-date-str []
  (let [calendar (java.util.Calendar/getInstance)
        format   (java.text.SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss z")]
    (.format format (.getTime calendar))))

(defn http-str-reply [content]
  (let [content-length (count content)]
  (clojure.string/join "\n"
                       ["HTTP/1.1 200 OK"
                        (str "Date: " (get-date-str))
                        "Server: Raptor/0.01 (Unix)"
                        (str "Content-length: " content-length)
                        "Content-type: text/html; charset=UTF-8"
                        ""
                        content])))
