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
package org.apache.stanbol.contenthub.servicesapi.index;

import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.contenthub.servicesapi.index.ldpath.LDPathException;
import org.apache.stanbol.contenthub.servicesapi.index.ldpath.LDPathProgramCollection;

import at.newmedialab.ldpath.model.programs.Program;

public interface SemanticIndexManager {
    SemanticIndex getIndex(String name);

    List<SemanticIndex> getIndexes(String name);

    SemanticIndex getIndex(EndpointType endpointType);

    List<SemanticIndex> getIndexes(EndpointType endpointType);

    SemanticIndex getIndex(String name, EndpointType endpointType);

    List<SemanticIndex> getIndexes(String name, EndpointType endpointType);

	String getProgramByName(String programName);

	Program<Object> getParsedProgramByName(String programName);

	void deleteProgram(String programName);

	boolean isManagedProgram(String programName);

	void submitProgram(String programName, String ldPathProgram)
			throws LDPathException;

	void submitProgram(String programName, Reader ldPathProgramReader)
			throws LDPathException;

	LDPathProgramCollection retrieveAllPrograms();

	Map<String, Collection<?>> executeProgram(String programName,
			Set<String> contexts) throws LDPathException;
}
