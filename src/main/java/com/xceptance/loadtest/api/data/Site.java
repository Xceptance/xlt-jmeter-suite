package com.xceptance.loadtest.api.data;

import org.apache.commons.lang3.LocaleUtils;

import com.xceptance.loadtest.api.configuration.annotations.Property;
import com.xceptance.loadtest.api.configuration.interfaces.ById;
import com.xceptance.loadtest.api.configuration.interfaces.Initable;

/**
 * This represents a site. We always have at least one site.
 */
public class Site implements ById, Initable
{
    // Site id
    @Property(key = "id")
    public String id;

    // Site region
    @Property(key = "region", required = false)
    public String region;

    // Site locale
    @Property(key = "locale", required = false)
    public String locale;

    // active status
    @Property(key = "active", required = false)
    public boolean active;

    // the market share for automatic site mgmt, if not set, we assume 1
    @Property(key = "marketshare", required = false)
    public int marketshare = 1;

    // the derived language from the locale
    private String language;

    /**
     * Default constructor for the automatic setup
     */
    @SuppressWarnings("unused")
    private Site()
    {
        // don't need anything here
    }

    /**
     * Set the site from internal. This is meant to be used of you don't want to configure a site
     * via properties.
     *
     * @param id
     *            the id of the site
     * @param region
     *            the region the site is in
     * @param language
     *            the language of the site
     * @param active
     *            is the site active, does not really make sense from internal, because the active
     *            flag is for automatic site balancing of traffic
     */
    public Site(final String id, final String region, final String locale, final boolean active)
    {
        this.id = id;
        this.region = region;
        this.locale = locale; // as passed e.g. en_US
        this.active = active;
    }

    /**
     * Return the id of this instance
     */
    @Override
    public String getId()
    {
        return id;
    }

    /**
     * Returns the locale fitting the locale. This will be done only once, assuming that nobody
     * changes the site during runtime.
     *
     * @return the language of the locale
     */
    public String language()
    {
        return language == null ?
                        locale != null ?
                                        language = LocaleUtils.toLocale(locale).getLanguage()
                                        : null
                        : language;

    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        String string = id;
        if (region != null)
        {
            string += "/" + region;
        }
        if (locale != null)
        {
            string += "/" + locale;
        }
        return string;
    }

    @Override
    public void init()
    {
    }
}
