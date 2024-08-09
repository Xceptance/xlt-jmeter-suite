package com.xceptance.loadtest.jmeter.tests;

import com.xceptance.loadtest.test.AbstractComponentTest;

public class XLTJMeterBrowseFlowTakenFromCsv extends AbstractComponentTest
{
    public XLTJMeterBrowseFlowTakenFromCsv
    {
        useRequestMode = false;
        jmxSource = "/tests/browseFlowFromCsv";
    }
}
