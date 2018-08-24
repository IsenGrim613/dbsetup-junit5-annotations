#!/usr/bin/env bash

if [ -z "$TRAVIS_TAG" ]; then exit 0; fi

mvn deploy -P sign,generate-docs --settings cd/mvnsettings.xml