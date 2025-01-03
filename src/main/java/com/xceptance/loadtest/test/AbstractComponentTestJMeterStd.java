package com.xceptance.loadtest.test;

import com.xceptance.loadtest.api.data.DataFileProvider;
import com.xceptance.loadtest.control.JMeterTestCase;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.junit.Assert;

import java.io.File;
import java.util.Optional;

public abstract class AbstractComponentTestJMeterStd extends JMeterTestCase
{
    protected String jmxSource;

    public AbstractComponentTestJMeterStd()
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
        Optional<File> testPlan = DataFileProvider.dataFile(jmxSource);
        Assert.assertTrue(jmxSource + " file could not be found.", testPlan.isPresent());

        // load the jmx file into a HashTree structure
        HashTree tree = SaveService.loadTree(testPlan.get());

        JMeterUtils.initLocale();
        StandardJMeterEngine jmeter = new StandardJMeterEngine();
        jmeter.configure(tree);
        jmeter.run();
    }
}
