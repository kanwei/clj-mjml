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

(deftest basic-render-test
  (testing "Renders simple MJML to HTML"
    (let [html (mjml/render basic-template)]
      (is (string? html))
      (is (str/includes? html "<!doctype html>"))
      (is (str/includes? html "Hello World")))))

;; ========================================
;; Include Resolver Tests - Classpath
;; ========================================

(deftest classpath-include-single-test
  (testing "Includes a single template from classpath"
    (let [template "<mjml>
                     <mj-body>
                       <mj-include path=\"test-templates/header.mjml\" />
                       <mj-section>
                         <mj-column>
                           <mj-text>Main content goes here</mj-text>
                         </mj-column>
                       </mj-section>
                     </mj-body>
                   </mjml>"
          resolver (mjml/create-classpath-resolver)
          html (mjml/render template {:mj-include-resolver resolver})]
      (is (str/includes? html "Company Logo"))
      (is (str/includes? html "Main content goes here")))))

(deftest classpath-include-multiple-test
  (testing "Includes multiple templates from classpath"
    (let [template "<mjml>
                     <mj-body>
                       <mj-include path=\"test-templates/header.mjml\" />
                       <mj-section>
                         <mj-column>
                           <mj-text>Email body content</mj-text>
                         </mj-column>
                       </mj-section>
                       <mj-include path=\"test-templates/footer.mjml\" />
                     </mj-body>
                   </mjml>"
          resolver (mjml/create-classpath-resolver)
          html (mjml/render template {:mj-include-resolver resolver})]
      (is (str/includes? html "Company Logo"))
      (is (str/includes? html "Email body content"))
      (is (str/includes? html "© 2026 Company Name")))))

(deftest classpath-include-nested-path-test
  (testing "Includes template from nested directory in classpath"
    (let [template "<mjml>
                     <mj-body>
                       <mj-section>
                         <mj-column>
                           <mj-include path=\"test-templates/partials/navigation.mjml\" />
                         </mj-column>
                       </mj-section>
                     </mj-body>
                   </mjml>"
          resolver (mjml/create-classpath-resolver)
          html (mjml/render template {:mj-include-resolver resolver})]
      (is (str/includes? html "Home"))
      (is (str/includes? html "About"))
      (is (str/includes? html "Products"))
      (is (str/includes? html "Contact")))))

(deftest classpath-include-within-section-test
  (testing "Includes template within a section"
    (let [template "<mjml>
                     <mj-body>
                       <mj-section>
                         <mj-column>
                           <mj-text>Before include</mj-text>
                           <mj-include path=\"test-templates/partials/content-block.mjml\" />
                           <mj-text>After include</mj-text>
                         </mj-column>
                       </mj-section>
                     </mj-body>
                   </mjml>"
          resolver (mjml/create-classpath-resolver)
          html (mjml/render template {:mj-include-resolver resolver})]
      (is (str/includes? html "Before include"))
      (is (str/includes? html "reusable content block"))
      (is (str/includes? html "After include")))))

(deftest classpath-include-not-found-test
  (testing "Throws exception when classpath resource not found"
    (let [template "<mjml>
                     <mj-body>
                       <mj-include path=\"test-templates/nonexistent.mjml\" />
                     </mj-body>
                   </mjml>"
          resolver (mjml/create-classpath-resolver)]
      (is (thrown? Exception
                   (mjml/render template {:mj-include-resolver resolver}))))))

;; ========================================
;; Include Resolver Tests - Filesystem
;; ========================================

(deftest filesystem-include-single-test
  (testing "Includes a single template from filesystem"
    (let [template "<mjml>
                     <mj-body>
                       <mj-include path=\"fs-header.mjml\" />
                       <mj-section>
                         <mj-column>
                           <mj-text>Filesystem content</mj-text>
                         </mj-column>
                       </mj-section>
                     </mj-body>
                   </mjml>"
          base-path (str (System/getProperty "user.dir") "/test/test-files")
          resolver (mjml/create-filesystem-resolver base-path)
          html (mjml/render template {:mj-include-resolver resolver})]
      (is (str/includes? html "Filesystem Header"))
      (is (str/includes? html "Filesystem content")))))

