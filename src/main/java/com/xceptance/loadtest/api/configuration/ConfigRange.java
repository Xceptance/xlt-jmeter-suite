package com.xceptance.loadtest.api.configuration;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.xceptance.xlt.api.util.XltRandom;

/**
 * A range of integers.
 *
 * @author Rene Schwietzke
 */
public class ConfigRange
{
    /**
     * The raw value read for min
     */
    public final int min;

    /**
     * The raw value read max
     */
    public final int max;

    /**
     * The full range object for neither playing
     */
    public final Range range;

    /**
     * A fixed value for immutable access
     */
    public final int value;

    /**
     * The constructor
     */
    private ConfigRange(final String key, final String rangeString)
    {
        if (rangeString != null)
        {
            range = Range.parse(rangeString);

            if (range == null)
            {
                throw new NumberFormatException(MessageFormat.format("Cannot parse the given range: ''{0}' = '{1}''", key, rangeString));
            }

            this.min = range.min;
            this.max = range.max;

            this.value = XltRandom.nextInt(this.min, this.max);
        }
        else
        {
            this.min = this.max = this.value = 0;
            range = new Range(0, 0);
        }
    }

    /**
     * To get a new random value at every access
     *
     * @return a new value of the range min and max
     */
    public int random()
    {
        return XltRandom.nextInt(min, max);
    }

    public static ConfigRange build(final String key, final String range)
    {
        return new ConfigRange(key, range);
    }

    public static ConfigRange buildImmutable(final String key, final String range)
    {
        return new ImmutableConfigRange(key, range);
    }

    static class ImmutableConfigRange extends ConfigRange
    {
        private ImmutableConfigRange(final String key, final String range)
        {
            super(key, range);
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * com.xceptance.xlt.loadtest.util.configuration.ConfigList#random()
         */
        @Override
        public int random()
        {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Just a simplified way to get two return values
     *
     * @author rschwietzke
     */
    public static class Range
    {
        /**
         * The range parsing patterns
         */
        private static final Pattern TORANGEPATTERN = Pattern.compile("^-\\s*(-?[0-9]+)$");
        private static final Pattern FROMRANGEPATTERN = Pattern.compile("^(-?[0-9]+)\\s*-$");
        private static final Pattern FULLRANGEPATTERN = Pattern.compile("^(-?[0-9]+)\\s*-\\s*(-?[0-9]+)$");

        public final int min;
        public final int max;

        public Range(final int min, final int max)
        {
            // make sure min is really min
            if (min > max)
            {
                // switch them
                this.min = max;
                this.max = min;
            }
            else
            {
                // all fine
                this.min = min;
                this.max = max;
            }
        }

        /**
         * Do the ranges overlap?
         *
         * @param value
         *            the other range to check against
         * @return true if the value range is full or partially within this
         *         range
         */
        public boolean overlap(final Range value)
        {
            if (this.inside(value.min) || value.inside(this.min))
            {
                return true;
            }
            if (this.inside(value.max) || value.inside(this.max))
            {
                return true;
            }

            return false;
        }

        /**
         * Is the value in the range, including boundaries
         *
         * @param value
         *            value to check
         * @return true if the value is in the range including boundaries
         */
        public boolean inside(final int value)
        {
            if (min <= value && value <= max)
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        /**
         * Is the value in the range, including boundaries
         *
         * @param value
         *            value to check
         * @return true if the value is in the range including boundaries
         */
        public boolean inside(final double value)
        {
            if (min <= value && value <= max)
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        /**
         * Is the value in the range, excluding boundaries
         *
         * @param value
         *            value to check
         * @return true if the value is in the range excluding boundaries
         */
        public boolean insideExclusive(final int value)
        {
            if (min < value && value < max)
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        /**
         * Is the value in the range, excluding boundaries
         *
         * @param value
         *            value to check
         * @return true if the value is in the range excluding boundaries
         */
        public boolean insideExclusive(final double value)
        {
            if (min < value && value < max)
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        /**
         * Turns things like 1-2 into 1 and 2.
         *
         * @param range
         *            the range definition to parse
         * @return a new range or null in case of problems
         */
        public static Range parse(final String range)
        {
            if (range == null)
            {
                return null;
            }

            // a-
            {
                final Matcher matcher = FROMRANGEPATTERN.matcher(range.trim());
                if (matcher.matches())
                {
                    if (matcher.groupCount() == 1)
                    {
                        final int min = Integer.valueOf(matcher.group(1));
                        final int max = Integer.MAX_VALUE;

                        return new Range(min, max);
                    }
                }
            }

            // -b
            {
                final Matcher matcher = TORANGEPATTERN.matcher(range.trim());
                if (matcher.matches())
                {
                    if (matcher.groupCount() == 1)
                    {
                        final int min = Integer.MIN_VALUE;
                        final int max = Integer.valueOf(matcher.group(1));

                        return new Range(min, max);
                    }
                }
            }

            // a-b
            {
                final Matcher matcher = FULLRANGEPATTERN.matcher(range.trim());
                if (matcher.matches())
                {
                    if (matcher.groupCount() == 2)
                    {
                        final int min = Integer.valueOf(matcher.group(1));
                        final int max = Integer.valueOf(matcher.group(2));

                        return new Range(min, max);
                    }
                }
            }

            // well, fail to parse all
            return null;
        }
    }
}
