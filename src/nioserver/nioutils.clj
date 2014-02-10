;   Copyright (c) Johan Astborg. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns nioserver.nioutils
  (:require [clojure.core.async :as async :refer [<! >! timeout chan alt! go pub sub]])
  (:import [java.nio ByteBuffer]
           [java.nio.file OpenOption StandardOpenOption]
           [java.nio ByteBuffer]
           [java.nio.file Paths Files FileSystems]
           [java.util.concurrent TimeUnit Executors]
           [java.nio.channels CompletionHandler
            AsynchronousSocketChannel
            AsynchronousFileChannel
            AsynchronousChannelGroup
            AsynchronousServerSocketChannel]
           [java.nio.channels.spi
            AsynchronousChannelProvider]))

(defn byte-buf [s]
  (letfn [(new-buf [size]
            (ByteBuffer/allocateDirect size))]
    (cond
     (instance? String s)
                (-> (new-buf (.length s))
                    (.put (.getBytes s))
                     .rewind)
     (instance? (Class/forName "[B") s)
                (-> (new-buf (count s))
                    (.put s)
                     .rewind)
     (instance? Number s) (new-buf s)
     true (new-buf 1024))))

(defmacro with-handlers [cbody fbody]
  `(reify CompletionHandler
     (completed [t# r# a#]
       ((~@cbody) t# r# a#))
     (failed [t# e# a#]
       ((~@fbody) t# e# a#))))

(defn read-socket-channel [channel size observer]
  (let [buf (ByteBuffer/allocateDirect size)]
    (.read channel buf nil
      (reify CompletionHandler
        (completed [this cnt _]
          (let [bytes (byte-array cnt)]
            (.flip buf)
            (.get buf bytes)
            (.clear buf)
            (observer (String. bytes))))
        (failed [this e _]
          (.close channel)
          (println "! Failed (read):" e))))))


(defn write-socket-channel [channel string close?]
  (let [bytes (.getBytes string)
        buf (ByteBuffer/allocateDirect (.length string))]
    (.put buf bytes)
    (.rewind buf)
    (.write channel buf nil
      (reify CompletionHandler
        (completed [this cnt _]
          (println "wrote" cnt)
          (read-so-channel channel ch1))
        (failed [this e _]
          (.close channel)
          (println "! Failed (write):" e (.getMessage e)))))))

(defn get-str [buf cnt]
  (let [bytes (byte-array cnt)]
    (.flip buf)
    (.get buf bytes)
    (.clear buf)
    (String. bytes)))

(defn read1 [nc rc nfn]
  (let [buf (byte-buf 1024)]
    (.read nc buf nil
           (with-handler
             (fn [a b c]
               (if (> b 0)
                 (do
                   (println "read" b)
                   (go (>! rc (get-str buf b)))
                   (read1 nc rc nfn))
                 (apply nfn [nc rc])))
             (fn [a b c] (println "err" b c))))))

(defn write1 [nc rc str nfn]
  (let [buf (byte-buf str)]
    (.write nc buf nil
            (with-handler
              (fn [a b c]
                (println "wrote" b)
                (apply nfn [nc rc]))
              (fn [a b c] (println "err" b c))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn read-buf
  [^ByteBuffer buf cnt]
  (when (pos? cnt)
    (let [bytes (byte-array cnt)]
      (.flip buf)
      (.get buf bytes)
      (.clear buf)
      bytes)))

(defn read-sock-ch
  "reads a socket channel using core.async channels"
  [^AsynchronousSocketChannel sch ach]
  (let [buf (byte-buf)]
    (.read sch buf nil
           (with-handlers
             (fn [t cnt a]
               (if-let [bytes (read-buf buf cnt)]
                 (do
                   (go (>! ach bytes))
                   (.read sch buf nil t))
                 (.close ach)))
             (fn [t e a]
               (.close ach)
               (println "! Failed (read):" e)))) ach))

(defn write-sock-ch
  "writes a byte array to a socket channel"
  [^AsynchronousSocketChannel sch bytes]
  (let [buf (byte-buf bytes)]
    (.write sch buf nil
            (with-handlers
              (fn [t cnt a]
                (println "wrote" cnt))
              (fn [t e a]
               (.close sch)
               (println "! Failed (read):" e))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn channel-group []
  (let [executor (Executors/newSingleThreadExecutor)]
       (AsynchronousChannelGroup/withThreadPool executor)))

(let [client (AsynchronousSocketChannel/open (channel-group))]
  (.connect client (java.net.InetSocketAddress. "www.lth.se" 80)
            nil (reify CompletionHandler
                  (completed [this _ _]
                    (write1 client rch "GET / HTTP/1.1\r\nHost: www.example.com\r\n\r\n"
                            (fn [nc rc] (read1 nc rc (fn [_ _] (.close client))))))
                  (failed [this e _]
                    (.close client)
                    (println "! Failed (write):" e (.getMessage e))))))
