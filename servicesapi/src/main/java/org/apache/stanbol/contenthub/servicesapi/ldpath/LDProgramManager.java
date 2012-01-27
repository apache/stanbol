/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.contenthub.servicesapi.ldpath;

import java.io.Reader;
import java.util.Collection;
import java.util.Map;

import org.apache.clerezza.rdf.core.MGraph;

public interface LDProgramManager {

	/**
	 * Saves program to persistent storage with given name and initialize Solr Core with same name
	 * @param programName name of the submitted program, also will be name of corresponding Solr Core
	 * @param ldPathProgram	LDPath Program in the form of {@link String}
	 * @throws LDPathException	is thrown while parsing program and creating Solr Core
	 */
	public void submitProgram(String programName, String ldPathProgram) throws LDPathException;

	/**
	 * Saves program to persistent storage with given name and initialize Solr Core with same name
	 * @param programName name of the submitted program, also will be name of corresponding Solr Core
	 * @param ldPathProgram	LDPath Program in the form of {@link java.io.Reader}
	 * @throws LDPathException	is thrown while parsing program and creating Solr Core
	 */
	public void submitProgram(String programName, Reader ldPathProgram) throws LDPathException;
	
	/**
	 * Checks whether a program-core pair exist with given name or not
	 * @param programName name of the program/core 
	 * @return {@link true} if a program with given name exists; {@link false} otherwise
	 */
	public boolean isManagedProgram(String programName);
	
	/**
	 * Retrieves the program managed by {@link ProgramManager} with given name
	 * @param programName name of the program that will be retrieved
	 * @return requested program as String, if such program does not exist, returns {@link false}
	 */
	public String getProgramByName(String programName);

	/**
	 * Deletes both the program and the corresponding Solr Core
	 * @param programName name of the program-core pair to be deleted
	 */
	public void deleteProgram(String programName);
	
	/**
	 * Used to retrieve names and programs of all currently managed program-core pairs
	 * @return All managed programs with their names as {@link LDProgramCollection}
	 */
	public LDProgramCollection retrieveAllPrograms();
	
	public Map<String,Collection<?>> executeProgram(String programName, MGraph graph) throws LDPathException;

}
