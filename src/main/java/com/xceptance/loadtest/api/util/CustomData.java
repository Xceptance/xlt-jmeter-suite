package com.xceptance.loadtest.api.util;

import com.xceptance.xlt.api.engine.GlobalClock;

/**
 * Extending XLT's CustomData but sets the start time at object creation and offers a method {@link #setRunTime()} that sets the runtime at method call, based on the object creation time.
 */
public class CustomData extends com.xceptance.xlt.api.engine.CustomData
{
    /**
     * Creates a new CustomData object.
     */
    public CustomData()
    {
        setTime(GlobalClock.millis());
    }

    /**
     * Creates a new CustomData object and sets the given name.
     * Furthermore, the start time attribute is set to the 
     * current time.
     * 
     *  @param name the statistics name
     */
    public CustomData(final String name)
    {
        super(name);
        setTime(GlobalClock.millis());
    }

    /**
     * Sets the runtime on call based on the object creation time.
     */
    public void setRunTime()
    {
        setRunTime(GlobalClock.millis() - getTime());
    }
}
