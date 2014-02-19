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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.paoding.analysis.dictionary.support.detection.Difference;
import net.paoding.analysis.dictionary.support.detection.DifferenceListener;
import net.paoding.analysis.dictionary.support.detection.Node;

/**
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 * @since 2.0.2
 * 
 */
public class FileDictionariesDifferenceListener implements DifferenceListener {

	private FileDictionaries dictionaries;

	private KnifeBox knifeBox;

	public FileDictionariesDifferenceListener() {
	}

	public FileDictionariesDifferenceListener(Dictionaries dictionaries,
			KnifeBox knifeBox) {
		this.dictionaries = (FileDictionaries) dictionaries;
		this.knifeBox = knifeBox;
	}

	public Dictionaries getDictionaries() {
		return dictionaries;
	}

	public void setDictionaries(Dictionaries dictionaries) {
		this.dictionaries = (FileDictionaries) dictionaries;
	}

	public KnifeBox getKnifeBox() {
		return knifeBox;
	}

	public void setKnifeBox(KnifeBox knifeBox) {
		this.knifeBox = knifeBox;
	}

	public synchronized void on(Difference diff) {
		List/* <Node> */all = new LinkedList/* <Node> */();
		all.addAll((List/* <Node> */) diff.getDeleted());
		all.addAll((List/* <Node> */) diff.getModified());
		all.addAll((List/* <Node> */) diff.getNewcome());
		for (Iterator iter = all.iterator(); iter.hasNext();) {
			Node node = (Node) iter.next();
			if (node.isFile()) {
				dictionaries.refreshDicWords(node.getPath());
			}
		}
		Knife[] knives = knifeBox.getKnives();
		for (int i = 0; i < knives.length; i ++) {
			Knife knife = knives[i];
			if (knife instanceof DictionariesWare) {
				((DictionariesWare) knife).setDictionaries(dictionaries);
			}
		}
	}

}
