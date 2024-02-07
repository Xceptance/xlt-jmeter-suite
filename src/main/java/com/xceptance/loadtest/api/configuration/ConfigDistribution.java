package com.xceptance.loadtest.api.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import com.xceptance.xlt.api.util.XltRandom;

/**
 * A value that is based on a distribution or 0 if nothing is set
 *
 * @author Rene Schwietzke
 */
public class ConfigDistribution
{
    /**
     * Default empty is always 0
     */
    private final static int[] DEFAULT = { 0 };

    /**
     * The raw value read
     */
    public final int[] raw;

    /**
     * A fixed value for immutable access
     */
    public final int value;

    /**
     * The constructor for the default
     */
    private ConfigDistribution(final String value)
    {
        this(value, " ");
    }

    /**
     * The constructor
     */
    private ConfigDistribution(final String value, final String delimiters)
    {
        String delimitersToBeUsed = delimiters;
        if (delimiters == null || delimiters.length() == 0)
        {
            // default if the Annotation default went away
            delimitersToBeUsed = " ";
        }

        // anything to do?
        this.raw = parseDistributionDefinition(value, delimitersToBeUsed);
        this.value = raw[XltRandom.nextInt(raw.length)];
    }

    /**
     * To get a new random value at every access
     *
     * @return an int value or an {@link IllegalArgumentException} when the data
     *         is not initalized
     */
    public int random()
    {
        return raw[XltRandom.nextInt(raw.length)];
    }

    static class ImmutableConfigDistribution extends ConfigDistribution
    {
        private ImmutableConfigDistribution(final String value)
        {
            super(value);
        }

        private ImmutableConfigDistribution(final String value, final String delimiter)
        {
            super(value, delimiter);
        }

        /* (non-Javadoc)
         * @see com.xceptance.xlt.loadtest.util.configuration.ConfigList#random()
         */
        @Override
        public int random()
        {
            throw new UnsupportedOperationException();
        }
    }

    public static ConfigDistribution build(final String value, final String delimiters)
    {
        return new ConfigDistribution(value, delimiters);
    }

    public static ConfigDistribution buildImmutable(final String value, final String delimiters)
    {
        return new ImmutableConfigDistribution(value, delimiters);
    }

    /**
     * Sets up the distribution
     *
     * @param data
     *            the parsable definition
     * @param delimiters
     *            possible delimiters to use
     * @return the distribution array
     */
    public static int[] parseDistributionDefinition(final String data, final String delimiters)
    {
        if (data == null)
        {
            // distribution is not desired at all
            return DEFAULT;
        }

        // Internal helper class
        class Pair
        {
            public Pair(final int value, final int amount)
            {
                super();
                this.value = value;
                this.amount = amount;
            }

            int value;

            int amount;
        }

        // Initialize buckets
        final List<Pair> buckets = new ArrayList<>();

        // Format is 1/12 2/34 40/2
        // So token is a whitespace.
        final StringTokenizer st = new StringTokenizer(data, delimiters);
        while (st.hasMoreTokens())
        {
            final String token = st.nextToken();
            final String[] splitString = token.split("/");
            if (splitString.length == 1)
            {
                // Just a 2 3 4 or something like that, it means that number
                // into the next bucket
                buckets.add(new Pair(buckets.size() + 1, Integer.valueOf(splitString[0])));
            }
            else if (splitString.length > 1)
            {
                final int value = Integer.valueOf(splitString[0].trim());
                final int amount = Integer.valueOf(splitString[1].trim());
                buckets.add(new Pair(value, amount));
            }
        }

        if (buckets.isEmpty())
        {
            // Still no list.
            return DEFAULT;
        }

        // Build the lookup array and count first to have its future size handy
        int total = 0;
        for (final Pair pair : buckets)
        {
            total = total + pair.amount;
        }

        if (total == 0)
        {
            return DEFAULT;
        }

        // Create the array of integers
        final int[] distributionList = new int[total];

        int pos = 0;
        for (final Pair pair : buckets)
        {
            for (int i = 0; i < pair.amount; i++)
            {
                distributionList[pos] = pair.value;
                pos++;
            }
        }

        return distributionList;
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
        return "ConfigDistribution [raw=" + (raw != null ? Arrays.toString(Arrays.copyOf(raw, Math.min(raw.length, maxLen))) : null) + ", value=" + value + "]";
    }

}
