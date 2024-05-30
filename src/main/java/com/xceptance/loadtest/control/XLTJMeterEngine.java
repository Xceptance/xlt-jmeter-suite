package com.xceptance.loadtest.control;

import com.xceptance.loadtest.data.util.Actions;
import com.xceptance.loadtest.jmeter.util.AssertionHandler;
import com.xceptance.loadtest.jmeter.util.HttpRequestHandler;
import com.xceptance.loadtest.jmeter.util.XLTJMeterUtils;
import com.xceptance.xlt.api.engine.Session;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.control.Controller;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.TransactionController;
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
import org.apache.jmeter.testbeans.TestBeanHelper;
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
import java.util.Collections;
import java.util.List;

/**
 * This class is based on {@link StandardJMeterEngine}. Additional it uses parts of the {@link JMeterThread} for the usage in XLT.
 * The class break up the usage of threads, as in the StandardJMeterEngine, to execute the requests recorded in JMeter for single thread execution.
 * Everything in the .jmx file will be executed as it would be in JMeter, except that the request get executed in a form that XLT can understand 
 * and use for load test execution and report generation.
 **/
public class XLTJMeterEngine extends StandardJMeterEngine
{
    /* If the setting below is set to <true> the code tries to inherit the action's name from the request name. If there is
     * no request name or the setting is <false> the closest TransactionController's name will be used, if available.
     * If there is no TransactionController the closest ThreadGroup's name is used. If a transaction controller or
     * thread group exists but has no name, an anonymous name is given, like "UnnamedThreadGroup_#", with # representing
     *  the current index of the unnamed element).
     */
    // maybe we should name it PROCESS_REQUESTS_INDEPENDENTLY
    private boolean useRequestNaming;

    private boolean running;
    private HashTree test;
    private static final List<TestStateListener> testList = new ArrayList<>();
    private JMeterVariables threadVars;
    private TestCompiler compiler;
    private XLTJMeterEngine engine = null; // For access to stop methods.
    private Controller mainController;
    private final boolean isSameUserOnNextIteration = true;
    private Collection<TestIterationListener> testIterationStartListeners;
    private Sampler sam;
    private String name;
    private AssertionHandler assertionHandler;

    private static final String VAR_IS_SAME_USER_KEY = "__jmv_SAME_USER";
    private static final String PACKAGE_OBJECT = "JMeterThread.pack"; // $NON-NLS-1$
    private static final String TRUE = Boolean.toString(true); // i.e. "true"

    private static final String UNNAMED_THREAD_GROUP = "UnnamedThreadGroup_";

    private static final String UNNAMED_TRANSACTION_CONTROLLER = "UnnamedTransactionController_";

    private static final String UNNAMED_REQUEST = "UnnamedRequest_";

    private Controller previousController;

    private Controller closestTransactionOrThreadParentController;

    // In case the user has selected "Generate parent sample" on a Transaction Controller we have to process
    // the sub samples instead of the actual samplers which usually follow.
    boolean isInsideOrDirectTransactionSampler = false;

    private String transactionControllerName = "";

    private List<String> actionNames;

    private int unnamedRequestCounter = 0;

    private int unnamedTransactionControllerCounter = 0;

    /**
     * Constructor for setting the request naming. If set to <false> the request naming will be dynamic.
     * 
     * @param useRequestNaming
     */
    public XLTJMeterEngine(boolean useRequestNaming)
    {
        this.useRequestNaming = useRequestNaming;
    }

    /**
     * Default Constructor.
     */
    public XLTJMeterEngine()
    {
        this.useRequestNaming = true;
    }

    /**
     * {@inheritDoc}
     * 
     * @param testTree read in testplan from jmx file
     */
    @Override
    public void configure(HashTree testTree)
    {
        super.configure(testTree);
        test = testTree;
        compiler = new TestCompiler(testTree);
        threadVars = new JMeterVariables();
    }

    /**
     * Setter for the engine.
     * @param engine
     */
    public void setEngine(XLTJMeterEngine engine)
    {
        this.engine = engine;
    }

