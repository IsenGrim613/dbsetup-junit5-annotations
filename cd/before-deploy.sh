#!/usr/bin/env bash

openssl aes-256-cbc -K $encrypted_fb3952287f03_key -iv $encrypted_fb3952287f03_iv -in cd/codesigning.asc.enc -out cd/codesigning.asc -d
gpg --fast-import cd/codesigning.asc