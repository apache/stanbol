<?xml version="1.0" encoding="UTF-8"?>
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
<stylesheet
    xmlns:xsl  ="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:h    ="http://www.w3.org/1999/xhtml"
    xmlns      ="http://www.w3.org/1999/XSL/Transform"
    xmlns:rdf  ="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:nfo="http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#"
  xmlns:nie="http://www.semanticdesktop.org/ontologies/2007/01/19/nie#"
  xmlns:owl="http://www.w3.org/2002/07/owl#"
    >

<output indent="yes" method="xml" media-type="application/rdf+xml" encoding="UTF-8" omit-xml-declaration="yes"/>

<!-- base of the current XML doc -->
<variable name='xml_base' select="//*/@xml:base[position()=1]"/>

<!-- base of the current HTML doc -->
<variable name='html_base' select="//*/head/base[position()=1]/@href"/>

<!-- url of the current XHTML page if provided by the XSLT engine -->
<param name='uri' select="'http://foobar.com/'"/>

<!-- this contains the URL of the source document whether it was provided by the base or as a parameter e.g. http://example.org/bla/file.html-->
<variable name='this' >
	<choose>
		<when test="string-length($html_base)>0"><value-of select="$html_base"/></when>
		<when test="string-length($xml_base)>0"><value-of select="$xml_base"/></when>
		<otherwise><value-of select="$uri"/></otherwise>
	</choose>
</variable>

<!-- this_location contains the location the source document e.g. http://example.org/bla/ -->
<variable name='this_location' >
	<call-template name="get-location"><with-param name="url" select="$this"/></call-template>
</variable>

<!-- this_root contains the root location of the source document e.g. http://example.org/ -->
<variable name='this_root' >
	<call-template name="get-root"><with-param name="url" select="$this"/></call-template>
</variable>


<!-- templates for parsing - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->

<!--Start the RDF generation-->
<template match="/">
<rdf:RDF>
<xsl:if test="html/@version = 'XHTML+RDFa 1.0' or descendant::*/@property or descendant::*/@typeof">
	<xsl:choose>
		<xsl:when test="$uri != $this">
			<nfo:HtmlDocument rdf:about="{$uri}">
				<owl:sameAs rdf:resource="{$this}"/>
			</nfo:HtmlDocument>	
				<xsl:apply-templates/>
		</xsl:when>
		<xsl:otherwise>
		  <apply-templates />
		</xsl:otherwise>
	</xsl:choose>
</xsl:if>
</rdf:RDF>
</template>



