<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
	xmlns:dc="http://purl.org/dc/elements/1.1/" 
	>

  <xsl:param name="uri" select="'http://foobar.com/'"/>

	<xsl:template match="/">
		<rdf:RDF>
			<xsl:apply-templates select="descendant::*[@rel='license']"/>
		</rdf:RDF>
	</xsl:template>
	
	<xsl:template match="*[@rel='license']">
		<dc:license rdf:resource="{@href}" />
	</xsl:template>

	<xsl:template match="text()"/>
	
</xsl:stylesheet>