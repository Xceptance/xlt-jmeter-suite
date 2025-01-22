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
package com.xceptance.loadtest.data.util;

import com.xceptance.loadtest.api.data.Site;
import com.xceptance.loadtest.jmeter.configuration.Configuration;
import com.xceptance.loadtest.jmeter.data.TestData;
import com.xceptance.xlt.api.engine.Session;
import com.xceptance.xlt.api.util.XltProperties;

/**
 * Site specific version of the context, parametrized by the employed Configuration and TestData variants.
 */
public class Context extends com.xceptance.loadtest.addons.util.Context<Configuration, TestData>
{
    /**
     * Creates an instance of the context.
     *
     * @param fullTestClassName The full class name of the test case.
     * @param site              The current site.
     */
    public Context(final String fullTestClassName, Site site)
    {
        super(XltProperties.getInstance(),
                Session.getCurrent().getUserName(),
                fullTestClassName,
                site, Configuration.class, new TestData());
    }

    /**
     * Retrieves an instance of the context.
     *
     * @return The context instance.
     */
    public static Context get()
    {
        return (Context) com.xceptance.loadtest.addons.util.Context.get();
    }
}
