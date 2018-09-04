# 1.2.0 (2018-09-04)
### New features
* Introduced `@DbSetupBinderConfiguration`

# 1.1.1 (2018-09-04)
### Bug fixes
* Because of the way `@Nested` test instances are created, `TestInstancePostProcessor::postProcessTestInstance` is 
  called twice, once for the outer class, once for the inner class. If the outer class does not have any 
  `@DbSetupOperations` the extension will throw an `IllegalArgumentException`, complaining about not finding any 
  `@DbSetupOperations`. The simple fix is to allow cases where there is no `@DbSetupOperations`.   

### Enhancements
* Added debug JUL logging

# 1.1.0 (2018-08-31)
### New features
* Support multiple data sources
    * `DbSetupSource::name`
    * `DbSetupOperation::sources`
    
### Bug fixes
* Use `ExtensionContext.Store` instead of state variables
    * Because JUnit Jupiter do not guarantee when extensions are instantiated or how long they are kept around 

# 1.0.0 (2018-08-24)
### New features
* Implemented annotations
    * `@DbSetup`
    * `@DbSetupSource`
    * `@DbSetupOperations`
    * `@DbSetupSkipNext`