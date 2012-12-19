<?xml version="1.0" encoding="UTF-8"?>
<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0" xmlns:h="http://www.w3.org/1999/xhtml"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:nfo="http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#"
	xmlns:nie="http://www.semanticdesktop.org/ontologies/2007/01/19/nie#"
	xmlns:owl="http://www.w3.org/2002/07/owl#">

	<xsl:output indent="yes" method="xml" media-type="application/rdf+xml"
		encoding="UTF-8" omit-xml-declaration="yes" />

	<!-- base of the current XML doc -->
	<xsl:variable name='xml_base' select="//*/@xml:base[position()=1]" />

	<!-- base of the current HTML doc -->
	<xsl:variable name='html_base' select="//*/head/base[position()=1]/@href" />

	<!-- url of the current XHTML page if provided by the XSLT engine -->
	<xsl:param name='uri' select="'http://foobar.com/'" />

	<!-- this contains the URL of the source document whether it was provided 
		by the base or as a parameter e.g. http://example.org/bla/file.html -->
	<xsl:variable name='this'>
		<xsl:choose>
			<xsl:when test="string-length($html_base)>0">
				<xsl:value-of select="$html_base" />
			</xsl:when>
			<xsl:when test="string-length($xml_base)>0">
				<xsl:value-of select="$xml_base" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$uri" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>

	<!-- this_location contains the location the source document e.g. http://example.org/bla/ -->
	<xsl:variable name='this_location'>
		<xsl:call-template name="get-location">
			<xsl:with-param name="url" select="$this" />
		</xsl:call-template>
	</xsl:variable>

	<!-- this_root contains the root location of the source document e.g. http://example.org/ -->
	<xsl:variable name='this_root'>
		<xsl:call-template name="get-root">
			<xsl:with-param name="url" select="$this" />
		</xsl:call-template>
	</xsl:variable>


	<!--  specify a registry for vocabs to parametrize mappings to RDF? -->
	<!--  this bottom up approach does not allow for list-valued properties -->
	
	<!-- templates for parsing - - - - - - - - - - - - - - - - - - - - - - - -->

	<!--Start the RDF generation -->
	<xsl:template match="/">
		<rdf:RDF>
			<!-- TODO: attributes can be in list! -->
			<xsl:if
				test="descendant::*/@itemprop or descendant::*/@itemprop or descendant::*/@itemtype">
				<xsl:choose>
					<xsl:when test="$uri != $this">
						<nfo:HtmlDocument rdf:about="{$uri}">
							<owl:sameAs rdf:resource="{$this}" />
						</nfo:HtmlDocument>
						<xsl:apply-templates />
					</xsl:when>
					<xsl:otherwise>
						<nfo:HtmlDocument rdf:about="{$uri}" />
						<xsl:apply-templates />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:if>
		</rdf:RDF>
	</xsl:template>

	<!--  TODO: itemref -->

	<xsl:template match="*[attribute::itemscope='']">
		<xsl:param name="refSource" />
		<xsl:variable name="itemId">
			<xsl:call-template name="getNodeId">
				<xsl:with-param name="node" select="." />
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<!-- object properties -->
			<xsl:when test="attribute::itemprop != ''">
				<xsl:variable name="subj">
					<xsl:choose>
						<xsl:when test="$refSource">
							<xsl:call-template name="getNodeId">
								<xsl:with-param name="node" select="$refSource" />
							</xsl:call-template>
						</xsl:when>
						<xsl:otherwise>
							<xsl:call-template name="getNodeId">
								<xsl:with-param name="node" select="(ancestor::*[attribute::itemscope=''])[last()]" />
							</xsl:call-template>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>

				<!-- TODO itemId of object is not passed to daughter nodes -->
				<xsl:call-template name="relation">
					<xsl:with-param name="subject" select="$subj" />
					<xsl:with-param name="predicate" select="@itemprop" />
					<xsl:with-param name="object" select="$itemId" />
				</xsl:call-template>
				<xsl:if test="attribute::itemtype != ''">
					<xsl:call-template name="class">
						<xsl:with-param name="resource" select="$itemId" />
						<xsl:with-param name="class" select="@itemtype" />
					</xsl:call-template>
				</xsl:if>
			</xsl:when>
			<xsl:when test="attribute::itemtype != ''">
				<xsl:call-template name="class">
					<xsl:with-param name="resource" select="$itemId" />
					<xsl:with-param name="class" select="@itemtype" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:if test="attribute::itemref">
			<xsl:call-template name="getReferences">
				<xsl:with-param name="references" select="@itemref" />
				<xsl:with-param name="refSource" select="." />
			</xsl:call-template>
		</xsl:if>
		<xsl:apply-templates select="*" />
	</xsl:template>

	<!-- datatype properties -->
	<!-- TODO datatype mappings -->
	<xsl:template match="*[attribute::itemprop != '' and not(attribute::itemscope)]">
		<xsl:param name="refSource"/>
		<xsl:variable name="subj">
			<xsl:choose>
				<xsl:when test="$refSource">
					<xsl:call-template name="getNodeId">
						<xsl:with-param name="node" select="$refSource"/>
					</xsl:call-template>
				</xsl:when>
				<xsl:otherwise>
					<xsl:call-template name="getNodeId">
						<xsl:with-param name="node" select="(ancestor::*[attribute::itemscope=''])[last()]"/>
					</xsl:call-template>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="propVal">
			<xsl:choose>
				<xsl:when test="attribute::content">
					<xsl:value-of select="attribute::content" />
				</xsl:when>
				<!--  TODO test element names too? -->
				<xsl:when test="attribute::src">
					<xsl:call-template name="resolveUri">
						<xsl:with-param name="base" select="$this" />
						<xsl:with-param name="ref" select="attribute::src" />
					</xsl:call-template>
				</xsl:when>
				<xsl:when test="attribute::href != ''">
					<xsl:call-template name="resolveUri">
						<xsl:with-param name="base" select="$this" />
						<xsl:with-param name="ref" select="attribute::href" />
					</xsl:call-template>
				</xsl:when>
				<xsl:when test="attribute::datetime">
					<!-- TODO datetime datatype declaration, value conversion necessary? -->
					<xsl:value-of select="attribute::datetime"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="." />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:call-template name="property">
			<xsl:with-param name="subject" select="$subj" />
			<xsl:with-param name="predicate" select="@itemprop" />
			<xsl:with-param name="object" select="$propVal" />
		</xsl:call-template>
	</xsl:template>

	<!-- named templates to process URIs and token lists - - - - - - - - - - -->

	<!-- tokenize a string using space as a delimiter -->
	<xsl:template name="tokenize">
		<xsl:param name="string" />
		<xsl:if test="string-length($string)>0">
			<xsl:choose>
				<xsl:when test="contains($string,' ')">
					<xsl:value-of select="normalize-space(substring-before($string,' '))" />
					<xsl:call-template name="tokenize">
						<xsl:with-param name="string"
							select="normalize-space(substring-after($string,' '))" />
					</xsl:call-template>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$string" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>

	<!-- get file location from URL -->
	<xsl:template name="get-location">
		<xsl:param name="url" />
		<xsl:if test="string-length($url)>0 and contains($url,'/')">
			<xsl:value-of select="concat(substring-before($url,'/'),'/')" />
			<xsl:call-template name="get-location">
				<xsl:with-param name="url" select="substring-after($url,'/')" />
			</xsl:call-template>
		</xsl:if>
	</xsl:template>

	<!-- get root location from URL -->
	<xsl:template name="get-root">
		<xsl:param name="url" />
		<xsl:choose>
			<xsl:when test="contains($url,'//')">
				<xsl:value-of
					select="concat(substring-before($url,'//'),'//',substring-before(substring-after($url,'//'),'/'),'/')" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="concat($url,'/')" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- returns the first token in a list separated by spaces -->
	<xsl:template name="get-first-token">
		<xsl:param name="tokens" />
		<xsl:if test="string-length($tokens)>0">
			<xsl:choose>
				<xsl:when test="contains($tokens,' ')">
					<xsl:value-of select="normalize-space(substring-before($tokens,' '))" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$tokens" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>

	<!-- returns the namespace for a predicate -->
	<xsl:template name="get-predicate-ns">
		<xsl:param name="qname" />
		<xsl:choose>
			<xsl:when test="contains($qname,'#')">
				<xsl:value-of select="concat(substring-before($qname,'#'),'#')"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="uriPrefix">
					<xsl:call-template name="longestPrefix">
						<xsl:with-param name="string" select="$qname"/>
						<xsl:with-param name="sep" select="'/'"/>
					</xsl:call-template>
				</xsl:variable>
				<xsl:choose>
					<xsl:when test="$uriPrefix = ''">
						<xsl:value-of select="$this_location"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="concat($uriPrefix,'/')"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!--  extract atomic name from URI name -->
	<xsl:template name="getAtomicPredName">
		<xsl:param name="qname"/>
		<xsl:choose>
			<xsl:when test="contains($qname,'#')">
				<xsl:value-of select="substring-after($qname,'#')"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="lastIndexOf">
					<xsl:with-param name="string" select="$qname"/>
					<xsl:with-param name="sep" select="'/'"/>
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!-- expand the name for a predicate -->
	<xsl:template name="get-expanded-predicate-name">
		<xsl:param name="qname" />
		<!--  TODO multiple type declarations (lists) or missing type -->
		<xsl:variable name="typeUri" select="(ancestor::*[@itemscope='' and @itemtype!=''])[last()]/@itemtype"/>
		<xsl:choose>
			<xsl:when test="$typeUri != ''">
				<xsl:call-template name="resolveUri">
					<xsl:with-param name="base" select="$typeUri"/>
					<xsl:with-param name="ref" select="$qname"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="concat($this_location,$qname)" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="getNodeId">
		<xsl:param name="node" />
		<xsl:choose>
			<xsl:when test="$node">
				<xsl:choose>
					<xsl:when test="$node/@itemid != ''">
						<xsl:value-of select="$node/@itemid" />
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="concat('blank:node:',generate-id($node))" />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$this" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="getReferences">
		<xsl:param name="references"/>
		<xsl:param name="refSource"/>
		<xsl:variable name="oneReference">
			<xsl:call-template name="get-first-token">
				<xsl:with-param name="tokens" select="$references" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:for-each select="//*[@id = $oneReference]">
			<xsl:apply-templates select=".">
				<xsl:with-param name="refSource" select="$refSource"/>
			</xsl:apply-templates>
		</xsl:for-each>
		<!-- recursive call for multiple references -->
		<xsl:variable name="otherRefs"
			select="normalize-space(substring-after($references,' '))" />
		<xsl:if test="string-length($otherRefs)>0">
			<xsl:call-template name="getReferences">
				<xsl:with-param name="references" select="$otherRefs" />
				<xsl:with-param name="refSource" select="$refSource"/>
			</xsl:call-template>
		</xsl:if>	
	</xsl:template>
	
	<!-- named templates to generate RDF - - - - - - - - - - - - - - - - - - -->

	<!-- generate an RDF statement for a relation -->
	<xsl:template name="relation">
		<xsl:param name="subject" />
		<xsl:param name="predicate" />
		<xsl:param name="object" />

		<!-- test for multiple predicates -->
		<xsl:variable name="single-predicate">
			<xsl:call-template name="get-first-token">
				<xsl:with-param name="tokens" select="$predicate" />
			</xsl:call-template>
		</xsl:variable>	
		<xsl:variable name="expandedPredName">
			<xsl:call-template name="get-expanded-predicate-name">
				<xsl:with-param name="qname" select="$predicate"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="atomicPredName">
			<xsl:call-template name="getAtomicPredName">
				<xsl:with-param name="qname" select="$expandedPredName"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="predNs">
			<xsl:call-template name="get-predicate-ns">
				<xsl:with-param name="qname" select="$expandedPredName"/>
			</xsl:call-template>
		</xsl:variable>
		
		<xsl:element name="rdf:Description">
			<xsl:choose>
				<xsl:when test="starts-with($subject,'blank:node:')">
					<xsl:attribute name="rdf:nodeID"><xsl:value-of
						select="substring-after($subject,'blank:node:')" /></xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="rdf:about"><xsl:value-of
						select="$subject" /></xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:element name="{$atomicPredName}" namespace="{$predNs}">
				<xsl:choose>
					<xsl:when test="starts-with($object,'blank:node:')">
						<xsl:attribute name="rdf:nodeID"><xsl:value-of
							select="substring-after($object,'blank:node:')" /></xsl:attribute>
					</xsl:when>
					<xsl:otherwise>
						<xsl:attribute name="rdf:resource"><xsl:value-of
							select="$object" /></xsl:attribute>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>
		</xsl:element>

		<!-- recursive call for multiple predicates -->
		<xsl:variable name="other-predicates"
			select="normalize-space(substring-after($predicate,' '))" />
		<xsl:if test="string-length($other-predicates)>0">
			<xsl:call-template name="relation">
				<xsl:with-param name="subject" select="$subject" />
				<xsl:with-param name="predicate" select="$other-predicates" />
				<xsl:with-param name="object" select="$object" />
			</xsl:call-template>
		</xsl:if>

	</xsl:template>


	<!-- generate an RDF statement for a property -->
	<xsl:template name="property">
		<xsl:param name="subject" />
		<xsl:param name="predicate" />
		<xsl:param name="object" />
		<xsl:param name="datatype" />
		<xsl:param name="attrib" /> <!-- is the content from an attribute ? true /false -->
		<xsl:param name="language" />

		<!-- test for multiple predicates -->
		<xsl:variable name="single-predicate">
			<xsl:call-template name="get-first-token">
				<xsl:with-param name="tokens" select="$predicate" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="expandedPredName">
			<xsl:call-template name="get-expanded-predicate-name">
				<xsl:with-param name="qname" select="$predicate"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="atomicPredName">
			<xsl:call-template name="getAtomicPredName">
				<xsl:with-param name="qname" select="$expandedPredName"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="predNs">
			<xsl:call-template name="get-predicate-ns">
				<xsl:with-param name="qname" select="$expandedPredName"/>
			</xsl:call-template>
		</xsl:variable>
		
	<!-- TODO expand property name with URI -->
		<xsl:element name="rdf:Description">
			<xsl:choose>
				<xsl:when test="starts-with($subject,'blank:node:')">
					<xsl:attribute name="rdf:nodeID"><xsl:value-of
						select="substring-after($subject,'blank:node:')" /></xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="rdf:about"><xsl:value-of
						select="$subject" /></xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:element name="{$atomicPredName}" namespace="{$predNs}">
				<xsl:if test="string-length($language)>0">
					<xsl:attribute name="xml:lang"><xsl:value-of
						select="$language" /></xsl:attribute>
				</xsl:if>
				<xsl:choose>
					<xsl:when
						test="$datatype='http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral'">
						<xsl:choose>
							<xsl:when test="$attrib='true'"> <!-- content is in an attribute -->
								<xsl:value-of select="normalize-space(string($object))" />
							</xsl:when>
							<xsl:otherwise> <!-- content is in the element and may include some tags -->
								<!-- <attribute name="rdf:datatype"><value-of select="$datatype" 
									/></attribute> -->
								<attribute name="rdf:parseType">
									<value-of select="'Literal'" />
								</attribute>
								<xsl:value-of select="normalize-space($object)" />
							</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<xsl:when test="string-length($datatype)>0">
						<!-- there is a datatype other than XMLLiteral -->
						<xsl:attribute name="rdf:datatype"><xsl:value-of
							select="$datatype" /></xsl:attribute>
						<xsl:choose>
							<xsl:when test="$attrib='true'"> <!-- content is in an attribute -->
								<xsl:value-of select="normalize-space(string($object))" />
							</xsl:when>
							<xsl:otherwise> <!-- content is in the text nodes of the element -->
								<xsl:value-of select="normalize-space($object)" />
							</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<xsl:otherwise> <!-- there is no datatype -->
						<xsl:choose>
							<xsl:when test="$attrib='true'"> <!-- content is in an attribute -->
								<xsl:value-of select="normalize-space(string($object))" />
							</xsl:when>
							<xsl:otherwise> <!-- content is in the text nodes of the element -->
								<xsl:value-of select="normalize-space($object)" />
							</xsl:otherwise>
						</xsl:choose>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>
		</xsl:element>

		<!-- recursive call for multiple predicates -->
		<xsl:variable name="other-predicates"
			select="normalize-space(substring-after($predicate,' '))" />
		<xsl:if test="string-length($other-predicates)>0">
			<xsl:call-template name="property">
				<xsl:with-param name="subject" select="$subject" />
				<xsl:with-param name="predicate" select="$other-predicates" />
				<xsl:with-param name="object" select="$object" />
				<xsl:with-param name="datatype" select="$datatype" />
				<xsl:with-param name="attrib" select="$attrib" />
				<xsl:with-param name="language" select="$language" />
			</xsl:call-template>
		</xsl:if>

	</xsl:template>



	<!-- generate an RDF statement for a class -->
	<xsl:template name="class">
		<xsl:param name="resource" />
		<xsl:param name="class" />

		<!-- case multiple classes -->
		<xsl:variable name="single-class">
			<xsl:call-template name="get-first-token">
				<xsl:with-param name="tokens" select="$class" />
			</xsl:call-template>
		</xsl:variable>

		<xsl:element name="rdf:Description">
			<xsl:choose>
				<xsl:when test="starts-with($resource,'blank:node:')">
					<xsl:attribute name="rdf:nodeID"><xsl:value-of
						select="substring-after($resource,'blank:node:')" /></xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="rdf:about"><xsl:value-of
						select="$resource" /></xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:element name="rdf:type">
				<xsl:attribute name="rdf:resource">
					<xsl:call-template name="correctURI">
						<xsl:with-param name="uri" select="$class"/>
					</xsl:call-template>
