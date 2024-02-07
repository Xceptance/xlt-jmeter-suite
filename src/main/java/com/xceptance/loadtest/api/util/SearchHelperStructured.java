package com.xceptance.loadtest.api.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.HashTreeTraverser;
import org.apache.jorphan.collections.ListedHashTree;

import com.xceptance.loadtest.api.data.SearchClass;

/**
 * 
 * 
 * @param <T>
 */
public class SearchHelperStructured<R, T, K> implements HashTreeTraverser 
{
    private final List<T> objectsOfClass = new ArrayList<>();
    private final List<K> objectsOfClass2 = new ArrayList<>();
    
    // generic list of the 2 searched classes
    private final List<SearchClass<T, K>> result = new ArrayList<>();
    // structured list elements, in accordance to appearance in the XML file
    private final LinkedHashMap<R, List<SearchClass<T, K>>> structuredList = new LinkedHashMap<>();
    
    private final IdentityHashMap<Object, ListedHashTree> subTrees = new IdentityHashMap<>();

    private final Class<T> firstClass;
    private final Class<K> secondClass;
    private final Class<R> rootClass;
//    private final RegexExtractor regexClass;
    
    private int index = 0;
    
    private boolean previousClassFound = false;
    private R currentRootNode;

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

    /**
     * After traversing the HashTree, call this method to get a collection of
     * the nodes that were found.
     *
     * @return Collection All found nodes of the requested type
     */
    public Collection<SearchClass<T, K>> getSearchResults() 
    { 
        return result;
    }
    
    public LinkedHashMap<R, List<SearchClass<T, K>>> getStructuredResult() 
    { 
        return structuredList;
    }

    /**
     * Given a specific found node, this method will return the sub tree of that
     * node.
     *
     * @param root
     *            the node for which the sub tree is requested
     * @return HashTree
     */
    public HashTree getSubTree(Object root) 
    {
        return subTrees.get(root);
    }
    
    public IdentityHashMap<Object, ListedHashTree> getAllSubtress()
    {
        return subTrees;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public void addNode(Object node, HashTree subTree) 
    {
        // if none of the searched nodes are found and we have encountered a root node, we write the found data
        if (currentRootNode != null &&
            !firstClass.isAssignableFrom(node.getClass()) &&
            !secondClass.isAssignableFrom(node.getClass()) &&
            !result.isEmpty())
        {
            // copy of the results for storing it into the map
            List<SearchClass<T, K>> currentState = new ArrayList<SearchClass<T, K>>(result);
            structuredList.put((R)currentRootNode, currentState);
            result.clear();
        }
        
        if (rootClass.isAssignableFrom(node.getClass()))
        {
            currentRootNode = (R)node;
        }
        
        // n to n correlation, in assumption every first class has a child class which is searched for
        if (firstClass.isAssignableFrom(node.getClass())) 
        {
            objectsOfClass.add((T) node);
            // TODO check if needed
            ListedHashTree tree = new ListedHashTree(node);
            tree.set(node, subTree);
            subTrees.put(node, tree);
            previousClassFound = true;
        }
        else if (secondClass.isAssignableFrom(node.getClass()))
        {
            objectsOfClass2.add((K) node);
            // TODO check if needed
            ListedHashTree tree = new ListedHashTree(node);
            tree.set(node, subTree);
            subTrees.put(node, tree);
            
            if (previousClassFound)
            {
                result.add(new SearchClass<T,K>((T)objectsOfClass.get(index), (K)objectsOfClass2.get(index)));
                previousClassFound = false;
                index++;
            }
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