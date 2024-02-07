package com.xceptance.loadtest.api.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;

import com.xceptance.loadtest.api.util.Context;
import com.xceptance.xlt.api.util.XltRandom;

/**
 * Data Handling class, to read test data from predefined files.
 *
 * @author Rene Schwietzke
 */
public class DataSupplier
{
    /**
     * Keep our data loaded and shared based on the site key and they type. We set concurrency for
     * write low but still harvest good read performance
     */
    private final static ConcurrentHashMap<String, List<String>> data = new ConcurrentHashMap<>(1);

    /**
     * Get us the source list for the data in the site context
     *
     * @param filename
     *            file to open in the hierarchy
     * @return the list with the data
     */
    private static List<String> getSourceList(final String filename)
    {
        final Site site = Context.get().data.getSite();

        // get us a key, just use
        final String key = site.toString() + File.separator + filename;

        final List<String> list = data.computeIfAbsent(key, k -> {
            // load the data otherwise break
            final Optional<File> file = DataFileProvider.dataFileBySite(site, filename);

            if (file.isPresent())
            {
                try
                {
                    return Files.readAllLines(file.get().toPath())
                                    .stream()
                                    .map(s -> s.trim())
                                    .filter(s ->
                                    {
                                        return s.length() > 0 && !s.startsWith("#");
                                    })
                                    .collect(Collectors.toList());
                }
                catch (final IOException e)
                {
                    // we will get to the assertion
                }
            }

            Assert.fail(MessageFormat.format("Unable to find data file {0} for site {1}", filename, site));

            // for the compiler, we are not going to reach this otherwise
            return Collections.emptyList();
        });

        // determine a random value
        return list;
    }

    /**
     * Get us a first name from a plain file
     */
    public static String firstName()
    {
        return randomEntry(getSourceList("firstnames.txt"));
    }

    /**
     * Get us a last name from a plain file
     */
    public static String lastName()
    {
        return randomEntry(getSourceList("lastnames.txt"));
    }

    /**
     * Get us a last name from a plain file
     */
    public static String company()
    {
        return randomEntry(getSourceList("companies.txt"));
    }

    /**
     * Get us a last name from a plain file
     */
    public static String town()
    {
        return randomEntry(getSourceList("towns.txt"));
    }

    /**
     * Get us a last name from a plain file
     */
    public static String country()
    {
        return randomEntry(getSourceList("countries.txt"));
    }

    /**
     * Get us a last name from a plain file
     */
    public static String noun()
    {
        return randomEntry(getSourceList("nouns.txt"));
    }

    /**
     * Get us a last name from a plain file
     */
    public static String street()
    {
        return randomEntry(getSourceList("streets.txt"));
    }

    /**
     * Get us a last name from a plain file
     */
    public static String word()
    {
        return randomEntry(getSourceList("words.txt"));
    }

    /**
     * Just get us a random entry from our list
     */
    public static <T> T randomEntry(final List<T> list)
    {
        return list.get(XltRandom.nextInt(list.size()));
    }

    public static String searchterm()
    {
        return randomEntry(getSourceList("searchterms.txt"));
    }

    public static String sentence()
    {
        return randomEntry(getSourceList("sentences.txt"));
    }

    /**
     * Returns a text composed of several sentences.
     *
     * @param sentenceCount
     *            the number of sentences to use
     * @param removeWhitespace
     *            whether or not any whitespace is to be removed from the string
     * @return a text
     */
    public static String getText(final int sentenceCount, final boolean removeWhitespace)
    {
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < sentenceCount; i++)
        {
            if (i > 0)
            {
                b.append(' ');
            }

            b.append(sentence());
        }
        return removeWhitespace ? StringUtils.deleteWhitespace(b.toString()) : b.toString();
    }
}
