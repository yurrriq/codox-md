(ns codox-md.writer
  "Output of codox documentation using markdown."
  (:use
   [codox.writer.html :only []]
   [codox-md.markdown :only [md]]
   [net.cgrand.enlive-html
    :only [any-node clone-for content do-> html-content html-resource set-attr
           template transformation]])
  (:require
   [clojure.java.io :as io]
   [codox.writer.html :as html-writer]))

(def ns-template (html-resource "codox/ns_template.html"))
(def index-template (html-resource "codox/index_template.html"))

(defn ns-link [namespace]
  (transformation
   [:a] (do->
         (set-attr :href (#'html-writer/ns-filename namespace))
         (html-content (:name namespace)))))

(defn ns-var-link [namespace v]
  (transformation
   [:a] (do->
         (set-attr :href (#'html-writer/var-uri namespace v))
         (html-content (:name v)))))

(def namespace-page
  (template
   "codox/ns_template.html" [project namespace]
   [:title] (html-content (#'html-writer/namespace-title namespace))
   [:.project-title] (content (#'html-writer/project-title project))
   [:.ns-links :.ns-link] (clone-for
                           [namespace (:namespaces project)]
                           (ns-link namespace))
   [:.ns-var-links :.ns-var-link] (clone-for
                                   [v (:publics namespace)]
                                   (ns-var-link namespace v))
   [:.namespace-title] (html-content (#'html-writer/namespace-title namespace))
   [:.namespace-docs :.doc] (html-content (md (:doc namespace)))
   [:.public] (clone-for
               [v (:publics namespace)]
               [:h3] (do->
                      (html-content (:name v))
                      (set-attr :id (#'html-writer/var-id v)))
               [:.doc] (html-content (md (:doc v)))
               [:.usage] (clone-for
                          [arg-list (:arglists v)]
                          (html-content arg-list)))))

(def index-page
  (template
   "codox/index_template.html" [project]
   [:title] (content (#'html-writer/project-title project))
   [:.project-title] (html-content (#'html-writer/project-title project))
   [:.doc] (html-content (md (:description project)))
   [:.ns-links :.ns-link] (clone-for
                           [namespace (:namespaces project)]
                           (ns-link namespace))
   [:.namespace] (clone-for
                  [namespace (:namespaces project)]
                  [:.ns-link] (ns-link namespace)
                  [:.doc] (html-content (md (:doc namespace)))
                  [:.ns-var-link] (clone-for
                                   [v (:publics namespace)]
                                   (ns-var-link namespace v)))))

(defn- write-index [output-dir project]
  (spit (io/file output-dir "index.html") (apply str (index-page project))))

(defn- write-namespaces [output-dir project]
  (doseq [namespace (:namespaces project)]
    (spit (#'html-writer/ns-filepath output-dir namespace)
          (apply str (namespace-page project namespace)))))

(defn write-docs
  "Take raw documentation info and turn it into formatted HTML."
  [project]
  (doto (:output-dir project "doc")
    (#'html-writer/mkdirs "css" "js")
    (#'html-writer/copy-resource
     "codox/css/default.css" "css/default.css")
    (#'html-writer/copy-resource
     "codox/js/jquery.min.js" "js/jquery.min.js")
    (#'html-writer/copy-resource
     "codox/js/page_effects.js" "js/page_effects.js")
    (write-index project)
    (write-namespaces project))
  nil)
