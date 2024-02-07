package com.xceptance.loadtest.api.data;

import java.util.ArrayList;

import org.apache.jmeter.control.TransactionController;

public class SearchTransaction<T,K>
{
    public ArrayList<SearchClass<T, K>> data; 
    public TransactionController tc;
    
    public SearchTransaction(TransactionController i,ArrayList<SearchClass<T, K>> input)
    {
        this.data = input;
        this.tc = i;
    }
}
