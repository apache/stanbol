/*
 * Copyright 2012 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.contenthub.faceted;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.UriRef;

/**
 * A set of documents that can be narrowed by applying constraints that are 
 * grouped by facets. Instances of this class are immutable, narrowing and 
 * broadening return new instances.
 */
public interface ConstrainedDocumentSet {
	
	/**
	 * A constraint requires a document property to have a certain value
	 */
	//TODO two versions of Constraint one with value (like this one) and the other with a value range
	interface Constraint {
		/**
		 * 
		 * @return the facet this constraint relates too
		 */
		Facet getFacet();
		
		/**
		 * 
		 * @return the URIRef or Literal that documents matching this constraint have as value of this facet
		 */
		Resource getValue();
	}
	
	/**
	 * A facet (aspect) by which the ConstrainedDocumentSet can be narrowed
	 */
	interface Facet {
		
		/**
		 * 
		 * @return a set of constraints that reduce the document to a non-empty set
		 */
		Set<Constraint> getConstraints();
		
		/**
		 * 
		 * @return The property this facet maps too
		 */
		UriRef getProperty();
		
		/**
		 * 
		 * @param locale the desired locale or null if no preference
		 * @return a label for this facet
		 */
		String getLabel(Locale locale);
		
	}
	
	
	/**
	 * The documents contained in this ConstrainedDocumentSet. There is no 
	 * defined order of the list, but implementations should keep the order
	 * stable as too allow stateless pagination.
	 * 
	 * Implementations may populate the list just when the respective 
	 * elements are accessed and implement size() to access optimized backend 
	 * functionality. Clients must thus take into account the possibility that 
	 * the list changes while they are using it. For example the size returned by List.size() may not match 
	 * the actual number of elements when iterating throw it at a later point 
	 * in time. The iterate() as well as the subList(int,int) method
	 * are safe.
	 * 
	 * 
	 * @return the documents matching this ConstrainedDocumentSet
	 */
	//TODO is just retuning the UriRef enough of should we rettur a DocumentResult or use ContentItem
	List<UriRef> getDocuments();
	
	/**
	 * This is the breadcrumb of this ConstrainedDocumentSet
	 * 
	 * @return the constrains that apply to this ConstrainedDocumentSet (might be empty)
	 */
	Set<Constraint> getConstraints();
	
	/**
	 * 
	 * @return the facets by which this ConstrainedDocumentSet can be restricted
	 */
	Set<Facet> getFacets();
	
	/**
	 * Note that the new set need not to be computed when this method is called,
	 * the matching document might be computed when needed. So implimentations
	 * can provided efficient way to allow a client to call
	 * 
	 * <code>narrow(additionalConstraint).getDocuments().size()</code>
	 * 
	 * @param constraint the additional constraint to apply
	 * @return the ConstrainedDocumentSet with that additional constraint
	 */
	ConstrainedDocumentSet narrow(Constraint constraint);
	
	/**
	 * Removes a constraint
	 * 
	 * @param constraint the constraint which must be be member of the set returned by getConstraints()
	 * @return the broadened ConstrainedDocumentSet
	 */
	ConstrainedDocumentSet broaden(Constraint constraint);
	
}