    /**
     * Console output for debugging purpose.
     * 
     * @param text to print on console
     */
    private void printDbgMsg(String text)
    {
        if(Session.getCurrent().isLoadTest() == false)
        {
            System.err.println(text);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run()
    {
        running = true;

        actionNames = new ArrayList<>();

        /*
         * Ensure that the sample variables are correctly initialized for each run.
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
        catch(RuntimeException e)
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

        // Make sure to inform the listeners about the test start
        for(TestStateListener tL : testListeners.getSearchResults())
        {
            tL.testStarted();
        }

        testList.clear(); // no longer needed

        test.traverse(new TurnElementsOn());

        // init searcher classes
        SearchByClass<SetupThreadGroup> setupSearcher = new SearchByClass<>(SetupThreadGroup.class);
        SearchByClass<AbstractThreadGroup> searcher = new SearchByClass<>(AbstractThreadGroup.class);
        SearchByClass<PostThreadGroup> postSearcher = new SearchByClass<>(PostThreadGroup.class);

        // read in classes from jmx file
        test.traverse(setupSearcher);
        test.traverse(searcher);
        test.traverse(postSearcher);

        // default methods for JMeter
        TestCompiler.initialize();
        JMeterContextService.clearTotalThreads();

        Collection<AbstractThreadGroup> searchResults = searcher.getSearchResults();

        // if no thread group is found, no point in continuing
        Assert.assertFalse("No usable requests in xml file found.", searchResults.isEmpty());

        // init JMeter context
        JMeterContext context = JMeterContextService.getContext();

        // Counter for unnamed ThreadGroups
        int unknownThreadGroupCounter = 0;
        unnamedRequestCounter = 0;
        unnamedTransactionControllerCounter = 0;

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

            ListedHashTree groupTree = (ListedHashTree) searcher.getSubTree(currentThreadGroup);

            // execute the run
            initRun(context);
            processThreadGroup(context, currentThreadGroupName, groupTree);
        }

        JMeterContextService.endTest();
    }

    /**
     * Process the thread group, which is found in tesplan.
     * 
     * @param context
     * @param threadGroupName
     * @param threadGroupHashtree
     */
    private void processThreadGroup(JMeterContext context, String threadGroupName, ListedHashTree threadGroupHashtree)
    {
        printDbgMsg("Entering ThreadGroup:" + threadGroupName);

        // At the beginning we have to make sure *all* transaction controllers generate a parent sample, in order to
        // work with the action naming we are going to use
        SearchByClass<TransactionController> transactionControllerSearch = new SearchByClass<>(TransactionController.class);
        threadGroupHashtree.traverse(transactionControllerSearch);
        Collection<TransactionController> allTransactionControllers = transactionControllerSearch.getSearchResults();
        for(TransactionController tC : allTransactionControllers)
        {
            tC.setGenerateParentSample(true);
        }

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

        // Reset of global variables
        running = true;
        isInsideOrDirectTransactionSampler = false;
        transactionControllerName = "";

        // main loop
        while(running)
        {
            // Check if there are TransactionSamplers
            while(sam instanceof TransactionSampler)
            {
                isInsideOrDirectTransactionSampler = true;
                // We have to save the controller's name at the beginning before switching to sub samplers
                transactionControllerName = sam.getName();
                sam = ((TransactionSampler) sam).getSubSampler();
            }
            // Skip in case sam is empty - but we might
            // have some more things to process
            // This is important for finishing a (nested) transaction controller and proceeding to the next
            if(sam == null)
            {
                sam = mainController.next();
                if(sam != null)
                {
                    continue;
                }

                running = false;
                continue;
            }

            // First check if the action name is derived from the request name. If requests have no name a default name
            // is used
            if(useRequestNaming)
            {
                String requestName = "";
                if(sam instanceof HTTPSamplerProxy)
                {
                    requestName = sam.getName();
                    if(StringUtils.isBlank(requestName))
                    {
                        requestName = String.format(UNNAMED_REQUEST + "%d", ++unnamedRequestCounter);
                    }
                    name = requestName;
                }
                else
                {
                    // Fallback, in case we want request naming but do not have a request to process
                    if(isInsideOrDirectTransactionSampler)
                    {
                        if(StringUtils.isBlank(transactionControllerName))
                        {
                            transactionControllerName = String.format(UNNAMED_TRANSACTION_CONTROLLER + "%d",
                                                                      ++unnamedTransactionControllerCounter);
                        }
                        name = transactionControllerName;
                    }
                    else
                    {
                        // Fallback: there is ALWAYS a thread group with a name (we gave a default one in the caller method
                        // in case it was empty)
                        name = threadGroupName;
                    }
                }
            }
            else
            {
                // If no request naming is used, check if there is an active transaction controller where we need to
                // process the children
                if(isInsideOrDirectTransactionSampler)
                {
                    if(StringUtils.isBlank(transactionControllerName))
                    {
                        transactionControllerName = String.format(UNNAMED_TRANSACTION_CONTROLLER + "%d",
                                                                  ++unnamedTransactionControllerCounter);
                    }
                    // Either the TC has a name or we assigned one
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
            try
            {
                // start the action naming for XLT, all request in the action will be grouped in the report
                actionNames.add(name);
                Actions.run(name, t ->
                {
                    while(running && sam != null)
                    {
                        // if null the variables are not used in the context (TransactionController : notifyListeners())
                        context.setThreadGroup((AbstractThreadGroup) mainController);

                        // execution
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

                        if(sam == null || mainController.isDone())
                        {
                            running = false;
                            break;
                        }

                        // If JMeter is processing TransactionControllers with "Generate parent sample" we
                        // work on the underlying sub samples of the transaction sampler
                        if(sam instanceof TransactionSampler)
                        {
                            if(((TransactionSampler) sam).isTransactionDone())
                            {
                                break;
                            }

                            isInsideOrDirectTransactionSampler = true;
                            previousController = closestTransactionOrThreadParentController;
                            closestTransactionOrThreadParentController = getClosestTransactionOrThreadParentController(threadGroupHashtree,
                                                                                                                       ((TransactionSampler) sam).getSubSampler());

                            if(closestTransactionOrThreadParentController != null)
                            {
                                if(closestTransactionOrThreadParentController instanceof ThreadGroup)
                                {
                                    if(closestTransactionOrThreadParentController.getName().length() !=0 &&
                                       closestTransactionOrThreadParentController.getName().equals(threadGroupName) == false)
                                    {
                                        isInsideOrDirectTransactionSampler = true;
                                        // We must reuse the passed name, in case the thread group is unnamed (and has a fallback name)
                                        transactionControllerName = threadGroupName;
                                        break;
                                    }
                                }
                                // There is a new parent for the current sampler, so we need to open a new action
                                else if(closestTransactionOrThreadParentController instanceof TransactionController)
                                {
                                    String currentName = closestTransactionOrThreadParentController.getName();
                                    // If the transaction controller is unnamed check if the current unnamed transaction
                                    // controller is a new one (or if we need to fire the next request under the current one)
                                    if(currentName.length() == 0 || closestTransactionOrThreadParentController == previousController)
                                    {
                                        continue;
                                    }
                                    if(currentName.equals(name) == false)
                                    {
                                        transactionControllerName = currentName;
                                        break;
                                    }
                                }
                                else if(closestTransactionOrThreadParentController instanceof TransactionSampler)
                                {
                                    String currentName = closestTransactionOrThreadParentController.getName();
                                    // Unnamed but the same controller
                                    if(closestTransactionOrThreadParentController == previousController)
                                    {
                                        // keep the same name
                                        continue;
                                    }
                                    else
                                    {
                                        transactionControllerName = currentName;
                                        break;
                                    }
                                }
                                else
                                {
                                    // No fallback implemented yet
                                    Assert.fail("FAIL!");
                                }
                            }
                            else
                            {
                                break;
                            }
                        }

                        if(useRequestNaming)
                        {
                            break;
                        }

                        // Check if we are still below the current parent for naming (transaction controller or thread group)
                        // If we are below a thread group: the name cannot change because we call it from outside
                        if(closestTransactionOrThreadParentController != null)
                        {
                            previousController = closestTransactionOrThreadParentController;
                        }

                        closestTransactionOrThreadParentController = getClosestTransactionOrThreadParentController(threadGroupHashtree, sam);

                        if(closestTransactionOrThreadParentController != null)
                        {
                            if(closestTransactionOrThreadParentController instanceof ThreadGroup)
                            {
                                if(closestTransactionOrThreadParentController.getName().length() !=0 &&
                                   closestTransactionOrThreadParentController.getName().equals(threadGroupName) == false)
                                {
                                    isInsideOrDirectTransactionSampler = true;
                                    // We must reuse the passed name, in case the thread group is unnamed (and has a fallback name)
                                    transactionControllerName = threadGroupName;
                                    break;
                                }
                            }
                            // There is a new parent for the current sampler, so we need to open a new action
                            else if(closestTransactionOrThreadParentController instanceof TransactionController)
                            {
                                String currentName = closestTransactionOrThreadParentController.getName();
                                // If the transaction controller is unnamed check if the current unnamed transaction
                                // controller is a new one (or if we need to fire the next request under the current one)
                                if(currentName.length() == 0 && closestTransactionOrThreadParentController == previousController)
                                {
                                    continue;
                                }
                                if(currentName.equals(name) == false)
                                {
                                    isInsideOrDirectTransactionSampler = true;
                                    transactionControllerName = currentName;
                                    break;
                                }
                            }
                            else
                            {
                                // No fallback implemented yet
                                Assert.fail("FAIL!");
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

    /**
     * Traverse the tree structure to find the parent for the current sampler.
     * Return a Controllers (TransactionController or ThreadGroup) for the current Sampler, in case nothing is found return null.
     * 
     * @param groupTree complete test structure
     * @param sampler
     * @return Controller parent controller or null
     */
    private Controller getClosestTransactionOrThreadParentController(ListedHashTree groupTree, Sampler sampler)
    {
        // Retrace the path from the current element to the root item.
        FindTestElementsUpToRootTraverser path = new FindTestElementsUpToRootTraverser(sampler);
        groupTree.traverse(path);
        List<Controller> controllers = path.getControllersToRoot();
        List<Controller> limitedList = new ArrayList<>();
        for(Controller c : controllers)
        {
            if(c instanceof TransactionController || c instanceof ThreadGroup)
            {
                limitedList.add(c);
            }
        }

        // If there is a path return the closest parent transactioncontroller or the thread group
        if(!limitedList.isEmpty())
        {
            return limitedList.get(0);
        }

        return null;
    }

    /**
     * Prepare the Sampler in the current context for execution. Most of the functionality is directly from JMeter and only adjusted 
     * on some points to be compatible with XLT.
     * 
     * @param current
     * @param parent
     * @param threadContext
     * @return {@link SampleResult}
     */
    private SampleResult processSampler(Sampler current, Sampler parent, JMeterContext threadContext)
    {
        SampleResult transactionResult = null;
        // Check if we are running a transaction
        TransactionSampler transactionSampler = null;
        // Find the package for the transaction
        SamplePackage transactionPack = null;
        try
        {
            // only transactions are usable for request building
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
                    // for context usage
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

    /**
     * processing and execution of the Sampler.
     * 
     * @param current
     * @param transactionSampler
     * @param transactionPack
     * @param threadContext
     */
    @SuppressWarnings("unchecked")
    private void executeSamplePackage(Sampler current,
                                      TransactionSampler transactionSampler,
                                      SamplePackage transactionPack,
                                      JMeterContext threadContext)
    {
        threadContext.setCurrentSampler(current);
        // Get the sampler ready to sample
        SamplePackage pack = compiler.configureSampler(current);

        // check for unsupported features
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

        // execute any preprocessors before request building
        XLTJMeterUtils.runPreProcessors(pack.getPreProcessors());
        current.setThreadContext(threadContext);

        // Hack: save the package for any transaction controllers
        threadVars.putObject(PACKAGE_OBJECT, pack);

        SampleResult result = null;
        if (running)
        {
            Sampler sampler = pack.getSampler();
            sampler.setThreadContext(JMeterContextService.getContext());
            TestBeanHelper.prepare(sampler);
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

    /**
     * Init the run, this is needed for JMeter execution environment.
     * 
     * @param threadContext
     * @return
     */
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

    /**
     * Nested class for default JMeter needed setup.
     */
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

    /**
     * Every registered listener get the notification about the current state.
     */
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

    /**
     * Set the data for JMeter engine.
     * 
     * @param transactionSampler
     * @param parent
     * @param transactionPack
     * @param threadContext
     * @return
     */
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

    /**
     * Even if not used, the JMeter thread need to be setup.
     * 
     * @param result
     * @param nbActiveThreadsInThreadGroup
     * @param nbTotalActiveThreads
     */
    private void fillThreadInformation(SampleResult result,
                                       int nbActiveThreadsInThreadGroup,
                                       int nbTotalActiveThreads)
    {
        result.setGroupThreads(nbActiveThreadsInThreadGroup);
        result.setAllThreads(nbTotalActiveThreads);
        result.setThreadName(""); // no thread name
    }

    /**
     * List of all actions, which could be found during execution.
     * @return String list of action names
     */
    public List<String> getActionNames()
    {
        return Collections.unmodifiableList(actionNames);
    }
}