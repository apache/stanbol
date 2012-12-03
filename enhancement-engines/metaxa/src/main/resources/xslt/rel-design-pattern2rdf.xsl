<?xml version="1.0" encoding="utf-8"?>
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
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
  xmlns:nie="http://www.semanticdesktop.org/ontologies/2007/01/19/nie#"
  xmlns:tag="http://aperture.sourceforge.net/ontologies/tagging#"
	xmlns:dc="http://purl.org/dc/elements/1.1/" 
                version="1.0">

	<xsl:import href="functions.xsl"/>
	
  <xsl:output method="xml"/>
  
  <xsl:strip-space elements="*"/>

  <xsl:param name="uri" select="'http://foobar.com/'"/>

  <xsl:template match="text()"/>

	<!-- 
	Generic handler for metadata in rel-design patterns that can occur in the context of other more complex microformats 
	If there are special constraints on the use of such rel-patterns then one should use more specialized transformers 
	-->
	
	<xsl:template match="/">
		<rdf:RDF>
			<tag:Item rdf:about="{$uri}">
				<xsl:apply-templates select="descendant::*[@rel='tag']"/>
				<xsl:apply-templates select="descendant::*[@rel='license']"/>
			</tag:Item>
		</rdf:RDF>
	</xsl:template>

	<!-- rel-tag pattern -->
	<xsl:template match="*[@rel='tag']">
		<xsl:variable name="href" select="@href" />
		<xsl:variable name="hrefForm">
			<xsl:call-template name="resolveUri">
				<xsl:with-param name="base" select="$uri" />
				<xsl:with-param name="ref" select="$href" />
			</xsl:call-template>
		</xsl:variable>
			<tag:hasTag>
				<tag:Tag>
					<xsl:attribute name="rdf:about">
						<xsl:value-of select="$hrefForm"/>
					</xsl:attribute>
					<rdfs:label>
						<xsl:value-of select="normalize-space(.)"/>
					</rdfs:label>
				</tag:Tag>
			</tag:hasTag>
	</xsl:template>

	<!-- rel-license pattern -->
	<xsl:template match="*[@rel='license']">
		<xsl:variable name="href" select="@href" />
		<xsl:variable name="hrefForm">
			<xsl:call-template name="resolveUri">
				<xsl:with-param name="base" select="$uri" />
				<xsl:with-param name="ref" select="$href" />
			</xsl:call-template>
		</xsl:variable>		
		<dc:license rdf:resource="{$hrefForm}" />
	</xsl:template>

	<!--  TODO: rel-bookmark, rel-me, rel-contact, ... ? -->
	
</xsl:stylesheet>
