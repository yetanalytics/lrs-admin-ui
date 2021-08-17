.phony: clean

clean:
	rm -rf target dist/*

node_modules:
	npm install

dist/main.js: node_modules
	clojure -M -m cljs.main -co prod.cljs.edn -c
