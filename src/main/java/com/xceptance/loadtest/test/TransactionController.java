package com.xceptance.loadtest.test;

public class TransactionController extends AbstractComponentTest
{
    public TransactionController()
    {
        jmxSource = "/tests/TransactionController.jmx";

        expectedActionNames = new String[]{"First", "Second",
                "Third", "First2", "FirstInTG2InTC2", "SecondInTG2InTC2", "ThirdInTG2InTC2"};
    }

    @Override
    public void test() throws Throwable
    {
        super.test();
        validateActions();
    }
}
