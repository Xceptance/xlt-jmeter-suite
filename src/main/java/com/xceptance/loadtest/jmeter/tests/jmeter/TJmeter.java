package com.xceptance.loadtest.jmeter.tests.jmeter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;

import com.xceptance.loadtest.api.data.DataFileProvider;
import com.xceptance.loadtest.api.data.NonSiteRelatedTest;
import com.xceptance.loadtest.api.tests.JMeterTestCase;
import com.xceptance.loadtest.api.util.HttpRequestJmeter;
import com.xceptance.loadtest.api.util.SearchHelper;


public class TJmeter extends JMeterTestCase implements NonSiteRelatedTest
{
    private HashTree tree;

    public TJmeter()
    {
        Optional<File> jmeterPorperties = DataFileProvider.dataFile("jmeter.properties");
        
        JMeterUtils.loadJMeterProperties(jmeterPorperties.get().getAbsolutePath());
//        JMeterUtils.setJMeterHome("C:\\Data\\apache-jmeter-5.6.3\\"); -> needed for .jmx file loading
        
        Optional<File> testPlan = DataFileProvider.dataFile("Test Plan.jmx");
        
        try
        {
            tree = SaveService.loadTree(testPlan.get());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        SearchHelper<TransactionController> searcher = new SearchHelper<TransactionController>(TransactionController.class);
        tree.traverse(searcher);
        
        setTestName(getTestName() + "_" + searcher.getSearchResults().iterator().next().getName());
    }
    
    /**
     * Using the http://jsonplaceholder.typicode.com/
     *
     * @throws Throwable
     */
    @Override
    public void test() throws Throwable
    {
//        JMeterTreeModel treeModel = new JMeterTreeModel();
//        JMeterTreeNode root = (JMeterTreeNode) treeModel.getRoot();
        
        SearchHelper<Arguments> argumentSearch = new SearchHelper<Arguments>(Arguments.class);
        tree.traverse(argumentSearch);
        
        // search helper, not in order
        SearchHelper<HTTPSamplerProxy> searcher = new SearchHelper<HTTPSamplerProxy>(HTTPSamplerProxy.class);
        tree.traverse(searcher);
        
        // get all request values at least -> but without header values kinda useless
        IdentityHashMap<Object, ListedHashTree> allSubtress = searcher.getAllSubtress();
        
        searcher.getSearchResults();
        
        // main elements, unsorted in reference to jmx file !  
        Iterator<ListedHashTree> iterator = allSubtress.values().iterator();
        
        
        do
        {
            // first element -> sorting via rules?
            ListedHashTree next = iterator.next();
            
            // better access?
            ArrayList<?> requestData = new ArrayList<>(Arrays.asList(next.getArray()));
            
            HTTPSamplerProxy httpDetailData = (HTTPSamplerProxy) requestData.get(0);
            
            Set<Object> keySet = next.keySet();
            HashTree treeData = next.getTree(keySet);
            
            ArrayList<?> headerData = new ArrayList<>(Arrays.asList(treeData.getArray()));
            HeaderManager headerDetaildata = (HeaderManager) headerData.get(0);
            
            buildRequest(httpDetailData, headerDetaildata);
        }
        while(iterator.hasNext());
    }
    
    public void buildRequest(HTTPSamplerProxy requestData, HeaderManager headerData) throws Throwable
    {
        String protocol = requestData.getProtocol();
        String domain = requestData.getDomain();
        String path = requestData.getPath();
        
        // TODO url builder
        HttpRequestJmeter request = new HttpRequestJmeter(path)
                .baseUrl(protocol +"://"+ domain + path)
                .method(requestData.getMethod())
                .assertStatus(200);
                
        CollectionProperty headers = headerData.getHeaders();
        headers.forEach(p -> {
            request.header(p.getName(), p.getStringValue());
        });
        
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
}

