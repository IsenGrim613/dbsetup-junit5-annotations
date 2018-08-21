Do you use [JUnit5](https://junit.org/junit5/) for your tests?

Do you use [DbSetup](http://dbsetup.ninja-squad.com/) to prepare your database for integration tests?

Do you write a lot of boilerplate code because of this?

# Introducing JUnit5 extensions for DbSetup

These are helper annotations that implement the JUnit5 extension API to help reduce boilerplate code when using 
DbSetup. These are very simple annotations that directly map to DbSetup operations.

### Annotations
##### @DbSetup
This is the main annotation that tells JUnit5 to look for other DbSetup related annotations. Without this, the other 
annotations will do nothing.

Requirements: 

* Annotation target: class

##### @DbSetupSourceFactory
This annotation tells DbSetup which data source to run operations on.

Requirements: 

*  Annotation target: method or field
* Must return or be of `javax.sql.DataSource` type  
* Can be static or not
* There can only be 1 @DbSetupSourceFactory

##### @DbSetupOperation
DbSetup will launch the operations that are annotated with this. The launch sequence is ordered in the following way:

1. Hierarchically from the outermost class, and then
1. Hierarchically from the deepest super class