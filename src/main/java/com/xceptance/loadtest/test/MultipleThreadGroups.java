package com.xceptance.loadtest.test;

public class MultipleThreadGroups extends AbstractComponentTest
{
    public MultipleThreadGroups()
    {
        jmxSource = "/tests/MultipleThreadGroups.jmx";

        expectedActionNamesRequestMode = new String[]{"HTTPRequest1", "HTTPRequest2"};

        expectedActionNamesTransactionMode = new String[]{"ThreadGroup1", "ThreadGroup2"};

        useRequestMode = false;

        expectedActionNames = useRequestMode ? expectedActionNamesRequestMode : expectedActionNamesTransactionMode;
    }

    @Override
    public void test() throws Throwable
    {
        super.test();
        validateActions();
    }
}
