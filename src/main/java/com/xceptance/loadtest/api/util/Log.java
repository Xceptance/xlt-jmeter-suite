package com.xceptance.loadtest.api.util;

import java.text.MessageFormat;

import com.xceptance.xlt.api.util.XltLogger;

/**
 * Helpers around logging to simplify logging if load test is enabled
 *
 * @author rschwietzke
 */
public class Log
{
    /**
     * Permits lazy evaluation of the msg and if load test is not on, logs it. This is using
     * MessageFormat.format under the hood. Don't build a pattern on the fly and pass it in, such
     * info("Foo" + i + "bar {0}", 1), because this renders the lazy evaluation partially useless.
     * Well still a little better than completely putting a string together...
     *
     * @param pattern
     *            a MessageFormat pattern
     * @param arguments
     *            a list of arguments for the pattern
     */
    public static void debugWhenDev(final String pattern, final Object... arguments)
    {
        if (Context.isLoadTest == false)
        {
            XltLogger.runTimeLogger.debug(MessageFormat.format(pattern, arguments));
        }
    }

    /**
     * Permits lazy evaluation of the msg and if load test is not on, logs it. This is using
     * MessageFormat.format under the hood. Don't build a pattern on the fly and pass it in, such
     * info("Foo" + i + "bar {0}", 1), because this renders the lazy evaluation partially useless.
     * Well still a little better than completely putting a string together...
     *
     * @param pattern
     *            a MessageFormat pattern
     * @param arguments
     *            a list of arguments for the pattern
     */
    public static void infoWhenDev(final String pattern, final Object... arguments)
    {
        if (Context.isLoadTest == false)
        {
            XltLogger.runTimeLogger.info(MessageFormat.format(pattern, arguments));
        }
    }

    /**
     * Permits lazy evaluation of the msg and if load test is not on, logs it. This is using
     * MessageFormat.format under the hood. Don't build a pattern on the fly and pass it in, such
     * info("Foo" + i + "bar {0}", 1), because this renders the lazy evaluation partially useless.
     * Well still a little better than completely putting a string together...
     *
     * @param pattern
     *            a MessageFormat pattern
     * @param arguments
     *            a list of arguments for the pattern
     */
    public static void warnWhenDev(final String pattern, final Object... arguments)
    {
        if (Context.isLoadTest == false)
        {
            XltLogger.runTimeLogger.warn(MessageFormat.format(pattern, arguments));
        }
    }

    /**
     * Permits lazy evaluation of the msg and if load test is not on, logs it. This is using
     * MessageFormat.format under the hood. Don't build a pattern on the fly and pass it in, such
     * info("Foo" + i + "bar {0}", 1), because this renders the lazy evaluation partially useless.
     * Well still a little better than completely putting a string together...
     *
     * @param pattern
     *            a MessageFormat pattern
     * @param arguments
     *            a list of arguments for the pattern
     */
    public static void errorWhenDev(final String pattern, final Object... arguments)
    {
        if (Context.isLoadTest == false)
        {
            XltLogger.runTimeLogger.error(MessageFormat.format(pattern, arguments));
        }
    }
}
