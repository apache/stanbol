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
package org.apache.stanbol.ontologymanager.ontonet.api.registry.io;

import java.io.InputStream;
import java.io.Reader;

import org.semanticweb.owlapi.model.IRI;

public class IRIRegistrySource implements XDRegistrySource {

	protected IRI iri;

	public IRIRegistrySource(IRI physicalIRI) {
		if (physicalIRI == null)
			throw new RuntimeException(
					"Cannot instantiate IRI registry source on null IRI.");
		this.iri = physicalIRI;
	}

	/*
	 * (non-Javadoc)
	 * @see org.stlab.xd.registry.io.XDRegistrySource#getInputStream()
	 */
	@Override
	public InputStream getInputStream() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.stlab.xd.registry.io.XDRegistrySource#getPhysicalIRI()
	 */
	@Override
	public IRI getPhysicalIRI() {
		return iri;
	}

	/*
	 * (non-Javadoc)
	 * @see org.stlab.xd.registry.io.XDRegistrySource#getReader()
	 */
	@Override
	public Reader getReader() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.stlab.xd.registry.io.XDRegistrySource#isInputStreamAvailable()
	 */
	@Override
	public boolean isInputStreamAvailable() {
		return false;
	}

	@Override
	public boolean isReaderAvailable() {
		return false;
	}

}
