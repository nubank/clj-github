(ns clj-github.utils)

(defn assoc-some
  "Assoc[iate] if the value is not nil.

  Examples:
  ```
    (assoc-some {:a 1} :b false)
    ;=>
    {:a 1 :b false}

    (assoc-some {:a 1} :b nil)
    ;=>
    {:a 1}
  ```
  "
  ([m k v]
   (if (nil? v) m (assoc m k v)))
  ([m k v & kvs]
   (let [ret (assoc-some m k v)]
     (if kvs
       (if (next kvs)
         (recur ret (first kvs) (second kvs) (nnext kvs))
         (throw (IllegalArgumentException.
                  "assoc-some expects even number of arguments after map/vector, found odd number")))
       ret))))
