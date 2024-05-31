package com.xceptance.loadtest.jmeter.configuration;

import com.xceptance.loadtest.api.configuration.ConfigDistribution;
import com.xceptance.loadtest.api.configuration.ConfigProbability;
import com.xceptance.loadtest.api.configuration.ConfigRange;
import com.xceptance.loadtest.api.configuration.annotations.Property;

/**
 * Configuration for the Test
 *
 */
public class Configuration extends com.xceptance.loadtest.addons.configuration.Configuration
{
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
    // jmeter default values
    
    @Property(key = "com.xceptance.xlt.default.jmeter.file", required = true)
    public String jmeterFile;
    
    @Property(key = "com.xceptance.xlt.default.jmeter.saveservice.file", required = true)
    public String saveServiceFile;
    
    @Property(key = "com.xceptance.xlt.default.jmeter.upgrade.file", required = true)
    public String upgradeFile;

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
}