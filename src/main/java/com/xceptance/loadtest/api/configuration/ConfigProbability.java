package com.xceptance.loadtest.api.configuration;

import java.util.Optional;
import java.util.function.Supplier;

import com.xceptance.xlt.api.util.XltRandom;

/**
 * Probably derived from a 0 to 100 int value.
 *
 * @see com.xceptance.xlt.api.util.XltRandom#nextBoolean(int)
 * @author Rene Schwietzke
 */
public class ConfigProbability
{
    /**
     * The raw value read
     */
    public final int raw;

    /**
     * The fixed value for immutable access
     */
    public final boolean value;

    /**
     * The constructor
     *
     * @param probability
     *            a value between 0 and 100 hopefully
     */
    private ConfigProbability(final String probability)
    {
        if (probability != null)
        {
            this.raw = Integer.parseInt(probability);
        }
        else
        {
            this.raw = 0;
        }
        this.value = XltRandom.nextBoolean(raw);
    }

    /**
     * To get a new random value at every access
     *
     * @return true or false depending on the int value initially defined
     */
    public boolean random()
    {
        return XltRandom.nextBoolean(raw);
    }

    /**
     * Execute the provided runnable
     *
     * @param runnable
     *            code to be executed when the value is true
     */
    public void random(final Runnable runnable)
    {
        if (XltRandom.nextBoolean(raw))
        {
            runnable.run();
        }
    }

    /**
     * Execute the provided supplier and return something
     *
     * @param supplier
     *            code to be executed when the value is true
     * @return returns the data from the supplier
     */
    public <T> Optional<T> random(final Supplier<T> supplier)
    {
        if (XltRandom.nextBoolean(raw))
        {
            return Optional.of(supplier.get());
        }

        return Optional.empty();
    }

    public static ConfigProbability build(final String probability)
    {
        return new ConfigProbability(probability);
    }

    public static ConfigProbability buildImmutable(final String probability)
    {
        return new ImmutableConfigProbability(probability);
    }

    static class ImmutableConfigProbability extends ConfigProbability
    {
        private ImmutableConfigProbability(final String probability)
        {
            super(probability);
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * com.xceptance.xlt.loadtest.util.configuration.ConfigList#random()
         */
        @Override
        public boolean random()
        {
            throw new UnsupportedOperationException();
        }
    }
}
