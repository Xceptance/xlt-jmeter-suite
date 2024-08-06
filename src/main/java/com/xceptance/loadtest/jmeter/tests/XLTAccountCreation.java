package com.xceptance.loadtest.jmeter.tests;

import com.xceptance.loadtest.test.AbstractComponentTest;

public class XLTAccountCreation extends AbstractComponentTest
{
    public XLTAccountCreation()
    {
      useRequestMode = false;
      jmxSource ="/tests/accountCreation.jmx";
    }
}