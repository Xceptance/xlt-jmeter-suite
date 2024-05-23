package com.xceptance.loadtest.test;

public class RandomOrderTest extends AbstractComponentTest
{
    public RandomOrderTest()
    {
        jmxSource = "/tests/RandomOrder.jmx";

        useRequestMode = true;
    }
}
