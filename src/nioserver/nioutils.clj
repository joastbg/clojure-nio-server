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
           [java.nio.channels CompletionHandler
            AsynchronousFileChannel
            AsynchronousChannelGroup
            AsynchronousServerSocketChannel]))

(defn read-socket-channel [channel size]
  (let [buf (ByteBuffer/allocateDirect size)]
    (.read channel buf 0 nil
      (reify CompletionHandler
        (completed [this cnt _]
          (let [bytes (byte-array cnt)]
            (.flip buf)
            (.get buf bytes)
            (.clear buf)
            (println "Read (" cnt "): " (String. bytes))))
        (failed [this e _]
          (.close channel)
          (println "! Failed (read):" e))))))

;; test with file

(def root-dir "/home/gecemmo/Development/clojure-nio-server/resources/")

(defn open-file [path]
  (println "* Opening file:" path)
  (AsynchronousFileChannel/open path (into-array OpenOption [StandardOpenOption/READ])))

(def test-path (Paths/get "/home/gecemmo/Development/clojure-nio-server/resources" (into-array String ["junk.txt"])))
(def test-channel (open-file test-path))

(read-socket-channel test-channel 1024)
; (defn read-nio-channel [channel])

(let [c1 (chan)
      c2 (chan)]
  (go (while true
        (let [[v ch] (alts! [c1 c2])]
          (println "Read" v "from" ch))))
  (go (>! c1 "hi"))
  (go (>! c2 "there")))

(def ch1 (chan))

(println (go (<! ch1)))
(go (>! ch1 "hello"))

(defn apa [ch]
  (go (while true
        (let [[v c] (alts! [ch])]
          (println "Read from channel:" v)))))

(apa ch1)

;; nio handler
(defmacro with-handler [cbody fbody]
  `(reify CompletionHandler
     (completed [_ cnt _]
       ~cbody)
     (failed [_ e _]
       ~fbody)))

(clojure.pprint/pprint
 (macroexpand-1 '(with-handler (println cnt) (println "Failed"))))



(defn read-channel [nio-ch size a-ch]
  (let [buf (ByteBuffer/allocateDirect size)]
    (.read nio-ch buf 0 nil
      (reify CompletionHandler
        (completed [this cnt _]
          (let [bytes (byte-array cnt)]
            (.flip buf)
            (.get buf bytes)
            (.clear buf)
            (go (>! a-ch (String. bytes)))
            (println "---------\nRead (" cnt "): " (String. bytes))



            ))
        (failed [this e _]
          (.close nio-ch)
          (println "! Failed (read):" e))))))

;; nio handler
(defmacro with-handler [cbody]
  `(reify CompletionHandler
     (completed [_ cnt _]
       ~cbody)
     (failed [_ e# _]
       (println "Failed:" (.getMessage e#)))))

(defn read-abc [buf cnt]
   (let [bytes (byte-array cnt)]
     (.flip buf)
     (.get buf bytes)
     (.rewind buf)
     (println "\n-------\n*" cnt (String. bytes))))

(defn read-channel
  ([nio-ch size a-ch]
     (read-channel nio-ch size a-ch 0))
  ([nio-ch size a-ch offset]
     (println "Read-channel offset:" offset)
     (let [buf (ByteBuffer/allocateDirect size)]
       (.read nio-ch buf offset nil
              (with-handler
                (do (read-abc buf cnt)
                    (if (< (+ offset cnt) (.size nio-ch))
                      (read-channel nio-ch size a-ch (+ offset cnt)))))))))


(defn apan ([a] (println "a" a)) ([a b] (println "a b" a b)))

(apan 1)
(apan 1 2)

(read-channel test-channel 1024 ch1)
(.close test-channel)

;; pub / sub

(def pch (chan))
(def sch (chan))

(defn topic-fn [val]
  (println "topic-fn:" val)
  "hello")

(def p (pub pch topic-fn))

(sub p "hello" sch)
(go (>! pch "hi world"))

(defn apa [ch]
  (go (while true
        (let [[v c] (alts! [ch])]
          (println "Read from channel:" v)))))

(apa sch)
