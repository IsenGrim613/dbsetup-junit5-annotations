#!/usr/bin/env bash

if [ -z "$TRAVIS_TAG" ]; then exit 0; fi

openssl aes-256-cbc -K $encrypted_fb3952287f03_key -iv $encrypted_fb3952287f03_iv -in cd/codesigning.asc.enc -out cd/codesigning.asc -d
gpg --fast-import cd/codesigning.asc