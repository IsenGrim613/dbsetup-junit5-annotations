Do you use [JUnit5](https://junit.org/junit5/) for your tests?

Do you use [DbSetup](http://dbsetup.ninja-squad.com/) to prepare your database for integration tests?

Do you write a lot of boilerplate code because of this?

# Introducing JUnit5 extensions for DbSetup

[![Build Status](https://travis-ci.org/isengrim613/junit5-dbsetup-annotations.svg?branch=master)](https://travis-ci.org/isengrim613/junit5-dbsetup-annotations)
[![codecov](https://codecov.io/gh/isengrim613/junit5-dbsetup-annotations/branch/master/graph/badge.svg)](https://codecov.io/gh/isengrim613/junit5-dbsetup-annotations)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.isengrim613/junit5-dbsetup-annotations/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.isengrim613/junit5-dbsetup-annotations)

These are helper annotations that implement the JUnit5 extension API to help reduce boilerplate code when using 
DbSetup. These are very simple annotations that directly map to DbSetup operations.

### @DbSetup
This is the main annotation that tells JUnit5 to look for other DbSetup related annotations. Without this, the other 
annotations will do nothing.

Requirements: 

* Annotation target: class only

### @DbSetupSource
This annotation tells DbSetup which data source to run operations on. 

There can be multiple data sources, but they must all be uniquely named. Operations will also need to specify which 
data source they are to be launched on.

Requirements: 

* Annotation target: field only<sup>[#](#fields-only)</sup>
* Target must be of `javax.sql.DataSource` type  
* Target can both be static or not static

### @DbSetupOperation
DbSetup will launch the operations that are annotated with this. Because SQL scripts innately require to be ordered, 
eg satisfying referential integrity, the operations will be launched in order. However, Java is a language that does 
not preserve declaration order of fields thus we cannot use declared order as our implicit order (how nice would it 
be if we could do so). 

There are 2 ways to declare the order of the operation. 

1. Explicitly by setting the `order` variable
    * Eg. `@DbSetupOperation(order = 0) Operation myOperation`
2. Implicitly by post-pending your variable with the order number
    * Eg. `@DbSetupOperation Operation myOperation0`
    
The `@DbSetupOperation.order` variable takes precedence if both are available.

If there are multiple data sources, the `sources()` field can be used to define which data source that this operation 
will be launched on. 

Requirements:

* Annotation target: field only<sup>[#](#fields-only)</sup>
* Target must be of `com.ninja_squad.dbsetup.operation.Operation` type  
* Target can both be static or not static
* There can multiple targets
* Targets must all be ordered either explicitly or implicitly

### @DbSetupSkipNext
If this annotation is placed on a test method, DbSetup will not be launched for the next test. This is synonymous to 
writing `dbTracker.skipNextLaunch();` in your test.

* Annotation target: method only
* Target must be a `@Test` otherwise it does nothing

---

### Example code
See [here](src/test/java/com/github/isengrim613/junit5/DbSetupSimpleTest.java) for a concrete example on how to use the annotations.

---

<sup><a name="fields-only">\#</a></sup>: The reason why only fields are supported is to prevent unintentional code that does not 
work, like this:
```
@DbSetupSource
static DataSource dataSource() {
    return new DataSource();
}
```

The code will compile but every operation will be run on a different data source and is very likely to be incorrect.  