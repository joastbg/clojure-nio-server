;   Copyright (c) Johan Astborg. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns nioserver.http
  (:use nioserver.websocket nioserver.files)
  (:import [org.apache.commons.codec.binary Base64]
           [java.util Calendar]
           [java.text SimpleDateFormat]
           [java.security MessageDigest]))

;; http code goes here

(def GUID "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")

(defn get-date-str []
  (let [calendar (Calendar/getInstance)
        format   (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss z")]
    (.format format (.getTime calendar))))

(defn http-str-reply [content]
  (let [content-length (count content)]
  (clojure.string/join "\r\n"
                       ["HTTP/1.1 200 OK"
                        (str "Date: " (get-date-str))
                        "Server: Raptor/0.01 (Unix)"
                        (str "Content-length: " content-length)
                        "Content-type: text/html; charset=UTF-8"
                        ""
                        content])))

(defn security-digest [key]
  (let [sha1 (MessageDigest/getInstance "SHA1")]
    (clojure.string/trim (.encodeToString (Base64.)
      (.digest sha1 (.getBytes (str (clojure.string/trim key) GUID)))))))

(defn handle-websocket-request [request]
  (println "* Handle websocket handshake:" (:sec-websocket-key request) (:origin request))
  (clojure.string/join "\r\n"
                       ["HTTP/1.1 101 Switching Protocols"
                        "Upgrade: websocket"
                        "Connection: Upgrade"
                        (str "Sec-WebSocket-Accept: " (security-digest (:sec-websocket-key request)))
                        ""
                        ""]))

(defn http-handle-request [request]
  (if (= (:upgrade request) "websocket")
    (handle-websocket-request request)
    (http-str-reply (serve-static (:path request)))))

(comment
  ;; using example from http://tools.ietf.org/html/rfc6455
  (= (security-digest "dGhlIHNhbXBsZSBub25jZQ==") "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=")
)
