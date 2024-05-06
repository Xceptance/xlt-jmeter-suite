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
import org.apache.jorphan.util.JMeterError;
import org.junit.Assert;

import com.xceptance.loadtest.api.events.EventLogger;

public class AssertionHandler
{
    private boolean onErrorStopTest;
    private boolean onErrorStopTestNow;
    private boolean onErrorStopThread;
    private boolean onErrorStartNextLoop;
    
    private boolean shutdown;
    private boolean logging;
    
    public AssertionHandler(AbstractThreadGroup group)
    {
        onErrorStopTest = group.getOnErrorStopTest();
        onErrorStopTestNow = group.getOnErrorStopTestNow();
        onErrorStopThread = group.getOnErrorStopThread();
        onErrorStartNextLoop = group.getOnErrorStartNextLoop();
        shutdown = false;
        logging = true;
    }
    
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
            shutdown();
        }
        if (result.isStopTest() || (!result.isSuccessful() && onErrorStopTest))
        {
            shutdown();
        }
        if (result.isStopTestNow() || (!result.isSuccessful() && onErrorStopTestNow))
        {
            shutdown();
        }
        if (result.getTestLogicalAction() != TestLogicalAction.CONTINUE) 
        {
            threadContext.setTestLogicalAction(result.getTestLogicalAction());
        }
    }
    
    public void shutdown()
    {
        shutdown = true;
    }

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
