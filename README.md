# This is a container module for the Bisq business logic. 

The usage is as a drop-in dependency when compiling a module with boot sequence. At the moment, the compliant boot module and shaded.jar provider is [bisq-engine](https://github.com/citkane/bisq-engine).

### Goal

To modularise the Bisq stack and allow for complete code re-usability. Any logic that could be re-used by any module of the Bisq stack goes in here.
 

### Architecture logic

`business` is structured into various classpath packages that supply specific functionality:

**[io.bisq.business.Data](https://github.com/citkane/bisq-business/blob/master/business/src/main/java/io/bisq/business/Data.java):**

This is the lowest level of functionality available to the module. The GUICE injector is provided to the module and exposes the Bisq Core service instances


**[io.bisq.business.models](https://github.com/citkane/bisq-business/tree/master/business/src/main/java/io/bisq/business/models):**

Various data models construct the Bisq services into runnable methods to perform actions


**[io.bisq.business.actions](https://github.com/citkane/bisq-business/tree/master/business/src/main/java/io/bisq/business/actions):**

External input is audited, formatted and sequenced for consumption by `io.bisq.business.actions`.


**[io.bisq.business.formatters](https://github.com/citkane/bisq-business/tree/master/business/src/main/java/io/bisq/business/formatters):**

Output data is formatted into classes for further consumption or return / Json conversion.


**[io.bisq.business.validation](https://github.com/citkane/bisq-business/tree/master/business/src/main/java/io/bisq/business/validation)**

Validation logic for input / outputs
