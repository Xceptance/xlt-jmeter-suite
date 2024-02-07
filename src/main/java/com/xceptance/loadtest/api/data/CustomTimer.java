package com.xceptance.loadtest.api.data;

import com.xceptance.loadtest.api.util.CustomData;
import com.xceptance.xlt.api.engine.Session;

public class CustomTimer
{
    // the instance that has a running timer attached
    private final CustomData customData;

    /**
     * Don't make it accessible from the outside
     *
     * @param name
     *            the name to use later
     */
    private CustomTimer(final String name)
    {
        this.customData = new CustomData(name);
    }

    /**
     * Start a new logger
     *
     * @param name
     *            the name to use
     * @return this logger with a ticking clock
     */
    public static CustomTimer start(final String name)
    {
        return new CustomTimer(name);
    }

    /**
     * Stop this logger
     *
     * @return the runtime
     */
    public CustomTimer stop()
    {
        this.customData.setRunTime();

        return this;
    }

    /**
     * Stop this logger and return the runtime
     *
     * @return the runtime
     */
    public long stopAndGet()
    {
        this.customData.setRunTime();

        return this.customData.getRunTime();
    }

    /**
     * Stop this logger and log the time as not failed
     *
     * @return the runtime
     */
    public long stopAndLog()
    {
        return stopAndLog(false);
    }

    /**
     * Stop this logger and report the runtime
     *
     * @return the runtime
     */
    public long stopAndLog(final boolean failed)
    {
        return stopAndLog(this.customData.getName(), failed);
    }

    /**
     * Stop this logger and report the runtime, redefine the name
     *
     * @param newName
     *            the new name of the log entry
     * @param failed
     *            the status to use
     * @return the runtime
     */
    public long stopAndLog(final String newName, final boolean failed)
    {
        stop();
        this.customData.setFailed(failed);
        this.customData.setName(newName);

        Session.getCurrent().getDataManager().logDataRecord(this.customData);

        return this.customData.getRunTime();
    }

    /**
     * Log custom data
     *
     * @param name
     *            the name to log
     * @param runtime
     *            self measured runtime
     * @param failed
     *            was that a failed measurement
     */
    public static void log(final String name, final long runtime, final boolean failed)
    {
        final CustomData data = new CustomData();
        data.setName(name);
        data.setRunTime(runtime);
        data.setFailed(false);

        Session.getCurrent().getDataManager().logDataRecord(data);
    }

    /**
     * Log custom data that was successful
     *
     * @param name
     *            the name to log
     * @param runtime
     *            self measured runtime
     */
    public static void log(final String name, final long runtime)
    {
        log(name, runtime, false);
    }

    /**
     * Functional interface for logging custom data runtimes. Just more elegant in the code but not
     * suitable for everything due to the scope of the code block.
     *
     * @param name
     *            the name of the custom data
     * @param task
     *            the task to measure
     */
    public static void runAndLog(final String name, final Runnable task)
    {
        final CustomData cd = new CustomData(name);

        try
        {
            task.run();
        }
        catch (final Error e)
        {
            cd.setFailed(false);
            throw e;
        }
        finally
        {
            cd.setRunTime();
            Session.getCurrent().getDataManager().logDataRecord(cd);
        }
    }
}

