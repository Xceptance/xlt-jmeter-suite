package com.xceptance.loadtest.data.util;

import java.util.LinkedHashMap;

import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.HashTreeTraverser;

import com.xceptance.loadtest.data.data.interfaces.DynamicTypeValue;

/**
 * 
 * 
 * @param <T>
 */
public class SearchHelperDynamic implements HashTreeTraverser 
{
    // structured list elements, in accordance to appearance in the XML file
    private final LinkedHashMap<String, Object> structuredList = new LinkedHashMap<>();

    /**
     * Creates an instance of SearchByClass, and sets the Class to be searched
     * for.
     *
     * @param searchClass
     *            class to be searched for
     */
    public SearchHelperDynamic() 
    {
    }
    
    public LinkedHashMap<String, Object> getResults()
    {
        return structuredList;
    }

    /** {@inheritDoc} */
    @Override
    public void addNode(Object node, HashTree subTree) 
    {
        if (DynamicTypeValue.get(node.getClass()) != null)
        {
            structuredList.put(node.toString(), node);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void subtractNode() 
    {
    }

    /** {@inheritDoc} */
    @Override
    public void processPath() 
    {
    }
}