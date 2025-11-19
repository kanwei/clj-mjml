(ns clj-mjml.mjml-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [clj-mjml.mjml :as mjml]))

(def basic-template
  "<mjml>
     <mj-body>
       <mj-section>
         <mj-column>
           <mj-text>Hello World</mj-text>
         </mj-column>
       </mj-section>
     </mj-body>
   </mjml>")

(def template-with-include
  "<mjml>
     <mj-body>
       <mj-include path=\"header.mjml\" />
       <mj-section>
         <mj-column>
           <mj-text>Main Content</mj-text>
         </mj-column>
       </mj-section>
     </mj-body>
   </mjml>")

(def header-partial
  "<mj-section>
     <mj-column>
       <mj-text>Header Content</mj-text>
     </mj-column>
   </mj-section>")

(deftest basic-render-test
  (testing "Renders simple MJML to HTML"
    (let [html (mjml/render basic-template)]
      (is (string? html))
      (is (str/includes? html "<!doctype html>"))
      (is (str/includes? html "Hello World")))))
