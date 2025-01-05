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

import java.util.List;

import com.xceptance.loadtest.control.JMeterTestCase;

public class LoopController extends JMeterTestCase
{
    public LoopController()
    {
        super("LoopController.jmx");
    }

    @Override
    public void test() throws Throwable
    {
        super.test();
        validateActions(List.of("Request1", "Request1", "Request2", "Request2", "Request3",
                "Request3", "Request4", "Request4", "Request5", "Request5"));
    }
}
