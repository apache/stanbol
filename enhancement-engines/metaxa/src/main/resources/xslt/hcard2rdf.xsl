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
		xmlns:v="http://www.w3.org/2006/vcard/ns#"
                version="1.0">


<xsl:import href="functions.xsl"/>

<xsl:output method="xml" encoding="utf-8" indent="yes"/>

<xsl:strip-space elements="*"/>

<xsl:param name="uri" select="'http://foobar.com'"/>
  
<!-- I18N warning, but all our names are US ASCII so it doesn't matter -->
<xsl:param name="lcLetters" select="'abcdefghijklmnopqrstuvwxyz'"/>
<xsl:param name="ucLetters" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"/>

<xsl:param name="use-camel-case" select="1"/>

<xsl:template match="/">
  <rdf:RDF>
    <xsl:apply-templates select="descendant::*[contains(concat(' ',normalize-space(@class),' '),' vcard ')]"/>
  </rdf:RDF>
</xsl:template>

<!--  top-level rule should not match all nodes! -->
	<xsl:template match="*[contains(concat(' ',normalize-space(@class),' '),' vcard ')]">
		<xsl:param name="outer"/>
		<xsl:param name="ns"/>
		<xsl:variable name="id" select="generate-id(.)"/>
		<xsl:variable name="visited">
			<xsl:call-template name="containsVisited">
				<xsl:with-param name="id" select="$id"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
 			<xsl:when test="$visited = 'true'"/>
			<xsl:when test="$outer and $ns">
				<xsl:element name="{$outer}" namespace="{$ns}">
					<xsl:apply-templates select="." mode="vcard-toplevel" />
				</xsl:element>
				<xsl:variable name="nop">
					<xsl:call-template name="addVisited">
						<xsl:with-param name="id" select="$id"/>
					</xsl:call-template>
				</xsl:variable>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="." mode="vcard-toplevel" />
				<xsl:variable name="nop">
					<xsl:call-template name="addVisited">
						<xsl:with-param name="id" select="$id"/>
					</xsl:call-template>
				</xsl:variable>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="*" mode="vcard-toplevel">
		<v:VCard>
			<xsl:if test="@id">
				<xsl:attribute name="rdf:nodeID">
					<xsl:value-of select="@id" />
				</xsl:attribute>
			</xsl:if>
			<xsl:apply-templates mode="extract-vcard" />
		</v:VCard>
		<!--  TODO overlapping/mixing microformats: geo done twice? disable for now -->
	  <!-- <xsl:apply-templates/> -->
	</xsl:template>
	
<xsl:template match="comment()|processing-instruction()|text()"/>

<!-- ============================================================ -->

