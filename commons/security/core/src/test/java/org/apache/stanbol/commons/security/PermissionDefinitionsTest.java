/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stanbol.commons.security;

import java.util.Arrays;
import java.util.List;

import org.junit.*;
import org.osgi.service.permissionadmin.PermissionInfo;
import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.stanbol.commons.security.PermissionDefinitions;

/**
 * 
 * @author clemens
 */
public class PermissionDefinitionsTest {

	private PermissionDefinitions permissionDefinitions;
	private PermissionInfo[] allPermissions;
	private PermissionInfo[] nullPermission;

	public PermissionDefinitionsTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() {

		final ImmutableGraph graph = Parser.getInstance()
				.parse(getClass().getResourceAsStream("systemgraph.nt"),
						"text/rdf+n3");		
		this.permissionDefinitions = new PermissionDefinitions(
				new SimpleGraph(graph.iterator()));

		this.allPermissions = new PermissionInfo[] {
				new PermissionInfo(
						"(java.io.FilePermission \"file:///home/foo/-\" \"read,write,delete\")"),
				new PermissionInfo(
						"(java.io.FilePermission \"file:///home/foo/*\" \"read,write\")"),
				new PermissionInfo(
						"(java.io.FilePermission \"file:///home/*\" \"read,write\")") };
		this.nullPermission = null;
	}

	@Test
	public void testAllowedBundle() {
		PermissionInfo[] permInfos = this.permissionDefinitions
				.retrievePermissions("file:///home/foo/foobar/testbundle-1.0-SNAPSHOT.jar");
		List<PermissionInfo> permInfoList = Arrays.asList(permInfos);
		List<PermissionInfo> expectedPermInfos = Arrays.asList(allPermissions);
		
		Assert.assertTrue(permInfoList.containsAll(expectedPermInfos));
		Assert.assertTrue(expectedPermInfos.containsAll(permInfoList));
	}

	@Test
	public void testUnknownBundle() {
		Assert.assertNotSame(allPermissions, this.permissionDefinitions
				.retrievePermissions("file:///foo.jar"));
		Assert.assertArrayEquals(nullPermission, this.permissionDefinitions
				.retrievePermissions("file:///foo.jar"));
	}
}