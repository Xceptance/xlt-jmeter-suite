package com.xceptance.loadtest.jmeter.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.protocol.http.control.AuthManager;
import org.apache.jmeter.protocol.http.control.Authorization;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.threads.SamplePackage;
import org.htmlunit.HttpMethod;

import com.xceptance.xlt.engine.httprequest.HttpRequest;
import com.xceptance.xlt.engine.httprequest.HttpResponse;

public class HttpRequestHandler
{
    public static SampleResult buildAndExecuteRequest(SampleResult pack, SamplePackage data, String requestName) throws Throwable
    {
        HTTPSamplerProxy sampler = null;
        List<HeaderManager> hm = new ArrayList<>();
        AuthManager am = null;
        
        if (data.getSampler() instanceof HTTPSamplerProxy)
        {
            sampler = (HTTPSamplerProxy) data.getSampler();
        }
        else
        {
            // nothing to do here, probably a script/non executable request
            return pack;
        }
        
        List<ConfigTestElement> configs = data.getConfigs();

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
        
        String method = sampler.getMethod();
        String baseUrl = pack.getUrlAsString();
        
        HttpRequest request = new HttpRequest()
                .timerName(requestName)
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
            addHeaderData(request, hm);
        }
        
        addArgumentData(request, sampler);
        HttpResponse response = request.fire();
        
        // set the response to jmeter results, null for platform default encoding
        pack.setResponseData(response.getContentAsString(), null);
        pack.setResponseHeaders(response.getHeaders().toString());
        pack.setResponseCode(String.valueOf(response.getStatusCode())); // set status code for assertion check
        pack.setResponseMessage(response.getStatusMessage());
        pack.setSuccessful(true); // set the request to success -> otherwise the assertion checker will fail
        
        return pack;
    }
    
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
    
    private static HttpRequest addArgumentData(HttpRequest request, HTTPSamplerProxy requestData)
    {
        // check and add arguments if there are any
        Arguments arguments = requestData.getArguments();
        if (arguments.getArgumentCount() > 0)
        {
            Map<String, String> argumentsAsMap = arguments.getArgumentsAsMap();
            for (Map.Entry<String, String> entry : argumentsAsMap.entrySet())
            {
                request.param(entry.getKey(), entry.getValue());
            };
        }
        return request;
    }
    
    private static HttpRequest addHeaderData(HttpRequest request, List<HeaderManager> headerData)
    {
        // transform header keys/values from loaded data to request confirm data
        headerData.forEach(e ->
        {
            CollectionProperty headers = e.getHeaders();
            headers.forEach(p -> 
            {
                // remove name from the combined value attribute
                request.header(p.getName(), p.getStringValue().replace(p.getName(), "")); 
            });
        });
        return request;
    }
}
