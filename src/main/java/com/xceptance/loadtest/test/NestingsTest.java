package com.xceptance.loadtest.test;

public class NestingsTest extends AbstractComponentTest
{
    public NestingsTest()
    {
        jmxSource = "/tests/Nestings.jmx";

        expectedActionNamesRequestMode = new String[]{"RequestK", "RequestL", "RequestL"};

        expectedActionNamesTransactionMode = new String[]{"ParentTransactionControllerWithNoParentSample",
                                                          "NestedTransactionControllerWithNoParentSample",
                                                          "NestedTransactionControllerWithNoParentSample"};

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
