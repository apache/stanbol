<?xml version="1.0" encoding="utf-8"?>
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
  	<nie:creator>
  		<nco:Contact>
  			<nco:fullname>
  				<xsl:value-of select="normalize-space(@content)"/>
  			</nco:fullname>
  		</nco:Contact>
  	</nie:creator>
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
