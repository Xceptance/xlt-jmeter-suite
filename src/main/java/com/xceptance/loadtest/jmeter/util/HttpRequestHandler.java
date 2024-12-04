package com.xceptance.loadtest.jmeter.util;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.protocol.http.control.AuthManager;
import org.apache.jmeter.protocol.http.control.Authorization;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.parser.BaseParser;
import org.apache.jmeter.protocol.http.parser.LinkExtractorParseException;
import org.apache.jmeter.protocol.http.parser.LinkExtractorParser;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.http.util.ConversionUtils;
import org.apache.jmeter.protocol.http.util.HTTPConstantsInterface;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.threads.SamplePackage;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.util.JOrphanUtils;
import org.apache.oro.text.MalformedCachePatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;
import org.htmlunit.HttpMethod;

import com.xceptance.xlt.engine.httprequest.HttpRequest;
import com.xceptance.xlt.engine.httprequest.HttpResponse;

/**
 * All request handling is done in this class. Instead of using the JMeter native request handling, the request is transformed in a XLT understandable form.
 * Results from the XTC are are bundles together in a understandable form for JMeter, for further actions.
 */
public class HttpRequestHandler
{
    private static final boolean USE_JAVA_REGEX = !JMeterUtils.getPropDefault(
            "jmeter.regex.engine", "oro").equalsIgnoreCase("oro");

    private static final String RESPONSE_PARSERS = // list of parsers
            JMeterUtils.getProperty("HTTPResponse.parsers");//$NON-NLS-1$

    private static final String USER_AGENT = "User-Agent";

    private static final Map<String, String> PARSERS_FOR_CONTENT_TYPE = new ConcurrentHashMap<>();

    static
    {
        String[] parsers = JOrphanUtils.split(RESPONSE_PARSERS, " ", true);// returns empty array for null
        for(final String parser : parsers)
        {
            String classname = JMeterUtils.getProperty(parser + ".className");//$NON-NLS-1$
            if(classname == null)
            {
                continue;
            }
            String typeList = JMeterUtils.getProperty(parser + ".types");//$NON-NLS-1$
            if(typeList != null)
            {
                String[] types = JOrphanUtils.split(typeList, " ", true);
                for(final String type : types)
                {
                    registerParser(type, classname);
                }
            }
        }
    }

    /**
     * Identify the parser for the current class and content type.
     * 
     * @param contentType
     * @param className
     */
    static void registerParser(String contentType, String className)
    {
        PARSERS_FOR_CONTENT_TYPE.put(contentType, className);
    }

    /**
     * Create the {@link HTTPSampleResult} and set the URL and method for request.
     * 
     * @param url
     * @param method
     * @return HTTPSampleResult
     */
    public static HTTPSampleResult createSampleResult(URL url, String method) 
    {
        HTTPSampleResult res = new HTTPSampleResult();
        res.setHTTPMethod(method);
        res.setURL(url);

        return res;
    }

