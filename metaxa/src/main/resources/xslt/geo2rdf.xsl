<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
  xmlns:nie="http://www.semanticdesktop.org/ontologies/2007/01/19/nie#"
  xmlns:geo="http://www.w3.org/2003/01/geo/wgs84_pos#"
                version="1.0">


  <xsl:output method="xml"/>
  
  <xsl:strip-space elements="*"/>

  <xsl:param name="uri" select="'http://foobar.com/'"/>

  <xsl:template match="text()"/>

<xsl:template match="/">
	<rdf:RDF>
		<geo:SpatialThing>
  		<xsl:apply-templates select="descendant::*[@rel='geo']"/>
  	</geo:SpatialThing>
	</rdf:RDF>
</xsl:template>

<xsl:template match="*[@class='geo']">
  <xsl:variable name="latEle" select=".//*[class='latitude']"/>
  <xsl:variable name="longEle" select=".//*[class='longitude']"/>
  <xsl:variable name="latitude">
    <xsl:choose>
      <xsl:when test="$latEle/@title">
        <xsl:value-of select="$latEle/@title"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="normalize-space($latEle)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="longitude">
    <xsl:choose>
      <xsl:when test="$longEle/@title">
        <xsl:value-of select="$longEle/@title"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="normalize-space($longEle)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
	<geo:lat>
		<xsl:attribute name="rdf:datatype">http://www.w3.org/2001/XMLSchema#float</xsl:attribute>
		<xsl:value-of select="$latitude" />
	</geo:lat>
	<geo:long>
		<xsl:attribute name="rdf:datatype">http://www.w3.org/2001/XMLSchema#float</xsl:attribute>
		<xsl:value-of select="$longitude" />
	</geo:long>
</xsl:template>
</xsl:stylesheet>
