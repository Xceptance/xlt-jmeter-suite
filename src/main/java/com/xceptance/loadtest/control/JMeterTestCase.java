package com.xceptance.loadtest.control;

import java.io.File;
import java.text.MessageFormat;
import java.util.Optional;

import org.apache.jmeter.util.JMeterUtils;
import org.junit.Before;

import com.xceptance.loadtest.api.data.DataFileProvider;
import com.xceptance.loadtest.api.tests.HtmlUnitLoadTestCase;
import com.xceptance.loadtest.data.util.Context;

/**
 * Base class of a JMeter test
 *
 * @author Rene Schwietzke
 */
public abstract class JMeterTestCase extends HtmlUnitLoadTestCase
{
//    /**
//     * The determined site
//     */
//    private Site site;
//
//    /**
//     * The web client that is used by default for performing the requests.
//     */
//    private static final ThreadLocal<XltWebClient> WEBCLIENT = new ThreadLocal<>()
//    {
//        @Override
//        protected XltWebClient initialValue()
//        {
//            XltLogger.runTimeLogger.warn("New XltWebClient created");
//            return new XltWebClient();
//        }
//    };

    /**
     * Constructor
     */
    public JMeterTestCase()
    {
        super();

        // Create and attach context instance
        Context.attach(new Context(getClass().getName(), getSite()));

        // Set test name depending if we have sites or not
        setTestName(getSiteSpecificName(getTestName(), getSite().id));
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
