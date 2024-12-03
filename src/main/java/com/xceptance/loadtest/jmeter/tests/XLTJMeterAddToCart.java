package com.xceptance.loadtest.jmeter.tests;

import com.xceptance.loadtest.test.AbstractComponentTest;

public class XLTJMeterAddToCart extends AbstractComponentTest
{
    public XLTJMeterAddToCart()
    {
        useRequestMode = false;
        jmxSource ="/tests/addToCart.jmx";
    }
}