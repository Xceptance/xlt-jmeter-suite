package com.xceptance.loadtest.api.data;

/**
 *
 * General Interface to use the site specific configuration as well as the automated test
 * distribution by the market share of the site.
 *
 */
public interface SiteByMarketShare
{
    default Site supplySite()
    {
        return SiteSupplier.randomSite().get();
    }
}
