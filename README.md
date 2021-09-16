# LRS Admin UI

CLJS Project for UI for some admin functions shared by multiple Yet LRS implementations

## Overview

Figwheel reagent+re-frame app that launches a UI with administrative LRS functions. Goal is being imported into an LRS client build to share majority of client application structure between implementations. Currently supports SQL LRS Admin API.

## Development

To get an interactive development environment run:

    make dev

To build the stylesheets run:

    make build-sass

To clean all compiled files:

    make clean

To compile everything for production to `target/bundle`:

    make bundle

## License

Copyright © 2021 Yet Analytics, Inc.

Distributed under the Apache License version 2.0.
