package com.xceptance.loadtest.api.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;

import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.HashTreeTraverser;
import org.apache.jorphan.collections.ListedHashTree;

import com.xceptance.loadtest.api.data.SearchClass;


/**
 * See code from org.apache.jorphan.collections.SearchByClass, only added one method !
 * 
 * @param <T>
 */
public class SearchHelperOrdered<T, K> implements HashTreeTraverser 
{
    private final List<T> objectsOfClass = new ArrayList<>();
    private final List<K> objectsOfClass2 = new ArrayList<>();
    
    private final List<SearchClass<T, K>> result = new ArrayList<>();
    
    private final IdentityHashMap<Object, ListedHashTree> subTrees = new IdentityHashMap<>();

    private final Class<T> firstClass;
    private final Class<K> secondClass;
    
    private int index = 0;
    
    private boolean previousClassFound = false;

    /**
     * Creates an instance of SearchByClass, and sets the Class to be searched
     * for.
     *
     * @param searchClass
     *            class to be searched for
     */
    public SearchHelperOrdered(Class<T> firstSearchClass, Class<K> secondSearchClass) 
    {
        this.firstClass = firstSearchClass;
        this.secondClass = secondSearchClass;
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
        // n to n correlation, in assumption every first class has a child class which is searched for
        if (firstClass.isAssignableFrom(node.getClass())) 
        {
            objectsOfClass.add((T) node);
            ListedHashTree tree = new ListedHashTree(node);
            tree.set(node, subTree);
            subTrees.put(node, tree);
            previousClassFound = true;
        }
        else if (secondClass.isAssignableFrom(node.getClass()))
        {
            objectsOfClass2.add((K) node);
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