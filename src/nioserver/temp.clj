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
  (go (doseq [n [1 2 3]]
        (let [[v c] (alts! [ch])]
          (println "Read from channel:" (take 2 v))))))

(apa ch1)
(clojure.core.async/close! ch1)

;; nio handler
(defmacro with-handler [cbody fbody]
  `(reify CompletionHandler
     (completed [_ cnt _]
       ~cbody)
     (failed [_ e _]
       ~fbody)))

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



;(simple 8888)
(comment
(char (bit-xor 61829 30807))
; 0111100001010111
(bit-and -15 0xFF)
(bit-and -60 0xFF)
(bit-xor (bit-or (bit-shift-left (bit-and -15 0xFF) 8) (bit-and -60 0xFF)) 30807)
(bit-xor (bit-or (bit-shift-left (bit-and 11 0xFF) 8) (bit-and 56 0xFF)) 30807)

(char 137)
(char 147)
(char 115)
(char 111)

(def ws-msg [-127 -119 120 87 -15 -60 11 56 -100 -95 88 51 -112 -80 25])

(defn to-bytes [a b m]
  (let [octet (bit-xor (bit-or (bit-shift-left (bit-and a 0xFF) 8) (bit-and b 0xFF)) 30807)]
    (list (bit-shift-right octet 8) (bit-and octet 0xFF))))


; decode web-socket message
(let [[f1 f2 f3 f4 f5 f6] ws-msg
      msg (drop 6 ws-msg)]
  (println "mask?" (bit-shift-right (bit-and f2 0xFF) 7))
  (println "len" (bit-and f2 0x7f))
  ;  (println "mask" (bit-or (bit-shift-left f3 8) f4))
  (println "mask" f3)
  ;  (println (map #(bit-xor %1 f3) msg)))
  (println (apply str (map #(char (bit-xor %1 %2)) msg (cycle [f3 f4 f5 f6])))))
  ;(println (flatten (map #(to-bytes (first %1) (second %1) 120) (partition 2 msg)))))
)





;;;;;;;;;

;; todo, same for file channel, move to nio.clj
(defn write-bytes [channel bytes close?]
  (let [buf (ByteBuffer/allocateDirect (count bytes))]
    (.put buf bytes)
    (.rewind buf)
    (.write channel buf nil
      (reify CompletionHandler
        (completed [this cnt _])
        (failed [this e _]
          (.close channel)
          (println "! Failed (write):" e (.getMessage e)))))))

(defn info [s]
  (println "* INFO: " s))

(defn listen-ch
  "return a channel which listens on port, values in the channel are scs of
   AsynchronousSocketChannel"
  ([port]
     (listen-ch port (chan)))
  ([port ch]
     (let [^AsynchronousServerSocketChannel listener
           (-> (AsynchronousServerSocketChannel/open)
               (.bind (InetSocketAddress. port)))
           handler (reify CompletionHandler
                     (completed [this sc _]
                       (info {:event :connected :address (.getRemoteAddress sc)})
                       (go (>! ch sc))
                       (.accept listener nil this)))]
       (.accept listener nil handler)
       ch)))

(defn read-buf!
  [^ByteBuffer buf cnt]
  (when (pos? cnt)
    (let [bytes (byte-array cnt)
          _ (.flip buf)
          _ (.get buf bytes)
          _ (.clear buf)]
      bytes)))

(defn read-ch
  "returns a channel which read from asc, values in the channel are
   byte-array"
  ([asc]
     (read-ch asc (ByteBuffer/allocateDirect 1024) (chan)))
  ([^AsynchronousSocketChannel asc buf ch]
     (.read asc buf nil
            (reify CompletionHandler
              (completed [this cnt _]
                (on-debug {:event :read :count cnt})
                (if-let [bytes (read-buf! buf cnt)]
                  (do
                    (go (>! ch bytes))
                    ;(.read asc buf nil this)
                    )
                  (do
                    (info {:event :disconnected})
                    (.close asc))))))
     ch))

(defn read-sock-ch
  "reads a socket channel using core.async channels"
  [^AsynchronousSocketChannel sch buf ach]
  (let [buf
  (.read sch buf nul



(defn read-ch-loop
  "returns a channel which read from asc, values in the channel are
   byte-array"
  ([asc]
     (read-ch-loop asc (ByteBuffer/allocateDirect 1024) (chan)))
  ([^AsynchronousSocketChannel asc buf ch]
     (.read asc buf nil
            (reify CompletionHandler
              (completed [this cnt _]
                (on-debug (println {:event :read :count cnt}))
                (if-let [bytes (read-buf! buf cnt)]
                  (do
                    (go (>! ch bytes))
                    (.read asc buf nil this))
                  (do
                    (info {:event :disconnected})
                    (.close asc))))))
     ch))

;-127,len


(def pch (chan))
(def sch (chan))

(defn topic-fn [val]
  (println "topic-fn:" val)
  "hello")

(def p (pub pch topic-fn))

(sub p "hello" sch)
;(go (>! pch "hi world"))
;(go (println (<! sch)))

(defn decode-ws [ws-msg]
  (let [[f1 f2 f3 f4 f5 f6] ws-msg
      msg (drop 6 ws-msg)]
  (apply str (map #(char (bit-xor %1 %2)) msg (cycle [f3 f4 f5 f6])))))

(defn simple [port]
  (let [lc (listen-ch port)]
    (clojure.core.async/go-loop [asc (<! lc)]
      (when asc
        (let [rc (read-ch asc)]
          (clojure.core.async/go-loop [bs (<! rc)]
            (when bs
              (println "read: " (String. bs))
              (let [request (parse-request (String. bs))
                    rc2 (read-ch-loop asc)]
              (write-socket-channel asc (http-handle-request request) false)
              (if (= (:upgrade request) "websocket")
                (clojure.core.async/go-loop [subm (<! sch)]
                                            (when subm
                                              (println "sub msg: " subm)
                                              (write-bytes asc (create-ns subm) false)
                                              (recur (<! sch))))
                  (clojure.core.async/go-loop [wsm (<! rc2)]
                                              (when wsm
                                                (println "ws-m: " (decode-ws wsm))
                                                (recur (<! rc2)))))
              (recur nil))))))
      (recur (<! lc)))))

(simple 8888)
