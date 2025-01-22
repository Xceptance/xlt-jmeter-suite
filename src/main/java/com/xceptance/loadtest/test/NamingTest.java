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

public class NamingTest extends JMeterTestCase
{
    public NamingTest()
    {
    	super("NamingTest.jmx");
    }

    @Override
    public void test() throws Throwable
    {
        super.test();
        validateActions(List.of("RequestA", "UnnamedRequest_1", "UnnamedRequest_2",
                "RequestB", "UnnamedRequest_3", "UnnamedRequest_4",
                "RequestC", "UnnamedRequest_5", "UnnamedRequest_6",
                "TransactionControllerWithParentSample", "UnnamedTransactionController_1", "RequestF", "RequestG", "RequestH",
                "RequestI", "UnnamedRequest_7", "RequestJ", "UnnamedRequest_8", "ParentTransactionControllerWithParentSample",
                "NestedTransactionControllerWithParentSample", "NestedTransactionControllerWithParentSample",
                "UnnamedThreadGroup_1","RequestB",
                "UnnamedRequest_9", "UnnamedRequest_10", "RequestC",
                "UnnamedRequest_11","UnnamedRequest_12",
                "TransactionControllerWithParentSample", "UnnamedTransactionController_2", "RequestF",
                "RequestG", "RequestH", "RequestI", "UnnamedRequest_13", "RequestJ",
                "UnnamedRequest_14", "ParentTransactionControllerWithParentSample",
                "NestedTransactionControllerWithParentSample", "NestedTransactionControllerWithParentSample"));
    }
}
