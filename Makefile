.PHONY: clean clean-css dev build-sass build-sass-watch bundle

clean:
	rm -rf target *.log node_modules resources/public/css/style.css resources/public/css/style.css.map pom.xml

clean-css:
	rm resources/public/css/style.css

node_modules:
	npm audit && npm install

dev: 	node_modules
	clojure -A:fig:dev:build

resources/public/css/style.css: node_modules
	npx sass resources/sass/style.scss:resources/public/css/style.css -I ./node_modules

build-sass: resources/public/css/style.css

build-sass-watch: node_modules
	npx sass resources/sass/style.scss:resources/public/css/style.css -I ./node_modules --watch

target/public/cljs-out/prod/main_bundle.js: node_modules
	clojure -Mfig -m figwheel.main -O advanced -bo prod

target/bundle/index.html:
	mkdir -p target/bundle
	cp resources/public/index_prod.html target/bundle/index.html

target/bundle/main.js: target/public/cljs-out/prod/main_bundle.js
	mkdir -p target/bundle
	cp target/public/cljs-out/prod/main_bundle.js target/bundle/main.js

target/bundle/css: resources/public/css/style.css
	mkdir -p target/bundle
	cp -r resources/public/css target/bundle/css

target/bundle/images:
	mkdir -p target/bundle
	cp -r resources/public/images target/bundle/images

target/bundle: target/bundle/index.html target/bundle/main.js target/bundle/css target/bundle/images

bundle: target/bundle

## This pom.xml file is generated solely in an action to populate the GitHub
## Dependency Graph. This allows generation of an accurate SBOM.

pom.xml:
	clojure -Spom
