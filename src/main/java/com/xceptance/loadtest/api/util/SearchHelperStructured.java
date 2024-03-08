package com.xceptance.loadtest.api.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.HashTreeTraverser;

import com.xceptance.loadtest.api.data.SearchClass;

/**
 * 
 * 
 * @param <T>
 */
public class SearchHelperStructured<R, T, K> implements HashTreeTraverser 
{
    // generic list of the 2 searched classes
    private List<SearchClass<T, K>> result;
    // structured list elements, in accordance to appearance in the XML file
    private final LinkedHashMap<R, List<SearchClass<T, K>>> structuredList = new LinkedHashMap<>();
    
    private final Class<T> firstClass;
    private final Class<K> secondClass;
    private final Class<R> rootClass;
    
    private R currentRootNode;
    private SearchClass<T, K> searchClass;

    /**
     * Creates an instance of SearchByClass, and sets the Class to be searched
     * for.
     *
     * @param searchClass
     *            class to be searched for
     */
    public SearchHelperStructured(Class<T> firstSearchClass, Class<K> secondSearchClass) 
    {
        this.firstClass = firstSearchClass;
        this.secondClass = secondSearchClass;
        this.rootClass = null;
    }
    
    public SearchHelperStructured(Class<R> rootClass,Class<T> firstSearchClass, Class<K> secondSearchClass) 
    {
        this.firstClass = firstSearchClass;
        this.secondClass = secondSearchClass;
        this.rootClass = rootClass;
    }

    public LinkedHashMap<R, List<SearchClass<T, K>>> getStructuredResult() 
    { 
        return structuredList;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public void addNode(Object node, HashTree subTree) 
    {
        // create the root node and allocate memory for results
        if (rootClass.isAssignableFrom(node.getClass()))
        {
            currentRootNode = (R) node;
            result = new ArrayList<>();
            structuredList.put((R) currentRootNode, result);
        }
        
        // n to n correlation, in assumption every first class has a child class which is searched for
        if (firstClass.isAssignableFrom(node.getClass())) 
        {
            searchClass = new SearchClass<T,K>();
            searchClass.setFirstClass((T) node);
            result.add(searchClass);
        }
        else if (secondClass.isAssignableFrom(node.getClass()))
        {
            searchClass.setSecondClass((K) node);
        }
        // if we have a RegexExtractor in place
        else if (searchClass != null && 
                 searchClass.getRegexClass().isAssignableFrom(node.getClass()))
        {
            searchClass.setRegexExtractor((searchClass.getRegexClass().cast(node)));
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