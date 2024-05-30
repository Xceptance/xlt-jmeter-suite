package com.xceptance.loadtest.test;

import org.apache.jmeter.engine.StandardJMeterEngine;

public class DownloadAllEmbedded extends AbstractComponentTest
{
    private StandardJMeterEngine jmeter;

    public DownloadAllEmbedded()
    {
        jmxSource = "/tests/DownloadAllEmbedded.jmx";

        useRequestMode = true;
    }

    @Override
    protected void test() throws Throwable
    {
        super.test();
        /*
        Optional<File> testPlan = DataFileProvider.dataFile(jmxSource);
        Assert.assertTrue(jmxSource + " file could not be found.", testPlan.isPresent());

        // load the jmx file into a HashTree structure
        HashTree tree = SaveService.loadTree(testPlan.get());

        JMeterUtils.initLocale();
        jmeter = new StandardJMeterEngine();
        jmeter.configure(tree);
        jmeter.run();
        */
    }
}
