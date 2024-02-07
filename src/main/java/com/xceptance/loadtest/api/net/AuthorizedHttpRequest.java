package com.xceptance.loadtest.api.net;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import org.htmlunit.WebClient;
import com.xceptance.loadtest.api.util.Context;
import com.xceptance.xlt.engine.httprequest.HttpRequest;
import com.xceptance.xlt.engine.httprequest.HttpResponse;

/**
 * Adds the authorization automatically to this request. Assumes that this information is available
 * in the Context or in the configuration it will fail.
 *
 * @author Rene Schwietzke
 */
public class AuthorizedHttpRequest extends HttpRequest
{
    @Override
    public HttpResponse fire() throws IOException, URISyntaxException
    {
        authorize();
        return super.fire();
    }

    @Override
    public HttpResponse fire(final WebClient client) throws IOException, URISyntaxException
    {
        authorize();
        return super.fire(client);
    }

    private void authorize()
    {
        // check for specifically set authorization for this test case run first
        // Context.get().data.authorization
        // next take anything from external Context.configuration().authorization aka
        // "general.authorization"
        header("Authorization", Context.get().data.authorization
                        .orElseGet(() -> Optional.ofNullable(Context.configuration().authorization).orElseThrow()));
    }
}
