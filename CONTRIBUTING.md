## Making a release
1. Commit everything and push to origin
2. Make sure there are no changes
3. Run `mvn release:prepare -DdryRun=true` to check if the updated pom.xml is what is expected
4. Run `mvn release:rollback` to revert
5. Run `mvn release:prepare` to make the release
    * The release plugin will 
        1. Update the project version to a non-snapshot 
        2. Compile the javadocs
        3. Compile java sources
        4. Commit and push to origin
        5. Create a tag and push to origin
        6. Update the project version to the next snapshot
        7. Commit and push to origin again
    * Then upon push, Travis will
        1. Make a build according to the release push
        2. Make a build according to the tag (which will also deploy to Maven Central)
        3. Make a build according to the next snapshot push
6. Run `mvn release:clean` to clean up the release artifacts