.PHONY: clean dev build-sass

clean:
	rm -rf target *.log node_modules resources/public/css/style.css resources/public/css/style.css.map

node_modules:
	npm install

dev: 	node_modules
	clojure -A:fig:build

resources/public/css/style.css: node_modules
	clojure -A:build-sass

build-sass: resources/public/css/style.css
