package com.xceptance.loadtest.jmeter.tests;

import com.xceptance.loadtest.test.AbstractComponentTest;


public class TBrowse extends AbstractComponentTest
{
    public TBrowse()
    {
        useRequestMode = false;
        jmxSource = "/browse.jmx";
    } 
}