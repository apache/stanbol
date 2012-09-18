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
                version="1.0">

	<xsl:import href="xalan_functions.xsl"/>
	
  <!-- hack to avoid incomplete relative URIs -->
  <xsl:template name="resolveUri">
    <xsl:param name="base"/>
    <xsl:param name="ref"/>
    <xsl:choose>
    	<xsl:when test="starts-with($ref,'#')">
    		<xsl:value-of select="concat($base,$ref)"/>
    	</xsl:when>
      <xsl:when test="not(contains($ref,':/'))">
        <!-- TODO: remove double slashes? -->
        <xsl:variable name="baseUri">
        	<xsl:call-template name="longestPrefix">
        		<xsl:with-param name="string" select="$base"/>
        		<xsl:with-param name="sep" select="'/'"/>
        	</xsl:call-template>
        </xsl:variable>
        <xsl:choose>
        	<xsl:when test="starts-with($ref,'//')">
        	<!--  prefix is just the protocol part -->
        	<!--	<xsl:value-of select="concat(substring-before($baseUri,':'),':',$ref)"/> -->
        	<!--  hard code the prefix for Stanbol because there we will not see the real URLs anyway -->
        		<xsl:value-of select="concat('http:',$ref)"/>
        	</xsl:when>
        	<xsl:when test="starts-with($ref,'/')">
        		<xsl:value-of select="concat($baseUri,$ref)"/>
       		</xsl:when>
        	<xsl:otherwise>
        		<xsl:value-of select="concat($baseUri,'/',$ref)"/>
        	</xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$ref"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
	
	<xsl:template name="extract-values">
		<xsl:param name="sep" select="''"/>
		<xsl:for-each select="descendant::*[@class='value']">
			<xsl:choose>
				<xsl:when test="@title">
					<xsl:value-of select="concat(@title,$sep)"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="concat(.,$sep)"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>
	</xsl:template>
	
	
  <xsl:template name="extract-value-class">
  	<xsl:variable name="classNode" select="descendant::*[@class='value'][1]"/>
  	<xsl:choose>
  		<xsl:when test="$classNode">
  			<xsl:choose>
  				<xsl:when test="$classNode/@title">
  					<xsl:value-of select="$classNode/@title"/>
  				</xsl:when>
  				<xsl:otherwise>
  					<xsl:value-of select="normalize-space($classNode)"/>
  				</xsl:otherwise>
  			</xsl:choose>
  		</xsl:when>
  		<xsl:otherwise>
  			<xsl:value-of select="normalize-space(.)"/>
  		</xsl:otherwise>
  	</xsl:choose>
  </xsl:template>

	<xsl:template name="longestPrefix">		
   	<xsl:param name="string" />
   	<xsl:param name="sep" />
		<xsl:variable name="lastSeg">
			<xsl:call-template name="lastIndexOf">
				<xsl:with-param name="string" select="$string"/>
				<xsl:with-param name="sep" select="$sep"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:value-of select="substring($string,1,string-length($string) - string-length($sep) - string-length($lastSeg))"/>
	</xsl:template>

	<!--  returns the substring after the last occurrence of the separator -->	
	<xsl:template name="lastIndexOf">
   <xsl:param name="string" />
   <xsl:param name="sep" />
   <xsl:choose>
     <xsl:when test="contains($string, $sep)">
        <xsl:call-template name="lastIndexOf">
          <xsl:with-param name="string" select="substring-after($string, $sep)" />
            <xsl:with-param name="sep" select="$sep" />
         </xsl:call-template>
      </xsl:when>
      <xsl:otherwise><xsl:value-of select="$string" /></xsl:otherwise>
   </xsl:choose>
	</xsl:template>

<!--  for val use values already padded with blanks -->
	<xsl:template name="checkClass">
		<xsl:param name="class" select="@class" />
		<xsl:param name="val" select="''" />

		<xsl:choose>
			<xsl:when test="contains(concat(' ',normalize-space($class),' '),$val)">1</xsl:when>
			<xsl:otherwise>0</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
  
</xsl:stylesheet>
