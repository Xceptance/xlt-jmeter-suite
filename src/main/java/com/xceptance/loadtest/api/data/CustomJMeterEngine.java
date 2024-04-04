package com.xceptance.loadtest.api.data;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.control.Controller;
import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.control.TransactionSampler;
import org.apache.jmeter.engine.PreCompiler;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.engine.TurnElementsOn;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.processor.PostProcessor;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.protocol.http.control.AuthManager;
import org.apache.jmeter.protocol.http.control.Authorization;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleListener;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testbeans.TestBeanHelper;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestIterationListener;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.FindTestElementsUpToRootTraverser;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContext.TestLogicalAction;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.threads.ListenerNotifier;
import org.apache.jmeter.threads.PostThreadGroup;
import org.apache.jmeter.threads.SamplePackage;
import org.apache.jmeter.threads.SetupThreadGroup;
import org.apache.jmeter.threads.TestCompiler;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.apache.jorphan.util.JMeterStopTestException;
import org.apiguardian.api.API;
import org.htmlunit.HttpMethod;
import org.junit.Assert;

import com.xceptance.loadtest.api.util.Actions;
import com.xceptance.xlt.engine.httprequest.HttpRequest;
import com.xceptance.xlt.engine.httprequest.HttpResponse;

public class CustomJMeterEngine extends StandardJMeterEngine
{
    private boolean running;
    private HashTree test;
    private static final List<TestStateListener> testList = new ArrayList<>();
    private final List<AbstractThreadGroup> groups = new CopyOnWriteArrayList<>();
    private boolean tearDownOnShutdown;
    public JMeterVariables threadVars;
    private TestCompiler compiler;
    public static final String LAST_SAMPLE_OK = "JMeterThread.last_sample_ok"; // $NON-NLS-1$
    static final String VAR_IS_SAME_USER_KEY = "__jmv_SAME_USER";
    public static final String PACKAGE_OBJECT = "JMeterThread.pack"; // $NON-NLS-1$
    
    private static final String TRUE = Boolean.toString(true); // i.e. "true"
    
    private String jmeterVariable = "${%s}";
    
    private CustomJMeterEngine engine = null; // For access to stop methods.
    private Controller mainController;
    private final boolean isSameUserOnNextIteration = true;
    private Collection<TestIterationListener> testIterationStartListeners;
    private Map<String, String> variableMap;
    private List<SampleListener> sampleListeners;
    private Sampler sam;
    private FindTestElementsUpToRootTraverser pathToRootTraverser;
    private List<Controller> controllersToRoot;
    private Controller controller;
    private String name;
    private int index;
    
    @Override
    public void configure(HashTree testTree)
    {
        super.configure(testTree);
        test = testTree;
        compiler = new TestCompiler(testTree);
        variableMap = new HashMap<String, String>();
        threadVars = new JMeterVariables();
    }       
    
    public void setEngine(CustomJMeterEngine engine) {
        this.engine = engine;
    }
    
