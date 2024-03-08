package com.xceptance.loadtest.jmeter.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.xceptance.loadtest.api.data.Site;

/**
 * Common data collector for all data needed for a test during execution, kind of a global state.
 *
 * @author Rene Schwietzke
 */
public class TestData
{
    // simple key-value store
    public Map<String, Object> store = new HashMap<>(41);

    // The site we are living in
    public Site site;

    // Test case specific authorization (e.g. user specific token).
    public Optional<String> authorization = Optional.empty();

    /**
     * Set the site we are moving it
     *
     * @param newSite
     *            the new site
     * @return the old site if set, otherwise null
     */
    public Site setSite(final Site newSite)
    {
        final Site oldSite = this.site;
        this.site = newSite;

        return oldSite;
    }

    /**
     * Return the current site
     *
     * @return the current site
     */
    public Site getSite()
    {
        return site;
    }
}