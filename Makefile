.PHONY: clean dev build-sass

clean:
	rm -rf target *.log node_modules resources/public/css/style.css resources/public/css/style.css.map dist/*

node_modules:
	npm install

dist/main.js: node_modules
	clojure -Mfig -m figwheel.main -O advanced -bo prod

dev: 	node_modules
	clojure -A:fig:build

resources/public/css/style.css: node_modules
	clojure -A:build-sass

build-sass: resources/public/css/style.css
