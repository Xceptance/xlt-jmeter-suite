package com.xceptance.loadtest.api.util;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Assert;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xceptance.loadtest.api.configuration.ConfigurationBuilder;
import com.xceptance.loadtest.api.configuration.DefaultConfiguration;
import com.xceptance.loadtest.api.configuration.LTProperties;
import com.xceptance.loadtest.api.configuration.YamlPropertiesBuilder;
import com.xceptance.loadtest.api.data.Account;
import com.xceptance.loadtest.api.data.CustomTimer;
import com.xceptance.loadtest.api.data.ExclusiveDataSupplier;
import com.xceptance.loadtest.api.data.Site;
import com.xceptance.loadtest.jmeter.configuration.Configuration;
import com.xceptance.loadtest.jmeter.data.TestData;
import com.xceptance.xlt.api.engine.Session;
import com.xceptance.xlt.api.util.XltLogger;
import com.xceptance.xlt.api.util.XltProperties;
import com.xceptance.xlt.api.util.XltRandom;
import com.xceptance.xlt.common.XltConstants;

/**
 * Central component for configuration, current page, last action and all test related data.
 *
 * @author Bernd Weigel
 */
public class Context
{
    /**
     * Known test contexts.
     */
    private static final Map<ThreadGroup, Context> CONTEXTS = new ConcurrentHashMap<>(101);

    /**
     * The Configuration for the current thread, wrapped and buffered from the properties.
     */
    public final Configuration configuration;

    /**
     * Test data of the current execution to better isolate that from the context
     */
    public final TestData data = new TestData();

    // Our cached GSON parser and builder
    private Gson gson;

    // Keep a quickly accessible info that we are a load test run
    public static final boolean isLoadTest = Session.getCurrent().isLoadTest();

    /**
     * Constructor; Creates a new Context for a TestCase.
     *
     * @param xltProperties
     *            the initial set of XLT properties
     * @param userName
     *            the currently running user
     * @param fullTestClassName
     *            test name
     * @param site
     *            site context
     */
    private Context(final XltProperties xltProperties,
                    final String userName,
                    final String fullTestClassName,
                    final Site site)
    {
        // where we get the props from later in this code
        final LTProperties totalProperties = new LTProperties(userName, fullTestClassName, site.id);

        // xlt properties come first aka the last line of defense for the later look up
        totalProperties.addProperties(Optional.of(xltProperties.getProperties()));

        // initialize the config and log the time needed
        final CustomTimer cdl = CustomTimer.start("config.build.testcase");
        {
            // ### Get us sites.yaml and other more global structured properties first

            // ok, we have custom properties in YAML, get them before we do the configuration magic
            final String fileNames = xltProperties.getProperty("general.properties.yaml.global.files", "");
            for (final String fileName : fileNames.split("\\s|,|;"))
            {
                // don't try empty
                if (fileName.trim().length() > 0)
                {
                    final Optional<Properties> newProperties = YamlPropertiesBuilder.build(site.id, fileName);
                    if (newProperties.isPresent())
                    {
                        totalProperties.addProperties(newProperties);
                    }
                }
            }

            // now, the real fun starts, we need the rest of the properties
            final String siteFileNames = xltProperties.getProperty("general.properties.yaml.site.files", "");
            for (final String fileName : siteFileNames.split("\\s|,|;"))
            {
                // don't try empty
                if (fileName.trim().length() > 0)
                {
                    final Optional<Properties> newProperties = YamlPropertiesBuilder.buildWithFallback(site, fileName);
                    totalProperties.addProperties(newProperties);
                }
            }

            // dump all for debugging
            Log.debugWhenDev("{0}", totalProperties);

            // get XLT all our look up data so it is also up to date
            XltProperties.getInstance().setProperties(totalProperties.properties);

            // now, we can do what we always do, because all YAML stuff is available as regular
            // properties
            this.configuration = new ConfigurationBuilder(totalProperties).build(Configuration.class);
        }
        cdl.stopAndLog();

        // keep them for later
        this.configuration.properties = totalProperties;
        this.data.setSite(site);
    }

    /**
     * Gets the current configuration for this TestCase.
     *
     * @return the current configuration for this TestCase
     */
    public static Configuration configuration()
    {
        return get().configuration;
    }

    /**
     * Retrieves the context instance for the current Thread.
     *
     * @return the context instance for the current Thread
     */
    public static Context get()
    {
        final Context context = CONTEXTS.get(Thread.currentThread().getThreadGroup());

        if (context == null)
        {
            XltLogger.runTimeLogger.error("No context available for this thread.");
            Assert.fail("Context initialization problem. We need one, we don't have one.");
        }

        return context;
    }

    /**
     * Adds a new Context instance for the current Thread to the map. This Method is used by the
     * AbstractTestCase and therefore won't need to be called manually
     *
     * @param fullTestClassName
     *            the name of the test case for property identification
     * @param site
     *            site context
     */
    public static void createContext(final XltProperties xltProperties, final String userName, final String fullTestClassName, final Site site)
    {
        // NOTE: previous added Context instances for this Thread will be ignored
        CONTEXTS.put(
                        Thread.currentThread().getThreadGroup(),
                        new Context(xltProperties, userName, fullTestClassName, site));
    }

    /**
     * Releases the context for the current thread.
     */
    public static void releaseContext()
    {
        // log data that is used typically to re run a test under same conditions
        try
        {
            get().logRerunData();
        }
        catch (final Throwable e)
        {
            XltLogger.runTimeLogger.error("Error during debug info logging", e);
        }

        // remove the context finally
        CONTEXTS.remove(Thread.currentThread().getThreadGroup());

        // destroy default as well to avoid problems with the random pool
        defaultConfiguration.remove();
    }

