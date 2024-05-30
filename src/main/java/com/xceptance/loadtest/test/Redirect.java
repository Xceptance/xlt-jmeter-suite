package com.xceptance.loadtest.test;

import org.apache.jmeter.engine.StandardJMeterEngine;

public class Redirect extends AbstractComponentTest
{
    private StandardJMeterEngine jmeter;

    public Redirect()
    {
        jmxSource = "/tests/Redirect.jmx";

        useRequestMode = false;
    }

    @Override
    protected void test() throws Throwable
    {
        super.test();
    }
}