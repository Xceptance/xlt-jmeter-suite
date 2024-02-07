package com.xceptance.loadtest.jmeter.util;

import com.google.gson.Gson;

/**
 * Just things we don't want to do all over again
 *
 * @author rschwietzke
 *
 */
public class GsonUtil
{
    // it can be shared, so share it for efficency
    private static Gson gson = new Gson();

    public static Gson gson()
    {
        return gson;
    }
}