<!-- match RDFa element -->
<!-- here is a problem: @rel overlaps with MF extractor; check before whether RDFa is applicable! -->
<template match="*[attribute::property or attribute::rel or attribute::rev or attribute::typeof]">

   <!-- identify suject -->
   <variable name="subject"> 
    <choose>

     <!-- current node is a meta or a link in the head and with no about attribute -->
     <when test="(self::link or self::meta) and ( ancestor::head ) and not(attribute::about)">
     	<value-of select="$this"/>
     </when>
              	
     <!-- an attribute about was specified on the node -->
     <when test="self::*/attribute::about">
       <call-template name="expand-curie-or-uri"><with-param name="curie_or_uri" select="@about"/></call-template>
     </when>

     <!-- an attribute src was specified on the node -->
     <when test="self::*/attribute::src">
       <call-template name="expand-curie-or-uri"><with-param name="curie_or_uri" select="@src"/></call-template>
     </when>
     
        
     <!-- an attribute typeof was specified on the node -->
     <when test="self::*/attribute::typeof">
       <call-template name="self-curie-or-uri"><with-param name="node" select="."/></call-template>
     </when>
     
     <!-- current node is a meta or a link in the body and with no about attribute -->
     <when test="(self::link or self::meta) and not( ancestor::head ) and not(attribute::about)">
     	<call-template name="self-curie-or-uri"><with-param name="node" select="parent::*"/></call-template>
     </when>
          
     <!-- an about was specified on its parent or the parent had a rel or a rev attribute but no href or an typeof. -->
     <when test="ancestor::*[attribute::about or attribute::src or attribute::typeof or attribute::resource or attribute::href or attribute::rel or attribute::rev][position()=1]">
     	<variable name="selected_ancestor" select="ancestor::*[attribute::about or attribute::src or attribute::typeof or attribute::resource or attribute::href or attribute::rel or attribute::rev][position()=1]"/> 
     	<choose>
     	    <when test="$selected_ancestor[(attribute::rel or attribute::rev) and not (attribute::resource or attribute::href)]">
     			<value-of select="concat('blank:node:INSIDE_',generate-id($selected_ancestor))"/>
     		</when>
     		<when test="$selected_ancestor/attribute::about">
     			<call-template name="expand-curie-or-uri"><with-param name="curie_or_uri" select="$selected_ancestor/attribute::about"/></call-template>
     		</when>
     		<when test="$selected_ancestor/attribute::src">
     			<call-template name="expand-curie-or-uri"><with-param name="curie_or_uri" select="$selected_ancestor/attribute::src"/></call-template>
     		</when>
     		<when test="$selected_ancestor/attribute::resource">
     			<call-template name="expand-curie-or-uri"><with-param name="curie_or_uri" select="$selected_ancestor/attribute::resource"/></call-template>
     		</when>
     		<when test="$selected_ancestor/attribute::href">
     			<call-template name="expand-curie-or-uri"><with-param name="curie_or_uri" select="$selected_ancestor/attribute::href"/></call-template>
     		</when>
     		<otherwise>
     			<call-template name="self-curie-or-uri"><with-param name="node" select="$selected_ancestor"/></call-template>
     		</otherwise>
     	</choose>
     </when>
     
     <otherwise> <!-- it must be about the current document -->
     	<value-of select="$this"/>
     </otherwise>

    </choose>
   </variable>
   
   
   <!-- do we have object properties? -->
   <if test="@rel or @rev">
     <variable name="object">
       <choose>
	     <when test="@resource"> 
		   <call-template name="expand-curie-or-uri"><with-param name="curie_or_uri" select="@resource"/></call-template>
	     </when>
	     <when test="@href"> 
		   <call-template name="expand-curie-or-uri"><with-param name="curie_or_uri" select="@href"/></call-template>
	     </when>
	     <when test="descendant::*[attribute::about or attribute::src or attribute::typeof or
	     	 attribute::href or attribute::resource or
	     	 attribute::rel or attribute::rev or attribute::property]"> 
		   <xsl:for-each select="descendant::*/attribute::about | descendant::*/attribute::src |
		   	 descendant::*/attribute::href | descendant::*/attribute::resource">
		   	<call-template name="expand-curie-or-uri"><with-param name="curie_or_uri" select="."/></call-template><text> </text>
		   </xsl:for-each>
		   <xsl:for-each select="descendant::*[attribute::typeof and not (attribute::about)]"> <!-- typed blank node -->
		   	<call-template name="self-curie-or-uri"><with-param name="node" select="."/></call-template><text> </text>
		   </xsl:for-each>
		   <if test="descendant::*[attribute::rel or attribute::rev or attribute::property]">  <!-- implicit blank node -->
		     <value-of select="concat('blank:node:INSIDE_',generate-id(.))"/>
		   </if> 
	     </when>
	     <otherwise>
	     	<call-template name="self-curie-or-uri"><with-param name="node" select="."/></call-template>
	     </otherwise>
       </choose>
     </variable>
  
 	<call-template name="relrev">
		<with-param name="subject" select="$subject"/>
		<with-param name="object" select="$object"/>
	</call-template>  

   </if>

   
   <!-- do we have data properties ? -->
   <if test="@property">
   	
   	 <!-- identify language -->
   	 <variable name="language" select="string(ancestor-or-self::*/attribute::xml:lang[position()=1])" />
   	 
     <variable name="expended-pro"><call-template name="expand-ns"><with-param name="qname" select="@property"/></call-template></variable>

      <choose>
       <when test="@content"> <!-- there is a specific content -->
         <call-template name="property">
          <with-param name="subject" select ="$subject" />
          <with-param name="object" select ="@content" />
          <with-param name="datatype" >
          	<choose>
          	  <when test="@datatype='' or not(@datatype)"></when> <!-- enforcing plain literal -->
          	  <otherwise><call-template name="expand-ns"><with-param name="qname" select="@datatype"/></call-template></otherwise>
          	</choose>
          </with-param>
          <with-param name="predicate" select ="@property"/>
          <with-param name="attrib" select ="'true'"/>
          <with-param name="language" select ="$language"/>
         </call-template>   
       </when>
       <when test="not(*)"> <!-- there no specific content but there are no children elements in the content -->
         <call-template name="property">
          <with-param name="subject" select ="$subject" />
          <with-param name="object" select ="." />
          <with-param name="datatype">
          	<choose>
          	  <when test="@datatype='' or not(@datatype)"></when> <!-- enforcing plain literal -->
          	  <otherwise><call-template name="expand-ns"><with-param name="qname" select="@datatype"/></call-template></otherwise>
          	</choose>
          </with-param>
          <with-param name="predicate" select ="@property"/>
          <with-param name="attrib" select ="'true'"/>
          <with-param name="language" select ="$language"/>
         </call-template>   
       </when>
       <otherwise> <!-- there is no specific content; we use the value of element -->
         <call-template name="property">
          <with-param name="subject" select ="$subject" />
          <with-param name="object" select ="." />
          <with-param name="datatype">
          	<choose>
          	  <when test="@datatype='' or not(@datatype)">http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral</when> <!-- enforcing XML literal -->
          	  <otherwise><call-template name="expand-ns"><with-param name="qname" select="@datatype"/></call-template></otherwise>
          	</choose>
          </with-param>
          <with-param name="predicate" select ="@property"/>
          <with-param name="attrib" select ="'false'"/>
          <with-param name="language" select ="$language"/>
         </call-template> 
       </otherwise>
      </choose>
   </if>

   <!-- do we have classes ? -->
   <if test="@typeof">
 		<call-template name="class">
			<with-param name="resource">
			<choose>
          	  <when test="self::head or self::body or self::html"><value-of select="$this"/></when> <!-- enforcing the doc as subject -->
          	  <otherwise><call-template name="self-curie-or-uri"><with-param name="node" select="."/></call-template></otherwise>
          	</choose>
			</with-param>
			<with-param name="class" select="@typeof"/>
		</call-template>
	</if>

   <apply-templates /> 
   
