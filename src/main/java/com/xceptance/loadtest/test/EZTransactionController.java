package com.xceptance.loadtest.test;

public class EZTransactionController extends AbstractComponentTest
{
    public EZTransactionController()
    {
        jmxSource = "/tests/EZTransaction.jmx";

        expectedActionNamesRequestMode = new String[]{"First", "First", "Second"};

        expectedActionNamesTransactionMode = new String[]{"VisitTransactionController",
                                                          "UnnamedTransactionController_1",
                                                          "SecondTransactionController"};

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
