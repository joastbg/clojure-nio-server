;   Copyright (c) Johan Astborg. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns nioserver.nioutils
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

(def test-path (Paths/get "/home/gecemmo/Development/clojure-nio-server/resources" (into-array String ["index.html"])))
(def test-channel (open-file test-path))

(read-socket-channel test-channel 1024)
; (defn read-nio-channel [channel])
