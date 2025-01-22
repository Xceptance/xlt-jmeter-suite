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
package com.xceptance.loadtest.test;

import com.xceptance.loadtest.control.JMeterTestCase;

public class CSVTest extends JMeterTestCase
{
    public CSVTest()
    {
    	// relative to config/data/jmeter
    	// Important: Any data you refer to has to be relative to
    	// config/data/jmeter as well. Not to the script directory!
    	// To make this work with Jmeter UI, keep the scripts in
    	// config/data/jmeter and the imported data in
    	// config/data/jmeter/data for instance.
        super("CSVTest.jmx");
    }
}