</template>



<!-- named templates to process URIs and token lists - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->

  <!-- tokenize a string using space as a delimiter -->
  <template name="tokenize">
    <param name="string" />
  	<if test="string-length($string)>0">
  		<choose>
  			<when test="contains($string,' ')">
				<value-of select="normalize-space(substring-before($string,' '))"/>
				<call-template name="tokenize"><with-param name="string" select="normalize-space(substring-after($string,' '))"/></call-template>  	  				
  			</when>
  			<otherwise><value-of select="$string"/></otherwise>
  		</choose>
  	</if>
  </template>

  <!-- get file location from URL -->
  <template name="get-location">
    <param name="url" />
  	<if test="string-length($url)>0 and contains($url,'/')">
  		<value-of select="concat(substring-before($url,'/'),'/')"/>
  		<call-template name="get-location"><with-param name="url" select="substring-after($url,'/')"/></call-template>
  	</if>
  </template>

  <!-- get root location from URL -->
  <template name="get-root">
		<param name="url" />
		<choose>
			<when test="contains($url,'//')">
				<value-of
					select="concat(substring-before($url,'//'),'//',substring-before(substring-after($url,'//'),'/'),'/')" />
			</when>
			<otherwise>
				<value-of select="concat($url,'/')" />
			</otherwise>
		</choose>
	</template>

  <!-- return namespace of a qname -->
  <template name="return-ns" >
    <param name="qname" />
    <variable name="ns_prefix" select="substring-before($qname,':')" />
    <if test="string-length($ns_prefix)>0"> <!-- prefix must be explicit -->
      <variable name="name" select="substring-after($qname,':')" />
      <value-of select="ancestor-or-self::*/namespace::*[name()=$ns_prefix][position()=1]" />
    </if>
    <if test="string-length($ns_prefix)=0 and ancestor-or-self::*/namespace::*[name()=''][position()=1]"> <!-- no prefix -->
		<variable name="name" select="substring-after($qname,':')" />
		<value-of select="ancestor-or-self::*/namespace::*[name()=''][position()=1]" />
    </if>
  </template>


  <!-- expand namespace of a qname -->
  <template name="expand-ns" >
    <param name="qname" />
    <variable name="ns_prefix" select="substring-before($qname,':')" />
    <if test="string-length($ns_prefix)>0"> <!-- prefix must be explicit -->
		<variable name="name" select="substring-after($qname,':')" />
		<variable name="ns_uri" select="ancestor-or-self::*/namespace::*[name()=$ns_prefix][position()=1]" />
		<value-of select="concat($ns_uri,$name)" />
    </if>
    <if test="string-length($ns_prefix)=0 and ancestor-or-self::*/namespace::*[name()=''][position()=1]"> <!-- no prefix -->
		<variable name="name" select="substring-after($qname,':')" />
		<variable name="ns_uri" select="ancestor-or-self::*/namespace::*[name()=''][position()=1]" />
		<value-of select="concat($ns_uri,$name)" />
    </if>
  </template>

  <!-- determines the CURIE / URI of a node -->
  <template name="self-curie-or-uri" >
    <param name="node" />
    <choose>
     <when test="$node/attribute::about"> <!-- we have an about attribute to extend -->
       <call-template name="expand-curie-or-uri"><with-param name="curie_or_uri" select="$node/attribute::about"/></call-template>
     </when>
     <when test="$node/attribute::src"> <!-- we have an src attribute to extend -->
       <call-template name="expand-curie-or-uri"><with-param name="curie_or_uri" select="$node/attribute::src"/></call-template>
     </when>
     <when test="$node/attribute::id"> <!-- we have an id attribute to extend -->
       <value-of select="concat($this,'#',$node/attribute::id)" />
     </when>
     <otherwise>blank:node:<value-of select="generate-id($node)" /></otherwise>
    </choose>
  </template>  
			

  <!-- expand CURIE / URI -->
  <template name="expand-curie-or-uri" >
    <param name="curie_or_uri" />
    <choose>
     <when test="starts-with($curie_or_uri,'[_:')"> <!-- we have a CURIE blank node -->
      <value-of select="concat('blank:node:',substring-after(substring-before($curie_or_uri,']'),'[_:'))"/>
     </when>
     <when test="starts-with($curie_or_uri,'[')"> <!-- we have a CURIE between square brackets -->
      <call-template name="expand-ns"><with-param name="qname" select="substring-after(substring-before($curie_or_uri,']'),'[')"/></call-template>
     </when>
     <when test="starts-with($curie_or_uri,'#')"> <!-- we have an anchor -->
      <value-of select="concat($this,$curie_or_uri)" />
     </when>
     <when test="string-length($curie_or_uri)=0"> <!-- empty anchor means the document itself -->
      <value-of select="$this" />
     </when>
     <when test="not(starts-with($curie_or_uri,'[')) and contains($curie_or_uri,':')"> <!-- it is a URI -->
      <value-of select="$curie_or_uri" />
     </when>     
     <when test="not(contains($curie_or_uri,'://')) and not(starts-with($curie_or_uri,'/'))"> <!-- relative URL -->
      <value-of select="concat($this_location,$curie_or_uri)" />
     </when>
     <when test="not(contains($curie_or_uri,'://')) and starts-with($curie_or_uri,'//')"> <!-- protocol only -->
      <value-of select="concat(substring-before($this_root,':'),':',$curie_or_uri)" />
     </when>
     <when test="not(contains($curie_or_uri,'://')) and (starts-with($curie_or_uri,'/'))"> <!-- URL from root domain -->
      <value-of select="concat($this_root,substring-after($curie_or_uri,'/'))" />
     </when>
     <otherwise>UNKNOWN CURIE URI</otherwise>
    </choose>
  </template>  
  
  <!-- returns the first token in a list separated by spaces -->
  <template name="get-first-token">
  	<param name="tokens" />
	<if test="string-length($tokens)>0">
		<choose>
			<when test="contains($tokens,' ')">
				<value-of select="normalize-space(substring-before($tokens,' '))"/>			
			</when>
			<otherwise><value-of select="$tokens" /></otherwise>
		</choose>
	</if>
  </template>

  <!-- returns the namespace for a predicate -->
  <template name="get-predicate-ns">
  	<param name="qname" />
	<variable name="ns_prefix" select="substring-before(translate($qname,'[]',''),':')" />
	<choose>
	  <when test="string-length($ns_prefix)>0">
		<call-template name="return-ns"><with-param name="qname" select="$qname"/></call-template>
	   </when>
	   <!--  GR: by omitting this we avoid the inclusion 
	   of unqualified elements,namely, html elements  
	   <otherwise><value-of select="$default_voc" /></otherwise>
	-->
	   <otherwise></otherwise>

	</choose>
  </template>

  <!-- returns the name for a predicate -->
  <template name="get-predicate-name">
  	<param name="qname" />
	<variable name="after_prefix" select="substring-after(translate($qname,'[]',''),':')" />
	<choose>
	  <when test="string-length($after_prefix)>0">
		<value-of select="$after_prefix" />
	   </when>
	   <otherwise><value-of select="$qname" /></otherwise>
	</choose>
  </template>

