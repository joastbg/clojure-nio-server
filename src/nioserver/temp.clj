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

(go (>! chan "hell"))

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

;; nio handler (todo: rename args)
(defmacro with-handler [cbody]
  `(reify CompletionHandler
     (completed [this cnt _]
       ~cbody)
     (failed [this e# _]
       (println "Failed:" (.getMessage e#)))))

(defn read-abc [buf cnt a-ch]
   (let [bytes (byte-array cnt)]
     (.flip buf)
     (.get buf bytes)
     (.rewind buf)
     (go (>! a-ch (String. bytes)))
     (println "\n-------\n*" cnt (String. bytes))))

(defn read-channel
  ([nio-ch size a-ch]
     (read-channel nio-ch size a-ch 0))
  ([nio-ch size a-ch offset]
     (println "Read-channel offset:" offset)
     (let [buf (ByteBuffer/allocateDirect size)]
       (.read nio-ch buf offset nil
              (with-handler
                (do (read-abc buf cnt a-ch)
                    (if (< (+ offset cnt) (.size nio-ch))
                      (read-channel nio-ch size a-ch (+ offset cnt)))))))))

(defn read-so-channel
  ([nio-ch a-ch]
     (read-so-channel nio-ch a-ch 1024))
  ([nio-ch a-ch size]
     (println "Read-socket-channel")
     (let [buf (ByteBuffer/allocateDirect size)]
       (.read nio-ch buf nil
              (with-handler
                (do
                  (println "=====" cnt)
                  (if (> cnt 0)
                    (do (read-abc buf cnt a-ch)
                        (read-so-channel nio-ch a-ch size)))))))))

(def ch1 (chan))

(println (go (<! ch1)))
(go (>! ch1 "hello"))

(defn apa [ch]
  (go (while true
        (let [[v c] (alts! [ch])]
          (println "Read from channel:" v)))))

(apa ch1)

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

(defn read-socket-channel [channel size]
  (let [buf (ByteBuffer/allocateDirect size)]
    (.read channel buf nil
      (reify CompletionHandler
        (completed [this cnt _]
          (let [bytes (byte-array cnt)]
            (.flip buf)
            (.get buf bytes)
            (.clear buf)
            (println (String. bytes))))
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


(defn channel-group []
  (let [executor (Executors/newSingleThreadExecutor)]
       (AsynchronousChannelGroup/withThreadPool executor)))



; (let [assc (AsynchronousServerSocketChannel/open group)

(let [client (AsynchronousSocketChannel/open (channel-group))]
  (.connect client (java.net.InetSocketAddress. "www.google.com" 80)
            nil (reify CompletionHandler
                  (completed [this _ _]
                    (println "Connected...")
                    (write-socket-channel client "GET / HTTP/1.1\r\nHost: www.example.com\r\n\r\n" false))
                  (failed [this e _]
                    (println "! Failed (read):" e)))))

(let [client (AsynchronousSocketChannel/open (channel-group))]
  (.connect client (java.net.InetSocketAddress. "www.google.com" 80)
            nil (with-handler
                    (do (println "Connected...")
                    (write-socket-channel client "GET / HTTP/1.1\r\nHost: www.example.com\r\n\r\n" false)))))

;(nio-pipe! (nio-write "GET / HTTP/1.1....") (nio-read))
;(>>! in out nio-write nio-read)

; nio-channel, read-channel, write-channel
; (nio-pipe! nio-channel read-channel write-channel)


(def client (AsynchronousSocketChannel/open (channel-group)))

(def fut (.connect client (java.net.InetSocketAddress. "www.google.com" 80)))

@fut

(println (read-socket-channel client 1024))

(def channel1 (AsynchronousSocketChannel/connect
               (java.net.InetSocketAddress. "www.google.com" 80)
               nil
               (with-handler
                 (println cnt))))



(chan 10)

(let [c (chan)]
  (thread (>!! c "hello"))
  (assert (= "hello" (<!! c)))
  (close! c))


(let [c1 (chan)
      c2 (chan)]
  (go (while true
        (let [[v ch] (alts! [c1 c2])]
          (println "Read" v "from" ch))))
  (go (>! c1 "hi"))
  (go (>! c2 "there")))


(def cs [(chan) (chan) (chan)])

(let [[v c] (alts!! cs)]
  (println "Read" v "msg" c) "ms")

(go (>! (nth cs 1) "hello"))

(let [c1 (chan)
      c2 (chan)]
  (go (while true
        (let [[v ch] (alts! cs)]
          (println "Read" v "from" ch))))
  (go (>! c1 "hi"))
  (go (>! c2 "there")))
