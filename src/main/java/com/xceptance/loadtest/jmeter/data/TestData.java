package com.xceptance.loadtest.jmeter.data;

import java.util.Optional;

/**
 * Common data collector for all data needed for a test during execution, kind of a global state.
 *
 */
public class TestData extends com.xceptance.loadtest.addons.configuration.TestData
{

    // Test case specific authorization (e.g. user specific token).
    public Optional<String> authorization = Optional.empty();
}