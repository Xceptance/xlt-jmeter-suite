package com.xceptance.loadtest.api.data;

/**
 * Interface to disable Site specific test distribution. Use this instead of SiteByMarketShare if
 * you want to use a test to configure the test case the hard way and not via mapping. Or use it for
 * debugging.
 *
 * @author Bernd Weigel
 *
 */
public interface NonSiteRelatedTest extends SiteByMarketShare
{
    @Override
    default Site supplySite()
    {
        return SiteSupplier.siteById("noneSite").get();
    }
}