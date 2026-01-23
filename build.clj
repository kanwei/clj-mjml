(ns build
  (:refer-clojure :exclude [test])
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

(def lib 'com.github.kanwei/clj-mjml)
(def version "0.1.0")
#_; alternatively, use MAJOR.MINOR.COMMITS:
  (def version (format "1.0.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")

(defn test "Run all the tests." [opts]
  (let [basis (b/create-basis {:aliases [:test]})
        cmds (b/java-command
              {:basis basis
               :main 'clojure.main
               :main-args ["-m" "cognitect.test-runner"]})
        {:keys [exit]} (b/process cmds)]
    (when-not (zero? exit) (throw (ex-info "Tests failed" {}))))
  opts)

(defn- pom-template [version]
  [[:description "Clojure library for rendering MJML email templates to HTML"]
   [:url "https://github.com/kanwei/clj-mjml"]
   [:licenses
    [:license
     [:name "MIT License"]
     [:url "https://opensource.org/licenses/MIT"]]]
   [:developers
    [:developer
     [:name "Kanwei Li"]]]
   [:scm
    [:url "https://github.com/kanwei/clj-mjml"]
    [:connection "scm:git:https://github.com/kanwei/clj-mjml.git"]
    [:developerConnection "scm:git:ssh:git@github.com:kanwei/clj-mjml.git"]
    [:tag (str "v" version)]]])

(defn- jar-opts [opts]
  (assoc opts
         :lib lib :version version
         :jar-file (format "target/%s-%s.jar" lib version)
         :basis (b/create-basis {})
         :class-dir class-dir
         :target "target"
         :src-dirs ["src"]
         :pom-data (pom-template version)))

(defn ci "Run the CI pipeline of tests (and build the JAR)." [opts]
  (test opts)
  (b/delete {:path "target"})
  (let [opts (jar-opts opts)]
    (println "\nWriting pom.xml...")
    (b/write-pom opts)
    (println "\nCopying source...")
    (b/copy-dir {:src-dirs ["resources" "src"] :target-dir class-dir})
    (println "\nBuilding JAR..." (:jar-file opts))
    (b/jar opts))
  opts)

(defn install "Install the JAR locally." [opts]
  (let [opts (jar-opts opts)]
    (b/install opts))
  opts)

(defn deploy "Deploy the JAR to Clojars." [opts]
  (let [{:keys [jar-file] :as opts} (jar-opts opts)]
    (dd/deploy {:installer :remote :artifact (b/resolve-path jar-file)
                :pom-file (b/pom-path (select-keys opts [:lib :class-dir]))}))
  opts)

(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file "target/app.jar")

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main 'clj-mjml.mjml})) ;; Replace with your actual main namespace
