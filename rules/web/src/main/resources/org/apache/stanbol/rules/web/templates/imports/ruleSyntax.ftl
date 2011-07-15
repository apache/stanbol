<#--
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
<#macro view>

<H4>TOKEN</H4>
<TABLE> 
 <!-- Token --> 
 <TR> 
  <TD> 
   <PRE> 
&lt;DEFAULT&gt; SKIP : {
" "
}
</PRE> 
  </TD> 
 </TR> 
 <!-- Token --> 
 <TR> 
  <TD> 
   <PRE> 
&lt;DEFAULT&gt; SKIP : {
"\r"
| "\t"
| "\n"
}
   </PRE> 
  </TD> 
 </TR> 
 <!-- Token --> 
 <TR> 
  <TD> 
   <PRE> 
&lt;DEFAULT&gt; TOKEN : {
&lt;LARROW: "-&gt;"&gt;
| &lt;COLON: ":"&gt;
| &lt;EQUAL: "="&gt;
| &lt;AND: "."&gt;
| &lt;COMMA: ","&gt;
| &lt;REFLEXIVE: "+"&gt;
| &lt;SAME: "same"&gt;
| &lt;DIFFERENT: "different"&gt;
| &lt;LESSTHAN: "lt"&gt;
| &lt;GREATERTHAN: "gt"&gt;
| &lt;IS: "is"&gt;
| &lt;NEW_NODE: "newNode"&gt;
| &lt;LENGTH: "length"&gt;
| &lt;SUBSTRING: "substring"&gt;
| &lt;UPPERCASE: "upperCase"&gt;
| &lt;LOWERCASE: "lowerCase"&gt;
| &lt;STARTS_WITH: "startsWith"&gt;
| &lt;ENDS_WITH: "endsWith"&gt;
| &lt;LET: "let"&gt;
| &lt;CONCAT: "concat"&gt;
| &lt;HAS: "has"&gt;
| &lt;VALUES: "values"&gt;
| &lt;NOTEX: "notex"&gt;
| &lt;PLUS: "sum"&gt;
| &lt;MINUS: "sub"&gt;
| &lt;NOT: "not"&gt;
| &lt;NAMESPACE: "namespace"&gt;
| &lt;LOCALNAME: "localname"&gt;
| &lt;STR: "str"&gt;
| &lt;APOX: "^"&gt;
| &lt;UNION: "union"&gt;
| &lt;CREATE_LABEL: "createLabel"&gt;
| &lt;SPARQL_C: "sparql-c"&gt;
| &lt;SPARQL_D: "sparql-d"&gt;
| &lt;SPARQL_DD: "sparql-dd"&gt;
| &lt;PROP: "prop"&gt;
| &lt;IS_BLANK: "isBlank"&gt;
| &lt;FORWARD_CHAIN: "!"&gt;
}
   </PRE> 
  </TD> 
 </TR> 
 <!-- Token --> 
 <TR> 
  <TD> 
   <PRE> 
&lt;DEFAULT&gt; TOKEN : {
&lt;LPAR: "("&gt;
| &lt;RPAR: ")"&gt;
| &lt;DQUOT: "\""&gt;
| &lt;LQUAD: "["&gt;
| &lt;RQUAD: "]"&gt;
}
   </PRE> 
  </TD> 
 </TR> 
 <!-- Token --> 
 <TR> 
  <TD> 
   <PRE> 
&lt;DEFAULT&gt; TOKEN : {
&lt;NUM: (["0"-"9"])+&gt;
| &lt;VAR: (["0"-"9","a"-"z","A"-"Z","-","_","."])+&gt;
| &lt;VARIABLE: "?" (["0"-"9","a"-"z","A"-"Z","-","_"])+&gt;
| &lt;URI: "&lt;" (["0"-"9","a"-"z","A"-"Z","-","_",".","#",":","/","(",")"])+ "&gt;"&gt;
| &lt;STRING: "\"" (["0"-"9","a"-"z","A"-"Z","-","_",".",":","/","#","\\","?"," ","!","$","%"])+ "\""&gt;
| &lt;SPARQL_STRING: "%" (["0"-"9","a"-"z","A"-"Z","-","_",".",":","/","#","\\","?"," ","!","$","%","{","}","(",")","\"","&lt;","&gt;","=","+","\n","\t","&amp;","|",","])+ "%"&gt;
| &lt;BNODE: "_:" (["0"-"9","a"-"z","A"-"Z","-","_","."])+&gt;
}
   </PRE> 
  </TD> 
 </TR> 
