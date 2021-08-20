.PHONY: clean dev build-sass bundle

clean:
	rm -rf target *.log node_modules resources/public/css/style.css resources/public/css/style.css.map

node_modules:
	npm install

dev: 	node_modules
	clojure -A:fig:build

resources/public/css/style.css: node_modules
	clojure -A:build-sass

build-sass: resources/public/css/style.css

target/public/cljs-out/prod/main_bundle.js: node_modules
	clojure -Mfig -m figwheel.main -O advanced -bo prod

target/bundle/index.html:
	mkdir -p target/bundle
	cp resources/public/index_prod.html target/bundle/index.html

target/bundle/main.js: target/public/cljs-out/prod/main_bundle.js
	mkdir -p target/bundle
	cp target/public/cljs-out/prod/main_bundle.js target/bundle/main.js

target/bundle/css: build-sass
	mkdir -p target/bundle
	cp -r resources/public/css target/bundle/css

target/bundle/images:
	mkdir -p target/bundle
	cp -r resources/public/images target/bundle/images

target/bundle: target/bundle/index.html target/bundle/main.js target/bundle/css target/bundle/images

bundle: target/bundle
