(ns clj-mjml.mjml
  "Clojure wrapper for ch.digitalfondue.mjml4j"
  (:gen-class)
  (:import
   [ch.digitalfondue.mjml4j Mjml4j
    Mjml4j$Configuration
    Mjml4j$ResourceLoader
    Mjml4j$SimpleResourceResolver
    Mjml4j$FileSystemResolver
    Mjml4j$TextDirection])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli]))

;; ========================================
;; Include Resolvers
;; ========================================

(defn classpath-resource-loader
  "Loads a resource from the classpath.
  
  Args:
    resource-path: String path to the resource (e.g., \"templates/header.mjml\")
    
  Returns:
    String content of the resource
    
  Throws:
    Exception if resource is not found"
  [resource-path]
  (if-let [resource (io/resource resource-path)]
    (slurp resource)
    (throw (Exception. (str "Classpath resource not found: " resource-path)))))

(defn create-classpath-resolver
  "Creates an IncludeResolver that loads MJML includes from the classpath.
  
  Useful for:
  - Loading templates packaged in JAR files
  - Using resources in src/resources directory
  - Referencing templates via classpath paths
  
  Example:
    (def resolver (create-classpath-resolver))
    (render template {:mj-include-resolver resolver})
    
  MJML usage:
    <mj-include path=\"templates/header.mjml\" />"
  []
  (Mjml4j$SimpleResourceResolver.
   (reify Mjml4j$ResourceLoader
     (load [_ resource-path]
       (classpath-resource-loader resource-path)))))

(defn create-filesystem-resolver
  "Creates an IncludeResolver that loads MJML includes from the filesystem.
  
  Args:
    base-path: String or File representing the base directory for includes
               (defaults to current working directory if not provided)
  
  Useful for:
  - Development workflows with local files
  - Loading templates from user directories
  - Dynamic template systems with file-based storage
  
  Example:
    (def resolver (create-filesystem-resolver \"/path/to/templates\"))
    (render template {:mj-include-resolver resolver})
    
  MJML usage:
    <mj-include path=\"partials/header.mjml\" />"
  ([]
   (create-filesystem-resolver (System/getProperty "user.dir")))
  ([base-path]
   (let [path (if (instance? java.nio.file.Path base-path)
                base-path
                (.toPath (io/file base-path)))]
     (Mjml4j$FileSystemResolver. path))))

(defn create-custom-resolver
  "Creates a custom IncludeResolver using a provided loader function.
  
  Args:
    loader-fn: Function that takes a resource path (String) and returns 
               the MJML content (String)
  
  Useful for:
  - Loading templates from databases
  - Fetching templates from remote URLs
  - Implementing custom caching strategies
  - Integration with existing template systems
  
  Example:
    (def resolver 
      (create-custom-resolver
        (fn [path]
          (db/fetch-template path))))
    (render template {:mj-include-resolver resolver})
    
  MJML usage:
    <mj-include path=\"email-templates/123\" />"
  [loader-fn]
  (Mjml4j$SimpleResourceResolver.
   (reify Mjml4j$ResourceLoader
     (load [_ resource-path]
       (loader-fn resource-path)))))

;; ========================================
;; Configuration
;; ========================================

(defn make-config
  "Internal function that creates an Mjml4j Configuration from an options map.
  
  Most users should call render directly with an options map rather than 
  calling this function."
  [{:keys [mj-include-resolver language text-direction]
    :or {language "en" text-direction :ltr}}]
  (let [dir (case text-direction
              :ltr Mjml4j$TextDirection/LTR
              :rtl Mjml4j$TextDirection/RTL
              Mjml4j$TextDirection/LTR)]
    (if mj-include-resolver
      (Mjml4j$Configuration. language dir mj-include-resolver)
      (Mjml4j$Configuration. language dir))))

;; ========================================
;; Render Functions
;; ========================================

(defn render
  "Renders MJML to HTML.

  Arity:
  - (render template): Render with default configuration (no includes).
  - (render template options): Render with specific configuration.
  
  Options map:
    :mj-include-resolver - An IncludeResolver for handling mj-include tags.
                           Use create-filesystem-resolver, create-classpath-resolver,
                           or create-custom-resolver to create one.
    :language            - Language code for the email (optional, defaults to \"en\").
                           Examples: \"en\", \"fr\", \"es\", \"de\"
    :text-direction      - Text direction for the email (optional, defaults to :ltr).
                           Use :ltr for left-to-right or :rtl for right-to-left languages.
  
  Examples:
    Basic rendering:
      (render \"<mjml>...</mjml>\")
    
    With filesystem includes:
      (render template 
              {:mj-include-resolver (create-filesystem-resolver \"/templates\")})
    
    With classpath includes:
      (render template 
              {:mj-include-resolver (create-classpath-resolver)})
    
    With custom loader:
      (render template 
              {:mj-include-resolver 
               (create-custom-resolver 
                 (fn [path] (fetch-from-db path)))})
    
    With language and text direction (e.g., for Arabic emails):
      (render template 
              {:mj-include-resolver resolver
               :language \"ar\"
               :text-direction :rtl})
    
    Multiple options combined:
      (render template
              {:mj-include-resolver (create-classpath-resolver)
               :language \"fr\"
               :text-direction :ltr})"
  ([^String template]
   (Mjml4j/render template))
  ([^String template options]
   (let [^Mjml4j$Configuration config (make-config options)]
     (Mjml4j/render template config))))

;; ---------------------------------------------------------
;; CLI Logic
;; ---------------------------------------------------------

(def cli-options
  [#_["-r" "--read" "Compile MJML File(s)"]
   #_["-i" "--stdin" "Compiles MJML from input stream"]
   #_["-s" "--stdout" "Output HTML to stdout"]
   #_["-o" "--output" "Filename/Directory to output compiled files"]
   #_["-v" "--validate" "Run validator on File(s)"]
   #_["-w" "--watch" "Watch and compile MJML File(s) when modified"]
   ["-V" "--version" "Show version number"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["MJML4J Wrapper"
        ""
        "Usage: mjml [options] MJML"
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]

    ;; 1. Handle Errors & Help
    (cond
      (:help options) (exit 0 (usage summary))
      (:version options) (exit 0 "clj-mjml")
      errors (exit 1 (error-msg errors)))

    (let [config (make-config options)
          use-stdin (:stdin options)]

      (if (and (empty? arguments) (not use-stdin))
        (exit 1 (usage summary))

        (println (render (first args)))))))

(comment
  (do (require 'criterium.core)
      (criterium.core/bench (render
                             "<mjml>
     <mj-body>
       <mj-section>
         <mj-column>
           <mj-text>Hello World</mj-text>
         </mj-column>
       </mj-section>
     </mj-body>
   </mjml>")))
 ; Evaluation count : 392580 in 60 samples of 6543 calls.
 ;             Execution time mean : 156.360026 µs
 ;    Execution time std-deviation : 2.784316 µs
 ;   Execution time lower quantile : 153.459474 µs ( 2.5%)
 ;   Execution time upper quantile : 162.552168 µs (97.5%)
 ;                   Overhead used : 1.081978 ns
 ;
 ;Found 3 outliers in 60 samples (5.0000 %)
 ;	low-severe	 2 (3.3333 %)
 ;	low-mild	 1 (1.6667 %)
 ; Variance from outliers : 7.7630 % Variance is slightly inflated by outliers

  (-main "<mjml>\n     <mj-body>\n       <mj-section>\n         <mj-column>\n           <mj-text>Hello World</mj-text>\n         </mj-column>\n       </mj-section>\n     </mj-body>\n   </mjml>"))