<!-- named templates to generate RDF - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->

  <template name="recursive-copy"> <!-- full copy -->
  	<!-- Old Hack for copy.
  	<choose>
  		<when test="self::*[node()]">
  		<xsl:text disable-output-escaping="yes">&lt;</xsl:text><value-of select="name(.)"/> xmlns='<value-of select="namespace-uri(.)"/>'<for-each select="/attribute::* "><value-of select="concat(' ',name(.))"/>='<value-of select="."/>'</for-each><text disable-output-escaping="yes">&gt;</text><for-each select="node()"><call-template name="recursive-copy" /></for-each><text disable-output-escaping="yes">&lt;</text>/<value-of select="name(.)"/><text disable-output-escaping="yes">&gt;</text>			
  		</when>
  		<when test="self::*[not (node())]">
  		<xsl:text disable-output-escaping="yes">&lt;</xsl:text><value-of select="name(.)"/> xmlns='<value-of select="namespace-uri(.)"/>'<for-each select="/attribute::* "><value-of select="concat(' ',name(.))"/>='<value-of select="."/>'</for-each>/<xsl:text disable-output-escaping="yes">&gt;</xsl:text>			
  		</when>
  		<otherwise>
			<copy>
			 <for-each select="node()">
				<call-template name="recursive-copy" />
			 </for-each>
			</copy>
  		</otherwise>
  	</choose> -->
  	<copy><for-each select="node()|attribute::* "><call-template name="recursive-copy" /></for-each></copy>
  </template>

  
    <!-- generate recursive call for multiple objects in rel or rev -->
  <template name="relrev" >
    <param name="subject" />
    <param name="object" />
    
    <!-- test for multiple predicates -->
    <variable name="single-object"><call-template name="get-first-token"><with-param name="tokens" select="$object"/></call-template></variable> 
  	 
     <if test="@rel">
       <call-template name="relation">
        <with-param name="subject" select ="$subject" />
        <with-param name="object" select ="$single-object" />
        <with-param name="predicate" select ="@rel"/>
       </call-template>       
     </if>

     <if test="@rev">
       <call-template name="relation">
        <with-param name="subject" select ="$single-object" />
        <with-param name="object" select ="$subject" />
        <with-param name="predicate" select ="@rev"/>
       </call-template>      
     </if>

    <!-- recursive call for multiple predicates -->
    <variable name="other-objects" select="normalize-space(substring-after($object,' '))" />
    <if test="string-length($other-objects)>0">
		<call-template name="relrev">
			<with-param name="subject" select="$subject"/>
			<with-param name="object" select="$other-objects"/>
		</call-template>
    </if>
           	
  </template>
  
  
  <!-- generate an RDF statement for a relation -->
  <template name="relation" >
    <param name="subject" />
    <param name="predicate" />
    <param name="object" />
  
    <!-- test for multiple predicates -->
    <variable name="single-predicate"><call-template name="get-first-token"><with-param name="tokens" select="$predicate"/></call-template></variable>
    
    <!-- get namespace of the predicate -->
    <variable name="predicate-ns"><call-template name="get-predicate-ns"><with-param name="qname" select="$single-predicate"/></call-template></variable>
 
     <!-- get name of the predicate -->
    <variable name="predicate-name"><call-template name="get-predicate-name"><with-param name="qname" select="$single-predicate"/></call-template></variable>
    
    <choose>
     <when test="string-length($predicate-ns)>0"> <!-- there is a known namespace for the predicate -->
	    <element name = "rdf:Description" namespace="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
	      <choose>
	      	<when test="starts-with($subject,'blank:node:')"><attribute name="rdf:nodeID"><value-of select="substring-after($subject,'blank:node:')" /></attribute></when>
	      	<otherwise><attribute name="rdf:about"><value-of select="$subject" /></attribute></otherwise>
	      </choose>
	      <element name = "{$predicate-name}" namespace="{$predicate-ns}">
	        <choose>
	      	  <when test="starts-with($object,'blank:node:')"><attribute name="rdf:nodeID"><value-of select="substring-after($object,'blank:node:')" /></attribute></when>
	      	  <otherwise><attribute name="rdf:resource"><value-of select="$object" /></attribute></otherwise>
	        </choose>
	      </element>     
	    </element>
     </when>
     <otherwise> <!-- generate a comment for debug
            <xsl:comment>Could not produce the triple for: <value-of select="$subject" /> - <value-of select="$single-predicate" /> - <value-of select="$object" /></xsl:comment>
      -->
     </otherwise>
    </choose>

    <!-- recursive call for multiple predicates -->
    <variable name="other-predicates" select="normalize-space(substring-after($predicate,' '))" />
    <if test="string-length($other-predicates)>0">
		<call-template name="relation">
			<with-param name="subject" select="$subject"/>
			<with-param name="predicate" select="$other-predicates"/>
			<with-param name="object" select="$object"/>
		</call-template>    	
    </if>

  </template>


  <!-- generate an RDF statement for a property -->
  <template name="property" >
    <param name="subject" />
    <param name="predicate" />
    <param name="object" />
    <param name="datatype" />
    <param name="attrib" /> <!-- is the content from an attribute ? true /false -->
    <param name="language" />

    <!-- test for multiple predicates -->
    <variable name="single-predicate"><call-template name="get-first-token"><with-param name="tokens" select="$predicate"/></call-template></variable>
     
    <!-- get namespace of the predicate -->
    <variable name="predicate-ns"><call-template name="get-predicate-ns"><with-param name="qname" select="$single-predicate"/></call-template></variable>
 
     <!-- get name of the predicate -->
    <variable name="predicate-name"><call-template name="get-predicate-name"><with-param name="qname" select="$single-predicate"/></call-template></variable>
     
    <choose>
     <when test="string-length($predicate-ns)>0"> <!-- there is a known namespace for the predicate -->
	    <element name = "rdf:Description" namespace="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
	      <choose>
	      	<when test="starts-with($subject,'blank:node:')"><attribute name="rdf:nodeID"><value-of select="substring-after($subject,'blank:node:')" /></attribute></when>
	      	<otherwise><attribute name="rdf:about"><value-of select="$subject" /></attribute></otherwise>
	      </choose>
	      <element name = "{$predicate-name}" namespace="{$predicate-ns}">
	      <if test="string-length($language)>0"><attribute name="xml:lang"><value-of select="$language" /></attribute></if>
	      <choose>
	        <when test="$datatype='http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral'">
	         <choose>
	         	<when test="$attrib='true'"> <!-- content is in an attribute -->
	         	  <value-of select="normalize-space(string($object))" />
	            </when>
	         	<otherwise> <!-- content is in the element and may include some tags -->
