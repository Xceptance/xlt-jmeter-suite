package com.xceptance.loadtest.api.util;

@FunctionalInterface
public interface Action
{
    public abstract void run(String timerNamer) throws Throwable;
}
