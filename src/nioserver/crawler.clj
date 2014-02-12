(ns nioserver.crawler
  (:require [clojure.core.async :as async :refer [<! >! timeout chan alt! go]])
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


;; nio handler
(defmacro with-handlers [cbody fbody]
  `(reify CompletionHandler
     (completed [t# r# a#]
       ((~@cbody) t# r# a#))
     (failed [t# e# a#]
       ((~@fbody) t# e# a#))))

(defmacro with-handler [cbody]
  `(reify CompletionHandler
     (completed [t# r# a#]
       ((~@cbody) t# r# a#))))

(defn byte-buf [s]
  (letfn [(new-buf [size]
            (ByteBuffer/allocateDirect size))]
    (cond
     (instance? String s)
                (-> (new-buf (.length s))
                    (.put (.getBytes s))
                     .rewind)

     (instance? Number s) (new-buf s)
     true (new-buf 1024))))

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
  (let [buf (byte-buf 1024)]
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

(defn channel-group []
  (let [executor (Executors/newSingleThreadExecutor)]
       (AsynchronousChannelGroup/withThreadPool executor)))

(defn channel-group []
  (let [executor (Executors/newFixedThreadPool 8)]
       (AsynchronousChannelGroup/withThreadPool executor)))

(let [client (AsynchronousSocketChannel/open (channel-group))]
  (.connect client (java.net.InetSocketAddress. "www.google.com" 80)
            nil (with-handler
                    (do (println "Connected...")
                        (write-sock-ch client (.getBytes "GET / HTTP/1.1\r\nHost: www.example.com\r\n\r\n"))

                        (let [rc (read-sock-ch client (chan))]
                          (go-loop [bs (<! rc)]
                                   (when bs
                                     (println "read: " (String. bs))
                                     (recur (<! rc)))))


                        ))))

(defn get-str [buf cnt]
  (let [bytes (byte-array cnt)]
    (.flip buf)
    (.get buf bytes)
    (.clear buf)
    (String. bytes)))

(defn read1 [nc rc nfn]
  (let [buf (byte-buf 1024)]
    ;(println "read...")
    (.read nc buf nil
           (with-handlers
             (fn [a b c]
               (if (> b 0)
                 (do
                   ;(println "read" b)
                   (go (>! rc (get-str buf b)))
                   (read1 nc rc nfn))
                 (apply nfn [nc rc])))
             (fn [a b c] (println "err" b c))))))

(defn write1 [nc rc str nfn]
  (let [buf (byte-buf str)]
    (.write nc buf nil
            (with-handlers
              (fn [a b c]
                ;(println "wrote" b)
                (apply nfn [nc rc]))
              (fn [a b c] (println "err" b c))))))

(def rch (chan))
(defn apa [ch]
  (go (doseq [n [1 2 3]]
        (let [[v c] (alts! [ch])]
          (doseq [s (re-seq  #"<a href=\"([^\"]*)\"" v)] (println s))
          (println (str "Read from channel:\n" v))))))

(apa rch)

(defn parse-html [html]
  (doseq [s (re-seq  #"<a href=\"([^\"]*)\"" html)] (println s)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; NORMALIZE URLs

(defn normalize-url [base url]
  (cond (re-find #"http" url)  url
        (re-find #"mailto" url) url
        (re-find #"www" url) (str "http://" url)
        true (do ;; fix url
                 (str "http://" base (if (not (= (first url) \/)) "/") url))))


(defn normalize-url [base url]
  (if (re-find #"http" url)
    (println "url ok" url)
    (do ;; fix url
      (str base (if (not (= (first url) \/)) "/") url))))

(normalize-url "cs.lth.se" "english/about")
(normalize-url "cs.lth.se" "http://www.lu.se/english/")
(normalize-url "cs.lth.se" "mailto:klas@cs.lth.se")
(normalize-url "cs.lth.se" "www.lu.se/english/")

(re-find #"www" "http://www.google.se")
(re-find #"www" "english/about")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; WORKING

;; add urls to hash
(def url-count (agent 0))

@url-count

(defn ach-reader [ch base-url]
  "a channel reader that will close on channel close"
  (async/go-loop []
    (when-let [in (<! ch)]
      (doseq [s (re-seq  #"(?i)<a href=[\"\']([^>^\"\']*)" (String. in))]
        (send url-count inc)
        (println "*** NEW: " (normalize-url base-url (second s))))
        ;(println "*** NEW: " (java.net.URL. (normalize-url base-url (second s))))))
      ;(println "got-data" (String. in))
      (recur))
    (println "Closing..." (.toString (java.util.Date.)) @url-count)))

(def aach (chan))
(ach-reader aach)
(go (>! aach "hello"))
(async/close! aach)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(if-let [x 1]
  "then"
  "else")

(when-let [x 1]
  (println "run") (println "run"))

(go-test (async/to-chan [1 2 3]))
(async/close! rch)
(go (>! rch "hello"))

(def cg (channel-group))

;;;;;;;;;;;;;; NIO UTILS

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
  (let [buf (byte-buf 1024)]
    (.read sch buf 5 TimeUnit/SECONDS nil
           (with-handlers
             (fn [t cnt a]
               ;(println "READ: " cnt)
               (when (neg? cnt)
                 (async/close! ach)
                 (.close sch))
               (when-let [bytes (read-buf buf cnt)]
                   (go (>! ach bytes))
                   (.read sch buf 5 TimeUnit/SECONDS nil t)))
             (fn [t e a]
               (if (instance? java.nio.channels.InterruptedByTimeoutException e)
                 (println "! Timeout (read), closing.")
                 (println "! Failed (read):" e))
               (async/close! ach)
               (.close sch)))) ach))

(defn apan ([^Integer a b] (println "a b" a b)) ([a b] (println "a" a)))

(apan 1)
(when-match [[(odd? ?) (even? ?) ?x _ _] $] x)

(defn write-sock-ch
  "writes a byte array to a socket channel"
  [^AsynchronousSocketChannel sch str]
  (let [buf (byte-buf str)]
    (.write sch buf nil
            (with-handlers
              (fn [t cnt a])
                ;(println "wrote" cnt))
              (fn [t e a]
               (.close sch)
               (println "! Failed (read):" e))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn crawl-url [^java.net.URL url]
  (let [ach (chan)
        _ (ach-reader ach (.getHost url))
        client (AsynchronousSocketChannel/open cg)]
    (.connect client (java.net.InetSocketAddress. (.getHost url) 80)
              nil (reify CompletionHandler
                    (completed [this _ _]
                      (println "connected...")
                      (write1 client ach
                              (str "GET " (.getPath url) " HTTP/1.1\r\nHost:" (.getHost url) "\r\n\r\n")
                              (fn [nc rc]
                                (read1 nc rc
                                       (fn [_ _]
                                         (.close client)
                                         (async/close! rc))))))
                    (failed [this e _]
                      (.close client)
                      (println "! Failed (write):" e (.getMessage e)))))))

(defn crawl-url2 [^java.net.URL url]
  (let [ach (chan)
        _ (ach-reader ach (.getHost url))
        client (AsynchronousSocketChannel/open cg)]
    (.connect client (java.net.InetSocketAddress. (.getHost url) 80)
              nil (reify CompletionHandler
                    (completed [this _ _]
                      (println "connected...")
                      (write-sock-ch client (str "GET " (.getPath url) " HTTP/1.1\r\nHost:" (.getHost url) "\r\n\r\n"))
                      (read-sock-ch client ach))))))


(def url-count (agent 0))

@url-count

(time
 (do
   (println "Starting: " (.toString (java.util.Date.)))
   (crawl-url2 (java.net.URL. "http://news.yahoo.com/"))
   (crawl-url2 (java.net.URL. "https://news.google.se/"))
   (crawl-url2 (java.net.URL. "http://clojuredocs.org/clojure_core"))
   (crawl-url2 (java.net.URL. "http://docs.oracle.com/javase/7/docs/api/java/io/OutputStream.html"))
   (crawl-url2 (java.net.URL. "http://nakkaya.com/2009/12/05/distributed-clojure-using-rmi/"))
   (crawl-url2 (java.net.URL. "http://cs.lth.se/kurs/eda095-naetverksprogrammering/laborationer/laboration-4/"))
   (crawl-url2 (java.net.URL. "http://cs.lth.se/kurs/eda095-naetverksprogrammering/laborationer/laboration-5/"))
   (crawl-url2 (java.net.URL. "http://www.google.com/chrome"))
   (crawl-url2 (java.net.URL. "http://cs.lth.se/forskning/"))
   (crawl-url2 (java.net.URL. "http://getfirefox.com"))
   (crawl-url2 (java.net.URL. "http://tv.yahoo.com/"))
   (crawl-url2 (java.net.URL. "http://celebrity.yahoo.com/"))
   (crawl-url2 (java.net.URL. "http://finance.yahoo.com/"))
   (crawl-url2 (java.net.URL. "http://www.cse.psu.edu/~groenvel/urls.html"))
   (.awaitTermination cg 60 TimeUnit/SECONDS)
   (println "Finished: " (.toString (java.util.Date.)))))

(.getPath (java.net.URL. "http://www.cse.psu.edu/~groenvel/urls.html"))

(.close cg)
(+ 1 2)

;;;;;;;;;;;; Crawl

(def crawled-urls (java.util.concurrent.ConcurrentHashMap.))

(.size crawled-urls)

(def nurl-ch2 (chan))

(defn apa [url]
  (if-let [date (.putIfAbsent crawled-urls url (.toString (java.util.Date.)))]
    (println "already parsed:\t" url "// @" date)
    (do
      (println "new url:\t" url)
      (go (>! nurl-ch2 url)))))

(.putIfAbsent crawled-urls (java.net.URL."http://cs.lth.se/english/dsdsa") "")

(apa (java.net.URL. "http://docs.oracle.com/javase/7/docs/api/java/net/URL.html"))

(defn nch-reader [ch]
  "a channel reader that will close on channel close"
  (go-loop []
    (when-let [in (<! ch)]
      (println "new url to parse: " in)
      (crawl-url my-url)
      (recur))
    (println "closing...")))

(nch-reader nurl-ch2)
(async/close! nurl-ch2)
;;;;;;;;;;;; Separate URL

;; Use java.net.URL

(def my-url (java.net.URL. "http://cs.lth.se/english/"))
(def my-email (java.net.URL. "http://cs.lth.se/&#35;"))

(.getHost my-url)
(.getProtocol my-url)
(.getPath my-url)
(.getPort my-url)

(separate-url "http://cs.lth.se/english/course/eda040-concurrent-programming/lectures/")

;;;;;;;;;;;; Parse HTML

;; handle unblanced quotations
(?idmsux-idmsux)
(re-seq #"(?i)<a href=[\"\']([^>^\"\']*)" "<A HREF=\"english/course/eda040-concurrent-programming/about-this-course/\" class=\"internal-link\" >About this course</a><a href=\"www.google.se\">saasd</a><a href=\"mailto:klas@cs.lth.se>Klas Nilsson</a><br /><A HREF='dasdsa'>dsaasd</a>")

(def sample-html
"<div>
    <h1>Example Domain</h1>
    <p>This domain is established to be used for illustrative examples in documents. You may use this
    domain in examples without prior coordination or asking for permission.</p>
    <p><a href=\"http://www.iana.org/domains/example\">More information...</a></p>
</div>
</body>
</html>")

(re-seq  #"<a href=\"([^\"]*)\"" sample-html)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; RMI

(def rmi-registry (java.rmi.registry.LocateRegistry/createRegistry 1099))

(defn stop-rmi []
  (java.rmi.server.UnicastRemoteObject/unexportObject rmi-registry true))

(deftype Hello []
  java.rmi.Remote
  (sayHello [x]))

(defprotocol Hello
    (sayHello [x]))

(interface java.rmi.Remote
  Hello
  {:sayHello (fn [])})

(gen-interface
  :name nioserver.crawler.HelloA
  :extend java.rmi.Remote
  :methods [[sayHello [] #=java.lang.String]])

(defn HelloImpl []
  (reify nioserver.crawler.HelloA
    (sayHello [_]
      "Hello")
    java.rmi.Remote
    ))

(defn register-server []
  (.bind
   (java.rmi.registry.LocateRegistry/getRegistry)
   "nioserver.crawler.HelloA"
   (java.rmi.server.UnicastRemoteObject/exportObject (HelloImpl) 0)))

(type HelloServer)

(register-server)

(def rmi-registry (java.rmi.registry.LocateRegistry/getRegistry "127.0.0.1"))

(let [hello (.lookup rmi-registry "nioserver.crawler.HelloA")]
  (println (.sayHello #^nioserver.crawler.HelloA hello)))
