package com.xceptance.loadtest.test;

public class HttpRequestTest extends AbstractComponentTest
{
    public HttpRequestTest()
    {
        jmxSource = "/tests/HttpRequest.jmx";

        expectedActionNamesRequestMode = new String[]{"VisitRequest", "ServiceRequest",
                                                      "UnnamedRequest_1", "UnnamedRequest_2",
                                                      "VisitRequestUnnamedThreadGroup", "ServiceRequestUnnamedThreadGroup",
                                                      "UnnamedRequest_3", "UnnamedRequest_4"};

        expectedActionNamesTransactionMode = new String[]{"ThreadGroup1", "ThreadGroup2", "UnnamedThreadGroup_1", "UnnamedThreadGroup_2"};

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
