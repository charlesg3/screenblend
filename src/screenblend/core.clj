(ns screenblend.core
  (:require [mikera.image.core :as i])
  (:import [java.awt Color])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn create-composite-context []
  (reify java.awt.CompositeContext
    (^void compose [_ ^java.awt.image.Raster src ^java.awt.image.Raster destIn ^java.awt.image.WritableRaster destOut]
      (let [src-px (int-array 4)
            dest-px (int-array 4)
            merged-px (int-array 4)]
        (doseq [^Integer x (range (.getWidth destIn))
                ^Integer y (range (.getHeight destIn))]
          (try
            (.getPixel src x y src-px)
            (.getPixel destIn x y dest-px)
            (dotimes [i 4]
              (aset merged-px i (min 255 (+ (aget src-px i) (aget dest-px i)))))
            (.setPixel destOut x y merged-px)
            (catch Throwable e))))
      nil)
    (dispose [_])))

(defn create-composite []
  (reify java.awt.Composite
    (createContext [_ srcColorModel destColorModel hints]
      (create-composite-context))))


(defn draw-rand-rect
  [^java.awt.Graphics2D g w h]
  (let [c (java.awt.Color. (int 0) (int (rand-int 32)) (int (rand-int 32)))]
    (.setColor g c)
    (.fillRect g  (rand-int (- w 40)) (rand-int (- h 40)) 40 40)))

(defn draw-rand-qcurve
  [^java.awt.Graphics2D g w h ^java.awt.geom.CubicCurve2D$Double curve]
  (let [max-r 0
        max-g 12
        max-b 12
        c (java.awt.Color. (int (rand-int max-r)) (int (rand-int max-g)) (int (rand-int max-b)))

        theta1 (* Math/PI 2 (rand))
        rx1 (- (/ w 2) (rand (/ w 4)))
        ry1 (- (/ h 2) (rand (/ h 4)))
        [x1 y1] [(+ (* rx1 (Math/cos theta1)) (/ w 2))
                 (+ (* ry1 (Math/sin theta1)) (/ h 2))]

        theta2 (* Math/PI 2 (rand))
        rx2 (- (/ w 2) (rand (/ w 4)))
        ry2 (- (/ h 2) (rand (/ h 4)))
        [x2 y2] [(+ (* rx2 (Math/cos theta2)) (/ w 2))
                 (+ (* ry2 (Math/sin theta2)) (/ h 2))]
        ctrl-pt-dist (/ (min h w) 18)
        get-ctrl-pt-x-fn #(+ (- (/ w 2) (/ ctrl-pt-dist 2)) (rand-int ctrl-pt-dist))
        get-ctrl-pt-y-fn #(+ (- (/ h 2) (/ ctrl-pt-dist 2)) (rand-int ctrl-pt-dist))
        ctrl-pt-dist2 (/ (min h w) 9)
        get-ctrl-pt-x-fn2 #(+ (- (/ w 2) (/ ctrl-pt-dist2 2)) (rand-int ctrl-pt-dist2))
        get-ctrl-pt-y-fn2 #(+ (- (/ h 2) (/ ctrl-pt-dist2 2)) (rand-int ctrl-pt-dist2))
        max-stroke-width 5
        stroke-width (* max-stroke-width (/ (/ (+ rx1 ry1 rx2 ry2) 4) (/ (max w h) 2)))
        ]
    (.setCurve curve x1 y1
               (get-ctrl-pt-x-fn) (get-ctrl-pt-y-fn)
               (get-ctrl-pt-x-fn2) (get-ctrl-pt-y-fn2)
               x2 y2)
    (.setStroke g (java.awt.BasicStroke. stroke-width java.awt.BasicStroke/CAP_ROUND java.awt.BasicStroke/JOIN_ROUND))
    (.setColor g c)
    (try
      (.draw g curve)
      (catch Throwable e))))

(defn -main
  [& args]
  (let [w 3840
        h 2160
        ;w 800
        ;h 450
        image (i/new-image w h)
        ^java.awt.Graphics2D g (.getGraphics image)
        curve (java.awt.geom.CubicCurve2D$Double.)]
    (.setColor g (java.awt.Color. 0 0 0))
    (.fillRect g 0 0 w h)
    (.setPaint g (java.awt.Color. 0 0 0 0))
    (.setRenderingHints g (java.awt.RenderingHints.
                            java.awt.RenderingHints/KEY_ANTIALIASING
                            java.awt.RenderingHints/VALUE_ANTIALIAS_ON))
    (.setComposite g (create-composite))
    (doseq [i (range 5000)]
      (draw-rand-qcurve g w h curve))
    (i/show image)
    (i/save image "starburst.png")))
