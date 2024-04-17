package com.xceptance.loadtest.data.util;

@FunctionalInterface
public interface SupplierAction<T>
{
    public abstract T get(String timerName) throws Throwable;
}