    /**
     * Uses the data from the {@link SamplePackage} and build the XLT confirm request, in terms of header and authentification. Only request will be handled here
     * for all other types of Sampler, the native JMeter Sampler will be used. After firing the request data is also set in a JMeter understandable way 
     * and additional resources will be loaded, if so configured for the test. 
     * 
     * @param pack
     * @param requestName
     * @return SampleResult
     * @throws Throwable
     */
    public static SampleResult buildAndExecute(SamplePackage pack, String requestName) throws Throwable
    {
        HTTPSamplerProxy sampler = null;
        List<HeaderManager> hm = new ArrayList<>();
        AuthManager am = null;
        SampleResult resultPack = null;
        String method;
        String baseUrl;

        // identify the sampler, only HTTPSamplerProxys will be fired with XLT
        if (pack.getSampler() instanceof HTTPSamplerProxy)
        {
            sampler = (HTTPSamplerProxy) pack.getSampler();
            method = sampler.getMethod();
            baseUrl = sampler.getUrl().toString();

            resultPack = createSampleResult(new URL(baseUrl), method);
        }
        else
        {
            // let JMeter handle other types
            return pack.getSampler().sample(null);
        }

        List<ConfigTestElement> configs = pack.getConfigs();

        // get all relevant config elements
        for (ConfigTestElement element : configs)
        {
            if (element instanceof HeaderManager)
            {
                hm.add((HeaderManager)element);
            }
            if (element instanceof AuthManager)
            {
                am = (AuthManager) element;
            }
        }

        // build the request
        HttpRequest request = new HttpRequest().timerName(requestName)
                                               .baseUrl(baseUrl)
                                               .method(HttpMethod.valueOf(method));

        if (am != null)
        {
            Authorization authForURL = am.getAuthForURL(new URL(baseUrl));
            if (authForURL != null)
            {
                // used from auth manager which also ensure Kerberos connection, needs check if we need this
                // am.getSubjectForUrl(new URL(baseUrl));
                setBasicAuthenticationHeader(request, authForURL.getUser(), authForURL.getPass());
            }
        }

        if (hm != null)
        {
            // add header data
            addHeaderData(request, hm, sampler);
        }

        // add arguments
        addArgumentData(request, sampler);
        // fire the request
        HttpResponse response = request.fire();

        // get the current request header
        Map<String, String> additionalHeaders = response.getWebResponse().getWebRequest().getAdditionalHeaders();
        
        // set the XLT response into JMeter
        resultPack.setResponseData(response.getContentAsString(), null);
        resultPack.setResponseHeaders(response.getHeaders().toString());
        // retrieve the last request header data, important for JMeter assertion handling
        resultPack.setRequestHeaders(additionalHeaders.toString());
        resultPack.setResponseCode(String.valueOf(response.getStatusCode())); // set status code for assertion check
        resultPack.setResponseMessage(response.getStatusMessage());
        resultPack.setSuccessful(true); // set the request to success -> otherwise the assertion checker will fail
        resultPack.setContentType(response.getContentType());

        // embedded resources
        if(sampler.isImageParser())
        {
            HTTPSampleResult res = (HTTPSampleResult) resultPack;
            Iterator<URL> urls = null;
            try
            {
                final byte[] responseData = res.getResponseData();
                if(responseData.length > 0)
                {   // Bug 39205, taken from JMeter
                    final LinkExtractorParser parser = getParser(res);
                    if(parser != null)
                    {
                        String userAgent = getUserAgent(res);
                        urls = parser.getEmbeddedResourceURLs(userAgent, responseData, res.getURL(),
                                                              res.getDataEncodingWithDefault());
                    }
                }
            }
            catch(LinkExtractorParseException e)
            {
                // ignore
            }

            System.out.println("Test embedded downloading");
            HTTPSampleResult lContainer = null;
            if(urls != null && urls.hasNext())
            {
                if(lContainer == null)
                {
                    lContainer = new HTTPSampleResult(res);
                    lContainer.addRawSubResult(res);
                }
                res = lContainer;

                // Get the URL matcher
                JMeterProperty allowRegex = sampler.getPropertyOrNull("HTTPSampler.embedded_url_re");
                Predicate<URL> allowPredicate = null;
                if(allowRegex != null)
                {
                    allowPredicate = generateMatcherPredicate(allowRegex.getStringValue(), "allow", true);
                }
                else
                {
                    allowPredicate = generateMatcherPredicate("", "allow", true);
                }

                JMeterProperty excludeRegex = sampler.getPropertyOrNull("HTTPSampler.embedded_url_exclude_re");
                Predicate<URL> excludePredicate = null;
                if(excludeRegex != null)
                {
                    excludePredicate = generateMatcherPredicate(excludeRegex.getStringValue(), "exclude", false);
                }
                else
                {
                    excludePredicate = generateMatcherPredicate("", "exclude", false);
                }

                while(urls.hasNext())
                {
                    Object binURL = urls.next(); // See catch clause below
                    try
                    {
                        URL url = (URL) binURL;
                        if(url == null)
                        {
                            // log.warn("Null URL detected (should not happen)");
                        }
                        else
                        {
                            try
                            {
                                url = escapeIllegalURLCharacters(url);
                            }
                            catch(Exception e)
                            {
                                /*
                                res.addSubResult(errorResult(new Exception(url.toString() + " is not a correct URI", e),
                                                             new HTTPSampleResult(res)));
                                setParentSampleSuccess(res, false);
                                 */
                                continue;
                            }
                            // log.debug("allowPattern: {}, excludePattern: {}, url: {}", allowRegex, excludeRegex, url);
                            if(!allowPredicate.test(url))
                            {
                                continue; // we have a pattern and the URL does not match, so skip it
                            }
                            if(excludePredicate.test(url))
                            {
                                continue; // we have a pattern and the URL does not match, so skip it
                            }
                            try
                            {
                                url = url.toURI().normalize().toURL();
                            }
                            catch(MalformedURLException | URISyntaxException e)
                            {
                                /*
                                res.addSubResult(
                                        errorResult(new Exception(url.toString() + " URI can not be normalized", e),
                                                    new HTTPSampleResult(res)));
                                setParentSampleSuccess(res, false);
                                 */
                                continue;
                            }

                            new HttpRequest().timerName(requestName).baseUrl(url.toString())
                                             .method(HttpMethod.GET)
                                             .fire();
                        }
                    }
                    catch(ClassCastException e)
                    {
                        /*
                        res.addSubResult(errorResult(new Exception(binURL + " is not a correct URI", e),
                                                     new HTTPSampleResult(res)));
                        setParentSampleSuccess(res, false);
                         */
                    }
                }
            }
        }

        return resultPack;
    }

