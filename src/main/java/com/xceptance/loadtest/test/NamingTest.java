package com.xceptance.loadtest.test;

public class NamingTest extends AbstractComponentTest
{
    public NamingTest()
    {
        jmxSource = "/tests/NamingTest.jmx";

        expectedActionNames = new String[]{"RequestA", "UnnamedRequest_1", "UnnamedRequest_2",
                                                      "RequestB", "UnnamedRequest_3", "UnnamedRequest_4",
                                                      "RequestC", "UnnamedRequest_5", "UnnamedRequest_6",
                                                      "TransactionControllerWithParentSample", "UnnamedTransactionController_1", "RequestF", "RequestG", "RequestH",
                                                      "RequestI", "UnnamedRequest_7", "RequestJ", "UnnamedRequest_8", "ParentTransactionControllerWithParentSample",
                                                      "NestedTransactionControllerWithParentSample", "NestedTransactionControllerWithParentSample",
                                                      "UnnamedThreadGroup_1","RequestB",
                                                      "UnnamedRequest_9", "UnnamedRequest_10", "RequestC",
                                                      "UnnamedRequest_11","UnnamedRequest_12",
                                                      "TransactionControllerWithParentSample", "UnnamedTransactionController_2", "RequestF",
                                                      "RequestG", "RequestH", "RequestI", "UnnamedRequest_13", "RequestJ",
                                                      "UnnamedRequest_14", "ParentTransactionControllerWithParentSample",
                                                      "NestedTransactionControllerWithParentSample", "NestedTransactionControllerWithParentSample"};
    }

    @Override
    public void test() throws Throwable
    {
        super.test();
        validateActions();
    }
}
