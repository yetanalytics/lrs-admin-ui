.PHONY: clean clean-css dev build-sass bundle dev-min

clean:
	rm -rf target *.log node_modules resources/public/css/style.css resources/public/css/style.css.map

clean-css:
	rm resources/public/css/style.css

node_modules:
	npm audit && npm install

dev: 	node_modules
	clojure -A:fig:dev:build

dev-min: node_modules
	clojure -M:fig:min

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

target/bundle/css: resources/public/css/style.css
	mkdir -p target/bundle
	cp -r resources/public/css target/bundle/css

target/bundle/images:
	mkdir -p target/bundle
	cp -r resources/public/images target/bundle/images

target/bundle: target/bundle/index.html target/bundle/main.js target/bundle/css target/bundle/images

bundle: target/bundle
