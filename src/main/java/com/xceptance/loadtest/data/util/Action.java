package com.xceptance.loadtest.data.util;

@FunctionalInterface
public interface Action
{
    public abstract void run(String timerNamer) throws Throwable;
}
