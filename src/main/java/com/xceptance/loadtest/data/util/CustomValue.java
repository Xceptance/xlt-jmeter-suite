package com.xceptance.loadtest.data.util;

import com.xceptance.xlt.api.engine.GlobalClock;

/**
 * Extending XLT's CustomValue but sets the start time at object creation and offers a method {@link #setRunTime()} that sets the runtime at method call, based on the object creation time.
 */
public class CustomValue extends com.xceptance.xlt.api.engine.CustomValue
{
    /**
     * Creates a new CustomValue object.
     */
    public CustomValue()
    {
   
     setTime(GlobalClock.millis());
    }

    /**
     * Creates a new CustomValue object and gives it the specified name. Furthermore, the start time attribute is set to
     * the current time.
     * 
     *  @param name the statistics name
     */
    public CustomValue(final String name)
    {
        super(name);
        setTime(GlobalClock.millis());
    }
}