<xsl:template match="*" mode="extract-vcard">
  <xsl:variable name="fn">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'fn'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="n">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'n'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="n-ancestor">
    <xsl:apply-templates select="." mode="check-ancestor">
      <xsl:with-param name="field" select="'n'"/>
    </xsl:apply-templates>
  </xsl:variable>

  <xsl:variable name="given-name">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'given-name'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="family-name">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'family-name'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="additional-name">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'additional-name'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="honorific-prefix">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'honorific-prefix'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="honorific-suffix">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'honorific-suffix'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="nickname">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'nickname'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="sort-string">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'sort-string'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="url">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'url'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="email">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'email'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="tel">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'tel'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="adr">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'adr'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="label">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'label'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="geo">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'geo'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="tz">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'tz'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="photo">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'photo'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="logo">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'logo'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="sound">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'sound'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="bday">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'bday'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="title">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'title'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="role">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'role'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="org">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'org'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="category">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'category'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="note">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'note'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="class">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'class'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="key">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'key'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="mailer">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'mailer'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="uid">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'uid'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="rev">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="'rev'"/>
    </xsl:call-template>
  </xsl:variable>

  <!-- ============================================================ -->

  <xsl:if test="$fn != 0">
    <v:fn><xsl:value-of select="normalize-space(.)"/></v:fn>
  </xsl:if>

  <xsl:if test="$sort-string != 0">
    <v:sort-string><xsl:value-of select="."/></v:sort-string>
  </xsl:if>

  <xsl:if test="$n != 0 or $n-ancestor = 0">
    <xsl:if test="($n != 0 or $n-ancestor = 0)
		  and ($n != 0 or $given-name != 0 or $family-name != 0
		       or $additional-name != 0
		       or $honorific-prefix != 0 or $honorific-suffix != 0)">
      <v:n rdf:parseType="Resource">
	<rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Name"/>
	<xsl:apply-templates select="." mode="extract-field">
	  <xsl:with-param name="field" select="'given-name'"/>
	</xsl:apply-templates>
	<xsl:apply-templates select="." mode="extract-field">
	  <xsl:with-param name="field" select="'family-name'"/>
	</xsl:apply-templates>
	<xsl:apply-templates select="." mode="extract-field">
	  <xsl:with-param name="field" select="'additional-name'"/>
	</xsl:apply-templates>
	<xsl:apply-templates select="." mode="extract-field">
	  <xsl:with-param name="field" select="'honorific-prefix'"/>
	</xsl:apply-templates>
	<xsl:apply-templates select="." mode="extract-field">
	  <xsl:with-param name="field" select="'honorific-suffix'"/>
	</xsl:apply-templates>
      </v:n>
    </xsl:if>
  </xsl:if>

  <xsl:if test="$nickname != 0">
    <xsl:apply-templates select="." mode="extract-field">
      <xsl:with-param name="field" select="'nickname'"/>
    </xsl:apply-templates>
  </xsl:if>

  <xsl:if test="$url != 0">
    <v:url>
      <xsl:attribute name="rdf:resource">
	<xsl:choose>
	  <xsl:when test="@href">
            <xsl:call-template name="resolveUri">
              <xsl:with-param name="base" select="$uri"/>
              <xsl:with-param name="ref" select="@href"/>
            </xsl:call-template>
	  </xsl:when>
	  <xsl:otherwise>
	    <xsl:if test="not(contains(.,':'))">http://</xsl:if>
	    <xsl:value-of select="string(.)"/>
	  </xsl:otherwise>
	</xsl:choose>
      </xsl:attribute>
    </v:url>
  </xsl:if>

  <xsl:if test="$email != 0">
    <xsl:apply-templates select="." mode="extract-email"/>
  </xsl:if>

  <xsl:if test="$tel != 0">
    <xsl:apply-templates select="." mode="extract-tel"/>
  </xsl:if>

  <xsl:if test="$adr != 0">
    <xsl:apply-templates select="." mode="extract-adr"/>
  </xsl:if>

  <xsl:if test="$label != 0">
    <v:label>
      <xsl:value-of select="."/>
    </v:label>
  </xsl:if>

	<!--  alternatively, call geo template (uses different ontology!) -->
  <xsl:if test="$geo != 0">
    <v:geo>
      <v:Location>
      	<xsl:apply-templates select="." mode="extract-field">
					<xsl:with-param name="field" select="'latitude'"/>
      	</xsl:apply-templates>
      		<xsl:apply-templates select="." mode="extract-field">
					<xsl:with-param name="field" select="'longitude'"/>
      	</xsl:apply-templates>
      </v:Location>
    </v:geo>
  </xsl:if>

  <xsl:if test="$tz != 0">
    <v:tz>
      <xsl:value-of select="."/>
    </v:tz>
  </xsl:if>

  <xsl:if test="$photo != 0 and @src">
    <v:photo rdf:resource="{@src}"/>
  </xsl:if>

  <xsl:if test="$logo != 0 and @src">
    <v:logo rdf:resource="{@src}"/>
  </xsl:if>

  <xsl:if test="$sound != 0 and @data">
    <v:sound rdf:resource="{@src}"/>
  </xsl:if>

  <xsl:if test="$bday != 0 and @title">
    <v:bday>
      <xsl:value-of select="@title"/>
    </v:bday>
  </xsl:if>

  <xsl:if test="$title != 0">
    <v:title>
      <xsl:choose>
        <xsl:when test="@title">
          <xsl:value-of select="@title"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="."/>          
        </xsl:otherwise>
      </xsl:choose>
    </v:title>
  </xsl:if>

  <xsl:if test="$role != 0">
    <v:role>
      <xsl:value-of select="."/>
    </v:role>
  </xsl:if>

  <xsl:if test="$org != 0">
    <xsl:variable name="exists">
      <xsl:apply-templates select="." mode="field-exists">
	<xsl:with-param name="field" select="'organization-name'"/>
      </xsl:apply-templates>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="$exists != ''">
	<v:org rdf:parseType="Resource">
	  <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Organization"/>
	  <xsl:apply-templates select="." mode="extract-field">
	    <xsl:with-param name="field" select="'organization-name'"/>
	  </xsl:apply-templates>
	  <xsl:apply-templates select="." mode="extract-field">
	    <xsl:with-param name="field" select="'organization-unit'"/>
	  </xsl:apply-templates>
	</v:org>
      </xsl:when>
      <xsl:otherwise>
	<v:org rdf:parseType="Resource">
	  <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Organization"/>
	  <v:organization-name>
	    <xsl:value-of select="."/>
	  </v:organization-name>
	</v:org>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:if>

  <xsl:if test="$category != 0">
    <v:category>
      <xsl:value-of select="."/>
    </v:category>
  </xsl:if>

  <xsl:if test="$note != 0">
    <v:note>
      <xsl:value-of select="."/>
    </v:note>
  </xsl:if>

  <xsl:if test="$class != 0">
    <v:class>
      <xsl:value-of select="."/>
    </v:class>
  </xsl:if>

  <xsl:if test="$key != 0 and @data">
    <v:key rdf:resource="{@data}"/>
  </xsl:if>

  <xsl:if test="$mailer != 0">
    <v:mailer>
      <xsl:value-of select="."/>
    </v:mailer>
  </xsl:if>

  <xsl:if test="$uid != 0">
    <v:uid>
      <xsl:value-of select="."/>
    </v:uid>
  </xsl:if>

  <xsl:if test="$rev != 0 and @title">
    <v:rev>
      <xsl:value-of select="@title"/>
    </v:rev>
  </xsl:if>

  <xsl:apply-templates mode="extract-vcard"/>
