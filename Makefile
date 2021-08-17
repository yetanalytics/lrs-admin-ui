.phony: clean

clean:
	rm -rf target

node_modules:
	npm install

dist/main.js: node_modules
	clojure -M -m cljs.main -d dist -o "dist/main.js" -O advanced -c com.yetanalytics.lrs-admin-ui.core
