package com.xceptance.loadtest.api.configuration.interfaces;

/**
 * Interface to make any object as part of a list also by lookupable by Id
 *
 * @author Rene Schwietzke
 *
 */
public interface ById
{
    /**
     * Returns anything the developer might thing is a good idea to be used as an id of this very
     * object that implements this interface.
     *
     * @return an id
     */
    public String getId();
}
