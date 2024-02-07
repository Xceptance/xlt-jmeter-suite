package com.xceptance.loadtest.api.data;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import com.xceptance.common.util.CsvUtils;
import com.xceptance.xlt.api.data.DataProvider;
import com.xceptance.xlt.api.data.ExclusiveDataProvider.Parser;

/**
 * Simple username / password account. Including a parser to generate accounts from a csv string
 * representation.
 *
 * @author Bernd Weigel
 *
 */
public class Account
{
    public static final Parser<Account> ACCOUNT_PARSER = new Parser<Account>()
    {
        @Override
        public List<Account> parse(final List<String> lines)
        {
            final List<Account> c = new ArrayList<>();
            for (final String line : lines)
            {
                final String trimmedLine = line.trim();
                if (!trimmedLine.startsWith(DataProvider.DEFAULT_LINE_COMMENT_MARKER))
                {
                    final String[] decodedLine = CsvUtils.decode(trimmedLine);

                    Assert.assertTrue(trimmedLine + " does not contain username,password", decodedLine.length > 1);

                    c.add(new Account(decodedLine[0], decodedLine[1]));
                }
            }
            return c;
        }
    };

    final public String user;
    final public String password;

    public Account(final String user, final String password)
    {
        this.password = password;
        this.user = user;
    }

}
