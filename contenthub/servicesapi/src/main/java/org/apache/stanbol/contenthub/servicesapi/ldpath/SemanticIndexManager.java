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
import java.util.Set;

import at.newmedialab.ldpath.model.programs.Program;

/**
 * This interface provides create, retrieve, delete operations for LDPath
 * programs to be managed in the scope of Contenthub.
 * 
 * @author anil.sinaci
 * 
 */
public interface SemanticIndexManager {

	/**
	 * Saves LDPath program to persistent storage with given name and
	 * initializes a new Solr core with the same name
	 * 
	 * @param programName
	 *            name of the submitted program, also will be name of
	 *            corresponding Solr Core
	 * @param ldPathProgram
	 *            LDPath Program in the form of {@link String}
	 * @throws LDPathException
	 *             is thrown while parsing program and creating Solr Core
	 */
	public void submitProgram(String programName, String ldPathProgram)
			throws LDPathException;

	/**
	 * Saves LDPath program to persistent storage with given name and
	 * initializes a new Solr core with the same name
	 * 
	 * @param programName
	 *            name of the submitted program, also will be name of
	 *            corresponding Solr Core
	 * @param ldPathProgram
	 *            LDPath Program in the form of {@link java.io.Reader}
	 * @throws LDPathException
	 *             is thrown while parsing program and creating Solr Core
	 */
	public void submitProgram(String programName, Reader ldPathProgram)
			throws LDPathException;

	/**
	 * Checks whether a program-core pair exists with given name or not
	 * 
	 * @param programName
	 *            name of the program/core
	 * @return {@link true} if a program with given name exists; {@link false}
	 *         otherwise
	 */
	public boolean isManagedProgram(String programName);

	/**
	 * Retrieves the program managed by {@link ProgramManager} with given name
	 * 
	 * @param programName
	 *            name of the program that will be retrieved
	 * @return requested program as String, if such program does not exist,
	 *         returns {@link false}
	 */
	public String getProgramByName(String programName);

	 /**
	 * Retrieves the program managed by {@link ProgramManager} with given
	 name,
	 * parses it, and returns the {@link Progra}
	 *
	 * @param programName
	 * @return
	 * @throws LDPathException
	 */
	 public Program<Object> getParsedProgramByName(String programName);

	/**
	 * Deletes both the program and the corresponding Solr Core
	 * 
	 * @param programName
	 *            name of the program-core pair to be deleted
	 */
	public void deleteProgram(String programName);

	/**
	 * Used to retrieve names and programs of all currently managed program-core
	 * pairs
	 * 
	 * @return All managed programs as {@link LDProgramCollection}
	 */
	public LDProgramCollection retrieveAllPrograms();

	/**
	 * This method first tries to obtain the program itself through the given
	 * <code>programName</code> and if the program is obtained it is executed on
	 * the given <code>graph</code>.
	 * 
	 * @param programName
	 *            name of the program to be executed
	 * @param contexts
	 *            a {@link Set} of URIs (string representations) that are used
	 *            as starting nodes to execute LDPath program specified by
	 *            {@code programName} on the given {@code program}
	 * @param graph
	 *            a Clerezza graph on which the specified program will be
	 *            executed
	 * @return the {@link Map} containing the results obtained by executing the
	 *         given program on the given graph. Keys of the map corresponds to
	 *         fields in the program and values of the map corresponds to
	 *         results obtained for the field specified in the key.
	 * @throws LDPathException
	 */
	public Map<String, Collection<?>> executeProgram(String programName,
			Set<String> contexts) throws LDPathException;

}
