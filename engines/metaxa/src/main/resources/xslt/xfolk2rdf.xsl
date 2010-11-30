<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
  xmlns:nfo="http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#"
  xmlns:nie="http://www.semanticdesktop.org/ontologies/2007/01/19/nie#"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
  xmlns:tag="http://aperture.sourceforge.net/ontologies/tagging#"
  >

	<xsl:import href="functions.xsl"/>
	
  <xsl:output method="xml"/>
  
  <xsl:strip-space elements="*"/>

  <xsl:param name="uri" select="'http://foobar.com/'"/>

  <xsl:template match="text()"/>
  <xsl:template match="text()" mode="extract-xfolk"/>
  
	<xsl:template match="/">
		<rdf:RDF>
			<tag:Item rdf:about="{$uri}">
			<nie:contains>
				<xsl:apply-templates select="descendant::*[contains(concat(' ',normalize-space(@class),' '),' xfolkentry ')]"/>
				</nie:contains>
			</tag:Item>
		</rdf:RDF>
	</xsl:template>
	
	<xsl:template match="*[contains(concat(' ',normalize-space(@class),' '),' xfolkentry ')]"> 
		<xsl:param name="outer" />
		<xsl:param name="ns"/>
		<xsl:choose>
			<xsl:when test="$outer and $ns">
				<xsl:element name="{$outer}" namespace="{$ns}">
					<xsl:apply-templates select="." mode="extract-xfolk" />
				</xsl:element>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="." mode="extract-xfolk" />
			</xsl:otherwise>
		</xsl:choose>	
	</xsl:template>

<!--  TODO Bookmark might be wrong as NFO is meant to refer to file objects -->
	<xsl:template match="*" mode="extract-xfolk">
		<nfo:Bookmark>
		<xsl:variable name="link">
			<xsl:value-of select="descendant-or-self::*[contains(concat(' ',normalize-space(@class),' '),' taggedlink ')]/@href"/>
		</xsl:variable>
			<nfo:bookmarks>
				<xsl:attribute name="rdf:resource">
					<xsl:call-template name="resolveUri">
						<xsl:with-param name="base" select="$uri"/>
							<xsl:with-param name="ref" select="$link"/>
						</xsl:call-template>
				</xsl:attribute>
			</nfo:bookmarks>
			<xsl:for-each select="descendant-or-self::*[contains(concat(' ',normalize-space(@class),' '),' description ') or contains(concat(' ',normalize-space(@class),' '),' extended ')]">
				<dc:description>
					<xsl:value-of select="."/>
				</dc:description>
			</xsl:for-each>
			<xsl:apply-templates select="descendant-or-self::*[@rel='tag']"/>
		</nfo:Bookmark>
	</xsl:template>
</xsl:stylesheet>