    /**
     * Executed before the release of the context. Includes handling for additional debug logging.
     */
    private void logRerunData()
    {
        // Do for non-load-test mode only.
        if (isLoadTest == false)
        {
            final StringBuilder out = new StringBuilder(1024);

            // Test rerun information.
            out.append("\n\nIf you want to rerun this testcase, insert the following lines into your config/dev.properties file: \n\n");

            // Randomizer initialization value.
            out.append(XltConstants.RANDOM_INIT_VALUE_PROPERTY).append(" = ").append(XltRandom.getSeed()).append("\n\n");

            if (Boolean.parseBoolean(configuration().properties.getProperty("randomInitValueWasSet")))
            {
                out.append("Testcase was configured with that random value\n\n");
            }

            // Print the collected information.
            XltLogger.runTimeLogger.info(out.toString());
        }
    }

    /**
     * Get the site context.
     *
     * @return the site context
     */
    public static Site getSite()
    {
        return Context.get().data.getSite();
    }

    /**
     * Get a per user Gson instance back and reuse it. Lazy because not every user needs that.
     * Strongly assumes not concurrent usage of the GsonBuilder
     *
     * @return Gson instance
     */
    public static Gson gson()
    {
        final Context context = get();
        if (context.gson == null)
        {
            context.gson = new GsonBuilder().create();
        }

        return context.gson;
    }
    
    /////////////////////////////////////////////////////////////////
    // Load Default Configuration
    /////////////////////////////////////////////////////////////////

    public static ThreadLocal<DefaultConfiguration> defaultConfiguration = new ThreadLocal<DefaultConfiguration>()
    {
        private DefaultConfiguration defaultConfiguration;

        @Override
        public DefaultConfiguration get()
        {
            if (defaultConfiguration == null)
            {
                defaultConfiguration = Context.loadDefaultConfiguration();
            }
            return defaultConfiguration;
        }

        @Override
        public void remove()
        {
            defaultConfiguration = null;
        }
    };

    /**
     * Loads the default non test case dependent configuration. Usually very short. Helps to do
     * something outside of the regular context
     *
     * @return the default configuration
     */
    private static DefaultConfiguration loadDefaultConfiguration()
    {
        // save random seed
        final long savedSeed = XltRandom.getSeed();

        // where we get the props from later in this code
        final LTProperties totalProperties = new LTProperties("", "", "");

        // xlt properties come first aka the last line of defense for the later look up
        totalProperties.addProperties(Optional.of(XltProperties.getInstance().getProperties()));

        // that's what we want
        DefaultConfiguration defaultConfiguaration = null;

        // initialize the config and log the time needed
        final CustomTimer cdl = CustomTimer.start("config.build.default");
        {
            // ### Get us sites.yaml and other more global structured properties first

            // ok, we have custom properties in YAML, get them before we do the configuration magic
            final String fileNames = XltProperties.getInstance().getProperty("general.properties.yaml.global.files", "");
            for (final String fileName : fileNames.split("\\s|,|;"))
            {
                // don't try empty
                if (fileName.trim().length() > 0)
                {
                    final Optional<Properties> newProperties = YamlPropertiesBuilder.build("DEFAULTCONFIGUATION", fileName);
                    if (newProperties.isPresent())
                    {
                        totalProperties.addProperties(newProperties);
                    }
                }
            }

            // now, we can do what we always do, because all YAML stuff is available as regular
            // properties
            defaultConfiguaration = new ConfigurationBuilder(totalProperties).build(DefaultConfiguration.class);
        }
        cdl.stopAndLog();

        // restore seed for proper reproducibility
        XltRandom.setSeed(savedSeed);

        // keep them for later
        return defaultConfiguaration;
    }

    /**
     * Retrieves an Account object from the accounts.csv in the config/data/... directories. This
     * account will be available only for this test instance and is not shared between different
     * test users.
     *
     * If you want to reuse the account after the test has finished, it needs to be released with
     * the releaseExclusiveAccount() method.
     *
     * @return an Account
     */
    public static Account getExclusiveAccountFromFile()
    {
        try
        {
            final Account account = ExclusiveDataSupplier.getInstance("accounts.csv", Account.ACCOUNT_PARSER).getRandom();

            Assert.assertNotNull("No Account available in accounts.csv for site " + getSite(), account);
            return account;
        }
        catch (final Exception e)
        {
            Assert.fail("Could not retrieve account from accounts.csv for site " + getSite());
            return null;
        }
    }

    /**
     *
     * Puts the account object back to the pool, so other users can access it.
     *
     * @param account
     */
    public static void releaseExclusiveAccount(final Account account)
    {
        try
        {
            ExclusiveDataSupplier.getInstance("accounts.csv", Account.ACCOUNT_PARSER).add(account);
        }
        catch (final Exception e)
        {
            Assert.fail("Could not release account to accounts.csv for site " + getSite());
        }

    }

    /**
     * Wrapper method for the data store in the test data. Add Values to share in between actions
     * here. Everything stored here will be available to the current test instance only.
     *
     * @param key
     * @param value
     */
    public void store(final String key, final Object value)
    {
        data.store.put(key, value);
    }

    /**
     * Wrapper method for the data store in the test data. Retrieves values which were stored
     * previously during this test case.
     *
     * @param key
     * @return
     */
    public Object getStored(final String key)
    {
        return data.store.get(key);
    }

}
