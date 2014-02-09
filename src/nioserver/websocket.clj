;   Copyright (c) Johan Astborg. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns nioserver.websocket)

;; reference: http://tools.ietf.org/html/rfc6455

;; websocket code goes here

; a hack, need to check if length > 127 etc.
(defn create-ws-msg [str]
  (let [len (.length str)
        init (byte-array [(byte -127) (byte len)])]
    (byte-array (concat init (.getBytes str)))))
