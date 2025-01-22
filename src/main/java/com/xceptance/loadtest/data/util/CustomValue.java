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

import com.xceptance.xlt.api.engine.GlobalClock;

/**
 * Extending XLT's CustomValue but sets the start time at object creation and offers a method {@link #setRunTime()} that sets the runtime at method call, based on the object creation time.
 */
public class CustomValue extends com.xceptance.xlt.api.engine.CustomValue
{
    /**
     * Creates a new CustomValue object.
     */
    public CustomValue()
    {

     setTime(GlobalClock.millis());
    }

    /**
     * Creates a new CustomValue object and gives it the specified name. Furthermore, the start time attribute is set to
     * the current time.
     *
     *  @param name the statistics name
     */
    public CustomValue(final String name)
    {
        super(name);
        setTime(GlobalClock.millis());
    }
}
