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