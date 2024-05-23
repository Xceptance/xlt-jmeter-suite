package com.xceptance.loadtest.test;

public class TransactionController extends AbstractComponentTest
{
    public TransactionController()
    {
        jmxSource = "/tests/TransactionController.jmx";

        expectedActionNamesRequestMode = new String[]{"First", "Second", "Third", "First2", "FirstInTG2InTC2",
                "SecondInTG2InTC2", "ThirdInTG2InTC2"};

        expectedActionNamesTransactionMode = new String[]{"VisitTransactionController", "SecondTransactionController",
                "ThirdTC", "Visit2TransactionController", "ThirdTCInTG2", "Visit2TransactionControllerInTG2"};

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
