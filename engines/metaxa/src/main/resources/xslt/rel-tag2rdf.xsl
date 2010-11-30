<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
  xmlns:nie="http://www.semanticdesktop.org/ontologies/2007/01/19/nie#"
  xmlns:tag="http://aperture.sourceforge.net/ontologies/tagging#"
                version="1.0">

	<xsl:import href="functions.xsl"/>
	
  <xsl:output method="xml"/>
  
  <xsl:strip-space elements="*"/>

  <xsl:param name="uri" select="'http://foobar.com/'"/>

  <xsl:template match="text()"/>

	<xsl:template match="/">
		<rdf:RDF>
			<tag:Item rdf:about="{$uri}">
				<xsl:apply-templates select="descendant::*[@rel='tag']"/>
			</tag:Item>
		</rdf:RDF>
	</xsl:template>

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

</xsl:stylesheet>
