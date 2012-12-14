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
  xmlns:exslt="http://exslt.org/common"
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
  xmlns:nfo="http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#"
  xmlns:nie="http://www.semanticdesktop.org/ontologies/2007/01/19/nie#"
  xmlns:tag="http://aperture.sourceforge.net/ontologies/tagging#"
  xmlns:atom="http://www.w3.org/2005/Atom#"
  >

	<xsl:import href="functions.xsl"/>
	<xsl:import href="datetime.xsl"/>
	<xsl:import href="htmltextextract.xsl"/>
	<xsl:import href="hcard2rdf.xsl"/>
	
  <xsl:output method="xml"/>
  
  <xsl:strip-space elements="*"/>

  <xsl:param name="uri" select="'http://foobar.com/'"/>
  
  <xsl:variable name="AtomNS">http://www.w3.org/2005/Atom#</xsl:variable>
	<xsl:variable name="XdtNS">http://www.w3.org/2001/XMLSchema#</xsl:variable>

  <xsl:template match="text()"/>
  <xsl:template match="text()" mode="hatom-toplevel"/>
  <xsl:template match="text()" mode="extract-hatom"/>
  
	<xsl:template match="/">
		<rdf:RDF>
			<tag:Item rdf:about="{$uri}">
				<xsl:apply-templates select="descendant::*[contains(concat(' ',normalize-space(@class),' '),' hentry ')]">
					<xsl:with-param name="outer">contains</xsl:with-param>
					<xsl:with-param name="ns"  select="'http://www.semanticdesktop.org/ontologies/2007/01/19/nie#'"/>
				</xsl:apply-templates>
			</tag:Item>
		</rdf:RDF>
	</xsl:template>

	<xsl:template match="*[contains(concat(' ',normalize-space(@class),' '),' hentry ')]"> 
		<xsl:param name="outer" />
		<xsl:param name="ns"/>
		<xsl:choose>
			<xsl:when test="$outer and $ns">
				<xsl:element name="{$outer}" namespace="{$ns}">
					<xsl:apply-templates select="." mode="hatom-toplevel" />
				</xsl:element>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="." mode="hatom-toplevel" />
			</xsl:otherwise>
		</xsl:choose>	
	</xsl:template>

	<xsl:template match="*" mode="hatom-toplevel">
		<atom:Entry>
			<xsl:apply-templates mode="extract-hatom" />
			<xsl:apply-templates>			
				<xsl:with-param name="outer" select="'contains'"/>
				<xsl:with-param name="ns" select="'http://www.semanticdesktop.org/ontologies/2007/01/19/nie#'"/>
			</xsl:apply-templates>
		</atom:Entry>
	</xsl:template>
	
	<!--  atom spec looks complicated  hentry simpler? -->
	<!--  fields: entry-title, entry-content, entry-summary, updated (datetime), published (datetime), author (hCard), bookmark (rel-bookmark), tags -->
	<xsl:template match="*" mode="extract-hatom">
			<xsl:variable name="link">
				<xsl:call-template name="checkClass">
					<xsl:with-param name="class" select="@rel"/>
					<xsl:with-param name="val" select="' bookmark '"/>
				</xsl:call-template>
			</xsl:variable>
			<xsl:variable name="title">
				<xsl:call-template name="checkClass">
					<xsl:with-param name="val" select="' entry-title '"/>
				</xsl:call-template>
			</xsl:variable>
			<!--  TODO: might be several lines -->
			<xsl:variable name="content">
				<xsl:call-template name="checkClass">
					<xsl:with-param name="val" select="' entry-content '"/>
				</xsl:call-template>
			</xsl:variable>
			<xsl:variable name="summary">
				<xsl:call-template name="checkClass">
					<xsl:with-param name="val" select="' entry-summary '"/>
				</xsl:call-template>
			</xsl:variable>
			<xsl:variable name="updated">
				<xsl:call-template name="checkClass">
					<xsl:with-param name="val" select="' updated '"/>
				</xsl:call-template>
			</xsl:variable>
			<xsl:variable name="published">
				<xsl:call-template name="checkClass">
					<xsl:with-param name="val" select="' published '"/>
				</xsl:call-template>
			</xsl:variable>
			<xsl:variable name="author">
				<xsl:call-template name="checkClass">
					<xsl:with-param name="val" select="' author '"/>
				</xsl:call-template>
			</xsl:variable>
			<xsl:if test="$title != 0">
				<atom:title>
					<xsl:value-of select="normalize-space(.)"/>
				</atom:title>
			</xsl:if>
			<xsl:if test="$content != 0">
				<atom:content>
					<xsl:apply-templates mode="textextract"/>
				</atom:content>
			</xsl:if>
			<xsl:if test="$summary != 0">
				<atom:summary>
					<xsl:value-of select="normalize-space(.)"/>
				</atom:summary>
			</xsl:if>
			<xsl:if test="$updated != 0">
				<atom:updated>
					<xsl:call-template name="get-datetime"/>
				</atom:updated>
			</xsl:if>
			<xsl:if test="$published != 0">
				<atom:published>
					<xsl:call-template name="get-datetime"/>
				</atom:published>
			</xsl:if>
			<xsl:if test="$author != 0">
				<atom:author>
					<xsl:apply-templates select="." mode="vcard-toplevel"/>
					<xsl:call-template name="addVisited">
						<xsl:with-param name="id" select="generate-id(.)"/>
					</xsl:call-template>
				</atom:author>
			</xsl:if>
			<xsl:if test="$link != 0">
				<atom:link>
					<xsl:attribute name="rdf:resource">
						<xsl:call-template name="resolveUri">
							<xsl:with-param name="base" select="$uri" />
							<xsl:with-param name="ref" select="@href" />
						</xsl:call-template>
					</xsl:attribute>
				</atom:link>
			</xsl:if>
			<!--  might lead to duplication on vcard?  -->
		<xsl:apply-templates mode="extract-hatom"/>
	</xsl:template>

	<!--  from hcal2rdf -->
	<xsl:template name="get-datetime">
		<xsl:variable name="when">
			<xsl:choose>
				<xsl:when test="@title">
					<xsl:value-of select="normalize-space(@title)" />
				</xsl:when>
				<xsl:when
					test="descendant::*[contains(concat(' ',@class,' '),' value ')]">
					<!--
						TODO: datetime might be distributed over several class-value
						elements
					-->
					<xsl:call-template name="extract-value-class" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="normalize-space(.)" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="date">
			<xsl:call-template name="getDate">
				<xsl:with-param name="string" select="$when" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="time">
			<xsl:call-template name="getTime">
				<xsl:with-param name="string" select="$when" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when
				test="string-length($date) &gt; 0 and string-length($time) &gt; 0">
				<xsl:attribute name="rdf:datatype">
	    			<xsl:value-of select='concat($XdtNS, "dateTime")' />
	  			</xsl:attribute>
				<xsl:value-of select="concat($date,'T',$time)" />
			</xsl:when>
			<xsl:when test="string-length($date)">
				<xsl:attribute name="rdf:datatype">
	    			<xsl:value-of select='concat($XdtNS, "date")' />
	  			</xsl:attribute>
				<xsl:value-of select='$date' />
			</xsl:when>
			<!-- TODO resolve time against date? -->
			<xsl:when test="string-length($time)">
				<xsl:attribute name="rdf:datatype">
	    			<xsl:value-of select='concat($XdtNS, "time")' />
	  			</xsl:attribute>
				<xsl:value-of select='$time' />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$when" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
</xsl:stylesheet>