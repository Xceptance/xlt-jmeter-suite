package com.xceptance.loadtest.api.configuration;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Assert;

import com.xceptance.loadtest.api.data.DataFileProvider;
import com.xceptance.loadtest.api.data.Site;
import com.xceptance.loadtest.api.util.Log;
import com.xceptance.xlt.api.util.XltLogger;

/**
 * Takes care of building and caching of YamlProperties, tries to avoid expensive reloading and
 * reparsing.
 *
 * @author Rene Schwietzke
 *
 */
public class YamlPropertiesBuilder
{
    // our global cache
    private final static ConcurrentHashMap<String, Optional<Properties>> propertiesCache = new ConcurrentHashMap<>();

    /**
     * Non-public constructor
     */
    private YamlPropertiesBuilder()
    {
    }

    /**
     * Try to load
     *
     * @param file
     *            the property file to load
     * @return the loaded properties
     */
    private static Optional<Properties> loadFromFile(final Optional<File> file)
    {
        // do we have a file?
        if (file.isPresent() == false)
        {
            return Optional.empty();
        }

        try
        {
            Log.infoWhenDev("Loading custom YAML properties from {0}", file.get().toString());

            final Properties properties = YamlProperties.build(file.get());
            if (properties == null)
            {
                Log.warnWhenDev("Cannot load custom YAML properties from {0}", file.get().toString());
            }
            else
            {
                Log.infoWhenDev("Loaded custom YAML properties from {0}", file.get().toString());

                // ok, leave with our data
                return Optional.of(properties);
            }
        }
        catch (final IOException e)
        {
            XltLogger.runTimeLogger.error(
                            MessageFormat.format("Failure when loading custom YAML properties file {0}: {1}",
                                            file.get().toString(), e.getMessage()));
        }

        return Optional.empty();
    }

    /**
     * Load a file by name from the configured data directory of the test suite
     *
     * @param site
     *            the site context to look first
     * @param fileName
     *            the file name with path relative to the data directory
     */
    public static Optional<Properties> build(final String key, final String fileName)
    {
        // check whether or not we already now this thingy
        return propertiesCache.computeIfAbsent(key + File.separatorChar + fileName, k ->
        {
            final Optional<Properties> properties = loadFromFile(DataFileProvider.dataFile(fileName));
            if (properties.isPresent())
            {
                return properties;
            }
            else
            {
                // well... not found... that should not happen here
                Assert.fail(MessageFormat.format("Failed loading required YAML file: {0}", fileName));
            }

            return Optional.empty();
        });
    }

    public static Optional<Properties> buildWithFallback(final Site site, final String fileName)
    {
        // key, this is not a file system location!!!
        final String key = site.id + File.separatorChar + site.region + File.separatorChar + site.locale + File.separatorChar + fileName;

        // check whether or not we already now this thingy
        return propertiesCache.computeIfAbsent(key, k ->
        {
            final Optional<File> file = DataFileProvider.dataFileBySite(site, fileName);
            final Optional<Properties> result = loadFromFile(file);

            if (!result.isPresent())
            {
                // if not found, we fail!!
                Assert.fail(MessageFormat.format("Unable to load {0} from hierarchy for {1}", fileName, site));
            }

            return result;
        });
    }
}
