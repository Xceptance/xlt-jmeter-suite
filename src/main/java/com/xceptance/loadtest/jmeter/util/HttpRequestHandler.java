/*
 * Copyright (c) 2005-2025 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xceptance.loadtest.jmeter.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.http.control.AuthManager;
import org.apache.jmeter.protocol.http.control.Authorization;
import org.apache.jmeter.protocol.http.control.Cookie;
import org.apache.jmeter.protocol.http.control.CookieManager;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.parser.BaseParser;
import org.apache.jmeter.protocol.http.parser.LinkExtractorParseException;
import org.apache.jmeter.protocol.http.parser.LinkExtractorParser;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.http.util.ConversionUtils;
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.apache.jmeter.protocol.http.util.HTTPConstantsInterface;
import org.apache.jmeter.protocol.http.util.HTTPFileArg;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.services.FileServer;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.threads.SamplePackage;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.util.JOrphanUtils;
import org.apache.oro.text.MalformedCachePatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;
import org.htmlunit.HttpMethod;

import com.xceptance.loadtest.data.util.Context;
import com.xceptance.loadtest.data.util.HttpRequestJmeter;
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
        CookieManager cm = null;
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

            resultPack = createSampleResult(new URI(baseUrl).toURL(), method);
        }
        else
        {
            // let JMeter handle other types
            return pack.getSampler().sample(null);
        }
        
        // Handle the different manager for HttpRequests
        // avoid null or empty list
        if (sampler.getHeaderManager() != null)
        {
            hm.add(sampler.getHeaderManager());
        }
        am = sampler.getAuthManager();
        cm = sampler.getCookieManager();

        // build the request
        HttpRequestJmeter request = new HttpRequestJmeter().timerName(requestName)
                                               .baseUrl(baseUrl)
                                               .method(HttpMethod.valueOf(method));

        if (am != null)
        {
            Authorization authForURL = am.getAuthForURL(new URI(baseUrl).toURL());
            if (authForURL != null)
            {
                // used from auth manager which also ensure Kerberos connection, needs check if we need this
                // am.getSubjectForUrl(new URL(baseUrl));
                setBasicAuthenticationHeader(request, authForURL.getUser(), authForURL.getPass());
            }
        }

        if (hm != null)
        {
            // JMter managed header, we do not rely on the header Manager from XLT in case we have a managed header!
            request.additonalHeader(false);
            request.removeHeaders();
            
            // add header data
            addHeaderData(request, hm, sampler);
        }
        
        if (cm != null)
        {
            handleCookies(cm);
        }

        // add arguments
        addArgumentData(request, sampler);
        
        // check if we have a file upload and transform the request accordingly
        handleFileUpload(request, sampler);

        // check if the redirects are enabled or not
        if (sampler.getFollowRedirects() ||
            sampler.getAutoRedirects())
        {
            HttpRequestJmeter.getDefaultWebClient().getOptions().setRedirectEnabled(true);
        }
        
        // fire the request
        HttpResponse response = request.fire();

        // get the current request header
        Map<String, String> additionalHeaders = response.getWebResponse().getWebRequest().getAdditionalHeaders();

        // set the XLT response into JMeter
        resultPack.setResponseData(response.getContentAsString(), null);
        // map the data into the CRLF standard, which is needed for JMeter to handle the data
        resultPack.setResponseHeaders(response.getHeaders().stream().map(n -> n.toString()).collect(Collectors.joining("\r\n")));
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
                                continue;
                            }
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
                                continue;
                            }

                            new HttpRequestJmeter().timerName(requestName).baseUrl(url.toString())
                                             .method(HttpMethod.GET)
                                             .fire();
                        }
                    }
                    catch(ClassCastException e)
                    {
                        // ignore
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
            Optional<String> first = Stream.of(res.split(",")).filter(p -> p.contains(USER_AGENT)).findFirst();
            
            if (first.isPresent())
            {
                return first.get();
            }
            
            // take the set user agent as fallback
            return Context.get().configuration.userAgent;
        }
        else
        {
            // log.info("No user agent extracted from requestHeaders:{}", res);
            return null;
        }
    }

    /**
     * If user name and password are set for basic authorization, Base64 encoded user header is set.
     *
     * @param request
     * @param username
     * @param password
     */
    private static void setBasicAuthenticationHeader(HttpRequestJmeter request, final String username, final String password)
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
    private static void addArgumentData(HttpRequestJmeter request, HTTPSamplerProxy requestData)
    {
        // check and add arguments if there are any
        Arguments arguments = requestData.getArguments();
        boolean postBodyRaw = requestData.getPostBodyRaw();
        boolean useEncoding = false;
        if (arguments.getArgumentCount() > 0 && !postBodyRaw)
        {
            // to get the correct argument and flag
            int index = 0;
            Map<String, String> argumentsAsMap = arguments.getArgumentsAsMap();
            for (Map.Entry<String, String> entry : argumentsAsMap.entrySet())
            {
                // the strip() is needed if we have groovy script created variables!
                request.param(entry.getKey(), entry.getValue().strip());
                HTTPArgument arg = (HTTPArgument) arguments.getArgument(index);
                useEncoding |= arg.isAlwaysEncoded();
                index++;
            };
        }
        else if (postBodyRaw)
        {
            // add the raw request body to the request, values are saved in LinkedHashMap
            Map<String, String> argumentsAsMap = arguments.getArgumentsAsMap();
            request.body(String.valueOf(argumentsAsMap.entrySet().iterator().next().getValue()));
        }

        request.urlEncodingDesired(useEncoding);
    }

    /**
     * Add all global and request specific header data. Depending on the order of request the header values can already contain data.
     *
     * @param request
     * @param headerData
     * @return
     */
    private static void addHeaderData(HttpRequestJmeter request, List<HeaderManager> headerData, HTTPSamplerProxy sampler)
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
                    // add the header values, if they are already resolved to variables, may be in scripts later in the flow
                    if (!e.get(index).getName().contains("${") && 
                        !e.get(index).getValue().contains("${"))
                    {
                        request.header(e.get(index).getName(), e.get(index).getValue());
                    }
                }
            }
        });
    }
    
    /**
     * Handle all conversion and adjustments in case there is file upload involved. This method take care of multipart boundary handling
     * header and parameter adjustments/conversation. 
     * 
     * @param request current request to build
     * @param requestData JMeter request data
     * @throws Exception
     */
    private static void handleFileUpload(HttpRequestJmeter request, HTTPSamplerProxy requestData) throws Exception
    {
        // check of we need to handle file upload, otherwise simply return the request
        if (requestData.getHTTPFileCount() > 0 &&
            requestData.getDoMultipart())
        {
            MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
            
            // check and set browser compatible mode
            if (requestData.getDoBrowserCompatibleMultipart())
            {
                multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            }
            
            // transform  parameter since they get are part of the post body
            if (requestData.getArguments().getArgumentCount() > 0)
            {
                Map<String, String> argumentsAsMap = requestData.getArguments().getArgumentsAsMap();
                for (Map.Entry<String, String> entry : argumentsAsMap.entrySet())
                {
                    multipartEntityBuilder.addTextBody(entry.getKey(), entry.getValue());
                };
            }
            
            // remove params, since they are part of the body
            request.removeParams();
            
            // iterate over the files for upload
            for (int index = 0; index < requestData.getHTTPFileCount(); index++)
            {
                // get file arguments
                HTTPFileArg httpFileArg = requestData.getHTTPFiles()[index];
                
                // get the resolved file from jmeter file server
                File resolvedFile = FileServer.getFileServer().getResolvedFile(httpFileArg.getPath());
                
                // build the binary body
                final InputStream targetStream =  new FileInputStream(resolvedFile);
                multipartEntityBuilder.addBinaryBody(httpFileArg.getParamName(), 
                                                     targetStream, 
                                                     ContentType.create(httpFileArg.getMimeType()), 
                                                     httpFileArg.getPath());
            }
            
            HttpEntity httpEntity = multipartEntityBuilder.build();
            
            // write the body data
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            httpEntity.writeTo(os);
            os.flush();
            
            // overwrite content type and generate the boundary
            request.header(HTTPConstantsInterface.HEADER_CONTENT_TYPE, httpEntity.getContentType().getValue())
            .body(os.toByteArray());
        }
    }
    
    /**
     * Use the JMeter {@link CookieManager} which has the data from the jmx file and convert it into data which is compatible with XLT.
     * 
     * @param cm JMeter CookieManager
     */
    private static void handleCookies(CookieManager cm)
    {
        // XLT web driver cookie manager
        org.htmlunit.CookieManager cookieManager = HttpRequestJmeter.getDefaultWebClient().getCookieManager();

        for (int index = 0; index < cm.getCookieCount(); index++)
        {
            // get the JMeter cookie and 
            Cookie cookie = (Cookie) cm.getCookies().get(index).getObjectValue();
            
            cookieManager.addCookie(new org.htmlunit.util.Cookie
            (
                cookie.getDomain(), // domain
                cookie.getName(),   // name
                cookie.getValue(),  // value
                cookie.getPath(),   // path
                Integer.MAX_VALUE,  // maxAge, internal handled but without any time this will prevent cookie usage
                cookie.getSecure()  // secure
            ));
        }
    }
}
