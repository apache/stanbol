/**
 * Copyright 2007 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.paoding.analysis.knife;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.paoding.analysis.dictionary.BinaryDictionary;
import net.paoding.analysis.dictionary.Dictionary;
import net.paoding.analysis.dictionary.HashBinaryDictionary;
import net.paoding.analysis.dictionary.Hit;
import net.paoding.analysis.dictionary.Word;
import net.paoding.analysis.dictionary.support.detection.Detector;
import net.paoding.analysis.dictionary.support.detection.DifferenceListener;
import net.paoding.analysis.dictionary.support.detection.ExtensionFileFilter;
import net.paoding.analysis.dictionary.support.filewords.FileWordsReader;
import net.paoding.analysis.exception.PaodingAnalysisException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 中文字典缓存根据地,为{@link CJKKnife}所用。<br>
 * 从本对象可以获取中文需要的相关字典。包括词汇表、姓氏表、计量单位表、忽略的词或单字等。
 * <p>
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 * @see CJKKnife
 * 
 * @since 1.0
 */
public class FileDictionaries implements Dictionaries {

	// -------------------------------------------------

	protected Logger log = LoggerFactory.getLogger(FileDictionaries.class);

	// -------------------------------------------------

	/**
	 * 词汇表字典
	 */
	protected Dictionary vocabularyDictionary;

	/**
	 * lantin+cjk的词典
	 */
	protected Dictionary combinatoricsDictionary;

	/**
	 * 姓氏字典
	 * 
	 */
	protected Dictionary confucianFamilyNamesDictionary;

	/**
	 * 忽略的单字
	 */
	protected Dictionary noiseCharactorsDictionary;

	/**
	 * 忽略的词语
	 * 
	 */
	protected Dictionary noiseWordsDictionary;

	/**
	 * 计量单位
	 */
	protected Dictionary unitsDictionary;

	// -------------------------------------------------

	protected Map/* <String, Set<String>> */allWords;

	protected String dicHome;
	protected String skipPrefix;
	protected String noiseCharactor;
	protected String noiseWord;
	protected String unit;
	protected String confucianFamilyName;
	protected String combinatorics;
	protected String charsetName;
	protected int maxWordLen;

	// ----------------------

	public FileDictionaries() {
	}

	public FileDictionaries(String dicHome, String skipPrefix,
			String noiseCharactor, String noiseWord, String unit,
			String confucianFamilyName, String combinatorics, String charsetName, int maxWordLen) {
		this.dicHome = dicHome;
		this.skipPrefix = skipPrefix;
		this.noiseCharactor = noiseCharactor;
		this.noiseWord = noiseWord;
		this.unit = unit;
		this.confucianFamilyName = confucianFamilyName;
		this.combinatorics = combinatorics;
		this.charsetName = charsetName;
		this.maxWordLen = maxWordLen;
	}

	public String getDicHome() {
		return dicHome;
	}

	public void setDicHome(String dicHome) {
		this.dicHome = dicHome;
	}

	public String getSkipPrefix() {
		return skipPrefix;
	}

	public void setSkipPrefix(String skipPrefix) {
		this.skipPrefix = skipPrefix;
	}

	public String getNoiseCharactor() {
		return noiseCharactor;
	}

	public void setNoiseCharactor(String noiseCharactor) {
		this.noiseCharactor = noiseCharactor;
	}

	public String getNoiseWord() {
		return noiseWord;
	}

