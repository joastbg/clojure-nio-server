;   Copyright (c) Johan Astborg. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns nioserver.files)

;; serves static files using a cache (using hash map)
;; todo: WatchService for changes in dir, then cache
;; some inspiration https://gist.github.com/moonranger/4023683

(def chm (java.util.concurrent.ConcurrentHashMap.))

;(.get chm "index.html")
(.put chm "index.html" "<h1>Johans hemsida</h1>lite annat skit....")

(defn serve-static [file]
  ; change this
  (.get chm "index.html"))
