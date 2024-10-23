package com.xceptance.loadtest.jmeter.tests;

import com.xceptance.loadtest.test.AbstractComponentTest;


public class TSearch extends AbstractComponentTest
{
    public TSearch()
    {
        useRequestMode = false;
        jmxSource = "/search.jmx";
    } 
}