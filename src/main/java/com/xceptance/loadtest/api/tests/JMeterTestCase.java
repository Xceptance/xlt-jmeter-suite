package com.xceptance.loadtest.api.tests;

import java.io.File;
import java.text.MessageFormat;
import java.util.Optional;

import org.apache.jmeter.util.JMeterUtils;
import org.junit.Before;
import org.junit.Test;

import com.xceptance.loadtest.api.data.DataFileProvider;
import com.xceptance.loadtest.api.data.Site;
import com.xceptance.loadtest.api.data.SiteByMarketShare;
import com.xceptance.loadtest.api.util.Context;
import com.xceptance.xlt.api.engine.Session;
import com.xceptance.xlt.api.util.XltLogger;
import com.xceptance.xlt.engine.XltWebClient;
import com.xceptance.xlt.engine.httprequest.HttpRequest;

/**
 * Base class of a JMeter test
 *
 * @author Rene Schwietzke
 */
public abstract class JMeterTestCase extends com.xceptance.xlt.api.tests.AbstractTestCase implements SiteByMarketShare
{
    /**
     * The determined site
     */
    private Site site;

    /**
     * The web client that is used by default for performing the requests.
     */
    private static final ThreadLocal<XltWebClient> WEBCLIENT = new ThreadLocal<>()
    {
        @Override
        protected XltWebClient initialValue()
        {
            XltLogger.runTimeLogger.warn("New XltWebClient created");
            return new XltWebClient();
        }
    };

    /**
     * Constructor
     */
    public JMeterTestCase()
    {
        super();

        super.__setup();

        // ok, remove the client from the shutdown list so we can recylce it
        Session.getCurrent().removeShutdownListener(WEBCLIENT.get());

        // set the web client in this context
        HttpRequest.setDefaultWebClient(WEBCLIENT.get());

        // Set test name depending if we have sites or not
        setTestName(getSiteSpecificName(getTestName(), getSite().id));

        // Create and attach context instance
        Context.attach(new Context(getClass().getName(), getSite()));
    }

    public static String getSiteSpecificName(final String name, final String siteId)
    {
        if ("default".equals(siteId) == false && "noneSite".equals(siteId) == false)
        {
            // we have something non default
            return MessageFormat.format("{0}_{1}", name, siteId);
        }
        return name;
    }

    /**
     * Returns a random site
     *
     * @return
     */
    public Site getSite()
    {
        return site == null ? site = supplySite() : site;
    }

    /**
     * Test preparation. Nothing to do here by default. Feel free to override.
     *
     * @throws Throwable
     *             thrown on error
     */
    @Before
    public void init() throws Throwable
    {
        Optional<File> jmeterPorperties = DataFileProvider.dataFileBySite(Context.get().getSite(), Context.get().configuration.jmeterFile);
        Optional<File> upgradeProperties = DataFileProvider.dataFileBySite(Context.get().getSite(), Context.get().configuration.upgradeFile);
        Optional<File> saveServiceProperties = DataFileProvider.dataFileBySite(Context.get().getSite(), Context.get().configuration.saveServiceFile);
        
        // set minimum properties for tree loading/parsing
        JMeterUtils.loadJMeterProperties(jmeterPorperties.get().getPath());
        JMeterUtils.setProperty("upgrade_properties", upgradeProperties.get().getPath());
        JMeterUtils.setProperty("saveservice_properties", saveServiceProperties.get().getPath());
    }

    /**
     * Overwritten setup to avoid the logger message, this is for the last piece of speed only
     */
    @Override
    public void setUp()
    {
    }

    /**
     * Run the test scenario.
     *
     * @throws Throwable
     */
    @Test
    public void run() throws Throwable
    {
        test();
    }

    /**
     * Main test method.
     *
     * @throws Throwable
     */
    protected abstract void test() throws Throwable;

    /**
     * To avoid closing the connection and restart TLS as well as the low level connection, you can
     * optionally only reset the cookie state of your client. Way lighter!
     */
    public void clearCookies()
    {
        WEBCLIENT.get().getCookieManager().clearCookies();
    }

    /**
     * If you don't need the state reset, don't call it. It closes the client and removes all state
     * such as cookies but also closes the network connection and the TLS session state.
     */
    public void closeWebClient()
    {
        // this is the hard close
        WEBCLIENT.get().close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        // We don't call super, because it logs just a message and that costs even though
        // it is only important for debugging... speed!!!
        
        // Release context instance
        Context.get().releaseContext();
        
        if (!Context.isLoadTest)
        {
            super.tearDown();
        }
    }
}