(deftest filesystem-include-multiple-test
  (testing "Includes multiple templates from filesystem"
    (let [template "<mjml>
                     <mj-body>
                       <mj-include path=\"fs-header.mjml\" />
                       <mj-section>
                         <mj-column>
                           <mj-text>Main section</mj-text>
                         </mj-column>
                       </mj-section>
                       <mj-include path=\"fs-footer.mjml\" />
                     </mj-body>
                   </mjml>"
          base-path (str (System/getProperty "user.dir") "/test/test-files")
          resolver (mjml/create-filesystem-resolver base-path)
          html (mjml/render template {:mj-include-resolver resolver})]
      (is (str/includes? html "Filesystem Header"))
      (is (str/includes? html "Main section"))
      (is (str/includes? html "Filesystem Footer")))))

(deftest filesystem-include-default-base-path-test
  (testing "Uses current directory as default base path"
    (let [template "<mjml>
                     <mj-body>
                       <mj-include path=\"test/test-files/fs-header.mjml\" />
                     </mj-body>
                   </mjml>"
          resolver (mjml/create-filesystem-resolver)
          html (mjml/render template {:mj-include-resolver resolver})]
      (is (str/includes? html "Filesystem Header")))))

(deftest filesystem-include-not-found-test
  (testing "Handles missing filesystem file gracefully with error comment"
    (let [template "<mjml>
                     <mj-body>
                       <mj-include path=\"nonexistent.mjml\" />
                     </mj-body>
                   </mjml>"
          base-path (str (System/getProperty "user.dir") "/test/test-files")
          resolver (mjml/create-filesystem-resolver base-path)
          html (mjml/render template {:mj-include-resolver resolver})]
      (is (str/includes? html "<!-- mj-include fails to read file")))))

;; ========================================
;; Include Resolver Tests - Custom
;; ========================================

(deftest custom-resolver-with-map-test
  (testing "Custom resolver using in-memory map"
    (let [templates {"header" "<mj-section><mj-column><mj-text>Custom Header</mj-text></mj-column></mj-section>"
                     "footer" "<mj-section><mj-column><mj-text>Custom Footer</mj-text></mj-column></mj-section>"}
          loader-fn (fn [path] (get templates path))
          resolver (mjml/create-custom-resolver loader-fn)
          template "<mjml>
                     <mj-body>
                       <mj-include path=\"header\" />
                       <mj-section>
                         <mj-column>
                           <mj-text>Body content</mj-text>
                         </mj-column>
                       </mj-section>
                       <mj-include path=\"footer\" />
                     </mj-body>
                   </mjml>"
          html (mjml/render template {:mj-include-resolver resolver})]
      (is (str/includes? html "Custom Header"))
      (is (str/includes? html "Body content"))
      (is (str/includes? html "Custom Footer")))))

(deftest custom-resolver-with-prefix-test
  (testing "Custom resolver that adds prefix to paths"
    (let [loader-fn (fn [path]
                      (slurp (clojure.java.io/resource
                              (str "test-templates/" path))))
          resolver (mjml/create-custom-resolver loader-fn)
          template "<mjml>
                     <mj-body>
                       <mj-include path=\"header.mjml\" />
                       <mj-section>
                         <mj-column>
                           <mj-text>Content</mj-text>
                         </mj-column>
                       </mj-section>
                     </mj-body>
                   </mjml>"
          html (mjml/render template {:mj-include-resolver resolver})]
      (is (str/includes? html "Company Logo"))
      (is (str/includes? html "Content")))))

(deftest custom-resolver-with-fallback-test
  (testing "Custom resolver with fallback logic"
    (let [primary {"special" "<mj-section><mj-column><mj-text>Special Template</mj-text></mj-column></mj-section>"}
          loader-fn (fn [path]
                      (or (get primary path)
                          (slurp (clojure.java.io/resource
                                  (str "test-templates/" path)))))
          resolver (mjml/create-custom-resolver loader-fn)
          template "<mjml>
                     <mj-body>
                       <mj-include path=\"special\" />
                       <mj-include path=\"header.mjml\" />
                     </mj-body>
                   </mjml>"
          html (mjml/render template {:mj-include-resolver resolver})]
      (is (str/includes? html "Special Template"))
      (is (str/includes? html "Company Logo")))))

;; ========================================
;; Real-World Include Scenarios
;; ========================================

