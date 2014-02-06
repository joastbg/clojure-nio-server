(ns nioserver.files)

;; serves static files using a cache (using hash map)
;; todo: WatchService for changes in dir, then cache
;; some inspiration https://gist.github.com/moonranger/4023683

(def chm (java.util.concurrent.ConcurrentHashMap.))

;(.get chm "index.html")
(.put chm "index.html" "<h1>Johans hemsida</h1>lite annat skit....")

(defn serve-static [file]
  (.get chm file))
