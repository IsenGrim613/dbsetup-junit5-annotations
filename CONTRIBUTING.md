## Making a release

```
mvn release:clean
mvn release:prepare -DdryRun=true
mvn release:prepare
```

And then Travis will build the tag and deploy to Maven Central