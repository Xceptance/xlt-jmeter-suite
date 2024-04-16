package com.xceptance.loadtest.api.util;

import com.xceptance.loadtest.api.tests.JMeterTestCase;
import com.xceptance.xlt.api.actions.AbstractAction;
import com.xceptance.xlt.engine.SessionImpl;

/**
 * Convenience methods for actions to avoid that you have to program it yourself
 *
 * @author Rene Schwietzke
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



