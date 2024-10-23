package com.xceptance.loadtest.jmeter.tests;

import com.xceptance.loadtest.test.AbstractComponentTest;


public class TVisit extends AbstractComponentTest
{
    public TVisit()
    {
        useRequestMode = false;
        jmxSource = "/visit.jmx";
    } 
}