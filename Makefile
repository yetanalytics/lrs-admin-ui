.PHONY: clean dev

clean:
	rm -rf target *.log node_modules resources/public/css/style.css resources/public/css/style.css.map

dev:
	npm install
	clj -A:fig:build

build-sass:
	clj -A:build-sass
