# clj-mjml/mjml

Clojure library to use the excellent ch.digitalfondue.mjml4j/mjml4j library,
providing MJML functionality in pure Java.

## Usage

Use as a library, or from the CLI (WIP).

Run the project's tests:

    $ clojure -T:build test

Run the project's CI pipeline and build a JAR:

    $ clojure -T:build ci

This will produce an updated `pom.xml` file with synchronized dependencies inside the `META-INF`
directory inside `target/classes` and the JAR in `target`. You can update the version (and SCM tag)
information in generated `pom.xml` by updating `build.clj`.

Install it locally (requires the `ci` task be run first):

    $ clojure -T:build install

Deploy it to Clojars -- needs `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment
variables (requires the `ci` task be run first):

    $ clojure -T:build deploy

Your library will be deployed to net.clojars.clj-mjml/mjml on clojars.org by default.

## License

Copyright © 2025 Kanwei Li, MIT License
