# JMeter based Performance Test Suite

## Overview
The JMeter based Performance Test Suite is build on the basic ideas of [Apache JMeter](https://jmeter.apache.org/index.html) but with reworks in many areas to make it compatible with [XLT](https://www.xceptance.com/de/xlt/) and [XTC](https://xtc.xceptance.com/).

## Requirements

* Java Version 8 or higher
* Clone the [Posters Advanced Loadtest Suite](https://github.com/Xceptance/posters-advanced-loadtest-suite)
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
1. Clone the JMeter based Performance Test Suite on your local machine
2. Build a [test plan](https://jmeter.apache.org/usermanual/get-started.html#test_plan_building)
3. Set the output file of the recording should be of type `.jmx`
4. Expand the `Test Plan` to see its content
    * Add the needed controller and sampler
    * Optional
      * _set the number of threads_
      * _set ramp-up period_
      * _set loop count_
5. Run your test plan
6. See the results under `View Results Tree`
7. Save your `.jmx` file
8. Open the JMTeter based Performance Test Suite in your favourite IDEA
9. Go to `config > data > tests` and see your previous saved file
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
13. Save and comit your changes
14. Run your test and see the results in the provided result browswer
15. Go to the [Xceptance Test Center (XTC)](https://xtc.xceptance.com/)
16. Setup a new [load test](https://docs.xceptance.com/xtc/loadtesting/)
17. Start you loadtest

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

### Performance Comparison
* Compare standard load test vs. JMeter recorded test (both XLT report in the end)

### Post-Processors
* implemented but not all are working at the moment

### Loop Controller
* works with internal counter as designed

### While Controller
* works with internal counter

### CSV Data
* works but the path need adjustement in JMeterGUI

## Not Yet Supported Functionality
* File Upload
* XPath2 Assertions