package com.xceptance.loadtest.jmeter.tests;

import com.xceptance.loadtest.test.AbstractComponentTest;


public class XLTJMeterCheckout extends AbstractComponentTest
{
    public XLTJMeterCheckout()
    {
        useRequestMode = false;
        jmxSource = "/Checkout.jmx";
    } 
}