package com.xceptance.loadtest.jmeter.tests.jmeter;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.htmlunit.HttpMethod;
import org.junit.Assert;

import com.xceptance.loadtest.api.data.DataFileProvider;
import com.xceptance.loadtest.api.data.NonSiteRelatedTest;
import com.xceptance.loadtest.api.data.SearchClass;
import com.xceptance.loadtest.api.tests.JMeterTestCase;
import com.xceptance.loadtest.api.util.Actions;
import com.xceptance.loadtest.api.util.HttpRequestJmeter;
import com.xceptance.loadtest.api.util.SearchHelper;
import com.xceptance.loadtest.api.util.SearchHelperStructured;
import com.xceptance.xlt.engine.httprequest.HttpRequest;


public class TJmeterStructured extends JMeterTestCase implements NonSiteRelatedTest
{
    private HashTree tree;
    private String scheme;
    private String jmeterVariable = "${%s}";
    private Map<String, String> argumentsAsMap;

    public TJmeterStructured()
    {
        Optional<File> jmeterPorperties = DataFileProvider.dataFile("jmeter.properties");
        Optional<File> upgradeProperties = DataFileProvider.dataFile("upgrade.properties");
        Optional<File> saveServiceProperties = DataFileProvider.dataFile("saveservice.properties");
        
        // set minimum properties for tree loading/parsing
        JMeterUtils.loadJMeterProperties(jmeterPorperties.get().getAbsolutePath());
        JMeterUtils.setProperty("upgrade_properties", upgradeProperties.get().getAbsolutePath());
        JMeterUtils.setProperty("saveservice_properties", saveServiceProperties.get().getAbsolutePath());
        
//        Optional<File> testPlan = DataFileProvider.dataFile("SimpleAddToCart.jmx");
//        Optional<File> testPlan = DataFileProvider.dataFile("Test Plan.jmx");
        Optional<File> testPlan = DataFileProvider.dataFile("GuestOrder.jmx");
        
        try
        {
            // load the jmx file into a HashTree structure
            tree = SaveService.loadTree(testPlan.get());
        } 
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        setTestName(getTestName() + "_" + "SimpleAddToCart");
    }
    
    /**
     * 
     *
     * @throws Throwable
     */
    @Override
    public void test() throws Throwable
    {
        SearchHelper<Arguments> argumentSearch = new SearchHelper<Arguments>(Arguments.class);
        tree.traverse(argumentSearch);

        // value from test configuration, before request details
        Collection<Arguments> argumentResult = argumentSearch.getSearchResults();
        Arguments next = argumentResult.iterator().next();
        argumentsAsMap = next.getArgumentsAsMap();
        scheme = argumentsAsMap.get("scheme");
        
        // TODO properties values for allowed protocols
        Assert.assertTrue("Only HTTP or HTTPS are allowed as protocol.", PROTOCOLS.test(scheme));
        
        SearchHelperStructured<TransactionController, HTTPSamplerProxy, HeaderManager> searchHelperStructured = new SearchHelperStructured<TransactionController, HTTPSamplerProxy, HeaderManager>(TransactionController.class,HTTPSamplerProxy.class,HeaderManager.class);
        tree.traverse(searchHelperStructured);
        
        LinkedHashMap<TransactionController, List<SearchClass<HTTPSamplerProxy, HeaderManager>>> structuredResult = searchHelperStructured.getStructuredResult();
        
        // main elements, sorted in reference to jmx file  
        Set<Entry<TransactionController, List<SearchClass<HTTPSamplerProxy, HeaderManager>>>> entrySet = structuredResult.entrySet();
        
        entrySet.forEach(i -> 
        {
          try
          {
              String name = i.getKey().getName();
              Actions.run(name, t ->
              {
                  i.getValue().forEach(p -> 
                  {
                      try
                      {
                          buildStrucutredRequest(p.firstClass, p.secondClass, t);
                      } 
                      catch (Throwable e)
                      {
                          // TODO Auto-generated catch block
                          e.printStackTrace();
                      }
                  });
              });
            } 
            catch (Throwable e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
    }
    
    public void buildStrucutredRequest(HTTPSamplerProxy requestData, HeaderManager headerData, String t) throws Throwable
    {
        String domain = requestData.getDomain();
        String path = requestData.getPath();
        String method = requestData.getMethod();
        
        // TODO url builder?
        HttpRequest request = new HttpRequest()
                .timerName(t)
                .baseUrl(scheme + "://" + domain + path)
                .method(HttpMethod.valueOf(method));
        
        // transform header keys/values from loaded data to request confirm data
        CollectionProperty headers = headerData.getHeaders();
        headers.forEach(p -> 
        {
            // remove name from the combined value attribute
            request.header(p.getName(), p.getStringValue().replace(p.getName(), "")); 
            Set<String> keySet = argumentsAsMap.keySet();
            
            keySet.forEach(k ->
            {
               if (p.getStringValue().contains(k))
               {
                   // remove name from the combined value attribute
                   String preRefinedString = p.getStringValue().replace(p.getName(), ""); 
                   // replace and add jmeter variables
                   String replace = StringUtils.replaceOnceIgnoreCase(preRefinedString, 
                                                                      String.format(jmeterVariable, k),
                                                                      argumentsAsMap.get(k));
                   request.header(p.getName(), replace);
               }
            });
        });
        
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
        
        request.fire();   
    }
    
    public void buildRequest(HTTPSamplerProxy requestData, HeaderManager headerData) throws Throwable
    {
        String domain = requestData.getDomain();
        String path = requestData.getPath();
        String method = requestData.getMethod();
        
        // TODO url builder?
        HttpRequestJmeter request = new HttpRequestJmeter(requestData.getName())
                .baseUrl(scheme + "://" + domain + path)
                .method(method)
                .assertStatus(200);
        
        // transform header keys/values from loaded data to request confirm data
        CollectionProperty headers = headerData.getHeaders();
        headers.forEach(p -> 
        {
            // remove name from the combined value attribute
            request.header(p.getName(), p.getStringValue().replace(p.getName(), "")); 
            Set<String> keySet = argumentsAsMap.keySet();
            
            keySet.forEach(k ->
            {
               if (p.getStringValue().contains(k))
               {
                   // remove name from the combined value attribute
                   String preRefinedString = p.getStringValue().replace(p.getName(), ""); 
                   // replace and add jmeter variables
                   String replace = StringUtils.replaceOnceIgnoreCase(preRefinedString, 
                                                                      String.format(jmeterVariable, k),
                                                                      argumentsAsMap.get(k));
                   request.header(p.getName(), replace);
               }
            });
        });
        
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
        request.run();
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
        // this.closeWebClient();

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
        // only 2 protocol types are allowed
        if (e.toLowerCase().equals("https"))
        {
            return true;
        }
        else if (e.toLowerCase().equals("http"))
        {
            return true;
        }
        return false;
    };
}
