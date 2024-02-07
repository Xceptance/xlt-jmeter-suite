package com.xceptance.loadtest.api.configuration.interfaces;

/**
 * Your objects should implement this interface if you want to do certain stuff after the initial
 * setup from the properties.
 *
 * @author Rene Schwietzke
 *
 */
public interface Initable
{
    /**
     * Any kind of setup code that finishes the initialization of the just built object. Calculate
     * missing data, sort things... up to you.
     */
    public void init();
}
