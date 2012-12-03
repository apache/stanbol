<?xml version="1.0" encoding="ISO-8859-1"?>
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
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:geo="http://www.w3.org/2003/01/geo/wgs84_pos#"
	xmlns:review="http://www.purl.org/stuff/rev#"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:dc="http://purl.org/dc/elements/1.1/" 
	xmlns:dcterms="http://purl.org/dc/dcmitype/" 
	xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" 
	xmlns:foaf="http://xmlns.com/foaf/0.1/" 
	xmlns:vcard="http://www.w3.org/2006/vcard/ns#"
	xmlns:tag="http://www.holygoat.co.uk/owl/redwood/0.1/tags/"
>
	<xsl:import href="functions.xsl"/>
	<xsl:import href="rel-tag2rdf.xsl"/>
	<xsl:import href="rel-license2rdf.xsl"/>
	<xsl:import href="hcard2rdf.xsl"/>

	<xsl:output method="xml"/>

	<xsl:param name="uri" select="'http://foobar.com'"/>
	
	<xsl:template match="/">
		<rdf:RDF>
			<xsl:apply-templates select="descendant::*[contains(concat(' ',normalize-space(@class),' '),' hreview ')]"/>
		</rdf:RDF>
	</xsl:template>

	
	<xsl:template match="*[contains(concat(' ',normalize-space(@class),' '),' hreview ')]">
		<xsl:param name="outer"/>
		<xsl:param name="ns"/>		
		<xsl:choose>
			<xsl:when test="$outer and $ns">
				<xsl:element name="{$outer}" namespace="{$ns}">
					<xsl:apply-templates select="." mode="hreview-toplevel" />
				</xsl:element>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="." mode="hreview-toplevel" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="*" mode="hreview-toplevel">
		<review:Review>
			<xsl:apply-templates mode="extract-hreview"/>
		</review:Review>
	</xsl:template>
	
	<xsl:template match="*" mode="extract-hreview">
		<!-- <xsl:variable name="version" select="contains(concat(' ',normalize-space(@class),' '),' version ')"/> -->
		<xsl:variable name="summary" select="contains(concat(' ',normalize-space(@class),' '),' summary ')"/>
		<xsl:variable name="type" select="contains(concat(' ',normalize-space(@class),' '),' type ')"/>
		<xsl:variable name="item" select="contains(concat(' ',normalize-space(@class),' '),' item ')"/>
		<xsl:variable name="reviewer" select="contains(concat(' ',normalize-space(@class),' '),' reviewer ')"/>
		<xsl:variable name="dtreviewed" select="contains(concat(' ',normalize-space(@class),' '),' dtreviewed ')"/>
		<xsl:variable name="rating" select="contains(concat(' ',normalize-space(@class),' '),' rating ')"/>
		<xsl:variable name="description" select="contains(concat(' ',normalize-space(@class),' '),' description ')"/>
		<xsl:variable name="tags" select="contains(concat(' ',normalize-space(@rel),' '),' tag ')"/>
		<xsl:variable name="permalink" select="contains(concat(' ',normalize-space(@rel),' '),' bookmark ')"/>
		<xsl:variable name="license" select="contains(concat(' ',normalize-space(@rel),' '),' license ')"/>

<!-- Handle permalink as resource?  -->
		<xsl:if test="$permalink">
		</xsl:if>
		<xsl:if test="$summary">
			<rdfs:label>
				<xsl:value-of select="normalize-space(.)"/>
			</rdfs:label>
		</xsl:if>
		<xsl:if test="$description">
			<review:text>
				<xsl:value-of select="normalize-space(.)"/>
			</review:text>
		</xsl:if>
		<xsl:if test="$dtreviewed">
			<review:createdOn>
				<xsl:choose>
					<xsl:when test="@title">
					<!-- TODO: convert time?  -->
						<xsl:value-of select="@title"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="normalize-space(.)"/>
					</xsl:otherwise>
				</xsl:choose>
			</review:createdOn>
		</xsl:if>
		<xsl:if test="$rating">
		<!-- hasRating/Rating are my extensions of the Review ontology to accommodate multidimensional complex ratings. -->
			<review:hasRating>
				<review:Rating>
					<review:rating rdf:datatype="http://www.w3.org/2001/XMLSchema#float">
						<xsl:call-template name="extract-value-class"/>
					</review:rating>
					<xsl:if test="descendant::*[@class='best']">
						<review:maxRating rdf:datatype="http://www.w3.org/2001/XMLSchema#float">
							<xsl:value-of select="descendant::*[@class='best']"/>
						</review:maxRating>
					</xsl:if>
					<xsl:if test="descendant::*[@class='worst']">
						<review:minRating rdf:datatype="http://www.w3.org/2001/XMLSchema#float">
							<xsl:value-of select="descendant::*[@class='worst']"/>
						</review:minRating>
					</xsl:if>
					<xsl:apply-templates/>
				</review:Rating>
			</review:hasRating>
		</xsl:if>
		<xsl:if test="$type">
			<dc:type>
			<xsl:choose>
				<xsl:when test="@title">
					<!-- TODO: check values/implied types?  -->
					<xsl:value-of select="@title"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="normalize-space(.)"/>
				</xsl:otherwise>
			</xsl:choose>
			</dc:type>
		</xsl:if>
		<xsl:if test="$license">
			<xsl:apply-templates select="."/>
		</xsl:if>
		<xsl:if test="$tags">
			<xsl:choose>
				<xsl:when test="ancestor::*[@class='rating']"/>
				<xsl:otherwise>
					<xsl:apply-templates select="."/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
		<xsl:if test="$item">
			<xsl:choose>
				<xsl:when test="contains(concat(' ',normalize-space(@class),' '),' vcard ')">
				<!--  attach vcard properties to toplevel -->
					<xsl:apply-templates mode="extract-vcard"/>
				</xsl:when>
				<!--  TODO: other item types: fn, url, photo, hevent, inferred types?
				vcard = business or person
				hevent = event -->
				<xsl:when test="contains(concat(' ',normalize-space(@class),' '),' hevent ')">
				</xsl:when>
				<!--  these must be within the element, so check subtree (will become complicated) -->
				<xsl:when test="contains(concat(' ',normalize-space(@class),' '),' fn ')">
				</xsl:when>				
				<xsl:when test="contains(concat(' ',normalize-space(@class),' '),' url ')">
				</xsl:when>
				<xsl:when test="contains(concat(' ',normalize-space(@class),' '),' photo ')">
				</xsl:when>
			</xsl:choose>
		</xsl:if>
		<xsl:if test="$reviewer">
			<review:reviewer>
				<xsl:apply-templates select="."/>
			</review:reviewer>
		</xsl:if>
		<xsl:apply-templates mode="extract-hreview"/>
	</xsl:template>
	
	<xsl:template match="text()"/>
	<xsl:template match="text()" mode="extract-hreview"/>
	<xsl:template match="text()" mode="hreview-toplevel"/>
</xsl:stylesheet>