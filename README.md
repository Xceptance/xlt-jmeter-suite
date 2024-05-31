# JMeter based Performance Test Suite

## Overview
The XLT JMeter test suite is built on the ideas of the JMeter test suite but with clear reworks in many areas to make it compatible with XLT. the execution of the generated .jmx files from JMeter can be used for load test and replay without any changes needed in the jmx files. Therefore all changes with JMeter can be simply seen and used in the test suite.

## What is possible?
The current suite is suited for recorded sessions with JMeter and load testing with XLT using XTC (Xceptance Test Center).

Currently supported:
* Sampler: HTTP Request, JSR232 Sampler
* Logic Controller: Transaction, Loop, While, Random, Random Order
* Assertions: All except XPath2
* PostProccessors: All except Debug (will be ignored silently)
* PreProcessors: All
* Timer: not supported XLT use internal timer which can be set and configured

## How to use?
Simple create the Test Plan, using the recoding tool from JMeter, and import the jmx file into the suite. At the moment there are 2 pre configured test cases under jmeter\tests XLTJMeterCheckout and XLTJMeterOrder which are example for the setup and usage of the suite. The used jmx files needs to be put under config\data for usage. 

TODO