<!-- 				<xsl:value-of select="$class" /> -->
				</xsl:attribute>
			</xsl:element>
		</xsl:element>

		<!-- recursive call for multiple classes -->
		<xsl:variable name="other-classes"
			select="normalize-space(substring-after($class,' '))" />
		<xsl:if test="string-length($other-classes)>0">
			<xsl:call-template name="class">
				<xsl:with-param name="resource" select="$resource" />
				<xsl:with-param name="class" select="$other-classes" />
			</xsl:call-template>
		</xsl:if>

	</xsl:template>


	<!-- ignore the rest of the DOM -->
	<xsl:template match="*|text()|@*">
		<xsl:param name="refSource"/>
		<xsl:apply-templates>
			<xsl:with-param name="refSource" select="$refSource"/>
		</xsl:apply-templates>
	</xsl:template>

	<xsl:template name="resolveUri">
		<xsl:param name="base" />
		<xsl:param name="ref" />
		<xsl:variable name="base2">
			<xsl:call-template name="correctURI">
				<xsl:with-param name="uri" select="$base"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="starts-with($ref,'#')">
				<xsl:value-of select="concat($base2,$ref)" />
			</xsl:when>
			<xsl:when test="not(contains($ref,':/'))">
				<!-- TODO: remove double slashes? -->
				<xsl:variable name="baseUri">
					<xsl:call-template name="longestPrefix">
						<xsl:with-param name="string" select="$base2" />
						<xsl:with-param name="sep" select="'/'" />
					</xsl:call-template>
				</xsl:variable>
				<xsl:choose>
					<xsl:when test="starts-with($ref,'//')">
						<!-- prefix is just the protocol part -->
						<!-- <xsl:value-of select="concat(substring-before($baseUri,':'),':',$ref)"/> -->
						<!-- hard code the prefix for Stanbol because there we will not see 
							the real URLs anyway -->
						<xsl:value-of select="concat('http:',$ref)" />
					</xsl:when>
					<xsl:when test="starts-with($ref,'/')">
						<xsl:value-of select="concat($baseUri,$ref)" />
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="concat($baseUri,'/',$ref)" />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$ref" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="longestPrefix">
		<xsl:param name="string" />
		<xsl:param name="sep" />
		<xsl:variable name="lastSeg">
			<xsl:call-template name="lastIndexOf">
				<xsl:with-param name="string" select="$string" />
				<xsl:with-param name="sep" select="$sep" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:value-of
			select="substring($string,1,string-length($string) - string-length($sep) - string-length($lastSeg))" />
	</xsl:template>

	<!-- returns the substring after the last occurrence of the separator -->
	<xsl:template name="lastIndexOf">
		<xsl:param name="string" />
		<xsl:param name="sep" />
		<xsl:choose>
			<xsl:when test="contains($string, $sep)">
				<xsl:call-template name="lastIndexOf">
					<xsl:with-param name="string"
						select="substring-after($string, $sep)" />
					<xsl:with-param name="sep" select="$sep" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$string" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- hacks to create a formally correct URI from potential garbage -->
	<xsl:template name="correctURI">
		<xsl:param name="uri"/>
		<xsl:choose>
			<xsl:when test="starts-with($uri,'schema.org')">
				<xsl:value-of select="concat('http://',$uri)"/>
			</xsl:when>
			<xsl:when test="substring-before($uri,':') = ''">
				<xsl:call-template name="resolveUri">
					<xsl:with-param name="base" select="$this"/>
					<xsl:with-param name="ref" select="$uri"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$uri"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

</xsl:stylesheet>
