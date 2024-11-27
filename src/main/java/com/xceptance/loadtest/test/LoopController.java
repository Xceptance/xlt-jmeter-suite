package com.xceptance.loadtest.test;

public class LoopController extends AbstractComponentTest
{
    public LoopController()
    {
        jmxSource = "/tests/LoopController.jmx";
        
        expectedActionNames = new String[]{"Request1", "Request1", "Request2", "Request2", "Request3",
                                                      "Request3", "Request4", "Request4", "Request5", "Request5"};
    }

    @Override
    public void test() throws Throwable
    {
        super.test();
        validateActions();
    }
}
