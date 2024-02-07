package com.xceptance.loadtest.api.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import com.xceptance.xlt.api.util.XltRandom;

/**
 * A list of values taken from the properties. Auto split and easily accessible.
 *
 * @author Rene Schwietzke
 */
public class ConfigList
{
    /**
     * The raw values read
     */
    public final List<String> list;

    /**
     * A fixed value for immutable access
     */
    public final String value;

    /**
     * The constructor for the default
     */
    private ConfigList(final String value)
    {
        this(value, " ");
    }

    /**
     * The constructor
     */
    private ConfigList(final String value, final String delimiters)
    {
        String delimitersToBeUsed = delimiters;
        if (delimiters == null || delimiters.length() == 0)
        {
            // default of the Annotation default went away
            delimitersToBeUsed = " ";
        }

        // anything to do?
        if (value != null)
        {
            final List<String> newList = new ArrayList<>(10);

            // ok, parse it
            final StringTokenizer tokenizer = new StringTokenizer(value, delimitersToBeUsed);
            while (tokenizer.hasMoreTokens())
            {
                newList.add(tokenizer.nextToken());
            }

            this.list = Collections.unmodifiableList(newList);
            this.value = list.isEmpty() ? null : list.get(XltRandom.nextInt(list.size()));
        }
        else
        {
            this.list = Collections.emptyList();
            this.value = null;
        }
    }

    /**
     * To get a new random value at every access
     *
     * @return true or false depending on the int value initially defined
     */
    public String random()
    {
        return list.isEmpty() ? null : list.get(XltRandom.nextInt(list.size()));
    }

    static class ImmutableConfigList extends ConfigList
    {
        private ImmutableConfigList(final String value)
        {
            super(value);
        }

        private ImmutableConfigList(final String value, final String delimiter)
        {
            super(value, delimiter);
        }

        /* (non-Javadoc)
         * @see com.xceptance.xlt.loadtest.util.configuration.ConfigList#random()
         */
        @Override
        public String random()
        {
            throw new UnsupportedOperationException();
        }
    }

    public static ConfigList build(final String value, final String delimiter)
    {
        return new ConfigList(value, delimiter);
    }

    public static ConfigList buildImmutable(final String value, final String delimiter)
    {
        return new ImmutableConfigList(value, delimiter);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        final int maxLen = 10;
        return "ConfigList [raw=" + (list != null ? list.subList(0, Math.min(list.size(), maxLen)) : null) + ", value=" + value + "]";
    }
}
