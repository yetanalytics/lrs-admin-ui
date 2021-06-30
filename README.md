# LRS Admin UI

CLJS Project for UI for some admin functions shared by multiple LRS implementations

## Overview

Figwheel reagent+re-frame app that launches a UI with administrative LRS functions. Goal is being imported into an LRS client build to share majority of client application structure between implementations

## Development

To get an interactive development environment run:

    clojure -A:fig:build

To clean all compiled files:

    rm -rf target/public

To create a production build run:

	rm -rf target/public
	clojure -A:fig:min
