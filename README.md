# clj-mjml

A Clojure library for rendering [MJML](https://mjml.io/) email templates to HTML using the excellent [mjml4j](https://github.com/digitalfondue/mjml4j) library.

## Installation

Add to your `deps.edn`:

```clojure
{:deps {com.github.kanwei/clj-mjml {:mvn/version "0.1.0"}}}
```

Or to your `project.clj`:

```clojure
[com.github.kanwei/clj-mjml "0.1.0"]
```

## Quick Start

```clojure
(require '[clj-mjml.mjml :as mjml])

;; Basic rendering
(def html (mjml/render "<mjml>
                          <mj-body>
                            <mj-section>
                              <mj-column>
                                <mj-text>Hello World!</mj-text>
                              </mj-column>
                            </mj-section>
                          </mj-body>
                        </mjml>"))
```

## Usage

### Basic Rendering

```clojure
(require '[clj-mjml.mjml :as mjml])

;; Simple template
(mjml/render "<mjml>
               <mj-body>
                 <mj-section>
                   <mj-column>
                     <mj-text font-size=\"20px\">Hello World</mj-text>
                     <mj-button href=\"https://example.com\">Click Me</mj-button>
                   </mj-column>
                 </mj-section>
               </mj-body>
             </mjml>")
;; => "<!doctype html><html>...</html>"
```

### Using Template Includes

Split your templates into reusable components with `mj-include`:

```clojure
;; Load templates from classpath (resources directory)
(def resolver (mjml/create-classpath-resolver))

(mjml/render "<mjml>
               <mj-body>
                 <mj-include path=\"templates/header.mjml\" />
                 <mj-section>
                   <mj-column>
                     <mj-text>Email content here</mj-text>
                   </mj-column>
                 </mj-section>
                 <mj-include path=\"templates/footer.mjml\" />
               </mj-body>
             </mjml>"
             {:mj-include-resolver resolver})
```

### Language and Text Direction

Render emails with specific language and text direction:

```clojure
;; French email
(mjml/render template {:language "fr"})

;; Arabic email with right-to-left text
(mjml/render template 
             {:language "ar"
              :text-direction :rtl})

;; Combine with includes
(mjml/render template
             {:mj-include-resolver (mjml/create-classpath-resolver)
              :language "es"
              :text-direction :ltr})
```

### Configuration Options

The `render` function accepts an options map:

```clojure
(mjml/render template {
  :mj-include-resolver resolver   ; Handle mj-include tags
  :language "en"                  ; Set lang attribute (default: "en")
  :text-direction :ltr            ; :ltr or :rtl (default: :ltr)
})
```

## Include Resolvers

clj-mjml provides three types of include resolvers for loading template partials:

### 1. Classpath Resolver

Load templates from your project's resources directory (or JAR files):

```clojure
(def resolver (mjml/create-classpath-resolver))

;; Directory structure:
;; resources/
;;   templates/
;;     header.mjml
;;     footer.mjml
;;     partials/
;;       navigation.mjml

(mjml/render template {:mj-include-resolver resolver})
```

### 2. Filesystem Resolver

Load templates from the filesystem:

```clojure
;; With specific base path
(def resolver (mjml/create-filesystem-resolver "/path/to/templates"))

;; Or use current directory as base
(def resolver (mjml/create-filesystem-resolver))

(mjml/render template {:mj-include-resolver resolver})
```

### 3. Custom Resolver

Implement your own loading logic:

```clojure
;; Load from database
(def resolver 
  (mjml/create-custom-resolver
    (fn [path]
      (db/fetch-template-by-name path))))

;; Load from memory
(def templates {"header" "<mj-section>...</mj-section>"
                "footer" "<mj-section>...</mj-section>"})

(def resolver
  (mjml/create-custom-resolver
    (fn [path] (get templates path))))

(mjml/render template {:mj-include-resolver resolver})
```

## Examples

### Transactional Email

```clojure
(ns my-app.email
  (:require [clj-mjml.mjml :as mjml]))

(def resolver (mjml/create-classpath-resolver))

(defn render-order-confirmation [order]
  (mjml/render 
    (str "<mjml>
           <mj-head>
             <mj-title>Order Confirmation</mj-title>
           </mj-head>
           <mj-body>
             <mj-include path=\"templates/header.mjml\" />
             <mj-section>
               <mj-column>
                 <mj-text font-size=\"20px\">Order #" (:id order) "</mj-text>
                 <mj-text>Thank you for your purchase!</mj-text>
                 <mj-button href=\"" (:tracking-url order) "\">
                   Track Order
                 </mj-button>
               </mj-column>
             </mj-section>
             <mj-include path=\"templates/footer.mjml\" />
           </mj-body>
         </mjml>")
    {:mj-include-resolver resolver}))
```

### Multi-Language Support

```clojure
(defn render-welcome-email [user]
  (let [lang (:language user)
        dir (if (contains? #{"ar" "he"} lang) :rtl :ltr)
        template-path (str "templates/welcome-" lang ".mjml")]
    (mjml/render 
      (str "<mjml>
             <mj-body>
               <mj-include path=\"" template-path "\" />
             </mj-body>
           </mjml>")
      {:mj-include-resolver (mjml/create-classpath-resolver)
       :language lang
       :text-direction dir})))
```

### Newsletter Template

```clojure
(def resolver (mjml/create-filesystem-resolver "./newsletter-templates"))

(defn render-newsletter [articles]
  (let [article-sections 
        (map #(str "<mj-include path=\"articles/" (:id %) ".mjml\" />") 
             articles)]
    (mjml/render
      (str "<mjml>
             <mj-body>
               <mj-include path=\"branding/header.mjml\" />
               <mj-include path=\"branding/navigation.mjml\" />
               " (clojure.string/join "\n" article-sections) "
               <mj-include path=\"branding/footer.mjml\" />
             </mj-body>
           </mjml>")
      {:mj-include-resolver resolver})))
```

## Error Handling

When includes cannot be loaded:

- **Classpath Resolver**: Throws an exception
- **Filesystem Resolver**: Renders HTML with error comment: `<!-- mj-include fails to read file: path -->`
- **Custom Resolver**: Depends on your implementation

## API Reference

### Core Functions

#### `render`

```clojure
(render template)
(render template options)
```

Renders MJML to HTML.

**Arguments:**
- `template` - MJML template string
- `options` - Optional map with keys:
  - `:mj-include-resolver` - Include resolver for handling mj-include tags
  - `:language` - Language code (default: "en")
  - `:text-direction` - `:ltr` or `:rtl` (default: `:ltr`)

**Returns:** HTML string

#### `create-classpath-resolver`

```clojure
(create-classpath-resolver)
```

Creates a resolver that loads includes from the classpath.

#### `create-filesystem-resolver`

```clojure
(create-filesystem-resolver)
(create-filesystem-resolver base-path)
```

Creates a resolver that loads includes from the filesystem.

**Arguments:**
- `base-path` - String or File representing the base directory (optional, defaults to current directory)

#### `create-custom-resolver`

```clojure
(create-custom-resolver loader-fn)
```

Creates a custom resolver using a provided loader function.

**Arguments:**
- `loader-fn` - Function `(fn [path] => mjml-string)` that loads templates

## See Also

- [MJML Documentation](https://documentation.mjml.io/#mjml-guide)
- [mjml4j](https://github.com/digitalfondue/mjml4j) - The underlying Java library

## Development

### Running Tests

```bash
$ clojure -T:build test
```

## License

Copyright © 2025 Kanwei Li

Distributed under the MIT License.


### Building

Run the project's CI pipeline and build a JAR:

```bash
$ clojure -T:build ci
```

This will produce an updated `pom.xml` file with synchronized dependencies inside the `META-INF`
directory inside `target/classes` and the JAR in `target`. You can update the version (and SCM tag)
information in generated `pom.xml` by updating `build.clj`.

### Installing Locally

Install it locally (requires the `ci` task be run first):

```bash
$ clojure -T:build install
```

### Deploying to Clojars

Deploy to Clojars -- needs `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment variables (requires the `ci` task be run first):

```bash
$ clojure -T:build deploy
```

Your library will be deployed to `com.github.kanwei/clj-mjml` on clojars.org.