	public void setNoiseWord(String noiseWord) {
		this.noiseWord = noiseWord;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getConfucianFamilyName() {
		return confucianFamilyName;
	}

	public void setConfucianFamilyName(String confucianFamilyName) {
		this.confucianFamilyName = confucianFamilyName;
	}

	public String getCharsetName() {
		return charsetName;
	}

	public void setCharsetName(String charsetName) {
		this.charsetName = charsetName;
	}

	public int getMaxWordLen() {
		return maxWordLen;
	}

	public void setMaxWordLen(int maxWordLen) {
		this.maxWordLen = maxWordLen;
	}

	public void setLantinFllowedByCjk(String lantinFllowedByCjk) {
		this.combinatorics = lantinFllowedByCjk;
	}

	public String getLantinFllowedByCjk() {
		return combinatorics;
	}

	// -------------------------------------------------

	/**
	 * 词汇表字典
	 * 
	 * @return
	 */
	public synchronized Dictionary getVocabularyDictionary() {
		if (vocabularyDictionary == null) {
			// 大概有5639个字有词语，故取0x2fff=x^13>8000>8000*0.75=6000>5639
			vocabularyDictionary = new HashBinaryDictionary(
					getVocabularyWords(), 0x2fff, 0.75f);
			Dictionary noiseWordsDic = getNoiseWordsDictionary();
			for (int i = 0; i < noiseWordsDic.size(); i++) {
				Hit hit = vocabularyDictionary.search(noiseWordsDic.get(i), 0, noiseWordsDic.get(i).length());
				if (hit.isHit()) {
					hit.getWord().setNoiseWord();
				}
			}
			Dictionary noiseCharactorsDic = getNoiseCharactorsDictionary();
			for (int i = 0; i < noiseCharactorsDic.size(); i++) {
				Hit hit = vocabularyDictionary.search(noiseCharactorsDic.get(i), 0, noiseCharactorsDic.get(i).length());
				if (hit.isHit()) {
					hit.getWord().setNoiseCharactor();
				}
			}
			
		}
		return vocabularyDictionary;
	}

	/**
	 * 姓氏字典
	 * 
	 * @return
	 */
	public synchronized Dictionary getConfucianFamilyNamesDictionary() {
		if (confucianFamilyNamesDictionary == null) {
			confucianFamilyNamesDictionary = new BinaryDictionary(
					getConfucianFamilyNames());
		}
		return confucianFamilyNamesDictionary;
	}

	/**
	 * 忽略的词语
	 * 
	 * @return
	 */
	public synchronized Dictionary getNoiseCharactorsDictionary() {
		if (noiseCharactorsDictionary == null) {
			noiseCharactorsDictionary = new HashBinaryDictionary(
					getNoiseCharactors(), 256, 0.75f);
		}
		return noiseCharactorsDictionary;
	}

	/**
	 * 忽略的单字
	 * 
	 * @return
	 */
	public synchronized Dictionary getNoiseWordsDictionary() {
		if (noiseWordsDictionary == null) {
			noiseWordsDictionary = new BinaryDictionary(getNoiseWords());
		}
		return noiseWordsDictionary;
	}

	/**
	 * 计量单位
	 * 
	 * @return
	 */
	public synchronized Dictionary getUnitsDictionary() {
		if (unitsDictionary == null) {
			unitsDictionary = new HashBinaryDictionary(getUnits(), 1024, 0.75f);
		}
		return unitsDictionary;
	}

	public synchronized Dictionary getCombinatoricsDictionary() {
		if (combinatoricsDictionary == null) {
			combinatoricsDictionary = new BinaryDictionary(
					getCombinatoricsWords());
		}
		return combinatoricsDictionary;
	}

	
	private Detector detector;
	
	public synchronized void startDetecting(int interval, DifferenceListener l) {
		if (detector != null || interval < 0) {
			return;
		}
		Detector detector = new Detector();
		detector.setHome(dicHome);
		detector.setFilter(new ExtensionFileFilter(".dic"));
		detector.setLastSnapshot(detector.flash());
		detector.setListener(l);
		detector.setInterval(interval);
		detector.start(true);
		this.detector = detector;
	}


	public synchronized void stopDetecting() {
		if (detector == null) {
			return;
		}
		detector.setStop();
		detector = null;
	}
	
	/**
	 * 
	 * @param dicName
	 */
	protected synchronized void refreshDicWords(String dicPath) {
		int index = dicPath.lastIndexOf(".dic");
		String dicName = dicPath.substring(0, index);
		if (allWords != null) {
			try {
				Map/* <String, Set<String>> */temp = FileWordsReader
						.readWords(dicHome + dicPath, charsetName, maxWordLen);
				allWords.put(dicName, temp.values().iterator().next());
			} catch (FileNotFoundException e) {
				// 如果源文件已经被删除了，则表示该字典不要了
				allWords.remove(dicName);
			} catch (IOException e) {
				throw toRuntimeException(e);
			}
			if (!isSkipForVacabulary(dicName)) {
				this.vocabularyDictionary = null;
			}
			// 如果来的是noiseWord
			if (isNoiseWordDicFile(dicName)) {
				this.noiseWordsDictionary = null;
				// noiseWord和vocabulary有关，所以需要更新vocabulary
				this.vocabularyDictionary = null;
			}
			// 如果来的是noiseCharactors
			else if (isNoiseCharactorDicFile(dicName)) {
				this.noiseCharactorsDictionary = null;
				// noiseCharactorsDictionary和vocabulary有关，所以需要更新vocabulary
				this.vocabularyDictionary = null;
			}
			// 如果来的是单元
			else if (isUnitDicFile(dicName)) {
				this.unitsDictionary = null;
			}
			// 如果来的是亚洲人人姓氏
			else if (isConfucianFamilyNameDicFile(dicName)) {
				this.confucianFamilyNamesDictionary = null;
			}
			// 如果来的是以字母,数字等组合类语言为开头的词汇
			else if (isLantinFollowedByCjkDicFile(dicName)) {
				this.combinatoricsDictionary = null;
			}
		}
	}

	// ---------------------------------------------------------------
	// 以下为辅助性的方式-类私有或package私有

	protected Word[] getVocabularyWords() {
		Map/* <String, Set<Word>> */dics = loadAllWordsIfNecessary();
		Set/* <Word> */set = null;
		Iterator/* <Word> */iter = dics.keySet().iterator();
		while (iter.hasNext()) {
			String name = (String) iter.next();
			if (isSkipForVacabulary(name)) {
				continue;
			}
			Set/* <Word> */dic = (Set/* <Word> */) dics.get(name);
			if (set == null) {
				set = new HashSet/* <Word> */(dic);
			} else {
				set.addAll(dic);
			}
		}
		Word[] words = (Word[]) set.toArray(new Word[set.size()]);
		Arrays.sort(words);
		return words;
	}

	protected Word[] getConfucianFamilyNames() {
		return getDictionaryWords(confucianFamilyName);
	}

	protected Word[] getNoiseWords() {
		return getDictionaryWords(noiseWord);
	}

	protected Word[] getNoiseCharactors() {
		return getDictionaryWords(noiseCharactor);
	}

	protected Word[] getUnits() {
		return getDictionaryWords(unit);
	}

	protected Word[] getCombinatoricsWords() {
		return getDictionaryWords(combinatorics);
	}

	protected Word[] getDictionaryWords(String dicNameRelativeDicHome) {
		Map dics;
		try {
			dics = FileWordsReader.readWords(dicHome + "/"
					+ dicNameRelativeDicHome + ".dic", charsetName, maxWordLen);
		} catch (IOException e) {
			throw toRuntimeException(e);
		}
		Set/* <Word> */set = (Set/* <Word> */) dics.get(dicNameRelativeDicHome);
		Word[] words = (Word[]) set.toArray(new Word[set.size()]);
		Arrays.sort(words);
		return words;
	}

	// -------------------------------------

	/**
	 * 读取字典安装目录及子孙目录下的字典文件；并以该字典相对安装目录的路径(包括该字典的文件名，但不包括扩展名)作为key。
	 * 比如，如果字典安装在dic目录下，该目录下有division/china.dic，则该字典文件对应的key是"division/china"
	 */
	protected synchronized Map/* <String, Set<String>> */loadAllWordsIfNecessary() {
		if (allWords == null) {
			try {
				log.info("loading dictionaries from " + dicHome);
				allWords = FileWordsReader.readWords(dicHome, charsetName, maxWordLen);
				if (allWords.size() == 0) {
					String message = "Not found any dictionary files, have you set the 'paoding.dic.home' right? ("
							+ this.dicHome + ")";
					log.error(message);
					throw new PaodingAnalysisException(message);
				}
				log.info("loaded success!");
			} catch (IOException e) {
				throw toRuntimeException(e);
			}
		}
		return allWords;
	}

	// ---------------------------------------

	protected final boolean isSkipForVacabulary(String dicNameRelativeDicHome) {
		return dicNameRelativeDicHome.startsWith(skipPrefix)
				|| dicNameRelativeDicHome.indexOf("/" + skipPrefix) != -1;
	}

	protected boolean isUnitDicFile(String dicName) {
		return dicName.equals(this.unit);
	}

	protected boolean isNoiseCharactorDicFile(String dicName) {
		return dicName.equals(this.noiseCharactor);
	}

	protected boolean isNoiseWordDicFile(String dicName) {
		return dicName.equals(this.noiseWord);
	}

	protected boolean isConfucianFamilyNameDicFile(String dicName) {
		return dicName.equals(this.confucianFamilyName);
	}

	protected boolean isLantinFollowedByCjkDicFile(String dicName) {
		return dicName.equals(this.combinatorics);
	}

	// --------------------------------------

	protected RuntimeException toRuntimeException(IOException e) {
		return new PaodingAnalysisException(e);
	}
}