</xsl:template>

<xsl:template match="comment()|processing-instruction()|text()"
	      mode="extract-vcard"/>

	<xsl:template match="text()" mode="vcard-toplevel"/>
<!-- ============================================================ -->

<xsl:template match="*" mode="extract-tel">
  <xsl:variable name="type" select=".//*[@class='type']"/>
  <xsl:variable name="value" select=".//*[@class='value']"/>
  <xsl:variable name="lv" select=".//*[@class]"/>

  <xsl:choose>
    <xsl:when test="$type and $value">
      <xsl:call-template name="tel">
	<xsl:with-param name="type" select="string($type)"/>
	<xsl:with-param name="value" select="string($value)"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="$type">
    	<xsl:call-template name="tel">
				<xsl:with-param name="type" select="string($type)"/>
				<xsl:with-param name="value" select="string($type/following-sibling::node())"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="$lv">
    <xsl:message>Hier 1: <xsl:value-of select="$lv/@class"/></xsl:message>
      <xsl:call-template name="tel">
	<xsl:with-param name="type" select="$lv/@class"/>
	<xsl:with-param name="value" select="string($lv)"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="tel">
	<xsl:with-param name="type" select="''"/>
	<xsl:with-param name="value" select="string(.)"/>
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="tel">
  <xsl:param name="type" select="''"/>
  <xsl:param name="value" select="'+1-800-555-1212'"/>

  <xsl:variable name="token" select="translate($type,
                                               'ABCDEFGHIJKLMNOPQRSTUVWXYZ ',
					       'abcdefghijklmnopqrstuvwxyz')"/>

  <xsl:variable name="tel">
  	<xsl:call-template name="cleanuptel">
  		<xsl:with-param name="value" select="$value"/>
  	</xsl:call-template>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="$token = 'cell'">
      <v:mobileTel rdf:resource="tel:{$tel}"/>
    </xsl:when>
    <xsl:when test="$token = 'work' or $token = 'office'">
      <v:workTel rdf:resource="tel:{$tel}"/>
    </xsl:when>
    <xsl:when test="$token = 'fax'">
      <v:fax rdf:resource="tel:{$tel}"/>
    </xsl:when>
    <xsl:when test="$token = 'home'">
      <v:homeTel rdf:resource="tel:{$tel}"/>
    </xsl:when>
    <xsl:otherwise>
      <v:tel rdf:resource="tel:{$tel}"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="*" mode="extract-email">
  <xsl:variable name="type" select=".//*[@class='type']"/>
  <xsl:variable name="value" select=".//*[@class='value']/@href"/>
  <xsl:variable name="lv" select="@href"/>

  <xsl:variable name="token" select="translate($type,
                                               'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
					       'abcderghijklmnopqrstuvwxyz')"/>

  <xsl:variable name="uri">
    <xsl:choose>
      <xsl:when test="$value != ''">
	<xsl:value-of select="$value"/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:value-of select="$lv"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="$token = 'home' or $token = 'personal'">
      <v:personalEmail rdf:resource="{$uri}"/>
    </xsl:when>
    <xsl:when test="$token = 'work' or $token = 'office'">
      <v:workEmail rdf:resource="{$uri}"/>
    </xsl:when>
    <xsl:when test="$token = 'mobile'">
      <v:mobileEmail rdf:resource="{$uri}"/>
    </xsl:when>
    <xsl:otherwise>
      <v:email rdf:resource="{$uri}"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="*" mode="extract-adr">
  <xsl:variable name="type" select=".//*[@class='type']"/>
  <xsl:variable name="token" select="translate($type,
                                               'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
					       'abcderghijklmnopqrstuvwxyz')"/>

  <xsl:variable name="fields">
    <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Address"/>
    <xsl:apply-templates select="." mode="extract-field">
      <xsl:with-param name="field" select="'post-office-box'"/>
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="extract-field">
      <xsl:with-param name="field" select="'extended-address'"/>
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="extract-field">
      <xsl:with-param name="field" select="'street-address'"/>
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="extract-field">
      <xsl:with-param name="field" select="'locality'"/>
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="extract-field">
      <xsl:with-param name="field" select="'region'"/>
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="extract-field">
      <xsl:with-param name="field" select="'postal-code'"/>
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="extract-field">
      <xsl:with-param name="field" select="'country-name'"/>
    </xsl:apply-templates>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="$token = 'home' or $token = 'personal'">
      <v:homeAdr rdf:parseType="Resource">
	<xsl:copy-of select="$fields"/>
      </v:homeAdr>
    </xsl:when>
    <xsl:when test="$token = 'work' or $token = 'office'">
      <v:workAdr rdf:parseType="Resource">
	<xsl:copy-of select="$fields"/>
      </v:workAdr>
    </xsl:when>
    <xsl:otherwise>
      <v:adr rdf:parseType="Resource">
	<xsl:copy-of select="$fields"/>
      </v:adr>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="*" mode="field-exists">
  <xsl:param name="field" select="''"/>

  <xsl:variable name="f">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="$field"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="$f != 0">
      <xsl:value-of select="."/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="*" mode="field-exists">
	<xsl:with-param name="field" select="$field"/>
      </xsl:apply-templates>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="comment()|processing-instruction()|text()"
	      mode="field-exists"/>

