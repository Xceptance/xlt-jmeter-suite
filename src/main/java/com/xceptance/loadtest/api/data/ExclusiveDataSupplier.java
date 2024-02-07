package com.xceptance.loadtest.api.data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

import org.junit.Assert;

import com.xceptance.loadtest.api.util.Context;
import com.xceptance.xlt.api.data.ExclusiveDataProvider;
import com.xceptance.xlt.api.data.ExclusiveDataProvider.Parser;

/**
 * Maps the XLT ExclusiveDataProvider to the site based folder structure.
 *
 * @author Bernd Weigel
 */
public class ExclusiveDataSupplier<T>
{
    public static <T> ExclusiveDataProvider<T> getInstance(final String fileName, final Parser<T> parser) throws FileNotFoundException, IOException
    {

        final Site site = Context.getSite();
        final Optional<String> path = DataFileProvider.dataFilePathBySite(site, fileName);

        if (path.isPresent())
        {
            return ExclusiveDataProvider.getInstance(path.get(), parser);
        }
        else
        {
            Assert.fail("File " + fileName + " not found for Site " + site.getId());
            return null;
        }
    }

    public static ExclusiveDataProvider<String> getInstance(final String fileName) throws FileNotFoundException, IOException
    {
        return getInstance(fileName, ExclusiveDataProvider.DEFAULT_PARSER);
    }
}
