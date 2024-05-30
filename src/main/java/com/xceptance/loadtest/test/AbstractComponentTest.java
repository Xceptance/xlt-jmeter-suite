package com.xceptance.loadtest.test;

import com.xceptance.loadtest.api.data.DataFileProvider;
import com.xceptance.loadtest.control.JMeterTestCase;
import com.xceptance.loadtest.control.XLTJMeterEngine;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.junit.Assert;

import java.io.File;
import java.util.List;
import java.util.Optional;

public abstract class AbstractComponentTest extends JMeterTestCase
{
    protected String jmxSource;

    protected boolean useRequestMode = true;

    private XLTJMeterEngine jmeter;

    protected String[] expectedActionNames;

    protected String[] expectedActionNamesRequestMode;

    protected String[] expectedActionNamesTransactionMode;

    public AbstractComponentTest()
    {
        try
        {
            super.init();
        }
        catch (Throwable e)
        {
            AssertionError ae = new AssertionError(e.getMessage());
            ae.setStackTrace(e.getStackTrace());
            throw ae;
        }
    }

    /**
     * Main test method.
     *
     * @throws Throwable
     */
    @Override
    protected void test() throws Throwable
    {
        HashTree tree = initTest();
        jmeter = new XLTJMeterEngine(useRequestMode);
        jmeter.configure(tree);
        jmeter.run();
    }

    protected void testWithStandardEngine() throws Throwable
    {
        HashTree tree = initTest();
        StandardJMeterEngine stdEngine = new StandardJMeterEngine();
        stdEngine.configure(tree);
        stdEngine.run();
    }

    private HashTree initTest() throws Throwable
    {
        Optional<File> testPlan = DataFileProvider.dataFile(jmxSource);
        Assert.assertTrue(jmxSource + " file could not be found.", testPlan.isPresent());

        // load the jmx file into a HashTree structure
        HashTree tree = SaveService.loadTree(testPlan.get());

        JMeterUtils.initLocale();
        return tree;
    }

    public void validateActions() throws Throwable
    {
        int i = 0;
        System.out.println("Use RequestNaming: " + useRequestMode);
        for(String actionName : getActionNames())
        {
            String expectedActionName;
            if(i < expectedActionNames.length)
            {
                expectedActionName = expectedActionNames[i++];
            }
            else
            {
                expectedActionName = "[empty]";
            }
            Assert.assertEquals("Expected: " + expectedActionName + " Got: " + actionName, expectedActionName, actionName);
            System.out.println(expectedActionName + " vs " + actionName);
        }
    }

    protected List<String> getActionNames()
    {
        return jmeter.getActionNames();
    }
}
