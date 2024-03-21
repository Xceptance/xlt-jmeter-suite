package com.xceptance.loadtest.jmeter.tests;

import java.io.File;
import java.util.Optional;

import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.junit.Assert;

import com.xceptance.loadtest.api.data.CustomJMeterEngine;
import com.xceptance.loadtest.api.data.DataFileProvider;
import com.xceptance.loadtest.api.data.NonSiteRelatedTest;
import com.xceptance.loadtest.api.tests.JMeterTestCase;


public class TJmeterNative extends JMeterTestCase implements NonSiteRelatedTest
{
    private HashTree tree;
    private String fileName;
    
    public TJmeterNative()
    {
        try
        {
            super.init();
//            fileName = "CustomerAuthorization.jmx";
//            fileName = "GuestOrder.jmx";
            fileName = "CustomerAuthorizationExtended.jmx";
            
            Optional<File> testPlan = DataFileProvider.dataFile(fileName);
            Assert.assertTrue("The "+ fileName +" file could not be found.", testPlan.isPresent());
            
            // load the jmx file into a HashTree structure
            tree = SaveService.loadTree(testPlan.get());
        } 
        catch (Throwable e)
        {
            e.printStackTrace();
        }
        
        // remove file ending for naming 
        setTestName(getTestName() + "_" + fileName.replace(".jmx", ""));
    }
    
    /**
     * 
     *
     * @throws Throwable
     */
    @Override
    public void test() throws Throwable
    {
        JMeterUtils.initLocale();
        CustomJMeterEngine jmeter = new CustomJMeterEngine();
//        StandardJMeterEngine jmeter = new StandardJMeterEngine();
        
        jmeter.configure(tree);
        jmeter.run();
    }
}