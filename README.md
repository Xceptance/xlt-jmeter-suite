# XLT Test Suite with JMeter Support 

## Overview
The JMeter based Performance Test Suite is build on the basic ideas of [Apache JMeter](https://jmeter.apache.org/index.html) but with reworks in many areas to make it compatible with [XLT](https://www.xceptance.com/de/xlt/).

Bottom line, you record, edit, and maintain your tests in Jmeter, but you will use XLT to run and evaluate your tests. This gives you excellent debuggability because you can utilize the XLT features to see nicely what has been executed, requested, and fetched.

## Requirements
* Java Version 17 or higher
* An existing `.jmx` file for test execution. This can be created with [JMeter](https://jmeter.apache.org/).

## How To Use
1. Clone this test suite
2. Build a [test plan](https://jmeter.apache.org/usermanual/get-started.html#test_plan_building) or use an existing `.jmx` file
3. Save your `.jmx` file to this directory `<testsuite>/config/data/jmeter`
4. Open this test suite in your preferred Java IDE
5. Go to `config/data/jmeter` to see your previous saved file
6. Go to `src/main/java/com/xceptance/loadtest/jmeter/tests` and add your test, there are already some example test, for reference   
```
public class YourTestCaseName extends JMeterTestCase
{
    public YourTestCaseName()
    {
      super("yourTestFile.jmx");
    }
}
```
7. Add your test case(s) to the [test case mapping](https://docs.xceptance.com/xlt/load-testing/manual/480-test-suite-configuration/#test-class-mapping)
8. Add your test case(s) to the list of active test case(s) in the [test configuration](https://docs.xceptance.com/xlt/load-testing/manual/480-test-suite-configuration/#load-test-profile-configuration)
9. Save and commit your changes
10. Run your test and see the results in the provided result browser
11. Go to the [Xceptance Test Center (XTC)](https://xtc.xceptance.com/)
12. Setup a new [load test](https://docs.xceptance.com/xtc/loadtesting/)
13. Start your loadtest

## JMeter Dependencies
JMeter dependencies are upgrade.properties, saveservice.properties and jmeter.properties are taken from default JMeter setup and are needed for the engine. In case there are adjusted values simply, add the files under the created site. At the moment default is used for reference.

## Naming and Best Practice
Best practice is to always give meaningful names to thread groups, transactions and requests. These names are used for the report and it is recommended to identify the correct requests in the report. If there are no names given, the engine will create a default name for identification.
For reference in XLT each `.jmx` file is considered a single test case, therefore create the test cases in different `.jmx` files. All Transaction Controller in the current file are later listed as Actions in the report and all request under the given Transaction Controller, if the Generate parent sample option is selected. If the option is disabled all request will be listed individual.

## Supported Functionality
### Thread Group
* grouping different samplers below thread group
* XLT thread management instead of using JMeter. This means that infinite or loops, ramp up and users is managed by XTC and via properties.
* all thread groups in one `.jmx` file are executed one after the other, this is not recommended

### HTTP Request
* can fire simple HTTP requests and HTTP multipart

### Pre-Processors
* implemented, as it is in JMeter

### Assertions
* assertion checker from JMeter are implemented and fire events in XLT, additional we support the continue (only events) and stop function from JMeter (ResultBrowser and errors)

### Post-Processors
* implemented, as it is in JMeter

### Loop Controller
* works with internal counter as designed

### While Controller
* works with internal counter

### CSV Data
* Works, the `.csv` file(s) need to be placed at the follwoing location `<testsuite>/config/data/jmeter/data`.

## Not Yet Supported
* File Upload
* XPath2 Assertions

## Limitations
By design, the load test config is not read from the JMeter file. This has to be done the classical way via [property file](https://docs.xceptance.com/xlt/load-testing/manual/470-load-configuration). Think times and load will be controlled by XTC.

It is strictly recommended to only have one active thread group per scenario (`.jmx` file). Since this will be directly visible in the report later. Multiple thread groups, in one `.jmx` file, will be listed as actions.