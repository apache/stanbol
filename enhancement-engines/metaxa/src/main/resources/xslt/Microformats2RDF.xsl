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
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
  xmlns:nfo="http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#"
  xmlns:nie="http://www.semanticdesktop.org/ontologies/2007/01/19/nie#"
  >

	<xsl:import href="hcard2rdf.xsl"/>
  <xsl:import href="geo2rdf.xsl"/>
	<xsl:import href="hreview2rdf.xsl"/>
	<xsl:import href="hcal2rdf.xsl"/>
	<xsl:import href="xfolk2rdf.xsl"/>
  	<xsl:import href="hatom2rdf.xsl"/>
	<xsl:import href="rel-design-pattern2rdf.xsl"/>
	
	<!-- TODO: hAtom (hentry), rel-bookmark, ... -->
	<!--  TODO identify multiple feature values in lists (requires to keep track of visited nodes?) -->
	
	<xsl:output method="xml" omit-xml-declaration="yes"/>
  
  <xsl:strip-space elements="*"/>

  <xsl:param name="uri" select="'http://foobar.com/'"/>

  <xsl:template match="text()"/>

	<xsl:template match="/">
		<rdf:RDF>
			<nfo:HtmlDocument rdf:about="{$uri}">
				<xsl:apply-templates>
					<xsl:with-param name="outer" select="'contains'"/>
					<xsl:with-param name="ns" select="'http://www.semanticdesktop.org/ontologies/2007/01/19/nie#'"/>
					<!--  xsltproc ignoriert ns; sun-xalan erzeugt im Output neues Namespace-Prefix bei jedem Aufruf
					Jena sucht sich irgendeinen davon aus -->
				</xsl:apply-templates>
			</nfo:HtmlDocument>
		</rdf:RDF>
	</xsl:template>

</xsl:stylesheet>