/*
 * Copyright (c) 2005-2025 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
