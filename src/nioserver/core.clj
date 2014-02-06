;   Copyright (c) Johan Astborg. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns nioserver.core
  (:use nioserver.http)
  (:use nioserver.websocket)
  (:use nioserver.files)
  (:use nioserver.request))

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

(defn write-socket-channel [channel string]
  (let [bytes (.getBytes string)
        buf (java.nio.ByteBuffer/allocateDirect (.length string))]
    (.put buf bytes)
    (.rewind buf)
    (.write channel buf nil
      (reify java.nio.channels.CompletionHandler
        (completed [this cnt _]
          ;(println {:event :write :count cnt})
          (.close channel))
        (failed [this e _]
          (.close channel)
          (println "! Failed (write):" e (.getMessage e)))))))

(defn handler [listener]
  (reify java.nio.channels.CompletionHandler
    (completed [this sc _]
      (.accept listener nil this)
      ;(println {:address (.getRemoteAddress sc)})
      (letfn [(observer [str]
                (let [request (parse-request str)]
                  (println "** Raw request:" str)
                  (println "* New request:" request)
                  (write-socket-channel sc (http-str-reply (serve-static (:path request))))))]
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