<!-- ============================================================ -->

<xsl:template match="*" mode="extract-field">
  <xsl:param name="field" select="''"/>
  <xsl:param name="prop" select="$field"/>

  <xsl:variable name="f">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="$field"/>
    </xsl:call-template>
  </xsl:variable>

	<!--  check for value-title form -->  
  <xsl:variable name="value">
  	<xsl:choose>
  		<xsl:when test="*/@title">
  			<xsl:value-of select="*/@title"/>
  		</xsl:when>
  		<xsl:otherwise>
  			<xsl:value-of select="normalize-space(.)"/>
  		</xsl:otherwise>
  	</xsl:choose>
  </xsl:variable>

	<xsl:choose>
		<xsl:when test="$use-camel-case != 0">
			<!-- translate property name to camel case -->
			<xsl:variable name="propCC">
				<xsl:call-template name="camel-case">
					<xsl:with-param name="name" select="$prop" />
				</xsl:call-template>
			</xsl:variable>

			<xsl:if test="$f != 0">
				<xsl:element name="{$propCC}" namespace="http://www.w3.org/2006/vcard/ns#">
					<xsl:value-of select="$value" />
				</xsl:element>
			</xsl:if>
		</xsl:when>
		<xsl:otherwise>
			<xsl:if test="$f != 0">
				<xsl:element name="{$prop}" namespace="http://www.w3.org/2006/vcard/ns#">
					<xsl:value-of select="$value" />
				</xsl:element>
			</xsl:if>
		</xsl:otherwise>
	</xsl:choose>

  <xsl:apply-templates select="*" mode="extract-field">
    <xsl:with-param name="field" select="$field"/>
    <xsl:with-param name="prop" select="$prop"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="comment()|processing-instruction()|text()"
	      mode="extract-field"/>