    /**
     * Handle illegal URL characters for additional resource loading.
     * @param url
     * @return URL
     */
    private static URL escapeIllegalURLCharacters(java.net.URL url)
    {
        if(url == null || "file".equals(url.getProtocol()))
        {
            return url;
        }
        try
        {
            return ConversionUtils.sanitizeUrl(url)
                                  .toURL();
        }
        catch(Exception e1)
        {
            // log.error("Error escaping URL:'{}', message:{}", url, e1.getMessage());
            return url;
        }
    }

    /**
     * Gets parser from {@link HTTPSampleResult#getMediaType()}.
     * Returns null if no parser defined for it
     * @param res {@link HTTPSampleResult}
     * @return {@link LinkExtractorParser}
     * @throws LinkExtractorParseException
     */
    private static LinkExtractorParser getParser(HTTPSampleResult res) throws LinkExtractorParseException
    {
        String parserClassName = PARSERS_FOR_CONTENT_TYPE.get(res.getMediaType());
        if (!StringUtils.isEmpty(parserClassName))
        {
            return BaseParser.getParser(parserClassName);
        }
        return null;
    }

    /**
     * Generate the Predicate for regex matching.
     * 
     * @param regex
     * @param explanation
     * @param defaultAnswer
     * @return Predicate
     */
    private static Predicate<URL> generateMatcherPredicate(String regex, String explanation, boolean defaultAnswer) 
    {
        if (StringUtils.isEmpty(regex))
        {
            return s -> defaultAnswer;
        }
        if (USE_JAVA_REGEX)
        {
            try
            {
                java.util.regex.Pattern pattern = JMeterUtils.compilePattern(regex);
                return s -> pattern.matcher(s.toString()).matches();
            }
            catch (PatternSyntaxException e)
            {
                //log.warn("Ignoring embedded URL {} string: {}", explanation, e.getMessage());
                return s -> defaultAnswer;
            }
        }
        try
        {
            Pattern pattern = JMeterUtils.getPattern(regex);
            Perl5Matcher matcher = JMeterUtils.getMatcher();
            return s -> matcher.matches(s.toString(), pattern);
        }
        catch (MalformedCachePatternException e)
        { // NOSONAR
            return s -> defaultAnswer;
        }
    }

