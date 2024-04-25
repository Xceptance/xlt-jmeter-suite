package com.xceptance.loadtest.control;

import com.xceptance.loadtest.api.events.EventLogger;
import com.xceptance.loadtest.data.util.Actions;
import com.xceptance.xlt.engine.httprequest.HttpRequest;
import com.xceptance.xlt.engine.httprequest.HttpResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.assertions.Assertion;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.control.Controller;
import org.apache.jmeter.control.LoopController;
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
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testbeans.TestBeanHelper;
import org.apache.jmeter.testelement.*;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.threads.*;
import org.apache.jmeter.threads.JMeterContext.TestLogicalAction;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.apache.jorphan.util.JMeterError;
import org.apache.jorphan.util.JMeterStopTestException;
import org.apiguardian.api.API;
import org.htmlunit.HttpMethod;
import org.junit.Assert;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

public class CustomJMeterEngine extends StandardJMeterEngine
{
    private boolean running;
    private HashTree test;
    private static final List<TestStateListener> testList = new ArrayList<>();
    private JMeterVariables threadVars;
    private TestCompiler compiler;
    private CustomJMeterEngine engine = null; // For access to stop methods.
    private Controller mainController;
    private final boolean isSameUserOnNextIteration = true;
    private Collection<TestIterationListener> testIterationStartListeners;
    private Sampler sam;
    private FindTestElementsUpToRootTraverser pathToRootTraverser;
    private List<Controller> controllersToRoot;
    private Controller controller;
    private String name;
    private int index;
    private boolean onErrorStopTest;
    private boolean onErrorStopTestNow;
    private boolean onErrorStopThread;
    private boolean onErrorStartNextLoop;
    
    private static final String LAST_SAMPLE_OK = "JMeterThread.last_sample_ok"; // $NON-NLS-1$
    private static final String VAR_IS_SAME_USER_KEY = "__jmv_SAME_USER";
    private static final String PACKAGE_OBJECT = "JMeterThread.pack"; // $NON-NLS-1$
    private static final String TRUE = Boolean.toString(true); // i.e. "true"
    
