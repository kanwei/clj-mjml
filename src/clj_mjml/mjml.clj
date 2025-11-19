(ns clj-mjml.mjml
  "Clojure wrapper for ch.digitalfondue.mjml4j"
  (:gen-class)
  (:import
   [ch.digitalfondue.mjml4j Mjml4j
                            Mjml4j$Configuration
                            Mjml4j$IncludeResolver
                            Mjml4j$ResourceLoader
                            Mjml4j$SimpleResourceResolver
                            Mjml4j$TextDirection]))

(defn classpath-resource-loader [resource-path]
  (try
    (slurp (clojure.java.io/resource resource-path))
    (catch Exception _
      (throw (Exception. (str "Resource not found: " resource-path))))))

(defn create-resolver []
  (Mjml4j$SimpleResourceResolver.
   (reify Mjml4j$ResourceLoader
     (load [_ resource-path]
       (classpath-resource-loader resource-path)))))

(def default-options
  {})

(defn make-config [config-map])

(defn render
  "Renders MJML to HTML.

  Arity:
  - (render template): Render with default configuration.
  - (render template config): Render with specific configuration."
  ([template]
   (render template nil))
  ([^String template ^Mjml4j$Configuration config]
   (if config
     (Mjml4j/render template config)
     (Mjml4j/render template))))

(defn -main [& args]
  (println (render (first args))))