    /**
     * Return the user agent.
     * 
     * @param sampleResult
     * @return String user agent
     */
    private static String getUserAgent(HTTPSampleResult sampleResult)
    {
        String res = sampleResult.getRequestHeaders();
        int index = res.indexOf(USER_AGENT);
        if(index >= 0)
        {
            // see HTTPHC3Impl#getConnectionHeaders
            // see HTTPHC4Impl#getConnectionHeaders
            // see HTTPJavaImpl#getConnectionHeaders
            //': ' is used by JMeter to fill-in requestHeaders, see getConnectionHeaders
            final String userAgentPrefix = USER_AGENT;
            String userAgentHdr = res.substring(index + userAgentPrefix.length(), res.indexOf(index +
                                                                                              userAgentPrefix.length() +
                                                                                              1));
            return userAgentHdr.trim();
        }
        else
        {
            // log.info("No user agent extracted from requestHeaders:{}", res);
            return null;
        }
    }

    /**
     * If username and password are set for basic authorization, Base64 encoded user header is set.
     * 
     * @param request
     * @param username
     * @param password
     */
    private static void setBasicAuthenticationHeader(HttpRequest request, final String username, final String password)
    {
        // Is a username for Basic Authentication configured?
        if (StringUtils.isNotBlank(username))
        {
            // Set the request header.
            final String userPass = username + ":" + password;
            final String userPassBase64 = Base64.encodeBase64String(userPass.getBytes());

            request.header("Authorization", "Basic " + userPassBase64);
        }
    }
    
    /**
     * Add all arguments to the request. Arguments can be found in the used jmx file. Arguments are automaticaly organized into post and url parameter.
     * 
     * @param request
     * @param requestData
     * @return HttpRequest
     */
    private static HttpRequest addArgumentData(HttpRequest request, HTTPSamplerProxy requestData)
    {
        // check and add arguments if there are any
        Arguments arguments = requestData.getArguments();
        boolean postBodyRaw = requestData.getPostBodyRaw();
        if (arguments.getArgumentCount() > 0 && !postBodyRaw)
        {
            Map<String, String> argumentsAsMap = arguments.getArgumentsAsMap();
            for (Map.Entry<String, String> entry : argumentsAsMap.entrySet())
            {
                request.param(entry.getKey(), entry.getValue());
            };
        }
        else if (postBodyRaw)
        {
            // add the raw request body to the request, values are saved in LinkedHashMap
            Map<String, String> argumentsAsMap = arguments.getArgumentsAsMap();
            request.body(String.valueOf(argumentsAsMap.entrySet().iterator().next().getValue()));
        }
        return request;
    }
    
    /**
     * Add all global and request specific header data. Depending on the order of request the header values can already contain data.
     * 
     * @param request
     * @param headerData
     * @return
     */
    private static HttpRequest addHeaderData(HttpRequest request, List<HeaderManager> headerData, HTTPSamplerProxy sampler)
    {
        // transform header keys/values from loaded data to request confirm data
        headerData.forEach(e ->
        {
            // in case we have several header manager, add all values
            for (int index = 0; index < e.size(); index++)
            {
                // Multipart handling with additional Content type handling
                if (HTTPConstantsInterface.HEADER_CONTENT_TYPE.equals(e.get(index).getName()) &&
                    sampler.getUseMultipart())
                {
                    request.header(HTTPConstantsInterface.HEADER_CONTENT_TYPE, HTTPConstantsInterface.MULTIPART_FORM_DATA + "; " + e.get(index).getValue());
                }
                else
                {
                    // add the header values
                    request.header(e.get(index).getName(), e.get(index).getValue());
                }
            }
//            CollectionProperty headers = e.getHeaders();
//            headers.forEach(p ->
//            {
//                // remove name from the combined value attribute
//                request.header(p.getName(), p.getStringValue().replace(p.getName(), ""));
//            });
        });
        return request;
    }
}
