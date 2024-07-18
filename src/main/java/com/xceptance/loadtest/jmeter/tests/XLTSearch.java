package com.xceptance.loadtest.jmeter.tests;

import com.xceptance.loadtest.test.AbstractComponentTest;

public class XLTSearch extends AbstractComponentTest
{
  public XLTSearch()
  {
    useRequestMode = false;
    jmxSource = "/tests/posters.jmx";
  }
}
