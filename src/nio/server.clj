(ns nio-testing)

;;

(def listener
(let [assc (java.nio.channels.AsynchronousServerSocketChannel/open)
      sa (java.net.InetSocketAddress. 8888)]
  (.bind assc sa)))

;; Add protocol handlers

(defmacro with-action [component event & body]
  `(. ~component addActionListener
      (proxy [java.awt.event.ActionListener] []
        (actionPerformed [~event] ~@body))))

(defn make-reify [e]
  (reify e))

(defmacro with-completion-handler [completed-body failed-body]
  `(reify java.nio.channels.CompletionHandler
     (completed [this result attachment] ~completed-body)
     (failed [this exc attachment] ~failed-body)))

(with-completion-handler (println {:count result}) (println exc))

(macroexpand '(with-completion-handler (println {:count result}) (println exc))) ;;=>

(clojure.pprint/pprint (macroexpand '(with-completion-handler (println {:count result}) (println exc))))

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
(parse-method "GET /favicon.ico HTTP/1.1")
(parse-method "GET / HTTP/1.1")

(defn parse-request [req-str]
  (let [lines (clojure.string/split req-str #"\s")]
    (parse-method lines)))

(def handler
  (reify java.nio.channels.CompletionHandler
    (completed [this sc _]
      (println {:address (.getRemoteAddress sc)})
      (letfn [(observer [str]
                (println "Request:" (parse-request str))
                (write-socket-channel sc (http-str-reply (serve-static "index.html"))))]
        (read-socket-channel sc 1024 observer))
      (.accept listener nil this))))

(defn get-date-str []
  (let [calendar (java.util.Calendar/getInstance)
        format   (java.text.SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss z")]
    (.format format (.getTime calendar))))

(defn http-str-reply [content]
  (let [content-length (.length content)]
  (clojure.string/join "\n"
                       ["HTTP/1.1 200 OK"
                        (str "Date: " (get-date-str))
                        "Server: Raptor/0.01 (Unix)"
                        (str "Content-length: " content-length)
                        "Content-type: text/html; charset=UTF-8"
                        ""
                        content])))

(assoc {} :johan "nisse" :kalle "neger")

;; serves static files using a cache (using hash map)
;; todo: WatchService for changes in dir, then cache
;; some inspiration https://gist.github.com/moonranger/4023683

(defn serve-static [file]
  (.get chm file))

(def chm (java.util.concurrent.ConcurrentHashMap.))

(.get chm "index.html")
(.put chm "index.html" "<h1>Johans hemsida</h1>lite annat skit....")

(def listener
  (let [assc (java.nio.channels.AsynchronousServerSocketChannel/open)
        sa (java.net.InetSocketAddress. 8080)]
  (.bind assc sa)))

(.accept listener nil handler)

(.close listener)

(defn start-server [handler]
  (let [assc (java.nio.channels.AsynchronousServerSocketChannel/open)
      sa (java.net.InetSocketAddress. 8080)]
  (.accept (.bind assc sa) nil handler)))

(def server (start-server handler)






;(doto (java.nio.channels.AsynchronousServerSocketChannel/open)
                                        ;  .bin

(defn listen-ch
  "return a channel which listens on port, values in the channel are scs of
   AsynchronousSocketChannel"
     (let [^java.nio.channels.AsynchronousServerSocketChannel listener
           (-> (java.nio.channels.AsynchronousServerSocketChannel/open)
               (.bind (java.net.InetSocketAddress. port)))
           handler (reify java.nio.channels.CompletionHandler
                     (completed [this sc _]
                       (info {:event :connected :address (.getRemoteAddress sc)})

                       ;; read from channel (sc is of type AsynchronousSocketChannel)
                       (.read sc buf nil (reify java.nio.channels.CompletionHandler
                                           (completed [this cnt _]
                                             (println {:event :read :count cnt})
                                             (.close sc))))

                       (.accept listener nil this)))]
       (.accept listener nil handler)
       ch))


(defmacro with-action [component event & body]
  `(. ~component addActionListener
      (proxy [java.awt.event.ActionListener] []
        (actionPerformed [~event] ~@body))))


(defmacro with-action [component event & body]
  `(. ~component addActionListener
      (proxy [java.awt.event.ActionListener] []
        (actionPerformed [~event] ~@body))))

(defmacro m1 [arg1 arg2]
  `(~arg1 ~arg2))

(macroexpand '(m1 Math/exp 10))

(m1 Math/exp 10)

(macroexpand '(with-action (javax.swing.JButton. "Start") e (send flipper start)))
(. (javax.swing.JButton. "Start") nio-testing/addActionListener
     (clojure.core/proxy [java.awt.event.ActionListener] []
       (nio-testing/actionPerformed [e] (send flipper start))))
