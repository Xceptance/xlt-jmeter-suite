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

import com.xceptance.loadtest.control.JMeterTestCase;
import com.xceptance.xlt.api.actions.AbstractAction;
import com.xceptance.xlt.engine.SessionImpl;

/**
 * Convenience methods for actions to avoid that you have to program it yourself
 *
 */
public class Actions
{
    /**
     * Runs an Action defined by a Lambda Function and returns the value, which is returned in this
     * action for further use.
     *
     * @param timerName
     * @param action
     * @throws Throwable
     */
    public static void run(final String timerName, final Action action) throws Throwable
    {
        new AbstractAction(null, timerName)
        {
            @Override
            public void preValidate() throws Exception
            {
            }

            @Override
            protected void execute() throws Exception
            {
                try
                {
                    action.run(this.getTimerName());
                }
                catch (final Throwable e)
                {
                    if(e instanceof AssertionError)
                    {
                        AssertionError ae = new AssertionError(e.getMessage());
                        ae.setStackTrace(e.getStackTrace());
                        throw ae;
                    }

                    throw new RuntimeException(e);
                }
            }

            @Override
            protected void postValidate() throws Exception
            {
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void run() throws Throwable
            {
                try
                {
                    super.run();
                }
                finally
                {
                    // add an empty "page" as the result of this action
                    SessionImpl.getCurrent().getRequestHistory().add(JMeterTestCase.getSiteSpecificName(getTimerName(), Context.get().getSite().id));
                }
            }

        }.run();
    }

    /**
     * Runs an Action defined by a Lambda Function without any return value.
     *
     * @param timerName
     * @param action
     * @throws Throwable
     */
    public static <T> T get(final String timerName, final SupplierAction<T> action) throws Throwable
    {
        final AbstractActionWithResult<T> a = new AbstractActionWithResult<>(null, timerName, action);
        a.run();

        return a.result;
    }

    /**
     * Helper class to get the result out of the scope of the AbstractAction
     *
     * @author rschwietzke
     *
     * @param <T>
     *            the return type of the supplier action
     */
    static class AbstractActionWithResult<T> extends AbstractAction
    {
        public T result = null;
        private final SupplierAction<T> action;

        protected AbstractActionWithResult(final AbstractAction previousAction, final String timerName, final SupplierAction<T> action)
        {
            super(previousAction, timerName);
            this.action = action;
        }

        @Override
        public void preValidate() throws Exception
        {
        }

        @Override
        protected void execute() throws Exception
        {
            try
            {
                result = action.get(JMeterTestCase.getSiteSpecificName(getTimerName(), Context.get().getSite().id));
            }
            catch (final Throwable e)
            {
                // sadly, this is needed
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void postValidate() throws Exception
        {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() throws Throwable
        {
            try
            {
                super.run();
            }
            finally
            {
                // add an empty "page" as the result of this action
                SessionImpl.getCurrent().getRequestHistory().add(JMeterTestCase.getSiteSpecificName(getTimerName(), Context.get().getSite().id));
            }
        }
    }
}



