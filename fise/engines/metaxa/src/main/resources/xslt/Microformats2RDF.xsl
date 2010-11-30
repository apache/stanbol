<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"	
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
  xmlns:nfo="http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#"
  xmlns:nie="http://www.semanticdesktop.org/ontologies/2007/01/19/nie#"
  >

	<xsl:import href="hcard2rdf.xsl"/>
  <xsl:import href="geo2rdf.xsl"/>
	<xsl:import href="hreview2rdf.xsl"/>
	<xsl:import href="hcal2rdf.xsl"/>
	<xsl:import href="xfolk2rdf.xsl"/>
  	<xsl:import href="hatom2rdf.xsl"/>
	<xsl:import href="rel-design-pattern2rdf.xsl"/>
	
	<!-- TODO: hAtom (hentry), rel-bookmark, ... -->
	<!--  TODO identify multiple feature values in lists (requires to keep track of visited nodes?) -->
	
	<xsl:output method="xml" omit-xml-declaration="yes"/>
  
  <xsl:strip-space elements="*"/>

  <xsl:param name="uri" select="'http://foobar.com/'"/>

  <xsl:template match="text()"/>

	<xsl:template match="/">
		<rdf:RDF>
			<nfo:HtmlDocument rdf:about="{$uri}">
				<xsl:apply-templates>
					<xsl:with-param name="outer" select="'contains'"/>
					<xsl:with-param name="ns" select="'http://www.semanticdesktop.org/ontologies/2007/01/19/nie#'"/>
					<!--  xsltproc ignoriert ns; sun-xalan erzeugt im Output neues Namespace-Prefix bei jedem Aufruf
					Jena sucht sich irgendeinen davon aus -->
				</xsl:apply-templates>
			</nfo:HtmlDocument>
		</rdf:RDF>
	</xsl:template>

</xsl:stylesheet>