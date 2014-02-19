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

import net.paoding.analysis.dictionary.Dictionary;
import net.paoding.analysis.dictionary.support.detection.DifferenceListener;

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
public interface Dictionaries {
	/**
	 * 词汇表字典
	 * 
	 * @return
	 */
	public Dictionary getVocabularyDictionary();

	/**
	 * 姓氏字典
	 * 
	 * @return
	 */
	public Dictionary getConfucianFamilyNamesDictionary();

	/**
	 * 忽略的词语
	 * 
	 * @return
	 */
	public Dictionary getNoiseCharactorsDictionary();

	/**
	 * 忽略的单字
	 * 
	 * @return
	 */
	public Dictionary getNoiseWordsDictionary();

	/**
	 * 计量单位
	 * 
	 * @return
	 */
	public Dictionary getUnitsDictionary();
	
	/**
	 * lantin+cjk, num+cjk
	 * @return
	 */
	public Dictionary getCombinatoricsDictionary();
	
	/**
	 * 
	 * @param l
	 */
	public void startDetecting(int interval, DifferenceListener l);
	public void stopDetecting();
}
