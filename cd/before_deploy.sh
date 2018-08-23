#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    openssl aes-256-cbc -K $encrypted_6c3b7719f7ba_key -iv $encrypted_6c3b7719f7ba_iv -in codesigning.asc.enc -out codesigning.asc -d
    gpg --fast-import cd/codesigning.asc
fi