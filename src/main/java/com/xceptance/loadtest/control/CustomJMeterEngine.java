package com.xceptance.loadtest.control;

import com.xceptance.loadtest.data.util.Actions;
import com.xceptance.loadtest.jmeter.util.AssertionHandler;
import com.xceptance.loadtest.jmeter.util.HttpRequestHandler;
import com.xceptance.loadtest.jmeter.util.XLTJMeterUtils;
import com.xceptance.xlt.api.engine.Session;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.control.Controller;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.TransactionSampler;
import org.apache.jmeter.engine.PreCompiler;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.engine.TurnElementsOn;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestIterationListener;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.threads.*;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.apache.jorphan.util.JMeterStopTestException;
import org.apiguardian.api.API;
import org.junit.Assert;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomJMeterEngine extends StandardJMeterEngine
{
    /* If the setting below is set to <true> the code tries to inherit the action's name from the request name. If there is
     * no request name or the setting is <false> the closest TransactionController's name will be used, if available.
     * If there is no TransactionController the closest ThreadGroup's name is used. If a transaction controller or
     * thread group exists but has no name, an anonymous name is given, like "UnnamedThreadGroup_#", with # representing
     *  the current index of the unnamed element).
     */
    // maybe we should name it PROCESS_REQUESTS_INDEPENDENTLY
    private static final boolean USE_REQUEST_NAMING = false;

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
    private AssertionHandler assertionHandler;

    private static final String VAR_IS_SAME_USER_KEY = "__jmv_SAME_USER";
    private static final String PACKAGE_OBJECT = "JMeterThread.pack"; // $NON-NLS-1$
    private static final String TRUE = Boolean.toString(true); // i.e. "true"

    private static final String UNNAMED_THREAD_GROUP = "UnnamedThreadGroup_";

    private static final String UNNAMED_TRANSACTION_CONTROLLER = "UnnamedTransactionGroup_";

    private static final String UNNAMED_REQUEST = "UnnamedRequest_";

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

    private void printDbgMsg(String text)
    {
        if(Session.getCurrent().isLoadTest() == false)
        {
            System.out.println(text);
        }
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

        // Iterator<AbstractThreadGroup> iter = searcher.getSearchResults().iterator();
        JMeterContextService.clearTotalThreads();
        
        Collection<AbstractThreadGroup> searchResults = searcher.getSearchResults();
        
        Assert.assertFalse("No usable requests in xml file found.", searchResults.isEmpty());

        JMeterContext context = JMeterContextService.getContext();

        // Counter for unnamed ThreadGroups
        int unknownThreadGroupCounter = 0;

        // The first items in the test plan do always represent the thread groups. There can be multiple thread groups
        // in a test plan, so we have to iterate over all of them
        for(AbstractThreadGroup currentThreadGroup : searchResults)
        {
            mainController = currentThreadGroup;
            String currentThreadGroupName = currentThreadGroup.getName();
            if(StringUtils.isBlank(currentThreadGroupName))
            {
                currentThreadGroupName = String.format(UNNAMED_THREAD_GROUP + "%d", ++unknownThreadGroupCounter);
            }
            initRun(context);
            processThreadGroup(context, currentThreadGroupName);
        }

        JMeterContextService.endTest();
    }

    private void processThreadGroup(JMeterContext context, String threadGroupName)
    {
        printDbgMsg("Entering ThreadGroup:" + threadGroupName);

        // AbstractThreadGroup group = iter.next();
        //ListedHashTree groupTree = (ListedHashTree) searcher.getSubTree(mainController);
        JMeterContextService.getContext().setSamplingStarted(true);

        // init the assertion handling
        assertionHandler = new AssertionHandler((ThreadGroup) mainController);

        // Get the first item in the thread group
        sam = mainController.next();

        // Make sure there is something to run. Especially important if various thread groups
        // are part of the test
        if(sam == null)
        {
            return;
        }

        index = 0;
        int unnamedRequestCounter = 0;
        int unnamedTransactionControllerCounter = 0;
        running = true;

        while(running)
        {
            // In case the user has selected "Generate parent sample" on a Transaction Controller we have to process
            // the sub samples instead of the actual samplers which usually follow.
            boolean isTransactionSampler = false;
            String transactionControllerName = "";
            if(sam instanceof TransactionSampler)
            {
                isTransactionSampler = true;
                // We have to save the controller's name at the beginning before switching to sub samplers
                transactionControllerName = sam.getName();
                sam = ((TransactionSampler) sam).getSubSampler();
            }

            // First check if the action name is derived from the request name. If requests have no name a default name
            // is used
            if(USE_REQUEST_NAMING)
            {
                String requestName = "";
                if(sam instanceof HTTPSamplerProxy)
                {
                    requestName = sam.getName();
                    if(StringUtils.isBlank(requestName))
                    {
                        requestName = String.format(UNNAMED_REQUEST + "%d", ++unnamedRequestCounter);
                    }
                }
                name = requestName;
            }
            else
            {
                // If no request naming is used, check if there is an active transaction controller where we need to
                // process the children
                if(isTransactionSampler)
                {
                    if(StringUtils.isBlank(transactionControllerName))
                    {
                        transactionControllerName = String.format(UNNAMED_TRANSACTION_CONTROLLER + "%d",
                                                                  ++unnamedTransactionControllerCounter);
                    }
                }
                // Check if we got a name.
                if(StringUtils.isNotBlank(transactionControllerName))
                {
                    name = transactionControllerName;
                }
                else
                {
                    // Fallback: there is ALWAYS a thread group with a name (we gave a default one in the caller method
                    // in case it was empty)
                    name = threadGroupName;
                }
            }

            printDbgMsg("Process:" + name);
            index++;

            try
            {
                Actions.run(name, t ->
                {
                    while(running && sam != null)
                    {
                        // if null the variables are not used in the context (TransactionController : notifyListeners())
                        context.setThreadGroup((AbstractThreadGroup) mainController);

                        processSampler(sam, null, context);
                        context.cleanAfterSample();

                        boolean lastSampleOk = TRUE.equals(context.getVariables()
                                                                  .get(XLTJMeterUtils.LAST_SAMPLE_OK));
                        // restart of the next loop
                        // - was requested through threadContext
                        // - or the last sample failed AND the onErrorStartNextLoop option is enabled
                        if(context.getTestLogicalAction() != JMeterContext.TestLogicalAction.CONTINUE || !lastSampleOk)
                        {
                            context.setTestLogicalAction(JMeterContext.TestLogicalAction.CONTINUE);
                        }

                        sam = mainController.next();

                        // get the first parent controller node, for naming and action bundling
                        if(sam != null && !mainController.isDone())
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

                            if(USE_REQUEST_NAMING)
                            {
                                break;
                            }
                        }

                        // It would be possible to add finally for Thread Loop here
                        if(mainController.isDone() || sam == null)
                        {
                            running = false;
                        }
                    }
                });
            }
            catch(Throwable e)
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

        XLTJMeterUtils.runPreProcessors(pack.getPreProcessors());
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
                result = HttpRequestHandler.buildAndExecuteRequest(result, pack, current.getName());
                
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
                XLTJMeterUtils.runPostProcessors(pack.getPostProcessors());
                
                // check the current assertion status
                assertionHandler.checkAssertions(pack.getAssertions(), result, threadContext);
                
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
                XLTJMeterUtils.setLastSampleOk(threadContext.getVariables(), result.isSuccessful());
                compiler.done(pack);
            }
        } 
        else 
        {
            compiler.done(pack); // Finish up
        }
    }
    
    private IterationListener initRun(JMeterContext threadContext) 
    {
        threadVars.putObject(VAR_IS_SAME_USER_KEY, isSameUserOnNextIteration);
        // save all previous found variables (from compiler) into the thread 
        threadVars.putAll(threadContext.getVariables());
        threadContext.setVariables(threadVars);
        threadContext.setThreadNum(1);
        XLTJMeterUtils.setLastSampleOk(threadVars, true);
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
}
