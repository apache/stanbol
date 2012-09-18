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
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 xmlns:rdf   ="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
 xmlns:cal   ="http://www.w3.org/2002/12/cal/icaltzd#"
		xmlns:vcard ="http://www.w3.org/2006/vcard/ns#"
>

	<xsl:import href="datetime.xsl"/>
	<xsl:import href="functions.xsl"/>
	
	<xsl:output method="xml" indent="yes"/>

	<xsl:param name="uri" select="'http://foobar.com'"/>

	<xsl:variable name="CalNS">http://www.w3.org/2002/12/cal/icaltzd#</xsl:variable>
	<xsl:variable name="XdtNS">http://www.w3.org/2001/XMLSchema#</xsl:variable>
	
	<xsl:template match="/">
		<rdf:RDF>
			<cal:Vcalendar>
				<xsl:apply-templates select="descendant::*[contains(concat(' ',normalize-space(@class),' '),' vevent ')
								or contains(concat(' ',normalize-space(@class),' '),' vtodo ')
								or contains(concat(' ',normalize-space(@class),' '),' vjournal ')]"/>
			</cal:Vcalendar>
		</rdf:RDF>
	</xsl:template>

	<!-- vcalendar toplevel: -->
	<xsl:template match="*[contains(concat(' ',normalize-space(@class),' '),' vcalendar ')]">
		<xsl:apply-templates select="descendant::*[contains(concat(' ',normalize-space(@class),' '),' vevent ')
								or contains(concat(' ',normalize-space(@class),' '),' vtodo ')
								or contains(concat(' ',normalize-space(@class),' '),' vjournal ')]"/>
	</xsl:template>
	
	<xsl:template match="*[contains(concat(' ',normalize-space(@class),' '),' vevent ')]">
		<cal:component>
			<cal:Vevent>
				<xsl:apply-templates mode="extract-hcal"/>
			</cal:Vevent>
		</cal:component>
	</xsl:template>

	<xsl:template match="*[contains(concat(' ',normalize-space(@class),' '),' vtodo ')]">
		<cal:component>
			<cal:Vtodo>
				<xsl:apply-templates mode="extract-hcal"/>
			</cal:Vtodo>
		</cal:component>
	</xsl:template>

	<xsl:template match="*[contains(concat(' ',normalize-space(@class),' '),' vjournal ')]">
		<cal:component>
			<cal:Vjournal>
				<xsl:apply-templates mode="extract-hcal"/>
			</cal:Vjournal>
		</cal:component>
	</xsl:template>


	<xsl:template match="*" mode="extract-hcal">
		<xsl:variable name="summary" select="contains(concat(' ',normalize-space(@class),' '),' summary ')"/>
		<xsl:variable name="dtstart" select="contains(concat(' ',normalize-space(@class),' '),' dtstart ')"/>
		<xsl:variable name="dtend" select="contains(concat(' ',normalize-space(@class),' '),' dtend ')"/>
		<xsl:variable name="duration" select="contains(concat(' ',normalize-space(@class),' '),' duration ')"/>
		<xsl:variable name="category" select="contains(concat(' ',normalize-space(@class),' '),' category ')"/>
		<xsl:variable name="uid" select="contains(concat(' ',normalize-space(@class),' '),' uid ')"/>
		<xsl:variable name="geo" select="contains(concat(' ',normalize-space(@class),' '),' geo ')"/>
		<xsl:variable name="description" select="contains(concat(' ',normalize-space(@class),' '),' description ')"/>
		<xsl:variable name="attendee" select="contains(concat(' ',normalize-space(@class),' '),' attendee ')"/>
		<xsl:variable name="attach" select="contains(concat(' ',normalize-space(@class),' '),' attach ')"/>
		<xsl:variable name="dtstamp" select="contains(concat(' ',normalize-space(@class),' '),' dtstamp ')"/>
		<xsl:variable name="location" select="contains(concat(' ',normalize-space(@class),' '),' location ')"/>
		<xsl:variable name="status" select="contains(concat(' ',normalize-space(@class),' '),' status ')"/>
		<xsl:variable name="method" select="contains(concat(' ',normalize-space(@class),' '),' method ')"/>
		<xsl:variable name="contact" select="contains(concat(' ',normalize-space(@class),' '),' contact ')"/>
		<xsl:variable name="organizer" select="contains(concat(' ',normalize-space(@class),' '),' organizer ')"/>
		<xsl:variable name="class" select="contains(concat(' ',normalize-space(@class),' '),' class ')"/>
	<!-- TODO: there might be few more  and additional patterns -->

	<xsl:if test="$uid">
		<xsl:call-template name="textProp">
			<xsl:with-param name="class">uid</xsl:with-param>
		</xsl:call-template>
	</xsl:if>

	<xsl:if test="$dtstamp">
		<xsl:call-template name="dateProp">
			<xsl:with-param name="class">dtstamp</xsl:with-param>
		</xsl:call-template>
	</xsl:if>
	
	<xsl:if test="$summary">
		<xsl:call-template name="textProp">
			<xsl:with-param name="class">summary</xsl:with-param>
		</xsl:call-template>
	</xsl:if>
		
	<xsl:if test="$description">
		<xsl:call-template name="textProp">
			<xsl:with-param name="class">description</xsl:with-param>
		</xsl:call-template>
	</xsl:if>

	<xsl:if test="$dtstart">
		<xsl:call-template name="dateProp">
			<xsl:with-param name="class">dtstart</xsl:with-param>
		</xsl:call-template>
	</xsl:if>

	<xsl:if test="$dtend">
		<xsl:call-template name="dateProp">
			<xsl:with-param name="class">dtend</xsl:with-param>
		</xsl:call-template>
	</xsl:if>

	<xsl:if test="$duration">
		<xsl:call-template name="durProp">
			<xsl:with-param name="class">duration</xsl:with-param>
		</xsl:call-template>
	</xsl:if>

	<xsl:if test="$location">
		<!--  also as hCard, geo? -->
		<xsl:call-template name="textProp">
			<xsl:with-param name="class">location</xsl:with-param>
		</xsl:call-template>
	</xsl:if>

	<xsl:if test="$status">
		<xsl:call-template name="textProp">
			<xsl:with-param name="class">status</xsl:with-param>
		</xsl:call-template>
	</xsl:if>

	<xsl:if test="$attendee">
	<!--  hCard? partstat -->
		<xsl:call-template name="whoProp">
			<xsl:with-param name="class">attendee</xsl:with-param>
		</xsl:call-template>
	</xsl:if>

	<xsl:if test="$geo">
		<!--  Not geo ontology? -->
		<xsl:call-template name="floatPairProp">
			<xsl:with-param name="class">geo</xsl:with-param>
		</xsl:call-template>
	</xsl:if>

	<xsl:if test="$organizer">
		<xsl:call-template name="textProp">
			<xsl:with-param name="class">organizer</xsl:with-param>
		</xsl:call-template>
	</xsl:if>

	<xsl:if test="$contact">
		<xsl:call-template name="textProp">
			<xsl:with-param name="class">contact</xsl:with-param>
		</xsl:call-template>
	</xsl:if>

	<xsl:if test="$category">
		<xsl:call-template name="textProp">
			<xsl:with-param name="class">category</xsl:with-param>
		</xsl:call-template>
	</xsl:if>

	<xsl:if test="$class">	
		<xsl:call-template name="textProp">
			<xsl:with-param name="class">class</xsl:with-param>
		</xsl:call-template>
	</xsl:if>
			
	<xsl:apply-templates mode="extract-hcal"/>		

	</xsl:template>

	<xsl:template name="textProp">
  	<xsl:param name="class" />
  	<xsl:element name="{$class}" namespace="{$CalNS}">
		<xsl:choose>	
			<xsl:when test='@title'>
	  		<xsl:value-of select="@title" />
			</xsl:when>
	
			<xsl:otherwise>
	  		<xsl:value-of select="normalize-space(.)" />
			</xsl:otherwise>
    </xsl:choose>
      
    </xsl:element>
	</xsl:template>

	<xsl:template name="dateProp">
  	<xsl:param name="class" />
		<xsl:param name="ns" select="$CalNS"/>
    <xsl:element name="{$class}" namespace="{$ns}">
      
      <xsl:variable name="when">
				<xsl:choose>
	  			<xsl:when test="@title">
	    			<xsl:value-of select="normalize-space(@title)"/>
	  			</xsl:when>
	  			<xsl:when test="descendant::*[contains(concat(' ',@class,' '),' value ')]">
	  			<!-- TODO: datetime might be distributed over several class-value elements  -->
	  				<xsl:call-template name="extract-value-class"/>
	  			</xsl:when>
	  			<xsl:otherwise>
	    			<xsl:value-of select="normalize-space(.)" />
	  			</xsl:otherwise>
				</xsl:choose>
     	</xsl:variable>
     	<!--  other, perhaps more complete normalization in x2v-datetime.xsl? -->
     	<xsl:variable name="date">
     		<xsl:call-template name="getDate">
     			<xsl:with-param name="string" select="$when"/>
     		</xsl:call-template>
     	</xsl:variable>
     	<xsl:variable name="time">
     		<xsl:call-template name="getTime">
     			<xsl:with-param name="string" select="$when"/>
     		</xsl:call-template>
     	</xsl:variable>
      <xsl:choose>
				<xsl:when test="string-length($date) &gt; 0 and string-length($time) &gt; 0">
	  			<xsl:attribute name="rdf:datatype">
	    			<xsl:value-of select='concat($XdtNS, "dateTime")' />
	  			</xsl:attribute>
	  			<xsl:value-of select="concat($date,'T',$time)"/>
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
					<xsl:value-of select="$when"/>
				</xsl:otherwise>
      </xsl:choose>
    </xsl:element>
	</xsl:template>
	
	<xsl:template name="durProp">
  	<xsl:param name="class" />

   <xsl:element name="{$class}" namespace="{$CalNS}">
      <!-- commas aren't possible, are they? -->
      <!--  XSD normalization? -->
      <xsl:choose>
				<xsl:when test='@title'>
	  			<xsl:value-of select="@title" />
				</xsl:when>
	  			<xsl:when test="descendant::*[contains(concat(' ',@class,' '),' value ')]">
	  			<!-- TODO: datetime might be distributed over several class-value elements  -->
	  				<xsl:call-template name="extract-value-class"/>
	  			</xsl:when>
				<xsl:otherwise>
	  			<xsl:value-of select='normalize-space(.)' />
				</xsl:otherwise>
      </xsl:choose>
    </xsl:element>
	</xsl:template>

	<xsl:template name="floatPairProp">
  	<xsl:param name="class" />

    <xsl:variable name="xy">
      <xsl:choose>
				<xsl:when test='@title'>
	  			<xsl:value-of select="@title" />
				</xsl:when>
				<xsl:otherwise>
	  			<xsl:value-of select="." />
				</xsl:otherwise>
     	</xsl:choose>
   	</xsl:variable>

    <xsl:variable name="x" select='substring-before($xy, ";")' />
    <xsl:variable name="y" select='substring-after($xy, ";")' />

    <xsl:element name="{$class}" namespace="{$CalNS}">
      <xsl:attribute name="rdf:parseType">Resource</xsl:attribute>
      <rdf:first rdf:datatype="http://www.w3.org/2001/XMLSchema#double">
				<xsl:value-of select="$x" />
      </rdf:first>
      <rdf:rest rdf:parseType="Resource">
				<rdf:first rdf:datatype="http://www.w3.org/2001/XMLSchema#double">
	  			<xsl:value-of select="$y" />
				</rdf:first>
				<rdf:rest rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#nil" />
      </rdf:rest>
    </xsl:element>
	</xsl:template>

	<xsl:template name="whoProp">
		<xsl:param name="class" />
	<!--  TODO hcard etc.? -->
		<xsl:variable name="mbox">
			<xsl:choose>
				<!-- @@make absolute? -->
				<xsl:when test="@href">
					<xsl:value-of select="@href" />
				</xsl:when>
				<xsl:when test="descendant::*[contains(concat(' ',@class,' '),' value ') and @href]">
					<xsl:value-of select="descendant::*[contains(concat(' ',@class,' '),' value ') and @href]/@href"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="normalize-space(.)" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:variable name="cn">
			<xsl:choose>
				<xsl:when test="@href">
					<xsl:value-of select="normalize-space(.)" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select='""' />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:element name="{$class}" namespace="{$CalNS}">
		<!--  TODO: illegal Resource value; href can be a dtr -->
			<xsl:attribute name="rdf:parseType">Resource</xsl:attribute>
			<cal:calAddress rdf:resource="{$mbox}"/>
			<xsl:if test="$cn">
				<cal:cn>
					<xsl:value-of select="$cn" />
				</cal:cn>
			</xsl:if>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="text()"/>
	<xsl:template match="text()" mode="extract-hcal"/>

</xsl:stylesheet>