package com.xceptance.loadtest.api.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import com.xceptance.loadtest.api.configuration.interfaces.ById;

/**
 * A list of objects that are also build up via properties
 *
 * @author Rene Schwietzke
 */
public class EnumConfigList<T>
{
    /**
     * The raw weighted value read
     */
    public final WeightedList<T> weightedList;

    /**
     * Optionally we can also lookup by id, if this was set during construction
     */
    public final Optional<Map<String, T>> map;

    /**
     * A fixed value for immutable access
     */
    public final T value;

    /**
     * The constructor for the default
     */
    private EnumConfigList(final List<Pair<T, Integer>> list, final boolean byId)
    {
        super();

        this.weightedList = new WeightedList<>();
        weightedList.addAll(list);

        if (list.isEmpty())
        {
            this.value = null;
        }
        else
        {
            this.value = weightedList.getRandom();
        }

        // shall we enable by id?
        if (byId)
        {
            final Map<String, T> map = new HashMap<>(2 * list.size() + 1);

            for (final Pair<T, Integer> pair : list)
            {
                final T value = pair.getKey();
                if (value instanceof ById)
                {
                    // ok, we implement the interface, we are good
                    map.put(((ById) value).getId(), value);
                }
                else
                {
                    // complain
                    throw new UnsupportedOperationException("byId is only supported when the interface ById is implemented");
                }
            }

            // make it available
            this.map = Optional.of(map);
        }
        else
        {
            map = Optional.empty();
        }
    }

    /**
     * Get from the underlying list
     *
     * @param index
     *            the index to query
     * @return the value at the position
     */
    public T get(final int index)
    {
        return weightedList.get(index);
    }

    /**
     * Get by Id from the map, but this works only
     * if this was correctly announced during construction
     * and T implements ById interface. If this list has not
     * been enabled to support byId, it will complain with an UnsupportedOperationException.
     *
     * @param id the id to look for
     * @return an optional with T if found, otherwise an empty option
     * @throws UnsupportedOperationException
     */
    public Optional<T> getById(final String id)
    {
        if (map.isPresent())
        {
            final T value = map.get().get(id);
            return value == null ? Optional.empty() : Optional.of(value);
        }
        else
        {
            throw new UnsupportedOperationException("Not setup to support id lookup.");
        }
    }

    /**
     * Get all elements
     *
     * @return all contained elements
     */
    public List<T> weightedList()
    {
        return weightedList.weightedList();
    }

    /**
     * Return all elements
     *
     * @return a list with all elements
     */
    public List<T> unweightedList()
    {
        return weightedList.unweightedList();
    }

    /**
     * The size of the underlying list
     *
     * @return the size of the list
     */
    public int size()
    {
        return weightedList.size();
    }

    /**
     * Returns <tt>true</tt> if this list contains no elements.
     *
     * @return <tt>true</tt> if this list contains no elements
     */
    public boolean isEmpty()
    {
        return weightedList.size() <= 0;
    }

    /**
     * To get a new random value at every access
     *
     * @return true or false depending on the int value initially defined
     */
    public T random()
    {
        return weightedList.getRandom();
    }

    static class ImmutableConfigList<T> extends EnumConfigList<T>
    {
        private ImmutableConfigList(final List<Pair<T, Integer>> list, final boolean byId)
        {
            super(list, byId);
        }

        @Override
        public T random()
        {
            throw new UnsupportedOperationException();
        }
    }

    public static <T> EnumConfigList<T> build(final List<Pair<T, Integer>> list, final boolean byId)
    {
        return new EnumConfigList<>(list, byId);
    }

    public static <T> EnumConfigList<T> buildImmutable(final List<Pair<T, Integer>> list, final boolean byId)
    {
        return new ImmutableConfigList<>(list, byId);
    }

    @Override
    public String toString()
    {
        final int maxLen = 10;
        return "EnumConfigList [byId=" + map.isPresent() + " raw=" + (weightedList != null ? weightedList.subList(0, Math.min(weightedList.size(), maxLen)) : null) + ", size="
                        + weightedList.size() + "]";
    }
}
