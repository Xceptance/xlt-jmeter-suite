package com.xceptance.loadtest.data.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;

import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.HashTreeTraverser;
import org.apache.jorphan.collections.ListedHashTree;


/**
 * See code from org.apache.jorphan.collections.SearchByClass, only added one method !
 * 
 * @param <T>
 */
public class SearchHelper<T> implements HashTreeTraverser 
{
    private final List<T> objectsOfClass = new ArrayList<>();

    private final IdentityHashMap<Object, ListedHashTree> subTrees = new IdentityHashMap<>();

    private final Class<T> searchClass;

    /**
     * Creates an instance of SearchByClass, and sets the Class to be searched
     * for.
     *
     * @param searchClass
     *            class to be searched for
     */
    public SearchHelper(Class<T> searchClass) 
    {
        this.searchClass = searchClass;
    }

    /**
     * After traversing the HashTree, call this method to get a collection of
     * the nodes that were found.
     *
     * @return Collection All found nodes of the requested type
     */
    public Collection<T> getSearchResults() 
    { 
        return objectsOfClass;
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
        if (searchClass.isAssignableFrom(node.getClass())) 
        {
            objectsOfClass.add((T) node);
            ListedHashTree tree = new ListedHashTree(node);
            tree.set(node, subTree);
            subTrees.put(node, tree);
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