<!--	         	 <attribute name="rdf:datatype"><value-of select="$datatype" /></attribute> -->
	         	 <attribute name="rdf:parseType"><value-of select="'Literal'" /></attribute>
				 <for-each select="$object/node()"> 
					<call-template name="recursive-copy" />
				 </for-each>
				</otherwise>
			 </choose>
	        </when>
	        <when test="string-length($datatype)>0">
	        	<!-- there is a datatype other than XMLLiteral -->
	         <attribute name="rdf:datatype"><value-of select="$datatype" /></attribute>
	         <choose>
	         	<when test="$attrib='true'"> <!-- content is in an attribute -->
	         	  <value-of select="normalize-space(string($object))" />
	            </when>
	         	<otherwise> <!-- content is in the text nodes of the element -->
				 <value-of select="normalize-space($object)" />
				</otherwise>
			 </choose>
	        </when>
	        <otherwise> <!-- there is no datatype -->
	         <choose>
	         	<when test="$attrib='true'"> <!-- content is in an attribute -->
	         	  <value-of select="normalize-space(string($object))" />
	            </when>
	         	<otherwise> <!-- content is in the text nodes of the element -->
				 <for-each select="$object/node()"> 
					<call-template name="recursive-copy" />
				 </for-each>
				</otherwise>
			 </choose> 
	        </otherwise>
	      </choose>
	      </element>        
	    </element>
     </when>
     <otherwise> <!-- generate a comment for debug
       <xsl:comment>Could not produce the triple for: <value-of select="$subject" /> - <value-of select="$single-predicate" /> - <value-of select="$object" /></xsl:comment>
      -->
     </otherwise>
    </choose>

    <!-- recursive call for multiple predicates -->
    <variable name="other-predicates" select="normalize-space(substring-after($predicate,' '))" />
    <if test="string-length($other-predicates)>0">
		<call-template name="property">
			<with-param name="subject" select="$subject"/>
			<with-param name="predicate" select="$other-predicates"/>
			<with-param name="object" select="$object"/>
			<with-param name="datatype" select="$datatype"/>
			<with-param name="attrib" select="$attrib"/>
			<with-param name="language" select="$language"/>
		</call-template>    	
    </if>
     
  </template>



  <!-- generate an RDF statement for a class -->
  <template name="class" >
    <param name="resource" />
    <param name="class" />

    <!-- case multiple classes -->
    <variable name="single-class"><call-template name="get-first-token"><with-param name="tokens" select="$class"/></call-template></variable>
     
    <!-- get namespace of the class -->    
    <variable name="class-ns"><call-template name="return-ns"><with-param name="qname" select="$single-class"/></call-template></variable>
    
    <if test="string-length($class-ns)>0"> <!-- we have a qname for the class -->
   	     <variable name="expended-class"><call-template name="expand-ns"><with-param name="qname" select="$single-class"/></call-template></variable>        
		 <element name = "rdf:Description" namespace="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
	       <choose>
	      	<when test="starts-with($resource,'blank:node:')"><attribute name="rdf:nodeID"><value-of select="substring-after($resource,'blank:node:')" /></attribute></when>
	      	<otherwise><attribute name="rdf:about"><value-of select="$resource" /></attribute></otherwise>
	       </choose>
		   <element name = "rdf:type" namespace="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
		     <attribute name="rdf:resource"><value-of select="$expended-class" /></attribute>
		   </element>     
		 </element>
	 </if>     

    <!-- recursive call for multiple classes -->
    <variable name="other-classes" select="normalize-space(substring-after($class,' '))" />
    <if test="string-length($other-classes)>0">
		<call-template name="class">
			<with-param name="resource" select="$resource"/>
			<with-param name="class" select="$other-classes"/>
		</call-template>    	
    </if>
     
  </template>


<!-- ignore the rest of the DOM -->
<template match="text()|@*|*"><apply-templates /></template>


</stylesheet>
