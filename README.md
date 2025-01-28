# XLT Test Suite with JMeter Support 

## Overview
The JMeter based Performance Test Suite is build on the basic ideas of [Apache JMeter](https://jmeter.apache.org/index.html) but with reworks in many areas to make it compatible with [XLT](https://www.xceptance.com/de/xlt/) and [XTC](https://xtc.xceptance.com/).

Bottom line, you record, edit, and maintain your tests in Jmeter, but you will use XLT to run and evaluate your tests. This gives you excellent debuggability because you can utilize the resultbrowser feature to see nicely what has been executed, requested, and fetched.

## Requirements
* Java Version 17 or higher
* Download and Run Apache JMeter
    * [Download](https://jmeter.apache.org/download_jmeter.cgi) Apache JMeter
    * [Install](https://jmeter.apache.org/usermanual/get-started.html#install) Apache JMeter
    * [Run](https://jmeter.apache.org/usermanual/get-started.html#running) Apache JMeter

* Configure proxy access in the web browser
    * [Google Chrome](https://oxylabs.io/resources/integrations/chrome)
    * [Mozilla Firefox](https://smartproxy.com/configuration/how-to-setup-proxy-on-firefox-browser)
    * [Safari](https://smartproxy.com/configuration/how-to-setup-proxy-on-safari-browser)
* Enable the proxy


## How To Use
1. Clone this test suite
2. Build a [test plan](https://jmeter.apache.org/usermanual/get-started.html#test_plan_building)
3. Output file of the recording should be `.jmx`
4. Expand the `Test Plan` to see its content
    * Add the needed controller and sampler
5. Run your test plan
6. See the results under `View Results Tree`
7. Save your `.jmx` file to this directory `<testsuite>/config/data/tests`
8. Open this test suite in your beloved Java IDE
9. Go to `config/data/tests` to see your previous saved file
10. Go to `src/main/java/com/xceptance/loadtest/jmeter/tests` and add your test    
```
public class YourTestCaseName extends AbstractComponentTest
{
    public YourTestCaseName()
    {
      jmxSource ="/tests/yourTestFile.jmx";
    }
}
```
11. Add your test case(s) to the `config/testcase-mapping.properties`
12. Add your test case(s) to the list of active test case(s) `config/test.properties`
13. Save and commit your changes
14. Run your test and see the results in the provided result browser
15. Go to the [Xceptance Test Center (XTC)](https://xtc.xceptance.com/)
16. Setup a new [load test](https://docs.xceptance.com/xtc/loadtesting/)
17. Start your loadtest

## JMeter Dependencies
JMeter dependencies are upgrade.properties, saveservice.properties and jmeter.properties are taken from default JMeter setup and are needed for the engine. In case there are adjusted values simply, add the files under the created site. At the moment default is used for reference.

## Naming and Best Practice
Best practice is to always give meaningful names to thread groups, transactions and requests. These names are used for the report and it is recommended to identify the correct requests in the report. If there are no names given, the engine will create a default name for identification.
For reference in XLT each .jmx file is considered a single test case, therefore create the test cases in different .jmx files. All Transaction Controller in the current file are later listed as Actions in the report and all request under the given Transaction Controller, if the Generate parent sample option is selected. If the option is disabled all request will be listed individual.

## Supported Functionality
### Thread Group
* grouping different samplers below thread group
* XLT thread management instead of using JMeter

### HTTP Request
* can fire simple requests

### Pre-Processors
* implemented, as it is in JMeter

### Assertions
* assertion checker from JMeter are implemented and fire events in XLT, additonal we support the continue (only events) and stop function from JMeter (ResultBrowser and errors)

### Post-Processors
* implemented but not all are working at the moment

### Loop Controller
* works with internal counter as designed

### While Controller
* works with internal counter

### CSV Data
* Works, but the path must be next to or below the location of the JMX file, otherwise the test suite upload with XLT won't work

## Not Yet Supported
* File Upload
* XPath2 Assertions

## Limitations
By design, the load test config is not read from the JMeter file. This has to be done the classical way.
