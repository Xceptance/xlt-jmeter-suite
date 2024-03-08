package com.xceptance.loadtest.jmeter.tests;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.protocol.http.control.AuthManager;
import org.apache.jmeter.protocol.http.control.Authorization;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jorphan.collections.HashTree;
import org.htmlunit.HttpMethod;
import org.junit.Assert;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.xceptance.loadtest.api.data.DataFileProvider;
import com.xceptance.loadtest.api.data.NonSiteRelatedTest;
import com.xceptance.loadtest.api.tests.JMeterTestCase;
import com.xceptance.loadtest.api.util.Actions;
import com.xceptance.loadtest.api.util.Context;
import com.xceptance.loadtest.api.util.SearchHelper;
import com.xceptance.loadtest.api.util.SearchHelperDynamic;
import com.xceptance.xlt.engine.httprequest.HttpRequest;
import com.xceptance.xlt.engine.httprequest.HttpResponse;


public class TJmeterDyn extends JMeterTestCase implements NonSiteRelatedTest
{
    private HashTree tree;
    private String scheme;
    private String jmeterVariable = "${%s}";
    private Map<String, String> variableMap;
    private String fileName;
    private AuthManager authResults;
    
    private HeaderManager headerData = null;
    private HTTPSamplerProxy requestData = null;
    private TransactionController action = null;
    private String name;
    private HttpResponse strucutredRequest;
    
    public TJmeterDyn()
    {
        try
        {
            super.init();
//            fileName = "CustomerAuthorization.jmx";
//            fileName = "GuestOrder.jmx";
            fileName = "CustomerAuthorizationExtended.jmx";
            
            Optional<File> testPlan = DataFileProvider.dataFile(fileName);
            Assert.assertTrue("The "+ fileName +" file could not be found.", testPlan.isPresent());
            
            // load the jmx file into a HashTree structure
            tree = SaveService.loadTree(testPlan.get());
        } 
        catch (Throwable e)
        {
            e.printStackTrace();
        }
        
        // remove file ending for naming 
        setTestName(getTestName() + "_" + fileName.replace(".jmx", ""));
    }
    
    /**
     * 
     *
     * @throws Throwable
     */
    @Override
    public void test() throws Throwable
    {
        // TODO: bundle and rework data acquirement 
        
        // search the arguments, simple search
        SearchHelper<Arguments> argumentSearch = new SearchHelper<Arguments>(Arguments.class);
        tree.traverse(argumentSearch);

        // value from test configuration, before request details
        Collection<Arguments> argumentResult = argumentSearch.getSearchResults();
        Arguments arguments = argumentResult.iterator().next();
        // retrieve the argument map, holds all variables during the execution and get dynamically updated
        variableMap = arguments.getArgumentsAsMap();
        
        SearchHelper<AuthManager> authManager = new SearchHelper<AuthManager>(AuthManager.class);
        tree.traverse(authManager);
        
        Collection<AuthManager> authManagerResults = authManager.getSearchResults();
        if (authManagerResults != null && !authManagerResults.isEmpty())
        {
            authResults = authManagerResults.iterator().next();
        }
        
        // TODO: get the protocol
        scheme = variableMap.get("scheme") != null ? variableMap.get("scheme") : variableMap.get("protocol");
        
        // TODO protocols assertion
        Assert.assertTrue("Only HTTP or HTTPS are allowed as protocol.", PROTOCOLS.test(scheme));
        
        SearchHelperDynamic shd = new SearchHelperDynamic();
        tree.traverse(shd);
        
        LinkedHashMap<String, Object> results = shd.getResults();
        Multimap<String, Object> map = LinkedHashMultimap.create();
        
        // building the action related groups for requests
        results.entrySet().forEach(e ->
        {
            if (e.getValue() instanceof TransactionController)
            {
               action = (TransactionController) e.getValue();
               name = action.getName();
            }
            
            if (StringUtils.isNotBlank(name) &&
                e.getValue() instanceof TransactionController == false)
            {
                map.put(name, e.getValue());
            }
        });
        
        // fire the requests under the according action
        map.keySet().forEach(e ->
        {
            try
            {
                Actions.run(e, p ->
                {
                    map.entries().forEach(d -> 
                    {
                        Object value = d.getValue();
                        
                      if (value instanceof HTTPSamplerProxy)
                      {
                          requestData = (HTTPSamplerProxy) value;
                      }
                      if (value instanceof HeaderManager)
                      {
                          headerData = (HeaderManager) value;
                      }
                        
                      if (requestData != null && headerData != null)
                      {
                          try
                          {
                              strucutredRequest = buildStrucutredRequest(requestData, headerData, p);
                          } 
                          catch (Throwable e1)
                          {
                              // TODO Auto-generated catch block
                              e1.printStackTrace();
                          }
                          requestData = null;
                          headerData = null;
                      }
                        
                      if (value instanceof RegexExtractor)
                      {
                          applyRegexExtractor((RegexExtractor)value, strucutredRequest);
                      }
                        
                    });
                });
            } 
            catch (Throwable e1)
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        });
    }
    
