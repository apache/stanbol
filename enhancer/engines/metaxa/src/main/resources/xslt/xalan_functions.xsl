<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:java="http://xml.apache.org/xalan/java"
  >
  
  <xsl:variable name="visitedNodes" select="java:java.util.HashSet.new()"/>

	<xsl:template name="addVisited">
		<xsl:param name="id"/>
		<xsl:value-of select="java:add($visitedNodes,$id)"/>
	</xsl:template>
	
	<xsl:template name="containsVisited">
		<xsl:param name="id"/>
		<xsl:value-of select="java:contains($visitedNodes,$id)"/>
	</xsl:template>
	
</xsl:stylesheet>