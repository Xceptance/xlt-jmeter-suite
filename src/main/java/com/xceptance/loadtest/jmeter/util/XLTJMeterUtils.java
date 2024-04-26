package com.xceptance.loadtest.jmeter.util;

import java.util.List;

import org.apache.jmeter.processor.PostProcessor;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testbeans.TestBeanHelper;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;

public class XLTJMeterUtils
{
    public static final String LAST_SAMPLE_OK = "JMeterThread.last_sample_ok"; // $NON-NLS-1$
    
    public static void setLastSampleOk(JMeterVariables variables, boolean value) 
    {
        variables.put(LAST_SAMPLE_OK, Boolean.toString(value));
    }
    
    public static void runPostProcessors(List<? extends PostProcessor> extractors) 
    {
        for (PostProcessor ex : extractors) 
        {
            TestBeanHelper.prepare((TestElement) ex);
            ex.process();
        }
    }

    public static void runPreProcessors(List<? extends PreProcessor> preProcessors) 
    {
        for (PreProcessor ex : preProcessors) 
        {
            TestBeanHelper.prepare((TestElement) ex);
            ex.process();
        }
    }
}
