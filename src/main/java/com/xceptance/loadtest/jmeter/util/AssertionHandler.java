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

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.assertions.Assertion;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.TestBeanHelper;
import org.apache.jmeter.testelement.AbstractScopedAssertion;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContext.TestLogicalAction;
import org.apache.jmeter.threads.JMeterThread;
import org.apache.jorphan.util.JMeterError;
import org.junit.Assert;

import com.xceptance.loadtest.api.events.EventLogger;

/**
 * Handle all assertions which can be set on actions in JMeter. Depending on the ThreadGroup settings, this class will as well interrupt the test in
 * case of an assertions. If the mode is set to continue, in JMeter, XLT will log the assertions as events, to not interrupt the test.
 *
 * The assertions handling is based on code from the {@link JMeterThread}.
 */
public class AssertionHandler
{
    private boolean onErrorStopTest;
    private boolean onErrorStopTestNow;
    private boolean onErrorStopThread;
//    private boolean onErrorStartNextLoop; not used, but can be set in JMeter

    private boolean logging;

    /**
     * Constructor, loads the thread group settings for assertion handling.
     * @param group
     */
    public AssertionHandler(AbstractThreadGroup group)
    {
        onErrorStopTest = group.getOnErrorStopTest();
        onErrorStopTestNow = group.getOnErrorStopTestNow();
        onErrorStopThread = group.getOnErrorStopThread();
//        onErrorStartNextLoop = group.getOnErrorStartNextLoop();
        logging = true;
    }

    /**
     * Handle all possible assertions on the current actions in a recursive way.
     *
     * @param assertions
     * @param parent
     * @param threadContext
     */
    public void checkAssertions(List<? extends Assertion> assertions, SampleResult parent, JMeterContext threadContext)
    {
        for (Assertion assertion : assertions)
        {
            TestBeanHelper.prepare((TestElement) assertion);
            if (assertion instanceof AbstractScopedAssertion)
            {
                AbstractScopedAssertion scopedAssertion = (AbstractScopedAssertion) assertion;
                String scope = scopedAssertion.fetchScope();
                if (scopedAssertion.isScopeParent(scope)
                    || scopedAssertion.isScopeAll(scope)
                    || scopedAssertion.isScopeVariable(scope))
                {
                    processAssertion(parent, assertion);
                }
                if (scopedAssertion.isScopeChildren(scope)
                    || scopedAssertion.isScopeAll(scope))
                {
                    recurseAssertionChecks(parent, assertion, 3);
                }
            } else
            {
                processAssertion(parent, assertion);
            }
        }
        XLTJMeterUtils.setLastSampleOk(threadContext.getVariables(), parent.isSuccessful());
    }

    /**
     * Depending on the result and settings either log the error or set the test for termination.
     *
     * @param result
     * @param threadContext
     */
    public void checkAssertionStatus(SampleResult result, JMeterContext threadContext)
    {
        // no log for errors
        if (onErrorStopThread ||
            onErrorStopTest ||
            onErrorStopTestNow)
        {
            logging = false;
        }

        // Check if thread or test should be stopped
        if (result.isStopThread() || (!result.isSuccessful() && onErrorStopThread))
        {
            // nothing to do
        }
        if (result.isStopTest() || (!result.isSuccessful() && onErrorStopTest))
        {
            // nothing to do
        }
        if (result.isStopTestNow() || (!result.isSuccessful() && onErrorStopTestNow))
        {
            // nothing to do
        }
        if (result.getTestLogicalAction() != TestLogicalAction.CONTINUE)
        {
            threadContext.setTestLogicalAction(result.getTestLogicalAction());
        }
    }

    /**
     * Check all assertions on the current element in recursive way.
     *
     * @param parent
     * @param assertion
     * @param level
     */
    private void recurseAssertionChecks(SampleResult parent, Assertion assertion, int level)
    {
        if (level < 0)
        {
            return;
        }
        SampleResult[] children = parent.getSubResults();
        boolean childError = false;
        for (SampleResult childSampleResult : children)
        {
            processAssertion(childSampleResult, assertion);
            recurseAssertionChecks(childSampleResult, assertion, level - 1);
            if (!childSampleResult.isSuccessful())
            {
                childError = true;
            }
        }
        // If parent is OK, but child failed, add a message and flag the parent as failed
        if (childError && parent.isSuccessful())
        {
            AssertionResult assertionResult = new AssertionResult(((AbstractTestElement) assertion).getName());
            assertionResult.setResultForFailure("One or more sub-samples failed");
            parent.addAssertionResult(assertionResult);
            parent.setSuccessful(false);
        }
    }

    /**
     * Process the assertions, depending on the settings can this terminate the test. If logging is enabled all encountered errors will be logged as events,
     * if not the test will be terminated.
     *
     * @param result
     * @param assertion
     */
    private void processAssertion(SampleResult result, Assertion assertion)
    {
        AssertionResult assertionResult = null;
        try
        {
            assertionResult = assertion.getResult(result);
            // check the overall status
            checkAssertionStatus(result, null);
        }
        catch (AssertionError | JMeterError | Exception e)
        {
            EventLogger.DEFAULT.warn("Error processing Assertion.", e.getMessage());
            assertionResult = new AssertionResult("Assertion failed!");
            assertionResult.setFailure(true);
            assertionResult.setFailureMessage(e.toString());
        }
        finally
        {
            if (assertionResult != null &&
                assertionResult.isFailure() ||
                assertionResult.isError())
            {
                if (logging)
                {
                    EventLogger.DEFAULT.warn("Assertion was found.", assertionResult.getFailureMessage());
                }
                else
                {
                    // hard stop the test since it is configured to stop on error
                    Assert.fail(StringUtils.isNotEmpty(assertionResult.getFailureMessage()) ? assertionResult.getFailureMessage() : assertionResult.getName());
                }
            }
        }
        result.setSuccessful(result.isSuccessful() && !(assertionResult.isError() || assertionResult.isFailure()));
        result.addAssertionResult(assertionResult);
    }
}
