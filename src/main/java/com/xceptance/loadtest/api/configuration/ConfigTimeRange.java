package com.xceptance.loadtest.api.configuration;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.xceptance.common.util.ParseUtils;
import com.xceptance.xlt.api.util.XltRandom;

/**
 * A range of times. See {@link ParseUtils.TimeFormat} for supported time
 * units.<br>
 * Example: 5m - 10m<br>
 * Example: 5m -<br>
 * Example: - 10m<br>
 * Example: 5m<br>
 *
 * @author Matthias Ullrich
 */
public class ConfigTimeRange
{
    private static final Pattern RANGE_PATTERN = Pattern.compile("^([^-]*)-([^-]*)$");

    /**
     * The raw value read for min in seconds
     */
    public final int min;

    /**
     * The raw value read max in seconds
     */
    public final int max;

    /**
     * A fixed value for immutable access in seconds
     */
    public final int value;

    /**
     * The constructor
     */
    private ConfigTimeRange(final String key, final String valueString)
    {
        if (valueString != null)
        {
            String minString = null;
            String maxString = null;

            // split the input at the dash
            final Matcher m = RANGE_PATTERN.matcher(valueString);
            if (m.matches())
            {
                minString = m.group(1);
                maxString = m.group(2);
            }
            else
            {
                // no dash, okay, then it's a single time
                minString = maxString = valueString;
            }

            if (StringUtils.isBlank(minString) && StringUtils.isBlank(maxString))
            {
                throw new IllegalArgumentException(MessageFormat.format("Value is no valid time range pattern: ''{0}' = '{1}''", key, valueString));
            }

            // MIN / MAX
            min = parse(key, minString, 0);
            max = parse(key, maxString, upperLimit(min));

            if (min > max)
            {
                throw new IllegalArgumentException(MessageFormat.format("Time range MAX must be greater than or equals to MIN: ''{0}' = '{1}''", key, valueString));
            }

            // VALUE
            value = XltRandom.nextInt(min, max);
        }
        else
        {
            min = max = value = 0;
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

    public static ConfigTimeRange build(final String key, final String valueString)
    {
        return new ConfigTimeRange(key, valueString);
    }

    public static ConfigTimeRange buildImmutable(final String key, final String valueString)
    {
        return new ImmutableConfigTimeRange(key, valueString);
    }

    private static int parse(final String key, final String raw, final int defaultInt)
    {
        return StringUtils.isNotBlank(raw) ? parse(key, raw) : defaultInt;
    }

    private static int parse(final String key, final String raw)
    {
        try
        {
            return ParseUtils.parseTimePeriod(raw);
        }
        catch (final ParseException e)
        {
            throw new NumberFormatException(MessageFormat.format("{0} ({1})", e.getMessage(), key));
        }
    }

    static class ImmutableConfigTimeRange extends ConfigTimeRange
    {
        private ImmutableConfigTimeRange(final String key, final String valueString)
        {
            super(key, valueString);
        }

        @Override
        public int random()
        {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * We need an upper time limit below Integer.MAX_VALUE, because ranges from
     * 0 to Integer.MAX_VALUE would lead to problems with Randomizer.
     */
    private static int upperLimit(final int min)
    {
        return (min == 0) ? Integer.MAX_VALUE - 1 : Integer.MAX_VALUE;
    }
}
