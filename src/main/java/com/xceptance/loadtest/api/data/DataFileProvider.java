package com.xceptance.loadtest.api.data;

import java.io.File;
import java.util.Optional;
import java.util.regex.Pattern;

import com.xceptance.loadtest.api.util.Log;
import com.xceptance.xlt.api.util.XltProperties;
import com.xceptance.xlt.common.XltConstants;

/**
 * Manages the hierarchical access to our data files based on site, region, and language. This works
 * for any kind of file that is within our data hierarchy.
 *
 * @author Rene Schwietzke
 *
 */
public class DataFileProvider
{
    /**
     * Returns a file handle to a file in the hierarchy for later loading
     *
     * @param site
     *            the site
     * @param fileName
     *            the file name we are interested in
     * @return empty optional or the file to be used later
     */
    public static Optional<File> dataFileBySite(final Site site, final String fileName)
    {
        // from the site first (e.g. UnitedStates)
        final Optional<File> fromSite = dataFile("sites" + File.separatorChar + site.id + File.separatorChar + fileName);
        if (fromSite.isPresent())
        {
            return fromSite;
        }

        // if not found from default site,
        final Optional<File> fromDefaultSite = dataFile("sites" + File.separatorChar + "default" + File.separatorChar + fileName);
        if (fromDefaultSite.isPresent())
        {
            return fromDefaultSite;
        }

        // if not found from region,
        if (site.region != null)
        {
            final Optional<File> fromRegion = dataFile("regions" + File.separatorChar + site.region + File.separatorChar + fileName);
            if (fromRegion.isPresent())
            {
                return fromRegion;
            }
        }

        // if not found from default region,
        final Optional<File> fromDefaultRegion = dataFile("regions" + File.separatorChar + "default" + File.separatorChar + fileName);
        if (fromDefaultRegion.isPresent())
        {
            return fromDefaultRegion;
        }

        // if not found from locale (e.g. en_US, as specified for the site)
        if (site.locale != null)
        {
            final Optional<File> fromLocale = dataFile("languages" + File.separatorChar + site.locale + File.separatorChar + fileName);
            if (fromLocale.isPresent())
            {
                return fromLocale;
            }
        }

        // ok, just the language, such as en
        if (site.locale != null)
        {
            final String language = site.language();
            final Optional<File> fromLanguage = dataFile("languages" + File.separatorChar + language + File.separatorChar + fileName);
            if (fromLanguage.isPresent())
            {
                return fromLanguage;
            }
        }

        // if not found from default language
        final Optional<File> fromDefaultLanguage = dataFile("languages" + File.separatorChar + "default" + File.separatorChar + fileName);
        if (fromDefaultLanguage.isPresent())
        {
            return fromDefaultLanguage;
        }

        return Optional.empty();
    }

    /**
     * Try to check file. Return an optional with this file of it exists, an empty optional
     * otherwise
     *
     * @param fileName
     *            the file to load in the data directory
     * @return empty optional if file does not exists, the file otherwise
     */
    public static Optional<File> dataFile(final String fileName)
    {
        final File file = new File(getDataDirectory(), fileName);

        Log.infoWhenDev("Looking up file in hierarchy: {0} ...", file.toString());
        final Optional<File> result = file.exists() ? Optional.of(file) : Optional.empty();
        Log.infoWhenDev("...{0} was {1}", file.toString(), result.isPresent() ? "found." : "not found!");

        return result;
    }

    /**
     * Retrieves a relative file path to a site-specific data file WITHOUT the default data
     * directory contained in this path.
     *
     * This is done because the data provider assumes all data files are already located in the
     * default data directory.
     *
     * Example:<br>
     * We want to retrieve contents of file './config/data/sites/default/foo.txt'<br>
     * Our data provider would expect the path to be 'sites/default/foo.txt'<br>
     * Hence, we need to remove the './config/data/' part to get 'sites/default/foo.txt'.<br>
     *
     * @param site
     *            A site object.
     * @param fileName
     *            The filename of the data file.
     * @return The relative path without the default data directory.
     */
    public static Optional<String> dataFilePathBySite(final Site site, final String fileName)
    {
        // get path relative to program root
        final Optional<File> file = dataFileBySite(site, fileName);
        if (file.isPresent())
        {
            final String normalizedDataDirectoryPath = new File(getDataDirectory()).getPath();
            final String escapedNormalizedDirectoryPath = Pattern.quote(normalizedDataDirectoryPath);
            final String dataFilePathWithoutDataDirectory = file.get().getPath().replaceFirst(escapedNormalizedDirectoryPath, "");
            final String dataFilePathWithoutLeadingSlash = dataFilePathWithoutDataDirectory.substring(1);

            return Optional.of(dataFilePathWithoutLeadingSlash);
        }

        // file not found
        return Optional.empty();
    }

    public static String getDataDirectory()
    {
        return XltProperties.getInstance().getProperty(XltConstants.XLT_PACKAGE_PATH + ".data.directory", "config" + File.separatorChar + "data");
    }
}
