;   Copyright (c) Johan Astborg. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns nioserver.core
  (:use nioserver.http)
  (:use nioserver.request))

(comment
(defn test-buffer [string]
  (let [buf (java.nio.ByteBuffer/allocateDirect string)]
    (.put buf (.getBytes string))
    (.rewind buf)))
)

(defn test-buffer2 [string]
  (.rewind (java.nio.ByteBuffer/wrap string)))

(defn read-socket-channel [channel size observer]
  (let [buf (java.nio.ByteBuffer/allocateDirect size)]
    (.read channel buf nil
      (reify java.nio.channels.CompletionHandler
        (completed [this cnt _]
          (let [bytes (byte-array cnt)]
            (.flip buf)
            (.get buf bytes)
            (.clear buf)
            (observer (String. bytes))))
            ;(.close channel)))
        (failed [this e _]
          (.close channel)
          (println "! Failed (read):" e))))))

;(defn test-bytes [str]
;  (byte-array [(byte -127) (byte 0x06) (byte 0x48) (byte 0x48) (byte 0x65) (byte 0x6c) (byte 0x6c) (byte 0x6f)]))

; a hack, need to check if length > 127 etc.
(defn create-ws-msg [str]
  (let [len (.length str)
        init (byte-array [(byte -127) (byte len)])]
    (byte-array (concat init (.getBytes str)))))

;(type (byte-array (concat test-bytes (.getBytes "johan"))))

;; todo, same for file channel, move to nio.clj
(defn write-socket-channel [channel string close?]
  (let [bytes (.getBytes string)
        buf (java.nio.ByteBuffer/allocateDirect (.length string))]
    (.put buf bytes)
    (.rewind buf)
    (.write channel buf nil
      (reify java.nio.channels.CompletionHandler
        (completed [this cnt _]
          ; todo: cleanup
          (if close? (.close channel))
          (if (not close?) (.write channel (test-buffer2 (create-ws-msg "hello world!!!")))))
        (failed [this e _]
          (.close channel)
          (println "! Failed (write):" e (.getMessage e)))))))


(def ws-connections (agent {}))

(defn do-stuff [a]
  (Thread/sleep 5000)
  (println a "sending ws frame...")
  a)

(comment
(send ws-connections assoc :johan "apa")
;@ws-connections

(defn apa [& a]
  (println a))


(send-off ws-connections do-stuff)
@ws-connections
)



(defn handler [listener]
  (reify java.nio.channels.CompletionHandler
    (completed [this sc _]
      (.accept listener nil this)
      ;(println {:address (.getRemoteAddress sc)})
      (letfn [(observer [str]
                (let [request (parse-request str)]
                  (println "** Raw request:" str)
                  (println "* New request:" request)
                  (write-socket-channel sc (http-handle-request request) (not (= (:upgrade request) "websocket")))
                  ))]
                  ;(send ws-connections assoc :the-conn sc)
                  ;(send-off ws-connections do-stuff)))]
        (read-socket-channel sc 1024 observer)))))

(defn channel-group []
  (let [executor (java.util.concurrent.Executors/newSingleThreadExecutor)]
       (java.nio.channels.AsynchronousChannelGroup/withThreadPool executor)))

(defn start-server [group port]
  (let [assc (java.nio.channels.AsynchronousServerSocketChannel/open group)
        sa (java.net.InetSocketAddress. port)]
    (let [listener (.bind assc sa)]
          (.accept listener nil (handler listener)))))

(def default-port 8080)

(defn parse-options [args]
  (letfn [(parse-port [args]
            (if (re-find #"^\d+$" (first args))
              (read-string (first args))
              (do (println "* Port has to be a number, using default port.")
              default-port)))]
    {:port (parse-port args)}))

(defn -main [& args]
  (println "-- NIO-Server 0.01\n")
  (let [group (channel-group)
        time java.lang.Long/MAX_VALUE
        units java.util.concurrent.TimeUnit/SECONDS
        options (parse-options args)]
    (println "* Listening on port:" (:port options))
    (start-server group (:port options))
    (.awaitTermination group time units)))
