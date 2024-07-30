# JMeter based Performance Test Suite

## Overview
The XLT JMeter test suite is built on the ideas of the JMeter test suite but with clear reworks in many areas to make it compatible with XLT. The execution of the generated `.jmx` files from JMeter can be used for load test and replay without any changes needed in the jmx files. Therefore all changes with JMeter can be simply seen and used in the test suite. 

For demo purposes the XLT JMeter test suite is connected to the Xceptance [Posters](https://github.com/Xceptance/posters-demo-store) test suite.

## What is possible?
The suite is made for recorded sessions with JMeter and load testing with XLT using XTC (Xceptance Test Center).

### Currently supported:
* **Sampler**: _HTTP Request, JSR232 Sampler_
* **Logic Controller**: _Transaction, Loop, While, Random, Random Order_
* **Assertions**: _All except XPath2_
* **PostProcessors**: _All except Debug (will be ignored silently)_
* **PreProcessors**: _All_
* **Timer**: _not supported XLT use internal timer which can be set and configured_

## How to use?

Simple create the [Test Plan](https://jmeter.apache.org/usermanual/build-test-plan.html), using the [JMeter recording tool](https://jmeter.apache.org/usermanual/jmeter_proxy_step_by_step.html), and import the jmx file into the suite. 

At the moment there are two preconfigured test cases available in the testsuite:

1. `XLTJMeterCheckout`  
2. `XLTJMeterOrder` 

Both test cases can be found under [jmeter/tests](https://github.com/Xceptance/xlt-jmeter-extension/tree/main/src/main/java/com/xceptance/loadtest/jmeter/tests) and are examples for the setup and usage of the suite. The referenced jmx files are stored under [config/data](https://github.com/Xceptance/xlt-jmeter-extension/tree/main/config/data).

Mozilla Firefox is the recommended browser for the recording.