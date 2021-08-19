.PHONY: clean dev build-sass bundle

clean:
	rm -rf target *.log node_modules resources/public/css/style.css resources/public/css/style.css.map dist/main.js dist/css dist/images

node_modules:
	npm install

dev: 	node_modules
	clojure -A:fig:build

resources/public/css/style.css: node_modules
	clojure -A:build-sass

build-sass: resources/public/css/style.css

target/public/cljs-out/prod-main.js: node_modules
	clojure -Mfig -m figwheel.main -O advanced -bo prod

dist/main.js: target/public/cljs-out/prod-main.js
	mkdir -p dist
	cp target/public/cljs-out/prod-main.js dist/main.js

dist/css: build-sass
	mkdir -p dist
	cp -r resources/public/css dist/css

dist/images:
	cp -r resources/public/images dist/images

bundle: dist/main.js dist/css dist/images
