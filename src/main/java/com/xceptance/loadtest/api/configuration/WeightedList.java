package com.xceptance.loadtest.api.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.util.Args;

import com.xceptance.xlt.api.util.XltRandom;

/**
 * A weighted list to use a bucket like configuration
 *
 * @author Rene Schwietzke
 *
 * @param <T>
 */
public class WeightedList<T>
{
    /**
     * T = element<br>
     * Integer = weight
     */
    private final List<Pair<T, Integer>> weightedElements;
    private final List<T> unweightedElements;
    private int overallWeight = 0;

    public WeightedList()
    {
        this.weightedElements = new ArrayList<>();
        this.unweightedElements = new ArrayList<>();
    }

    public WeightedList(final Collection<Pair<T, Integer>> weightedElements)
    {
        this.weightedElements = new ArrayList<>(weightedElements.size());
        this.unweightedElements = new ArrayList<>(weightedElements.size());

        addAll(weightedElements);
    }

    /**
     * Add an element and the corresponding weight.
     *
     * @param element
     * @param weight
     * @return the updated {@link WeightedList} object
     */
    public WeightedList<T> add(final T element, final int weight)
    {
        Args.notNull(element, "The element");
        Args.notNegative(weight, "The weight");

        weightedElements.add(Pair.of(element, weight));
        overallWeight += weight;

        return this;
    }

    /**
     *
     * @param weightedElement
     *            must not be <code>null</code>
     * @return the updated {@link WeightedList} object
     */
    public WeightedList<T> add(final Pair<T, Integer> weightedElement)
    {
        Args.notNull(weightedElement, "The weighted element");

        weightedElements.add(weightedElement);
        overallWeight += weightedElement.getRight();

        return this;
    }

    /**
     *
     * @param weightedElements
     *            must not be <code>null</code>
     * @return the updated {@link WeightedList} object
     */
    public WeightedList<T> addAll(final Collection<Pair<T, Integer>> weightedElements)
    {
        Args.notNull(weightedElements, "The weighted elements collection");

        weightedElements.forEach(
                        pair ->
                        {
                            add(pair);
                            unweightedElements.add(pair.getKey());
                        });

        return this;
    }

    /**
     * Get the element on the given index position. This method provided common
     * list access to the elements and is not influenced by the weight feature.
     *
     * @param index
     * @return
     * @see {@link List#get(int)}
     */
    public T get(final int index)
    {
        return weightedElements.get(index).getLeft();
    }

    /**
     * Get all elements
     *
     * @return all elements
     */
    public List<T> weightedList()
    {
        return weightedElements.stream().map(
                        pair -> pair.getLeft())
                        .collect(Collectors.toList());
    }

    /**
     * Get all elements
     *
     * @return all elements
     */
    public List<T> unweightedList()
    {
        return unweightedElements;
    }

    /**
     * Get a random element, according to their weight.
     *
     * @return random element
     */
    public T getRandom()
    {
        int i = XltRandom.nextInt(overallWeight);

        for (final Pair<T, Integer> e : weightedElements)
        {
            final int currentWeight = e.getRight();
            if (i < currentWeight)
            {
                return e.getLeft();
            }
            else
            {
                i -= currentWeight;
            }
        }

        return null;
    }

    /**
     * Get a random element, according to their weight. If no element is
     * available, return the given default.
     *
     * @return random element or default
     */
    public T getRandomOrDefault(final T defaultElement)
    {
        final T t = getRandom();

        if (t == null)
        {
            return defaultElement;
        }

        return t;
    }

    /**
     *
     * @return number of elements in this list
     */
    public int size()
    {
        return weightedElements.size();
    }

    /**
     * Get a subset of elements according to the specified indexes.
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public WeightedList<T> subList(final int fromIndex, final int toIndex)
    {
        return new WeightedList<>(weightedElements.subList(fromIndex, toIndex));
    }

    @Override
    public String toString()
    {
        return weightedElements.toString();
    }
}
