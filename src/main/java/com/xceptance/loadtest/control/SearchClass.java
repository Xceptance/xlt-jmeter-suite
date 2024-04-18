package com.xceptance.loadtest.control;

import java.util.ArrayList;
import java.util.List;

import org.apache.jmeter.extractor.RegexExtractor;

public class SearchClass<T,K>
{
    private T firstClass;
    private K secondClass;
    private List<RegexExtractor> regexEx = new ArrayList<>();
    
    public SearchClass()
    {
        this.firstClass = null;
        this.secondClass = null;
    }
    
    public SearchClass(T searchClass, K secondClass)
    {
        this.firstClass = searchClass;
        this.secondClass = secondClass;
    }
    
    public Class<RegexExtractor> getRegexClass()
    {
        return RegexExtractor.class;
    }
    
    public void setRegexExtractor(RegexExtractor regEx)
    {
        regexEx.add(regEx);
    }
    
    public List<RegexExtractor> getRegexData()
    {
        return regexEx;
    }
    
    public void setFirstClass(T firstClass)
    {
        this.firstClass = firstClass;
    }
    
    public void setSecondClass(K secondClass)
    {
        this.secondClass = secondClass;
    }
    
    public T getFirstClass()
    {
        return firstClass;
    }
    
    public K getSecondClass()
    {
        return secondClass;
    }
}