<!-- ============================================================ -->

<xsl:template match="*" mode="check-ancestor">
  <xsl:param name="field" select="''"/>

  <xsl:variable name="f">
    <xsl:call-template name="testclass">
      <xsl:with-param name="val" select="$field"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="$f != 0">1</xsl:when>
    <xsl:when test="not(parent::*)">0</xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="parent::*" mode="check-ancestor">
	<xsl:with-param name="field" select="$field"/>
      </xsl:apply-templates>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ============================================================ -->

<xsl:template name="testclass">
  <xsl:param name="class" select="@class"/>
  <xsl:param name="val" select="''"/>

  <xsl:choose>
    <xsl:when test="contains(concat(' ',normalize-space($class),' '),concat(' ',$val,' '))">1</xsl:when>
    <xsl:otherwise>0</xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="cleanuptel">
  <xsl:param name="value" select="''"/>

  <xsl:choose>
    <xsl:when test="starts-with($value, 'tel:')">
      <xsl:value-of select="substring-after($value,'tel:')"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="ch" select="substring($value,1,1)"/>
      <xsl:if test="$ch = '0' or $ch = '1' or $ch = '2' or $ch = '3'
	            or $ch = '4' or $ch = '5' or $ch = '6' or $ch = '7'
	            or $ch = '8' or $ch = '9' or $ch = '-' or $ch = '+'">
	<xsl:value-of select="$ch"/>
      </xsl:if>
      <xsl:if test="string-length($value) &gt; 1">
	<xsl:call-template name="cleanuptel">
	  <xsl:with-param name="value" select="substring($value,2)"/>
	</xsl:call-template>
      </xsl:if>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="camel-case">
  <xsl:param name="name" select="''"/>
  <xsl:param name="ucfirst" select="0"/>

  <xsl:if test="$name != ''">
    <xsl:variable name="first" select="substring($name,1,1)"/>
    <xsl:variable name="rest" select="substring($name,2)"/>

    <xsl:choose>
      <xsl:when test="$ucfirst != 0">
	<xsl:value-of select="translate($first, $lcLetters, $ucLetters)"/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:value-of select="$first"/>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:choose>
      <xsl:when test="contains($rest,'-')">
	<xsl:value-of select="substring-before($rest,'-')"/>
	<xsl:call-template name="camel-case">
	  <xsl:with-param name="name" select="substring-after($rest,'-')"/>
	  <xsl:with-param name="ucfirst" select="1"/>
	</xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
	<xsl:value-of select="$rest"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:if>
</xsl:template>

</xsl:stylesheet>
