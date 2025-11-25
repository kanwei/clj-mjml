t:
	clj -T:build uber && java -jar target/app.jar "<mjml> \
                                          <mj-body> \
                                            <mj-section> \
                                              <mj-column> \
                                                <mj-text>Hello World</mj-text> \
                                              </mj-column> \
                                            </mj-section> \
                                          </mj-body> \
                                        </mjml>"

native:
	$(GRAALVM_HOME)/bin/native-image \
      -jar target/app.jar \
      -H:Name=mjml-cli \
      --no-fallback \
      --initialize-at-build-time