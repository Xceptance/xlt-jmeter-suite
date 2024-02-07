package com.xceptance.loadtest.jmeter.configuration;

import com.xceptance.loadtest.api.configuration.ConfigDistribution;
import com.xceptance.loadtest.api.configuration.ConfigProbability;
import com.xceptance.loadtest.api.configuration.ConfigRange;
import com.xceptance.loadtest.api.configuration.LTProperties;
import com.xceptance.loadtest.api.configuration.annotations.Property;

/**
 * Configuration for the Test
 *
 * @author Rene Schwietzke (Xceptance Software Technologies GmbH)
 */
public class Configuration
{
    /**
     * The name of the current running TestCase' class
     */
    public LTProperties properties;

    // ===============================================================
    // Common / General

    @Property(key = "general.host", required = false)
    public String host;

    @Property(key = "general.authorization", required = false)
    public String authorization;

    @Property(key = "general.url", required = false)
    public String baseUrl;

    @Property(key = "general.userAgent")
    public String userAgent;

    @Property(key = "general.closeWebClient")
    public boolean closeWebClient;

    @Property(key = "general.clearCookies")
    public boolean clearCookies;

    // =========================================================
    // jsonplaceholder test case config

    @Property(key = "jsonplaceholder.host")
    public String jsonplaceholderHost;

    @Property(key = "jsonplaceholder.get.count")
    public ConfigRange jsonplaceholderGetCount;

    // =========================================================
    // Wikipedia Test Case specific configurations

    @Property(key = "wiki.articleCount", immutable = false, required = false)
    public ConfigDistribution articleCount;

    @Property(key = "wiki.continueSearch", immutable = false, required = false)
    public ConfigProbability continueSearch;

    /**
     * Returns the properties that are current for this context and the source of this
     * configuration. You can also directly access them, if you like.
     *
     * @return the property set
     */
    public LTProperties getProperties()
    {
        return properties;
    }

    /**
     * Constructor
     */
    public Configuration()
    {
    	super();
    }
}