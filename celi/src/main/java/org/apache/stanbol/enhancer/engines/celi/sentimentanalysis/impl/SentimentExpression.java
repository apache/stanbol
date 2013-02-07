/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.enhancer.engines.celi.sentimentanalysis.impl;

import java.util.List;

public class SentimentExpression {
	
	private String sentimentType=null, snippetStr=null;
	private  Integer startSnippet=null, endSnippet=null;
	private  List<String> arguments=null;
	
	public SentimentExpression(){}
	
	public SentimentExpression(String sentimentType, String snippetStr, Integer startSnippet, Integer endSnippet, List<String> arguments) {
		super();
		this.sentimentType = sentimentType;
		this.snippetStr = snippetStr;
		this.startSnippet = startSnippet;
		this.endSnippet = endSnippet;
		this.arguments = arguments;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public Integer getEndSnippet() {
		return endSnippet;
	}

	public double getSentimentPolarityAsDoubleValue(){
		if(this.sentimentType.equalsIgnoreCase(SentimentAnalysisServiceClientHttp.positive))
			return 1.0;
		else if(this.sentimentType.equalsIgnoreCase(SentimentAnalysisServiceClientHttp.negative))
			return -1.0;
		else 
			return 0.0;
	}

	public String getSentimentType() {
		return sentimentType;
	}

	public String getSnippetStr() {
		return snippetStr;
	}

	public Integer getStartSnippet() {
		return startSnippet;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = arguments;
	}

	public void setEndSnippet(Integer endSnippet) {
		this.endSnippet = endSnippet;
	}

	public void setSentimentType(String sentimentType) {
		this.sentimentType = sentimentType;
	}

	public void setSnippetStr(String snippetStr) {
		this.snippetStr = snippetStr;
	}
	
	public void setStartSnippet(Integer startSnippet) {
		this.startSnippet = startSnippet;
	}

}
