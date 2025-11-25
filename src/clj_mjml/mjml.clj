(ns clj-mjml.mjml
  "Clojure wrapper for ch.digitalfondue.mjml4j"
  (:gen-class)
  (:import
   [ch.digitalfondue.mjml4j Mjml4j
                            Mjml4j$Configuration
                            Mjml4j$IncludeResolver
                            Mjml4j$ResourceLoader
                            Mjml4j$SimpleResourceResolver
                            Mjml4j$TextDirection])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli]))

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

 (-main "<mjml>\n     <mj-body>\n       <mj-section>\n         <mj-column>\n           <mj-text>Hello World</mj-text>\n         </mj-column>\n       </mj-section>\n     </mj-body>\n   </mjml>")

 )