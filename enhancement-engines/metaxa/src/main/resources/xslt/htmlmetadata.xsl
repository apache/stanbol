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
  xmlns:str="http://exslt.org/strings"
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
  xmlns:nie="http://www.semanticdesktop.org/ontologies/2007/01/19/nie#"
  xmlns:nco="http://www.semanticdesktop.org/ontologies/2007/3/22/nco#"
  xmlns:nfo="http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#"
  extension-element-prefixes="str"
  version="1.0">

  <xsl:output method="xml"/>

  <!-- not good for text extraction?
  <xsl:strip-space elements="*"/>
  -->

  <xsl:param name="uri" select="'http://foobar.com/'"/>
  <xsl:param name="extractText" select="true()"/>
  
  <xsl:template match="/">
    <rdf:RDF>
			<nfo:HtmlDocument rdf:about="{$uri}">
				<xsl:apply-templates select="html/head"/>
				<xsl:if test="$extractText">
					<nie:plainTextContent>
						<xsl:apply-templates select="html/body" mode="textextract"/>
					</nie:plainTextContent>
				</xsl:if>
			</nfo:HtmlDocument>
	  </rdf:RDF>
  </xsl:template>

  <xsl:template match="title">
  	<nie:title>
  		<xsl:value-of select="normalize-space(.)"/>
  	</nie:title>
  </xsl:template>

  <xsl:template match="meta[@name='author']">  
  	<nco:creator>
  		<nco:Contact>
  			<nco:fullname>
  				<xsl:value-of select="normalize-space(@content)"/>
  			</nco:fullname>
  		</nco:Contact>
  	</nco:creator>
  </xsl:template>

  <xsl:template match="meta[@name='keywords']">
    <xsl:for-each select="str:tokenize(@content,',')">
    	<nie:keyword>
    		<xsl:value-of select="normalize-space(.)"/>
    	</nie:keyword>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="meta[@name='description']">
  	<nie:description>
  		<xsl:value-of select="normalize-space(@content)"/>
  	</nie:description> 
  </xsl:template>

  <xsl:template match="text()"/>

<!-- Rules for formated text extraction -->
	
	<xsl:template match="tr|td|th|dt|dd" mode="textextract">
		<xsl:text>
</xsl:text>
		<xsl:apply-templates mode="textextract"/>
	</xsl:template>
	
	<xsl:template match="div|h1|h2|h3|h4|h5|h6|p|li" mode="textextract">
		<xsl:text>
</xsl:text>
		<xsl:apply-templates mode="textextract"/>
		<xsl:text>
</xsl:text>
	</xsl:template>

  <xsl:template match="br" mode="textextract">
    <xsl:text>
</xsl:text>
  </xsl:template>
  
  <xsl:template match="pre" mode="textextract">
    <xsl:value-of select="."/>
  </xsl:template>
  	
	<xsl:template match="*" mode="textextract">
		<xsl:apply-templates mode="textextract"/>
	</xsl:template>
	
	<xsl:template match="text()" mode="textextract">
		<xsl:call-template name="normalizeWhitespace">
			<xsl:with-param name="str" select="string(.)" />
		</xsl:call-template><xsl:text> </xsl:text>
	</xsl:template>

	<xsl:template name="normalizeWhitespace">
		<xsl:param name="str"/>
		<xsl:value-of select="normalize-space(translate($str,'&#160;',' '))"/>	
	</xsl:template>

</xsl:stylesheet>
