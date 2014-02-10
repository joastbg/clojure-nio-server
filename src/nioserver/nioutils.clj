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

; 1: HTTP-CLIENT: (with-pipe! p (with-writer! writer) (with-reader! reader))
; 2: HTTP-SERVER: (with-pipe! p (with-reader! reader) (with-writer! writer))
; 3: WS-SERVER:   (with-pipe! p (with-reader! reader) (with-writer! writer))

(let [bytes (.getBytes string)
        buf (ByteBuffer/allocateDirect (.length string))]
    (.put buf bytes)
    (.rewind buf))

(type (ByteBuffer/allocateDirect 1024))

(defn byte-buf [s]
  (letfn [(new-buf [size] (ByteBuffer/allocateDirect size))]
    (cond
     (instance? String s) (-> (new-buf (.length s))
                              (.put (.getBytes s))
                              .rewind)
     (instance? Number s) (new-buf s)
     true (new-buf 1024))))

(byte-buf 1024)
(byte-buf "johan")

(defmacro with-handler [cbody fbody]
  `(reify CompletionHandler
     (completed [t# r# a#]
       ((~@cbody) t# r# a#))
     (failed [t# e# a#]
       ((~@fbody) t# e# a#))))

(let [mystr 1]
  (cond
   (instance? String mystr) (println "is string")
   (instance? Number mystr) (println "is number")
    true (println "Other...")))

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

(apply (fn [a b] (println a b)) [1 2])

(nio-pipe! (write! "hello") (read!))

(clojure.pprint/pprint
 (macroexpand-1 '(with-handler
              (fn [a b c] (println "okk" a b c))
              (println "err"))))

(defn channel-group []
  (let [executor (Executors/newSingleThreadExecutor)]
       (AsynchronousChannelGroup/withThreadPool executor)))

(def wch (chan))
(def rch (chan))

(defn apa [ch]
  (go (doseq [n [1 2 3 4 5 6 7 8 9 10 11]]
        (let [[v c] (alts! [ch])]
          (println "Read from channel:" v)))))

(apa rch)
(apa wch)

(go (>! wch "GET / HTTP/1.1\r\nHost: www.example.com\r\n\r\n"))
(go (println "aa" (<! wch)))

(let [client (AsynchronousSocketChannel/open (channel-group))]
  (.connect client (java.net.InetSocketAddress. "www.lth.se" 80)
            nil (reify CompletionHandler
                  (completed [this _ _]
                    (write1 client rch "GET / HTTP/1.1\r\nHost: www.example.com\r\n\r\n"
                            (fn [nc rc] (read1 nc rc (fn [_ _] (.close client))))))
                  (failed [this e _]
                    (.close client)
                    (println "! Failed (write):" e (.getMessage e))))))
