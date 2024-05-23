package com.xceptance.loadtest.test;

public class NamingTest extends AbstractComponentTest
{
    public NamingTest()
    {
        jmxSource = "/tests/NamingTest.jmx";

        expectedActionNamesRequestMode = new String[]{"RequestA", "UnnamedRequest_1", "UnnamedRequest_2",
                                                      "RequestB", "UnnamedRequest_3", "UnnamedRequest_4",
                                                      "RequestC", "UnnamedRequest_5", "UnnamedRequest_6",
                                                      "RequestD", "RequestE", "RequestF", "RequestF", "RequestG",
                                                      "RequestG", "RequestH", "RequestH", "RequestI", "UnnamedRequest_7",
                                                      "RequestJ", "UnnamedRequest_8", "RequestK", "RequestL",
                                                      "UnnamedRequest_9", "RequestA", "UnnamedRequest_10",
                                                      "UnnamedRequest_11", "RequestB", "UnnamedRequest_12",
                                                      "UnnamedRequest_13", "RequestC", "UnnamedRequest_14",
                                                      "UnnamedRequest_15", "RequestD", "RequestE", "RequestF", "RequestF",
                                                      "RequestG", "RequestG", "RequestH", "RequestH", "RequestI",
                                                      "UnnamedRequest_16", "RequestJ", "UnnamedRequest_17", "RequestK",
                                                      "RequestL", "UnnamedRequest_18"};

        expectedActionNamesTransactionMode = new String[]{"ThreadGroup", "TransactionControllerNoParentSample", "UnnamedTransactionController_1",
                                                          "TransactionControllerWithParentSample", "UnnamedTransactionController_2",
                                                          "UnnamedTransactionController_3", "TransactionControllerInLoopControllerNoParentSample",
                                                          "TransactionControllerInLoopControllerWithParentSample", "ParentTransactionControllerNoParentSample",
                                                          "NestedTransactionControllerNoParentSample", "ParentTransactionControllerWithParentSample",
                                                          "NestedTransactionControllerWithParentSample", "NestedTransactionControllerWithParentSample",
                                                          "UnnamedThreadGroup_1", "TransactionControllerNoParentSample", "UnnamedTransactionController_4",
                                                          "TransactionControllerWithParentSample", "UnnamedTransactionController_5", "UnnamedTransactionController_6",
                                                          "TransactionControllerInLoopControllerNoParentSample", "TransactionControllerInLoopControllerWithParentSample",
                                                          "ParentTransactionControllerNoParentSample", "NestedTransactionControllerNoParentSample",
                                                          "ParentTransactionControllerWithParentSample", "NestedTransactionControllerWithParentSample",
                                                          "NestedTransactionControllerWithParentSample"};

        useRequestMode = true;

        expectedActionNames = useRequestMode ? expectedActionNamesRequestMode : expectedActionNamesTransactionMode;
    }

    @Override
    public void test() throws Throwable
    {
        super.test();
        validateActions();
    }
}
