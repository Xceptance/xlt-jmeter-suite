package com.xceptance.loadtest.api.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.jmeter.control.Controller;
import org.apache.jmeter.control.TransactionSampler;
import org.apache.jmeter.engine.PreCompiler;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.engine.TurnElementsOn;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.processor.PostProcessor;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testbeans.TestBeanHelper;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestIterationListener;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.AbstractThreadGroup;
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

public class CustomJMeterEngine extends StandardJMeterEngine
{
    private boolean running;
    private HashTree test;
    private HashTree testtree;
    private static final List<TestStateListener> testList = new ArrayList<>();
    private final List<AbstractThreadGroup> groups = new CopyOnWriteArrayList<>();
    private boolean tearDownOnShutdown;
    private final JMeterVariables threadVars = new JMeterVariables();
    private TestCompiler compiler;
    public static final String LAST_SAMPLE_OK = "JMeterThread.last_sample_ok"; // $NON-NLS-1$
    static final String VAR_IS_SAME_USER_KEY = "__jmv_SAME_USER";
    
    private CustomJMeterEngine engine = null; // For access to stop methods.
    private Controller threadGroupLoopController;
    private final boolean isSameUserOnNextIteration = true;
    private Collection<TestIterationListener> testIterationStartListeners;

    @Override
    public void configure(HashTree testTree)
    {
        super.configure(testTree);
        this.testtree = testTree;
        test = testTree;
        compiler = new TestCompiler(testTree);
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
        SearchByClass<TestIterationListener> threadListenerSearcher = new SearchByClass<>(TestIterationListener.class); // TL - IS
        testIterationStartListeners = threadListenerSearcher.getSearchResults();
        try 
        {
            PreCompiler compiler = new PreCompiler();
            test.traverse(compiler);
        }
        catch (RuntimeException e) {
            JMeterUtils.reportErrorToUser("Error occurred compiling the tree: - see log file", e);
            return; // no point continuing
        }
        /*
         * Notification of test listeners needs to happen after function
         * replacement, but before setting RunningVersion to true.
         */
        SearchByClass<TestStateListener> testListeners = new SearchByClass<>(TestStateListener.class); // TL - S&E
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
        Iterator<SetupThreadGroup> setupIter = setupSearcher.getSearchResults().iterator();
        Iterator<AbstractThreadGroup> iter = searcher.getSearchResults().iterator();
        Iterator<PostThreadGroup> postIter = postSearcher.getSearchResults().iterator();

//        ListenerNotifier notifier = new ListenerNotifier();
//
//        int groupCount = 0;
//        JMeterContextService.clearTotalThreads();
//
//        if (setupIter.hasNext()) 
//        {
//            while (running && setupIter.hasNext()) 
//            {
//                //for each setup thread group
//                AbstractThreadGroup group = setupIter.next();
//                groupCount++;
//                String groupName = group.getName();
//                startThreadGroup(group, groupCount, setupSearcher, testLevelElements, notifier);
//                if (setupIter.hasNext()) 
//                {
//                    group.waitThreadsStopped();
//                }
//            }
//            //wait for all Setup Threads To Exit
//            waitThreadsStopped();
//            groupCount=0;
//            JMeterContextService.clearTotalThreads();
//        }
//
//        groups.clear(); // The groups have all completed now
//
//        /*
//         * Here's where the test really starts. Run a Full GC now: it's no harm
//         * at all (just delays test start by a tiny amount) and hitting one too
//         * early in the test can impair results for short tests.
//         */
//        JMeterUtils.helpGC();
//
//        boolean mainGroups = running; // still running at this point, i.e. setUp was not cancelled
//        while (running && iter.hasNext()) {// for each thread group
//            AbstractThreadGroup group = iter.next();
//            //ignore Setup and Post here.  We could have filtered the searcher. but then
//            //future Thread Group objects wouldn't execute.
//            if (group instanceof SetupThreadGroup ||
//                group instanceof PostThreadGroup) {
//                continue;
//            }
//            groupCount++;
//            String groupName = group.getName();
//            startThreadGroup(group, groupCount, searcher, testLevelElements, notifier);
//            if (iter.hasNext()) 
//            {
//                group.waitThreadsStopped();
//            }
//        } // end of thread groups
//        
//        //wait for all Test Threads To Exit
//        waitThreadsStopped();
//        groups.clear(); // The groups have all completed now
//
//        if (postIter.hasNext())
//        {
//            groupCount = 0;
//            JMeterContextService.clearTotalThreads();
//            if (mainGroups && !running) { // i.e. shutdown/stopped during main thread groups
//                running = tearDownOnShutdown; // re-enable for tearDown if necessary
//            }
//            while (running && postIter.hasNext()) {//for each setup thread group
//                AbstractThreadGroup group = postIter.next();
//                groupCount++;
//                String groupName = group.getName();
//                startThreadGroup(group, groupCount, postSearcher, testLevelElements, notifier);
//                if (postIter.hasNext()) 
//                {
//                    group.waitThreadsStopped();
//                }
//            }
//            waitThreadsStopped(); // wait for Post threads to stop
//        }
        
        threadGroupLoopController = (Controller) searcher.getSearchResults().toArray()[0];
        threadGroupLoopController.initialize();
        
        JMeterContext threadContext = JMeterContextService.getContext();
        Object iterationListener = initRun(threadContext);
        
        JMeterContextService.getContext().setSamplingStarted(true);
        
        while (running) 
        {
            Sampler sam = threadGroupLoopController.next();
            while (running && sam != null) {
                processSampler(sam, null, threadContext);
                threadContext.cleanAfterSample();

                boolean lastSampleOk = true;
                // restart of the next loop
                // - was requested through threadContext
                // - or the last sample failed AND the onErrorStartNextLoop option is enabled
                if (threadContext.getTestLogicalAction() != TestLogicalAction.CONTINUE
                        || !lastSampleOk)
                {
                    threadContext.setTestLogicalAction(TestLogicalAction.CONTINUE);
                    sam = null;
                    setLastSampleOk(threadContext.getVariables(), true);
                }
                else 
                {
                    sam = threadGroupLoopController.next();
                }
            }

            // It would be possible to add finally for Thread Loop here
            if (threadGroupLoopController.isDone()) 
            {
                running = false;
            }
        }

//        notifyTestListenersOfEnd(testListeners);
        JMeterContextService.endTest();
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

        SampleResult result = null;
        if (running) 
        {
            Sampler sampler = pack.getSampler();
            result =  sampler.sample(null);
//            result = doSampling(threadContext, sampler);
        }
        // If we got any results, then perform processing on the result
        if (result != null) {
            if (!result.isIgnore()) {
                int nbActiveThreadsInThreadGroup = 1;
                int nbTotalActiveThreads = JMeterContextService.getNumberOfThreads();
                fillThreadInformation(result, nbActiveThreadsInThreadGroup, nbTotalActiveThreads);
                SampleResult[] subResults = result.getSubResults();
                if (subResults != null) {
                    for (SampleResult subResult : subResults) {
                        fillThreadInformation(subResult, nbActiveThreadsInThreadGroup, nbTotalActiveThreads);
                    }
                }
                threadContext.setPreviousResult(result);
                runPostProcessors(pack.getPostProcessors());
                compiler.done(pack);
                // Add the result as subsample of transaction if we are in a transaction
                if (transactionSampler != null && !result.isIgnore()) {
                    transactionSampler.addSubSamplerResult(result);
                }
            } else {
                // This call is done by checkAssertions() , as we don't call it
                // for isIgnore, we explictely call it here
                setLastSampleOk(threadContext.getVariables(), result.isSuccessful());
                compiler.done(pack);
            }
            if (result.getTestLogicalAction() != TestLogicalAction.CONTINUE) {
                threadContext.setTestLogicalAction(result.getTestLogicalAction());
            }
        } else 
        {
            compiler.done(pack); // Finish up
        }
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
        threadContext.setVariables(threadVars);
        threadContext.setThreadNum(1);
        setLastSampleOk(threadVars, true);
//        threadContext.setThread(JMeterThread this);
//        threadContext.setThreadGroup(threadGroup);
        threadContext.setEngine(engine);
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

        threadGroupLoopController.initialize();
        IterationListener iterationListener = new IterationListener();
        threadGroupLoopController.addIterationListener(iterationListener);

//        threadStarted();
        return iterationListener;
    }
    
    private SampleResult processSampler(Sampler current, Sampler parent, JMeterContext threadContext) 
    {
        SampleResult transactionResult = null;
        // Check if we are running a transaction
        TransactionSampler transactionSampler = null;
        // Find the package for the transaction
        SamplePackage transactionPack = null;
        try {
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
                // TODO request?
                System.out.println(current.getName());
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
//            transactionResult = doEndTransactionSampler(transactionSampler, parent, transactionPack, threadContext);
        }

        return transactionResult;
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
            listener.testIterationStart(new LoopIterationEvent(threadGroupLoopController, threadVars.getIteration()));
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