    public HttpResponse buildStrucutredRequest(HTTPSamplerProxy sampleProxy, HeaderManager header, String requestName) throws Throwable
    {
        HTTPSamplerProxy firstClass = sampleProxy;
        HeaderManager secondClass = header;
        
        String domain = firstClass.getDomain();
        String path = firstClass.getPath();
        String method = firstClass.getMethod();
        
        String baseUrl = scheme + "://" + domain + path;
        
        HttpRequest request = new HttpRequest()
                .timerName(requestName)
                .baseUrl(baseUrl)
                .method(HttpMethod.valueOf(method));
        
        if (authResults != null)
        {
            Authorization authForURL = authResults.getAuthForURL(new URL(baseUrl));
            if (authForURL != null)
            {
                setBasicAuthenticationHeader(request, authForURL.getUser(), authForURL.getPass());
            }
        }
        
        // add header data
        addHeaderData(request, secondClass);
        // add arguments, all query and/or post parameter
        addArgumentData(request, firstClass);
        
        HttpResponse response = request.fire();
        
        // check if the request was successful
        response.checkStatusCode(200);

        return response;
    }
    
    private void applyRegexExtractor(RegexExtractor regexData, HttpResponse response)
    {
        // simple pattern retrieve from file and regex
        String contentAsString = response.getContentAsString();
        int matchNumber = regexData.getMatchNumber();
        Pattern p = Pattern.compile(regexData.getRegex());
        Matcher matcher = p.matcher(contentAsString);
        
        // initialize string value
        String group = "";
        if (matcher.find())
        {
            // random value is 0 in JMeter, we simply take the first encounter in this case
            group = matcher.group(matchNumber == 0 ? 1 : matchNumber);
        }
        else
        {
            // print assertion message which is defined in jmx file
            Assert.fail(regexData.getDefaultValue());
        }
        // save the reference (key) and the extracted value in the arguments map
        variableMap.put(String.format(jmeterVariable, regexData.getRefName()), group);
    }
    
    private HttpRequest addHeaderData(HttpRequest request, HeaderManager headerData)
    {
        // transform header keys/values from loaded data to request confirm data
        CollectionProperty headers = headerData.getHeaders();
        headers.forEach(p -> 
        {
            // remove name from the combined value attribute
            request.header(p.getName(), p.getStringValue().replace(p.getName(), "")); 
            Set<String> keySet = variableMap.keySet();
            
            keySet.forEach(k ->
            {
               if (p.getStringValue().contains(k))
               {
                   // remove name from the combined value attribute
                   String preRefinedString = p.getStringValue().replace(p.getName(), ""); 
                   // replace and add jmeter variables
                   String replace = StringUtils.replaceOnceIgnoreCase(preRefinedString, 
                                                                      String.format(jmeterVariable, k),
                                                                      variableMap.get(k));
                   request.header(p.getName(), replace);
               }
            });
        });
        return request;
    }
    
    private HttpRequest addArgumentData(HttpRequest request, HTTPSamplerProxy requestData)
    {
        // check and add arguments if there are any
        Arguments arguments = requestData.getArguments();
        if (arguments.getArgumentCount() > 0)
        {
            Map<String, String> argumentsAsMap = arguments.getArgumentsAsMap();
            for (Map.Entry<String, String> entry : argumentsAsMap.entrySet())
            {
                if (variableMap.containsKey(entry.getValue()))
                {
                    request.param(entry.getKey(), variableMap.get(entry.getValue()));
                }
                else
                {
                    request.param(entry.getKey(), entry.getValue());
                }
            };
        }
        return request;
    }
    
    public void setBasicAuthenticationHeader(HttpRequest request, final String username, final String password)
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
     * {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        super.tearDown();

        // if you don't close it, it can reuse the connection and the negotiated keys of TLS
        // that is about 100x (!) faster than closing... but you have state of course, your call!!!!
        this.closeWebClient();

        // you can do alternatively just cleaning of the cookie state if you have any, if you
        // don't have any... don't run that code, because performance testing is performance
        // programming
        // this.clearCookies();
    }
    
    /**
     * Filter predicate for protocols. Only allowed are http and https
     */
    public static Predicate<String> PROTOCOLS = e ->
    {
        List<String> allowedProtocols = Context.get().configuration.allowedProtocols.list;
        
        Optional<String> allowed = allowedProtocols.stream().filter(l -> l.toLowerCase().equals(e)).findFirst();
        
        return allowed.isPresent();
    };
}