    @Override
    public void configure(HashTree testTree)
    {
        super.configure(testTree);
        test = testTree;
        compiler = new TestCompiler(testTree);
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

//        List<?> testLevelElements = new ArrayList<>(test.list(test.getArray()[0]));

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
        
        onErrorStopTest = group.getOnErrorStopTest();
        onErrorStopTestNow = group.getOnErrorStopTestNow();
        onErrorStopThread = group.getOnErrorStopThread();
        onErrorStartNextLoop = group.getOnErrorStartNextLoop();

        // get the first item
        sam = mainController.next();
        index = 0;
        
        while (running) 
        {
            // In case the user has selected "Generate parent sample" on a Transaction Controller we have to process
            // the sub samples instead of the actual samplers which usually follow
            if(sam instanceof TransactionSampler)
            {
                sam = ((TransactionSampler) sam).getSubSampler();
            }

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
                        if (context.getTestLogicalAction() != TestLogicalAction.CONTINUE || !lastSampleOk)
                        {
                            context.setTestLogicalAction(TestLogicalAction.CONTINUE);
                        }
                        
                        sam = mainController.next();
                        
                        // get the first parent controller node, for naming and action bundling
                        if (sam != null && !mainController.isDone())
                        {
                            // If JMeter is processing TransactionControllers with "Generate parent sample" we
                            // work on the underlying sub samples of the transaction sampler
                            if(sam instanceof TransactionSampler)
                            {
                                // If all requests, or whatever belongs to this transaction, are processed, continue
                                if(((TransactionSampler) sam).isTransactionDone())
                                {
                                    // Move to the next TransactionController
                                    sam = mainController.next();

                                    // If there are no further TransactionControllers we are done
                                    if(sam == null)
                                    {
                                        running = false;
                                    }
                                    break;
                                }
                                else
                                {
                                    // Default: The transaction (for example, Visit, has more requests to process)
                                    sam = ((TransactionSampler) sam).getSubSampler();
                                }
                            }

                            String newName = getParentController(groupTree, index);
                            
                            // TODO adjust naming and check ?
                            if (!StringUtils.equals(name, newName))
                            {
                                System.out.println(newName);
                                // new action started
                                break;
                            }
                        }

                        // It would be possible to add finally for Thread Loop here
                        if (mainController.isDone() || sam == null)
                        {
                            running = false;
                        }
                    }
                });
            } 
            catch (Throwable e)
            {
                if(e instanceof AssertionError)
                {
                    AssertionError ae = new AssertionError(e.getMessage());
                    ae.setStackTrace(e.getStackTrace());
                    throw ae;
                }

                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        JMeterContextService.endTest();
    }
    
    // TODO add tree location to every node before action loop?
    private String getParentController(ListedHashTree groupTree, int index)
    {
        // get the first parent controller node, for naming and action bundling
        pathToRootTraverser = new FindTestElementsUpToRootTraverser(sam);
        groupTree.traverse(pathToRootTraverser);
        controllersToRoot = pathToRootTraverser.getControllersToRoot();
        
        Assert.assertFalse("No controller found for current element.", controllersToRoot.isEmpty());
        
        controller = controllersToRoot.get(0);
        return StringUtils.isNotBlank(controller.getName()) ? controller.getName() : "Action " + index;
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

        List<Controller> controllers = new ArrayList<>();
        try
        {
            Field controllersField = pack.getClass()
                                         .getDeclaredField("controllers");
            controllersField.setAccessible(true);
            controllers = (List<Controller>) controllersField.get(pack);
        }
        catch(NoSuchFieldException | IllegalAccessException e)
        {
            Assert.fail("Cannot locate field <controllers> in SamplePackage class");
        }

        for(Controller c : controllers)
        {
            if(c instanceof LoopController && ((LoopController) c).getLoops() == -1)
            {
                Assert.fail("Infinite loops are currently not supported by XLT");
            }
        }

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
                HttpResponse request = buildAndExecuteRequest(result, pack, current.getName());

                // set the response to jmeter results, null for platform default encoding
                result.setResponseData(request.getContentAsString(), null);
                result.setResponseHeaders(request.getHeaders().toString());
                result.setResponseCode(String.valueOf(request.getStatusCode())); // set status code for assertion check
                result.setResponseMessage(request.getStatusMessage());
                result.setSuccessful(true); // set the request to success -> otherwise the assertion checker will fail
                
                threadVars.putObject(VAR_IS_SAME_USER_KEY, isSameUserOnNextIteration);
                threadContext.setVariables(threadVars);
                
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
                if (subResults != null) 
                {
                    for (SampleResult subResult : subResults) 
                    {
                        fillThreadInformation(subResult, nbActiveThreadsInThreadGroup, nbTotalActiveThreads);
                    }
                }
                threadContext.setPreviousResult(result);
                runPostProcessors(pack.getPostProcessors());
                checkAssertions(pack.getAssertions(), result, threadContext);
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
            
            // Check if thread or test should be stopped
            if (result.isStopThread() || (!result.isSuccessful() && onErrorStopThread)) 
            {
                shutdown();
            }
            if (result.isStopTest() || (!result.isSuccessful() && onErrorStopTest))
            {
                shutdown();
            }
            if (result.isStopTestNow() || (!result.isSuccessful() && onErrorStopTestNow))
            {
                shutdown();
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
    
    private void shutdown()
    {
        running = false;
        // TODO terminated error handling
        Assert.fail("Test encountered an issue and terminated early.");
    }
    
    public HttpResponse buildAndExecuteRequest(SampleResult pack, SamplePackage data, String requestName) throws Throwable
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
        
        return response;
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
                request.param(entry.getKey(), entry.getValue());
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
        });
        return request;
    }
    
    private static void runPostProcessors(List<? extends PostProcessor> extractors) 
    {
        for (PostProcessor ex : extractors) 
        {
            TestBeanHelper.prepare((TestElement) ex);
            ex.process();
        }
    }

    private static void runPreProcessors(List<? extends PreProcessor> preProcessors) 
    {
        for (PreProcessor ex : preProcessors) 
        {
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
        threadContext.setEngine(engine);
        
        // variables are set at this point
        test.traverse(compiler);
        
        /*
         * Setting SamplingStarted before the controllers are initialised allows
         * them to access the running values of functions and variables (however
         * it does not seem to help with the listeners)
         */
        threadContext.setSamplingStarted(true);

        mainController.initialize();
        IterationListener iterationListener = new IterationListener();
        mainController.addIterationListener(iterationListener);

        return iterationListener;
    }
    
    /**
     * Updates the variables with all entries found in the variables in {@code vars}
     * @param variables {@link JMeterVariables} with the entries to be updated
     */
    @API(status = API.Status.STABLE, since = "5.5")
    public void putVariables(JMeterVariables variables) 
    {
        threadVars.putAll(variables);
    }
    
    private static void setLastSampleOk(JMeterVariables variables, boolean value) 
    {
        variables.put(LAST_SAMPLE_OK, Boolean.toString(value));
    }
    
    private class IterationListener implements LoopIterationListener 
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public void iterationStart(LoopIterationEvent iterEvent) 
        {
            notifyTestListeners();
        }
    }
    
    void notifyTestListeners() 
    {
        threadVars.incIteration();
        for (TestIterationListener listener : testIterationStartListeners) 
        {
            listener.testIterationStart(new LoopIterationEvent(mainController, threadVars.getIteration()));
            if (listener instanceof TestElement) 
            {
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
    
    private static void checkAssertions(List<? extends Assertion> assertions, SampleResult parent, JMeterContext threadContext) 
    {
        for (Assertion assertion : assertions) 
        {
            TestBeanHelper.prepare((TestElement) assertion);
            if (assertion instanceof AbstractScopedAssertion) 
            {
                AbstractScopedAssertion scopedAssertion = (AbstractScopedAssertion) assertion;
                String scope = scopedAssertion.fetchScope();
                if (scopedAssertion.isScopeParent(scope)
                    || scopedAssertion.isScopeAll(scope)
                    || scopedAssertion.isScopeVariable(scope)) 
                {
                    processAssertion(parent, assertion);
                }
                if (scopedAssertion.isScopeChildren(scope)
                    || scopedAssertion.isScopeAll(scope)) 
                {
                    recurseAssertionChecks(parent, assertion, 3);
                }
            } else 
            {
                processAssertion(parent, assertion);
            }
        }
        setLastSampleOk(threadContext.getVariables(), parent.isSuccessful());
    }

    private static void recurseAssertionChecks(SampleResult parent, Assertion assertion, int level) 
    {
        if (level < 0) 
        {
            return;
        }
        SampleResult[] children = parent.getSubResults();
        boolean childError = false;
        for (SampleResult childSampleResult : children) 
        {
            processAssertion(childSampleResult, assertion);
            recurseAssertionChecks(childSampleResult, assertion, level - 1);
            if (!childSampleResult.isSuccessful()) 
            {
                childError = true;
            }
        }
        // If parent is OK, but child failed, add a message and flag the parent as failed
        if (childError && parent.isSuccessful()) 
        {
            AssertionResult assertionResult = new AssertionResult(((AbstractTestElement) assertion).getName());
            assertionResult.setResultForFailure("One or more sub-samples failed");
            parent.addAssertionResult(assertionResult);
            parent.setSuccessful(false);
        }
    }

    private static void processAssertion(SampleResult result, Assertion assertion) 
    {
        AssertionResult assertionResult = null;
        try 
        {
            assertionResult = assertion.getResult(result);
        } 
        catch (AssertionError e) 
        {
            EventLogger.DEFAULT.warn("Error processing Assertion.", e.getMessage());
            assertionResult = new AssertionResult("Assertion failed!");
            assertionResult.setFailure(true);
            assertionResult.setFailureMessage(e.toString());
        } 
        catch (JMeterError e) 
        {
            EventLogger.DEFAULT.warn("Error processing Assertion.", e.getMessage());
            assertionResult = new AssertionResult("Assertion failed!");
            assertionResult.setError(true);
            assertionResult.setFailureMessage(e.toString());
        } 
        catch (Exception e) 
        {
            EventLogger.DEFAULT.warn("Exception processing Assertion.", e.getMessage());
            assertionResult = new AssertionResult("Assertion failed!");
            assertionResult.setError(true);
            assertionResult.setFailureMessage(e.toString());
        }
        finally
        {
            if (assertionResult != null &&
                assertionResult.isFailure() ||
                assertionResult.isError())
            {
                EventLogger.DEFAULT.warn("Assertion was found.", assertionResult.getFailureMessage());
            }
        }
        result.setSuccessful(result.isSuccessful() && !(assertionResult.isError() || assertionResult.isFailure()));
        result.addAssertionResult(assertionResult);
    }
}
