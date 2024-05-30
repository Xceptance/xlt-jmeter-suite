package com.xceptance.loadtest.jmeter.util;

import java.util.List;

import org.apache.jmeter.extractor.DebugPostProcessor;
import org.apache.jmeter.processor.PostProcessor;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testbeans.TestBeanHelper;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;

/**
 * Utility class for additional functionalities.
 */
public class XLTJMeterUtils
{
    public static final String LAST_SAMPLE_OK = "JMeterThread.last_sample_ok"; // $NON-NLS-1$
    
    /**
     * Set the state of the last sample execution.
     * @param variables
     * @param value
     */
    public static void setLastSampleOk(JMeterVariables variables, boolean value) 
    {
        variables.put(LAST_SAMPLE_OK, Boolean.toString(value));
    }
    
    /**
     * Execute the post processors, if any are found. Ignore the @{@link DebugPostProcessor} since the debugger is mostly for the GUI mode.
     * 
     * @param extractors
     */
    public static void runPostProcessors(List<? extends PostProcessor> extractors) 
    {
        for (PostProcessor ex : extractors) 
        {
            TestBeanHelper.prepare((TestElement) ex);
            
            if (ex instanceof DebugPostProcessor)
            {
                // skip we do not support DebugPostProcessor, since this depend on an actual JMeter Thread for handling, which we do not use
            }
            else
            {
                ex.process();
            }
        }
    }

    /**
     * Execute the pre processors, if any are found.
     * 
     * @param preProcessors
     */
    public static void runPreProcessors(List<? extends PreProcessor> preProcessors) 
    {
        for (PreProcessor ex : preProcessors) 
        {
            TestBeanHelper.prepare((TestElement) ex);
            ex.process();
        }
    }
}
