package com.xceptance.loadtest.api.util;

@FunctionalInterface
public interface SupplierAction<T>
{
    public abstract T get(String timerName) throws Throwable;
}
