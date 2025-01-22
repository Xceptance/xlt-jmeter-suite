/*
 * Copyright (c) 2005-2025 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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