    @Override
    public void run()
    {
        running = true;
        
        /*
         * Ensure that the sample variables are correctly initialised for each run.
         */
        SampleEvent.initSampleVariables();
        JMeterContextService.startTest();
        
        // needed for engine
        SearchByClass<TestIterationListener> threadListenerSearcher = new SearchByClass<>(TestIterationListener.class);
        testIterationStartListeners = threadListenerSearcher.getSearchResults();
        
        try 
        {
            PreCompiler compiler = new PreCompiler();
            test.traverse(compiler);
        }
        catch 
        (RuntimeException e) 
        {
            JMeterUtils.reportErrorToUser("Error occurred compiling the tree: - see log file", e);
            return; // no point continuing
        }
        
        /*
         * Notification of test listeners needs to happen after function
         * replacement, but before setting RunningVersion to true.
         */
        SearchByClass<TestStateListener> testListeners = new SearchByClass<>(TestStateListener.class);
        test.traverse(testListeners);
        
        // Merge in any additional test listeners
        // currently only used by the function parser
        testListeners.getSearchResults().addAll(testList);
        testList.clear(); // no longer needed

        test.traverse(new TurnElementsOn());
//        notifyTestListenersOfStart(testListeners);

        List<?> testLevelElements = new ArrayList<>(test.list(test.getArray()[0]));
//        removeThreadGroups(testLevelElements);

        SearchByClass<SetupThreadGroup> setupSearcher = new SearchByClass<>(SetupThreadGroup.class);
        SearchByClass<AbstractThreadGroup> searcher = new SearchByClass<>(AbstractThreadGroup.class);
        SearchByClass<PostThreadGroup> postSearcher = new SearchByClass<>(PostThreadGroup.class);

        test.traverse(setupSearcher);
        test.traverse(searcher);
        test.traverse(postSearcher);

        TestCompiler.initialize();
        // for each thread group, generate threads
        // hand each thread the sampler controller
        // and the listeners, and the timer
        
        Iterator<AbstractThreadGroup> iter = searcher.getSearchResults().iterator();

//        ListenerNotifier notifier = new ListenerNotifier();

        JMeterContextService.clearTotalThreads();
        
        Collection<AbstractThreadGroup> searchResults = searcher.getSearchResults();
        
        Assert.assertFalse("No usable requests in xml file found.", searchResults.isEmpty());
        mainController = (Controller) searchResults.toArray()[0];
        
        JMeterContext context = JMeterContextService.getContext();
        initRun(context);
        
        // main hashtree
        AbstractThreadGroup group = iter.next();
        ListedHashTree groupTree = (ListedHashTree) searcher.getSubTree(group);
        JMeterContextService.getContext().setSamplingStarted(true);

        // get the first item
        sam = mainController.next();
        index = 0;
        
        while (running) 
        {
            name = getParentController(groupTree, index);
            index++;
            
            try
            {
                Actions.run(name, t ->
                {
                    while (running && sam != null) 
                    {
                        // if null the variables are not used in the context (TransactionController : notifyListeners())
                        context.setThreadGroup((AbstractThreadGroup) mainController);
                        
                        processSampler(sam, null, context);
                        context.cleanAfterSample();

                        boolean lastSampleOk = TRUE.equals(context.getVariables().get(LAST_SAMPLE_OK));
                        // restart of the next loop
                        // - was requested through threadContext
                        // - or the last sample failed AND the onErrorStartNextLoop option is enabled
                        if (context.getTestLogicalAction() != TestLogicalAction.CONTINUE
                                || !lastSampleOk)
                        {
                            context.setTestLogicalAction(TestLogicalAction.CONTINUE);
                            sam = null;
                            // already done after the request was called
//                            setLastSampleOk(context.getVariables(), true);
                        }
                        else 
                        {
                            sam = mainController.next();
                            
                            // get the first parent controller node, for naming and action bundling
                            if (sam != null)
                            {
                                String newName = getParentController(groupTree, index);
                                
                                // TODO adjust naming and check ?
                                if (!StringUtils.equals(name, newName))
                                {
                                    System.out.println(newName);
                                    // new action started
                                    break;
                                }
                            }
                        }

                        // It would be possible to add finally for Thread Loop here
                        if (mainController.isDone()) 
                        {
                            running = false;
                        }
                    }
                });
            } 
            catch (Throwable e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

//        notifyTestListenersOfEnd(testListeners);
        JMeterContextService.endTest();
    }
    
    // TODO add tree location to every node before action loop?
    private String getParentController(ListedHashTree groupTree, int index)
    {
        // get the first parent controller node, for naming and action bundling
        pathToRootTraverser = new FindTestElementsUpToRootTraverser(sam);
        groupTree.traverse(pathToRootTraverser);
        controllersToRoot = pathToRootTraverser.getControllersToRoot();
        
        Assert.assertFalse("No controller found fo current element.", controllersToRoot.isEmpty());
        
        controller = controllersToRoot.get(0);
        return StringUtils.isNotBlank(controller.getName()) ? controller.getName() : "Action " + index;
    }
    
    private static Controller findRealSampler(Sampler sampler) 
    {
        Controller contr = null;
        Sampler realSampler = sampler;
        while (realSampler instanceof Controller) 
        {
            contr = ((TransactionController) sampler);
        }
        return contr;
    }
    
    private SampleResult processSampler(Sampler current, Sampler parent, JMeterContext threadContext) 
    {
        SampleResult transactionResult = null;
        // Check if we are running a transaction
        TransactionSampler transactionSampler = null;
        // Find the package for the transaction
        SamplePackage transactionPack = null;
        try
        {
            if (current instanceof TransactionSampler) 
            {
                transactionSampler = (TransactionSampler) current;
                transactionPack = compiler.configureTransactionSampler(transactionSampler);

                // Check if the transaction is done
                if (transactionSampler.isTransactionDone()) 
                {
                    transactionResult = doEndTransactionSampler(transactionSampler,
                            parent,
                            transactionPack,
                            threadContext);
                    // Transaction is done, we do not have a sampler to sample
                    current = null;
                } 
                else
                {
                    Sampler prev = current;
                    // It is the sub sampler of the transaction that will be sampled
                    current = transactionSampler.getSubSampler();
                    if (current instanceof TransactionSampler) 
                    {
                        SampleResult res = processSampler(current, prev, threadContext);// recursive call
                        threadContext.setCurrentSampler(prev);
                        current = null;
                        if (res != null) 
                        {
                            transactionSampler.addSubSamplerResult(res);
                        }
                    }
                }
            }

            // Check if we have a sampler to sample
            if (current != null) 
            {
                executeSamplePackage(current, transactionSampler, transactionPack, threadContext);
            }

        } catch (JMeterStopTestException e) 
        { 
            // TODO logging and eventually actions
        }
        if (!running
                && transactionResult == null
                && transactionSampler != null
                && transactionPack != null) {
            transactionResult = doEndTransactionSampler(transactionSampler, parent, transactionPack, threadContext);
        }

        return transactionResult;
    }
    
    private void executeSamplePackage(Sampler current,
            TransactionSampler transactionSampler,
            SamplePackage transactionPack,
            JMeterContext threadContext) 
    {
        threadContext.setCurrentSampler(current);
        // Get the sampler ready to sample
        SamplePackage pack = compiler.configureSampler(current);
        runPreProcessors(pack.getPreProcessors());
        current.setThreadContext(threadContext);

        // Hack: save the package for any transaction controllers
        threadVars.putObject(PACKAGE_OBJECT, pack);
        
        SampleResult result = null;
        if (running) 
        {
            Sampler sampler = pack.getSampler();
            sampler.setThreadContext(JMeterContextService.getContext());
            result =  sampler.sample(null);
            
            //*************************************************************
            // XLT request
            //*************************************************************
            try
            {
                buildAndExecuteRequest(pack, current.getName());
                threadVars.putObject(VAR_IS_SAME_USER_KEY, isSameUserOnNextIteration);
                threadContext.setVariables(threadVars);
                setLastSampleOk(threadVars, true);
                
                // set the variables into the engine for mapping
                putVariables(threadVars);
            } 
            catch (Throwable e)
            {
                e.printStackTrace();
            }
        }
        // If we got any results, then perform processing on the result
        if (result != null) 
        {
            if (!result.isIgnore()) 
            {
                int nbActiveThreadsInThreadGroup = 1;
                int nbTotalActiveThreads = JMeterContextService.getNumberOfThreads();
                fillThreadInformation(result, nbActiveThreadsInThreadGroup, nbTotalActiveThreads);
                SampleResult[] subResults = result.getSubResults();
                if (subResults != null) {
                    for (SampleResult subResult : subResults) 
                    {
                        fillThreadInformation(subResult, nbActiveThreadsInThreadGroup, nbTotalActiveThreads);
                    }
                }
                threadContext.setPreviousResult(result);
                runPostProcessors(pack.getPostProcessors());
                compiler.done(pack);
                // Add the result as subsample of transaction if we are in a transaction
                if (transactionSampler != null && !result.isIgnore()) 
                {
                    transactionSampler.addSubSamplerResult(result);
                }
            } 
            else 
            {
                // This call is done by checkAssertions() , as we don't call it
                // for isIgnore, we explictely call it here
                setLastSampleOk(threadContext.getVariables(), result.isSuccessful());
                compiler.done(pack);
            }
            if (result.getTestLogicalAction() != TestLogicalAction.CONTINUE) 
            {
                threadContext.setTestLogicalAction(result.getTestLogicalAction());
            }
        } 
        else 
        {
            compiler.done(pack); // Finish up
        }
    }
    
    public void buildAndExecuteRequest(SamplePackage data, String requestName) throws Throwable
    {
        HTTPSamplerProxy sampler = (HTTPSamplerProxy) data.getSampler();
        HeaderManager hm = null;
        AuthManager am = null;
        
        List<ConfigTestElement> configs = data.getConfigs();

        for (ConfigTestElement element : configs)
        {
            if (element instanceof HeaderManager)
            {
                hm = (HeaderManager) element;
            }
            if (element instanceof AuthManager)
            {
                am = (AuthManager) element;
            }
        }
        
        String method = sampler.getMethod();
        String baseUrl = data.getSampler().toString();
        
        HttpRequest request = new HttpRequest()
                .timerName(requestName)
                .baseUrl(baseUrl)
                .method(HttpMethod.valueOf(method));
        
        if (am != null)
        {
            Authorization authForURL = am.getAuthForURL(new URL(baseUrl));
            if (authForURL != null)
            {
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
        
        // check if the request was successful
        response.checkStatusCode(200);
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
    
    private HttpRequest addHeaderData(HttpRequest request, HeaderManager headerData)
    {
        // transform header keys/values from loaded data to request confirm data
        CollectionProperty headers = headerData.getHeaders();
        headers.forEach(p -> 
        {
            // remove name from the combined value attribute
            request.header(p.getName(), p.getStringValue().replace(p.getName(), "")); 
            
//            Set<String> keySet = variableMap.keySet();
//            keySet.forEach(k ->
//            {
//               if (p.getStringValue().contains(k))
//               {
//                   // remove name from the combined value attribute
//                   String preRefinedString = p.getStringValue().replace(p.getName(), ""); 
//                   // replace and add jmeter variables
//                   String replace = StringUtils.replaceOnceIgnoreCase(preRefinedString, 
//                                                                      String.format(jmeterVariable, k),
//                                                                      variableMap.get(k));
//                   request.header(p.getName(), replace);
//               }
//            });
        });
        return request;
    }
    
    private static void runPostProcessors(List<? extends PostProcessor> extractors) 
    {
        for (PostProcessor ex : extractors) {
            TestBeanHelper.prepare((TestElement) ex);
            ex.process();
        }
    }

    private static void runPreProcessors(List<? extends PreProcessor> preProcessors) 
    {
        for (PreProcessor ex : preProcessors) {
            TestBeanHelper.prepare((TestElement) ex);
            ex.process();
        }
    }
    
    private IterationListener initRun(JMeterContext threadContext) 
    {
        threadVars.putObject(VAR_IS_SAME_USER_KEY, isSameUserOnNextIteration);
        // save all previous found variables (from compiler) into the thread 
        threadVars.putAll(threadContext.getVariables());
        threadContext.setVariables(threadVars);
        threadContext.setThreadNum(1);
        setLastSampleOk(threadVars, true);
//        threadContext.setThread(JMeterThread this);
//        threadContext.setThreadGroup(threadGroup);
        threadContext.setEngine(engine);
        
        // variables are set at this point
        test.traverse(compiler);
        
//        if (scheduler) {
//            // set the scheduler to start
//            startScheduler();
//        }
        /*
         * Setting SamplingStarted before the controllers are initialised allows
         * them to access the running values of functions and variables (however
         * it does not seem to help with the listeners)
         */
        threadContext.setSamplingStarted(true);

        mainController.initialize();
        IterationListener iterationListener = new IterationListener();
        mainController.addIterationListener(iterationListener);

//        threadStarted();
        return iterationListener;
    }
    
    /**
     * Updates the variables with all entries found in the variables in {@code vars}
     * @param variables {@link JMeterVariables} with the entries to be updated
     */
    @API(status = API.Status.STABLE, since = "5.5")
    public void putVariables(JMeterVariables variables) {
        threadVars.putAll(variables);
    }
    
    private static void setLastSampleOk(JMeterVariables variables, boolean value) 
    {
        variables.put(LAST_SAMPLE_OK, Boolean.toString(value));
    }
    
    private class IterationListener implements LoopIterationListener {
        /**
         * {@inheritDoc}
         */
        @Override
        public void iterationStart(LoopIterationEvent iterEvent) {
            notifyTestListeners();
        }
    }
    
    void notifyTestListeners() {
        threadVars.incIteration();
        for (TestIterationListener listener : testIterationStartListeners) 
        {
            listener.testIterationStart(new LoopIterationEvent(mainController, threadVars.getIteration()));
            if (listener instanceof TestElement) {
                ((TestElement) listener).recoverRunningVersion();
            }
        }
    }
    
    private SampleResult doEndTransactionSampler(
            TransactionSampler transactionSampler, Sampler parent,
            SamplePackage transactionPack, JMeterContext threadContext) 
    {
        // Get the transaction sample result
        SampleResult transactionResult = transactionSampler.getTransactionResult();
        fillThreadInformation(transactionResult, 1, JMeterContextService.getNumberOfThreads());
        compiler.done(transactionPack);
        return transactionResult;
    }
    
    private void fillThreadInformation(SampleResult result,
            int nbActiveThreadsInThreadGroup,
            int nbTotalActiveThreads) 
    {
        result.setGroupThreads(nbActiveThreadsInThreadGroup);
        result.setAllThreads(nbTotalActiveThreads);
        result.setThreadName(""); // no thread name
    }
    
    private void startThreadGroup(AbstractThreadGroup group, int groupCount, SearchByClass<?> searcher, List<?> testLevelElements, ListenerNotifier notifier)
    {
        try 
        {
            int numThreads = group.getNumThreads();
            JMeterContextService.addTotalThreads(numThreads);
//            String groupName = group.getName();
            ListedHashTree threadGroupTree = (ListedHashTree) searcher.getSubTree(group);
            threadGroupTree.add(group, testLevelElements);

            groups.add(group);
            group.start(groupCount, notifier, threadGroupTree, this);
        } 
        catch (JMeterStopTestException ex) { // NOSONAR Reported by log
            JMeterUtils.reportErrorToUser("Error occurred starting thread group :" + group.getName()+ ", error message:"+ex.getMessage()
                +", \r\nsee log file for more details", ex);
            return; // no point continuing
        }
    }
    
    private void waitThreadsStopped() 
    {
        // ConcurrentHashMap does not need synch. here
        for (AbstractThreadGroup threadGroup : groups) {
            threadGroup.waitThreadsStopped();
        }
    }
}
