(ns nio.server
  (:use nio.http)
  (:use nio.websocket))

;; serves static files using a cache (using hash map)
;; todo: WatchService for changes in dir, then cache
;; some inspiration https://gist.github.com/moonranger/4023683

(def chm (java.util.concurrent.ConcurrentHashMap.))

;(.get chm "index.html")
(.put chm "index.html" "<h1>Johans hemsida</h1>lite annat skit....")

(defn serve-static [file]
  (.get chm file))


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
          (println "Failed (read):" e))))))

(defn write-socket-channel [channel string]
  (let [bytes (.getBytes string)
        buf (java.nio.ByteBuffer/allocateDirect (.length string))]
    (.put buf bytes)
    (.rewind buf)
    (.write channel buf nil
      (reify java.nio.channels.CompletionHandler
        (completed [this cnt _]
          (println {:event :write :count cnt})
          (.close channel))
        (failed [this e _]
          (.close channel)
          (println "Failed (write):" e (.getMessage e)))))))

(defn parse-method [lines]
  (let [[method path proto] lines]
  {:method method :path path :proto proto}))

;; tests
;(parse-method "GET /favicon.ico HTTP/1.1")
;(parse-method "GET / HTTP/1.1")

(defn parse-request [req-str]
  (let [lines (clojure.string/split req-str #"\s")]
    (parse-method lines)))

(defn handler [listener]
  (reify java.nio.channels.CompletionHandler
    (completed [this sc _]
      (.accept listener nil this)
      (println {:address (.getRemoteAddress sc)})
      (letfn [(observer [str]
                (println "Request:" (parse-request str))
                (write-socket-channel sc (http-str-reply (serve-static "index.html"))))]
        (read-socket-channel sc 1024 observer)))))

(defn channel-group []
  (let [executor (java.util.concurrent.Executors/newSingleThreadExecutor)]
       (java.nio.channels.AsynchronousChannelGroup/withThreadPool executor)))

(defn start-server [group]
  (let [assc (java.nio.channels.AsynchronousServerSocketChannel/open group)
        sa (java.net.InetSocketAddress. 8080)]
    (let [listener (.bind assc sa)]
          (.accept listener nil (handler listener)))))

(defn -main [& args]
  (println "-- Web server ")
  (let [group (channel-group)]
    (start-server group)
    (.awaitTermination group java.lang.Long/MAX_VALUE java.util.concurrent.TimeUnit/SECONDS)))