(deftest complete-email-with-includes-test
  (testing "Complete transactional email using includes"
    (let [template "<mjml>
                     <mj-head>
                       <mj-title>Order Confirmation</mj-title>
                       <mj-preview>Your order has been confirmed</mj-preview>
                     </mj-head>
                     <mj-body>
                       <mj-include path=\"test-templates/header.mjml\" />
                       <mj-section>
                         <mj-column>
                           <mj-text font-size=\"20px\" font-weight=\"bold\">
                             Order #12345
                           </mj-text>
                           <mj-text>
                             Thank you for your purchase!
                           </mj-text>
                           <mj-button href=\"https://example.com/orders/12345\">
                             View Order
                           </mj-button>
                         </mj-column>
                       </mj-section>
                       <mj-include path=\"test-templates/footer.mjml\" />
                     </mj-body>
                   </mjml>"
          resolver (mjml/create-classpath-resolver)
          html (mjml/render template {:mj-include-resolver resolver})]
      (is (str/includes? html "Company Logo"))
      (is (str/includes? html "Order #12345"))
      (is (str/includes? html "Thank you for your purchase"))
      (is (str/includes? html "View Order"))
      (is (str/includes? html "© 2026 Company Name")))))

(deftest newsletter-with-includes-test
  (testing "Newsletter template with header and navigation includes"
    (let [template "<mjml>
                     <mj-body>
                       <mj-include path=\"test-templates/header.mjml\" />
                       <mj-section>
                         <mj-column>
                           <mj-include path=\"test-templates/partials/navigation.mjml\" />
                         </mj-column>
                       </mj-section>
                       <mj-section>
                         <mj-column>
                           <mj-text font-size=\"24px\" font-weight=\"bold\">
                             This Month's Newsletter
                           </mj-text>
                           <mj-text>
                             Check out our latest updates and news!
                           </mj-text>
                         </mj-column>
                       </mj-section>
                       <mj-include path=\"test-templates/footer.mjml\" />
                     </mj-body>
                   </mjml>"
          resolver (mjml/create-classpath-resolver)
          html (mjml/render template {:mj-include-resolver resolver})]
      (is (str/includes? html "Company Logo"))
      (is (str/includes? html "Home"))
      (is (str/includes? html "This Month's Newsletter"))
      (is (str/includes? html "© 2026 Company Name")))))

;; ========================================
;; Configuration and Options Tests
;; ========================================

(deftest render-without-resolver-test
  (testing "Renders successfully when no includes are present"
    (let [template "<mjml>
                     <mj-body>
                       <mj-section>
                         <mj-column>
                           <mj-text>Content without includes</mj-text>
                         </mj-column>
                       </mj-section>
                     </mj-body>
                   </mjml>"
          html (mjml/render template)]
      (is (str/includes? html "Content without includes")))))

(deftest language-option-test
  (testing "Renders with specified language"
    (let [template "<mjml>
                     <mj-body>
                       <mj-section>
                         <mj-column>
                           <mj-text>Bonjour le monde</mj-text>
                         </mj-column>
                       </mj-section>
                     </mj-body>
                   </mjml>"
          html (mjml/render template {:language "fr"})]
      (is (str/includes? html "lang=\"fr\""))
      (is (str/includes? html "Bonjour le monde")))))

(deftest text-direction-rtl-test
  (testing "Renders with right-to-left text direction"
    (let [template "<mjml>
                     <mj-body>
                       <mj-section>
                         <mj-column>
                           <mj-text>مرحبا بالعالم</mj-text>
                         </mj-column>
                       </mj-section>
                     </mj-body>
                   </mjml>"
          html (mjml/render template {:text-direction :rtl})]
      (is (str/includes? html "dir=\"rtl\""))
      (is (str/includes? html "مرحبا بالعالم")))))

(deftest combined-language-and-direction-test
  (testing "Renders with both language and text direction"
    (let [template "<mjml>
                     <mj-body>
                       <mj-section>
                         <mj-column>
                           <mj-text>مرحبا بالعالم</mj-text>
                         </mj-column>
                       </mj-section>
                     </mj-body>
                   </mjml>"
          html (mjml/render template {:language "ar" :text-direction :rtl})]
      (is (str/includes? html "lang=\"ar\""))
      (is (str/includes? html "dir=\"rtl\""))
      (is (str/includes? html "مرحبا بالعالم")))))

(deftest all-options-combined-test
  (testing "Renders with all options: resolver, language, and text direction"
    (let [template "<mjml>
                     <mj-body>
                       <mj-include path=\"test-templates/header.mjml\" />
                       <mj-section>
                         <mj-column>
                           <mj-text>Contenu en français</mj-text>
                         </mj-column>
                       </mj-section>
                     </mj-body>
                   </mjml>"
          resolver (mjml/create-classpath-resolver)
          html (mjml/render template {:mj-include-resolver resolver
                                      :language "fr"
                                      :text-direction :ltr})]
      (is (str/includes? html "lang=\"fr\""))
      (is (str/includes? html "dir=\"ltr\""))
      (is (str/includes? html "Company Logo"))
      (is (str/includes? html "Contenu en français")))))