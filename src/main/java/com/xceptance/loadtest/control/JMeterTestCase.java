/*
 * Copyright (c) 2005-2025 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xceptance.loadtest.control;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.services.FileServer;
import org.apache.jorphan.collections.HashTree;
import org.junit.Assert;

import com.xceptance.loadtest.api.tests.HtmlUnitLoadTestCase;
import com.xceptance.loadtest.data.util.Context;

/**
 * Base class of a JMeter test
 *
 */
public abstract class JMeterTestCase extends HtmlUnitLoadTestCase
{
	/*
	 * The JMX file to run
	 */
	private final File jmxFile;

	/*
	 * The execution engine for this test script
	 */
	private final XLTJMeterEngine engine;

	/**
	 * Constructor
	 *
	 * The path to the file relative to config/data/jmeter, likely something like
	 * anyfile.jmx. The data for that JMX file will then be relative to
	 * this location. This data is not validated before the file is loaded.
	 * Important: Jmeter does not really support multi-threading in the sense of several engines
	 * in one VM, hence all file must be in the same location and the data must be
	 * relative to this one directory.
	 *
	 * @param path the path relative to testsuite/config/data/jmeter
	 * @param applyRequestNaming shall names for request be automatically generated
	 */
	public JMeterTestCase(final String scriptFile, final boolean applyRequestNaming)
	{
		super();

		// Create and attach context instance
		Context.attach(new Context(getClass().getName(), getSite()));

		// get us the engine, so the base file server is up too
		this.engine = new XLTJMeterEngine(applyRequestNaming);

		// we already defined the base path and this is where the file is
		// Jmeter cannot handle data the way XLT can, so we have for
		// source and data files a single source. Maybe in the future, we can
		// support properties with context, but let's keep things simple for
		// the moment
		this.jmxFile = FileServer.getFileServer().getResolvedFile(scriptFile);
	}

	/**
	 * Constructor
	 *
	 * The path to the file relativ to config/data, likely something like
	 * jmeter/anyfile.jmx. The data for that JMX file will then be relative to
	 * this location. This data is not validated before the file is loaded.
	 *
	 * @param path the path relative to testsuite/config
	 */
	public JMeterTestCase(final String path)
	{
		this(path, false);
	}

	/**
	 * Helper to puzzle up a timer name including a site.
	 *
	 * @param name the timer default name
	 * @param siteId the site id
	 * @return a new name when the site is not default
	 */
	public static String getSiteSpecificName(final String name, final String siteId)
	{
		if (!"default".equals(siteId))
		{
			// we have something non default
			return MessageFormat.format("{0}_{1}", name, siteId);
		}
		return name;
	}

	/**
	 * Main test method.
	 *
	 * @throws Throwable
	 */
	@Override
	protected void test() throws Throwable
	{
		// load the jmx file into a HashTree structure
		final HashTree tree = SaveService.loadTree(jmxFile);

		// get the engine set up
		engine.configure(tree);

		// run the test
		engine.run();
	}

	/**
	 * Verifies the performed actions against a list of expectations
	 * @param expectedActionNames the list of expected names
	 */
	public void validateActions(final List<String> expectedActionNames)
	{
		Assert.assertEquals(expectedActionNames, getActualActionNames());
	}

	/**
	 * Returns a list of action names either preconfigured or made up if none
	 * have been defined.
	 *
	 * @return a list of action names
	 */
	protected List<String> getActualActionNames()
	{
		return engine.getActualActionNames();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void tearDown()
	{
		// Release context instance
		Context.get().releaseContext();

		// ensure everyone above us can do magic
        super.tearDown();
	}
}
