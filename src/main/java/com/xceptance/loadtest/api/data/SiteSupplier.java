package com.xceptance.loadtest.api.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.xceptance.loadtest.api.util.Context;
import com.xceptance.xlt.api.util.XltRandom;

/**
 * Will return either a random site based on the random distribution or a fixed one if you ask it by
 * id
 *
 * @author Rene Schwietzke
 */
public class SiteSupplier
{
    private static List<Site> getSitesAndMarketShare()
    {
        final List<Site> sites = new ArrayList<>(100);

        // ok, setup the sites
        for (final Site site : Context.defaultConfiguration.get().sites.unweightedList())
        {
            // if the site is not active, ignore it
            if (site.active == false)
            {
                continue;
            }

            // add the site as often as the market share indicates
            for (int i = 0; i < site.marketshare; i++)
            {
                sites.add(site);
            }
        }

        return sites;
    }

    /**
     * Instance based random site calculation
     *
     * @return a random site by initial weight chosen
     */
    private static Optional<Site> getRandomSite()
    {
        final List<Site> sites = getSitesAndMarketShare();

        if (sites.isEmpty())
        {
            return Optional.empty();
        }
        else
        {
            return Optional.of(sites.get(XltRandom.nextInt(sites.size())));
        }

    }

    /**
     * Return a random site based on the active sites and their marketshare
     *
     * @return a random site
     */
    public static Optional<Site> randomSite()
    {
        return getRandomSite();
    }

    /**
     * Returns a site by id or any empty optional
     *
     * @param id
     *            the site id
     * @return the site with this id or an empty optional
     */
    public static Optional<Site> siteById(final String id)
    {
        return Context.defaultConfiguration.get().sites.getById(id);
    }
}
