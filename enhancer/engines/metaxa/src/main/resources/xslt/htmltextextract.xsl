<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
  xmlns:nie="http://www.semanticdesktop.org/ontologies/2007/01/19/nie#"
  xmlns:nco="http://www.semanticdesktop.org/ontologies/2007/3/22/nco#"
  xmlns:nfo="http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#"
  version="1.0">

  <xsl:output method="xml"/>
  
  <!-- not good for text extraction?
  <xsl:strip-space elements="*"/>
  -->

  <xsl:param name="uri" select="'http://foobar.com/'"/>
  
  <xsl:template match="/">
    <rdf:RDF>
			<nfo:HtmlDocument rdf:about="{$uri}">
				<nie:plainTextContent>
						<xsl:apply-templates select="html/body" mode="textextract"/>
				</nie:plainTextContent>
			</nfo:HtmlDocument>
	  </rdf:RDF>
  </xsl:template>
  
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

  