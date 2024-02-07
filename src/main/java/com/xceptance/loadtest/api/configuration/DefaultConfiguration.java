package com.xceptance.loadtest.api.configuration;

import com.xceptance.loadtest.api.configuration.annotations.EnumProperty;
import com.xceptance.loadtest.api.data.Site;

/**
 * This class holds the initialized site values which are needed before a test case starts, hence
 * they don't fit into the general configuration. This also means they are not test case specific
 * and cannot be put into a user context.
 *
 * @author Rene Schwietzke
 *
 */
public class DefaultConfiguration
{
    // ================ Sites
    // General list of sites, just to 10, to keep lookups cheap
    @EnumProperty(key = "sites", clazz = Site.class, required = true, stopOnGap = true, byId = true)
    public EnumConfigList<Site> sites;
}