</TABLE> 
<H4>NON-TERMINALS</H4> 
<TABLE> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod1">start</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod2">expression</A> <A HREF="#prod3">expressionCont</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod3">expressionCont</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>( &lt;AND&gt; <A HREF="#prod2">expression</A> )</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod2">expression</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod4">prefix</A> <A HREF="#prod3">expressionCont</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod4">prefix</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod5">getVariable</A> ( <A HREF="#prod6">equality</A> | <A HREF="#prod7">rule</A> )</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;FORWARD_CHAIN&gt; <A HREF="#prod5">getVariable</A> <A HREF="#prod7">rule</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;REFLEXIVE&gt; <A HREF="#prod5">getVariable</A> <A HREF="#prod7">rule</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod6">equality</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;EQUAL&gt; ( <A HREF="#prod8">getURI</A> )</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod7">rule</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;LQUAD&gt; <A HREF="#prod9">ruleDefinition</A> &lt;RQUAD&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod9">ruleDefinition</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod10">atomList</A> &lt;LARROW&gt; <A HREF="#prod10">atomList</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;SPARQL_C&gt; &lt;LPAR&gt; &lt;SPARQL_STRING&gt; &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;SPARQL_D&gt; &lt;LPAR&gt; &lt;SPARQL_STRING&gt; &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;SPARQL_DD&gt; &lt;LPAR&gt; &lt;SPARQL_STRING&gt; &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod10">atomList</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod11">atom</A> <A HREF="#prod12">atomListRest</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod12">atomListRest</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;AND&gt; <A HREF="#prod10">atomList</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod11">atom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod13">classAtom</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod14">individualPropertyAtom</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod15">datavaluedPropertyAtom</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod16">letAtom</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod17">newNodeAtom</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod18">comparisonAtom</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod19">unionAtom</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod19">unionAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;UNION&gt; &lt;LPAR&gt; <A HREF="#prod10">atomList</A> &lt;COMMA&gt; <A HREF="#prod10">atomList</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod20">createLabelAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;CREATE_LABEL&gt; &lt;LPAR&gt; <A HREF="#prod21">stringFunctionAtom</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod22">propStringAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;PROP&gt; &lt;LPAR&gt; <A HREF="#prod21">stringFunctionAtom</A> &lt;COMMA&gt; <A HREF="#prod21">stringFunctionAtom</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod23">endsWithAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;ENDS_WITH&gt; &lt;LPAR&gt; <A HREF="#prod21">stringFunctionAtom</A> &lt;COMMA&gt; <A HREF="#prod21">stringFunctionAtom</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod24">startsWithAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;STARTS_WITH&gt; &lt;LPAR&gt; <A HREF="#prod21">stringFunctionAtom</A> &lt;COMMA&gt; <A HREF="#prod21">stringFunctionAtom</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod21">stringFunctionAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>( <A HREF="#prod25">concatAtom</A> | <A HREF="#prod26">upperCaseAtom</A> | <A HREF="#prod27">lowerCaseAtom</A> | <A HREF="#prod28">substringAtom</A> | <A HREF="#prod29">namespaceAtom</A> | <A HREF="#prod30">localnameAtom</A> | <A HREF="#prod31">strAtom</A> | <A HREF="#prod32">stringAtom</A> | <A HREF="#prod22">propStringAtom</A> | <A HREF="#prod20">createLabelAtom</A> )</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod31">strAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;STR&gt; &lt;LPAR&gt; <A HREF="#prod33">iObject</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod29">namespaceAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;NAMESPACE&gt; &lt;LPAR&gt; <A HREF="#prod33">iObject</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod30">localnameAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;LOCALNAME&gt; &lt;LPAR&gt; <A HREF="#prod33">iObject</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod32">stringAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod34">uObject</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod25">concatAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;CONCAT&gt; &lt;LPAR&gt; <A HREF="#prod21">stringFunctionAtom</A> &lt;COMMA&gt; <A HREF="#prod21">stringFunctionAtom</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod26">upperCaseAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;UPPERCASE&gt; &lt;LPAR&gt; <A HREF="#prod21">stringFunctionAtom</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod27">lowerCaseAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;LOWERCASE&gt; &lt;LPAR&gt; <A HREF="#prod21">stringFunctionAtom</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod28">substringAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;SUBSTRING&gt; &lt;LPAR&gt; <A HREF="#prod21">stringFunctionAtom</A> &lt;COMMA&gt; <A HREF="#prod35">numericFunctionAtom</A> &lt;COMMA&gt; <A HREF="#prod35">numericFunctionAtom</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod35">numericFunctionAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>( <A HREF="#prod36">sumAtom</A> | <A HREF="#prod37">subtractionAtom</A> | <A HREF="#prod38">lengthAtom</A> | <A HREF="#prod39">numberAtom</A> )</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod38">lengthAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;LENGTH&gt; &lt;LPAR&gt; <A HREF="#prod21">stringFunctionAtom</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod36">sumAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;PLUS&gt; &lt;LPAR&gt; <A HREF="#prod35">numericFunctionAtom</A> &lt;COMMA&gt; <A HREF="#prod35">numericFunctionAtom</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod37">subtractionAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;MINUS&gt; &lt;LPAR&gt; <A HREF="#prod35">numericFunctionAtom</A> &lt;COMMA&gt; <A HREF="#prod35">numericFunctionAtom</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod39">numberAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>( &lt;NUM&gt; | &lt;VARIABLE&gt; )</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod13">classAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;IS&gt; &lt;LPAR&gt; <A HREF="#prod33">iObject</A> &lt;COMMA&gt; <A HREF="#prod33">iObject</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod17">newNodeAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;NEW_NODE&gt; &lt;LPAR&gt; <A HREF="#prod33">iObject</A> &lt;COMMA&gt; <A HREF="#prod40">dObject</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod16">letAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;LET&gt; &lt;LPAR&gt; <A HREF="#prod33">iObject</A> &lt;COMMA&gt; <A HREF="#prod21">stringFunctionAtom</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod14">individualPropertyAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;HAS&gt; &lt;LPAR&gt; <A HREF="#prod33">iObject</A> &lt;COMMA&gt; <A HREF="#prod33">iObject</A> &lt;COMMA&gt; <A HREF="#prod33">iObject</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod15">datavaluedPropertyAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;VALUES&gt; &lt;LPAR&gt; <A HREF="#prod33">iObject</A> &lt;COMMA&gt; <A HREF="#prod33">iObject</A> &lt;COMMA&gt; <A HREF="#prod40">dObject</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod41">sameAsAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;SAME&gt; &lt;LPAR&gt; <A HREF="#prod21">stringFunctionAtom</A> &lt;COMMA&gt; <A HREF="#prod21">stringFunctionAtom</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod42">lessThanAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;LESSTHAN&gt; &lt;LPAR&gt; <A HREF="#prod33">iObject</A> &lt;COMMA&gt; <A HREF="#prod33">iObject</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod43">greaterThanAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;GREATERTHAN&gt; &lt;LPAR&gt; <A HREF="#prod33">iObject</A> &lt;COMMA&gt; <A HREF="#prod33">iObject</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod44">differentFromAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;DIFFERENT&gt; &lt;LPAR&gt; <A HREF="#prod21">stringFunctionAtom</A> &lt;COMMA&gt; <A HREF="#prod21">stringFunctionAtom</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod45">reference</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod8">getURI</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod5">getVariable</A> &lt;COLON&gt; <A HREF="#prod5">getVariable</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod46">varReference</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod8">getURI</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod5">getVariable</A> &lt;COLON&gt; <A HREF="#prod5">getVariable</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod8">getURI</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;URI&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod5">getVariable</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;VAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod47">getString</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;STRING&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod48">getInt</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;NUM&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod34">uObject</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>( <A HREF="#prod49">variable</A> | <A HREF="#prod45">reference</A> | <A HREF="#prod47">getString</A> | <A HREF="#prod48">getInt</A> )</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod33">iObject</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod49">variable</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod45">reference</A></TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod40">dObject</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>( <A HREF="#prod50">literal</A> | <A HREF="#prod49">variable</A> )</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod50">literal</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>( <A HREF="#prod47">getString</A> <A HREF="#prod51">typedLiteral</A> | <A HREF="#prod48">getInt</A> <A HREF="#prod51">typedLiteral</A> )</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod51">typedLiteral</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>( &lt;APOX&gt; &lt;APOX&gt; <A HREF="#prod45">reference</A> |  )</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod49">variable</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;NOTEX&gt; &lt;LPAR&gt; &lt;VARIABLE&gt; &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;VARIABLE&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;BNODE&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod52">notAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;NOT&gt; &lt;LPAR&gt; <A HREF="#prod18">comparisonAtom</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod53">isBlankAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;IS_BLANK&gt; &lt;LPAR&gt; <A HREF="#prod33">iObject</A> &lt;RPAR&gt;</TD> 
</TR> 
<TR> 
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod18">comparisonAtom</A></TD> 
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD> 
<TD ALIGN=LEFT VALIGN=BASELINE>( <A HREF="#prod41">sameAsAtom</A> | <A HREF="#prod42">lessThanAtom</A> | <A HREF="#prod43">greaterThanAtom</A> | <A HREF="#prod44">differentFromAtom</A> | <A HREF="#prod52">notAtom</A> | <A HREF="#prod24">startsWithAtom</A> | <A HREF="#prod23">endsWithAtom</A> | <A HREF="#prod53">isBlankAtom</A> )</TD> 
</TR> 
</TABLE>

</#macro>