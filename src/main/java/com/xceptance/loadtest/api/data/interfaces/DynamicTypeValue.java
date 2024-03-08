package com.xceptance.loadtest.api.data.interfaces;

import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;

public enum DynamicTypeValue
{
    TransactionController(TransactionController.class),
    SampleProxy(HTTPSamplerProxy.class),
    HeaderManager(HeaderManager.class),
    RegEx(RegexExtractor.class);
    
    private final Class<?> clazz;
    
    private DynamicTypeValue(Class<?> clazz)
    {
        this.clazz = clazz;
    }
    
    public static DynamicTypeValue get(Class<?> clazz)
    {
        for (DynamicTypeValue dtv : values())
        {
            if (dtv.getClazz().isAssignableFrom(clazz))
            {
                return dtv;
            }
        }
        return null;
    }
    
    public Class<?> getClazz()
    {
        return clazz;
    }
}
