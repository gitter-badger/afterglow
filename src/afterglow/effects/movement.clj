(ns afterglow.effects.movement
  "Effects pipeline functions for working with direction assignments
  to fixtures and heads."
  {:author "James Elliott"}
  (:require [afterglow.channels :as channels]
            [afterglow.effects.channel :as chan-fx]
            [afterglow.effects.params :as params]
            [afterglow.effects :as fx]
            [afterglow.rhythm :as rhythm]
            [afterglow.show-context :refer [*show*]]
            [afterglow.transform :refer [calculate-position calculate-aim]]
            [clojure.math.numeric-tower :as math]
            [com.evocomputing.colors :as colors]
            [taoensso.timbre.profiling :refer [pspy]]
            [taoensso.timbre :as timbre :refer [debug]])
  (:import [afterglow.effects Assigner Effect]
           [javax.media.j3d Transform3D]
           [javax.vecmath Point3d Vector3d]))

(defn find-moving-heads
  "Returns all heads of the supplied fixtures which are capable of
  movement, in other words they have either a pan or tilt channel."
  [fixtures]
  (filter #(pos? (count (filter #{:pan :tilt} (map :type (:channels %))))) (channels/expand-heads fixtures)))

(defn direction-effect
  "Returns an effect which assigns a direction parameter to all
  moving heads of the fixtures supplied when invoked. The direction is
  a vector in the frame of reference of the show, so standing in the
  audience facing the show, x increases to the left, y away from the
  ground, and z towards the audience."
  [name direction fixtures]
  {:pre [(some? name) (some? *show*) (sequential? fixtures)]}
  (params/validate-param-type direction Vector3d)
  (let [heads (find-moving-heads fixtures)
        assigners (fx/build-head-parameter-assigners :direction heads direction *show*)]
    (Effect. name fx/always-active (fn [show snapshot] assigners) fx/end-immediately)))

;; Resolves the assignment of a direction to a fixture or a head.
(defmethod fx/resolve-assignment :direction [assignment show snapshot buffers]
  ;; Resolve in case assignment is still frame dynamic
  (let [target (:target assignment)
        direction (params/resolve-param (:value assignment) show snapshot target)
        direction-key (keyword (str "pan-tilt-" (:id target)))
        former-values (direction-key (:previous @(:movement *show*)))
        [pan tilt] (calculate-position target direction former-values)]
    (debug "Direction resolver pan:" pan "tilt:" tilt)
    (doseq [c (filter #(= (:type %) :pan) (:channels target))]
      (chan-fx/apply-channel-value buffers c pan))
    (doseq [c (filter #(= (:type %) :tilt) (:channels target))]
      (chan-fx/apply-channel-value buffers c tilt))
    (swap! (:movement *show*) #(assoc-in % [:current direction-key] [pan tilt]))))

(defn current-rotation
  "Given a head and DMX pan and tilt values, calculate a
  transformation that represents the current orientation of the head
  as compared to the default orientation of facing directly towards
  the positive Z axis."
  [head pan tilt]
  (let [rotation (Transform3D. (:rotation head))]
    (when-let [pan-scale (:pan-half-circle head)]
      (let [dmx-pan (/ (- pan (:pan-center head)) pan-scale)
            adjust (Transform3D.)]
        (.rotY adjust (* Math/PI dmx-pan))
        (.mul rotation adjust)))
    (when-let [tilt-scale (:tilt-half-circle head)]
      (let [dmx-tilt (/ (- tilt (:tilt-center head) tilt-scale))
            adjust (Transform3D.)]
        (.rotX adjust (* Math/PI dmx-tilt))
        (.mul rotation adjust)))
    rotation))

(defn default-direction
  "Determine the default aiming vector for a head, in other words the
  direction it aims when sent zero values for its DMX pan and tilt
  channels."
  [head]
  (let [rotation (current-rotation head 0 0)
        direction (Vector3d. 0 0 1)]
    (.transform rotation direction)
    direction))

;; Fades between two direction assignments. A nil assignment is interpreted as the direction the head will face
;; when it is sent zero values for its pan and tilt channels, so that a fade to or from nothing looks right.
(defmethod fx/fade-between-assignments :direction [from-assignment to-assignment fraction show snapshot]
  (cond (<= fraction 0) from-assignment
        (>= fraction 1) to-assignment
        ;; We are blending, so we need to resolve any remaining dynamic parameters now, and make sure
        ;; fraction really does only range between 0 and 1.
        :else (let [default (default-direction (:target from-assignment))
                    from-resolved (Vector3d. (params/resolve-param (or (:value from-assignment) default) show snapshot))
                    to-resolved (Vector3d.  (params/resolve-param (or (:value to-assignment) default) show snapshot))
                    fraction (colors/clamp-unit-float fraction)]
                (.scale to-resolved fraction)
                (.scaleAdd from-resolved (- 1.0 fraction) to-resolved)
                (.normalize from-resolved)
                (merge from-assignment {:value from-resolved}))))

(defn aim-effect
  "Returns an effect which assigns an aim parameter to all moving
  heads of the fixtures supplied when invoked. The direction is a
  point in the frame of reference of the show, so standing in the
  audience facing the show, x increases to the left, y away from the
  ground, and z towards the audience, and the origin is the center of
  the show."
  [name target-point fixtures]
  {:pre [(some? name) (some? *show*) (sequential? fixtures)]}
  (params/validate-param-type target-point Point3d)
  (let [heads (find-moving-heads fixtures)
        assigners (fx/build-head-parameter-assigners :aim heads target-point *show*)]
    (Effect. name fx/always-active (fn [show snapshot] assigners) fx/end-immediately)))

;; Resolves the assignment of an aiming point to a fixture or a head.
(defmethod fx/resolve-assignment :aim [assignment show snapshot buffers]
  ;; Resolve in case assignment is still frame dynamic
  (let [target (:target assignment)
        target-point (params/resolve-param (:value assignment) show snapshot target)
        direction-key (keyword (str "pan-tilt-" (:id target)))
        former-values (direction-key (:previous @(:movement *show*)))
        [pan tilt] (calculate-aim target target-point former-values)]
    (debug "Aim resolver pan:" pan "tilt:" tilt)
    (doseq [c (filter #(= (:type %) :pan) (:channels target))]
      (chan-fx/apply-channel-value buffers c pan))
    (doseq [c (filter #(= (:type %) :tilt) (:channels target))]
      (chan-fx/apply-channel-value buffers c tilt))
    (swap! (:movement *show*) #(assoc-in % [:current direction-key] [pan tilt]))))

;; Fades between two aim assignments. A nil assignment is interpreted as a point in the direction the head will
;; aim when it is sent zero values for its pan and tilt channels, so that a fade to or from nothing looks right.
(defmethod fx/fade-between-assignments :aim [from-assignment to-assignment fraction show snapshot]
  (cond (<= fraction 0) from-assignment
        (>= fraction 1) to-assignment
        ;; We are blending, so we need to resolve any remaining dynamic parameters now, and make sure
        ;; fraction really does only range between 0 and 1.
        :else (let [target (:target from-assignment)
                    dir (default-direction target)
                    default (Point3d. (+ (:x target) (.x dir))
                                      (+ (:y target) (.y dir))
                                      (+ (:z target) (.z dir)))
                    from-resolved (Point3d. (params/resolve-param (or (:value from-assignment) default) show snapshot))
                    to-resolved (Point3d.  (params/resolve-param (or (:value to-assignment) default) show snapshot))
                    fraction (colors/clamp-unit-float fraction)]
                (.scale to-resolved fraction)
                (.scaleAdd from-resolved (- 1.0 fraction) to-resolved)
                (merge from-assignment {:value from-resolved}))))
