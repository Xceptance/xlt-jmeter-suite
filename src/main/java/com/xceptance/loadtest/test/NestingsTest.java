package com.xceptance.loadtest.test;

public class NestingsTest extends AbstractComponentTest
{
    public NestingsTest()
    {
        jmxSource = "/tests/Nestings.jmx";

        expectedActionNames = new String[]{"RequestK", "RequestL", "NestedTransactionControllerWithNoParentSample"};
    }

    @Override
    public void test() throws Throwable
    {
        super.test();
        validateActions();
    }
}
