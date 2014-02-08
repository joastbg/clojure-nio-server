;   Copyright (c) Johan Astborg. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns nioserver.files
  (:import [java.nio.file OpenOption StandardOpenOption])
  (:import [java.util.concurrent ConcurrentHashMap])
  (:import [java.nio.channels CompletionHandler AsynchronousFileChannel])
  (:import [java.nio ByteBuffer])
  (:import [java.nio.file Paths Files]))

;; serves static files using a cache (using hash map)
;; todo: WatchService for changes in dir, then cache
;; todo: keep bytes in cache
;; some inspiration https://gist.github.com/moonranger/4023683

(def root-dir "/home/gecemmo/Development/clojure-nio-server/resources/")

(defn file-not-found [file] (str "<h1>Sweet file not found</h1>" file))

;; consider using STM instead, refs
(def chm (ConcurrentHashMap.))

(defn serve-static [file]
  (let [sfile (if (= \/ (first file)) (subs file 1) file)]
  (println "wants file:" sfile)
  (if (.containsKey chm sfile) (String. (.get chm sfile))
      (file-not-found file))))

(defn read-file-channel [channel size callback]
  (let [buf (ByteBuffer/allocateDirect size)]
    (.read channel buf 0 nil
      (reify CompletionHandler
        (completed [this cnt _]
          (let [bytes (byte-array cnt)]
            (.flip buf)
            (.get buf bytes)
            (.clear buf)
            (callback bytes)
            (.close channel)))
        (failed [this e _]
          (.close channel)
          (println "! Failed (read):" e))))))

(defn open-file [path]
  (println "* Opening file:" (.toString (.getFileName path)))
  (AsynchronousFileChannel/open path (into-array OpenOption [StandardOpenOption/READ])))

; todo: read in chunks, or get filesize?
(defn read-and-cache [path]
  (println "* Caching file:" (.toString (.getFileName path)))
  (let [file-channel (open-file path)
        filename (.toString (.getFileName path))]
    (read-file-channel file-channel 2048 #(.put chm filename %1))))

(defn cache-files [root-dir]
  (let [root-path (Paths/get root-dir (into-array String []))]
    (doseq
        [entry (Files/newDirectoryStream root-path)]
      (read-and-cache entry))))

;; load files and put them in cache
(cache-files root-dir)
