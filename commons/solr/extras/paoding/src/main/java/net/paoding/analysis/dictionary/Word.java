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
package net.paoding.analysis.dictionary;

public class Word implements Comparable, CharSequence {

	public static final int DEFAUL = 0;
	private String text;
	private int modifiers = DEFAUL;

	public Word() {
	}

	public Word(String text) {
		this.text = text;
	}

	public Word(String text, int modifiers) {
		this.text = text;
		this.modifiers = modifiers;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getModifiers() {
		return modifiers;
	}

	public void setModifiers(int modifiers) {
		this.modifiers = modifiers;
	}

	public int compareTo(Object obj) {
		return this.text.compareTo(((Word) obj).text);
	}

	public String toString() {
		return text;
	}

	public int length() {
		return text.length();
	}

	public boolean startsWith(Word word) {
		return text.startsWith(word.text);
	}

	public char charAt(int j) {
		return text.charAt(j);
	}

	public CharSequence subSequence(int start, int end) {
		throw new UnsupportedOperationException();
	}

	public int hashCode() {
		return text.hashCode();
	}

	public boolean equals(Object obj) {
		return text.equals(((Word) obj).text);
	}

	public void setNoiseCharactor() {
		modifiers |= 1;
	}

	public void setNoiseWord() {
		modifiers |= (1 << 1);
	}

	public boolean isNoiseCharactor() {
		return (modifiers & 1) == 1;
	}

	public boolean isNoise() {
		return isNoiseCharactor() || isNoiseWord();
	}

	public boolean isNoiseWord() {
		return (modifiers >> 1 & 1) == 1;
	}
	
	public static void main(String[] args) {
		Word w = new Word("");
		System.out.println(w.isNoiseCharactor());
		w.setNoiseCharactor();
		System.out.println(w.isNoiseCharactor());
		System.out.println(w.isNoiseWord());
		w.setNoiseWord();
		System.out.println(w.isNoiseWord());
	}

}
