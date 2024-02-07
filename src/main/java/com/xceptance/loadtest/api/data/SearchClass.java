package com.xceptance.loadtest.api.data;

public class SearchClass<T,K>
{
    public final T firstClass;
    public final K secondClass;
    
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
}
