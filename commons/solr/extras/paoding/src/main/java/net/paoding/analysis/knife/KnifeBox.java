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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * KnifeBox负责决策当遇到字符串指定位置时应使用的Knife对象.
 * <p>
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 * @see Paoding
 * 
 * @since 1.0
 * 
 */
public class KnifeBox implements Knife {

	private Knife[] knives;

	private int size;

	public KnifeBox() {
	}

	public KnifeBox(List/* <Knife> */knives) {
		this.setKnives(knives);
	}

	public KnifeBox(Knife[] knives) {
		this.setKnives(knives);
	}

	/**
	 * 返回配置的所有Knife<br>
	 * !!!不要去变更返回数组中的元素
	 * 
	 * @return
	 */
	public Knife[] getKnives() {
		return knives;
	}

	public void setKnives(List/* <Knife> */knifeList) {
		if (knifeList == null) {
			knifeList = new ArrayList(0);
		}
		size = knifeList.size();
		this.knives = new Knife[size];
		Iterator iter = knifeList.iterator();
		for (int i = 0; i < size; i++) {
			this.knives[i] = (Knife) iter.next();
		}
	}
	
	public void setKnives(Knife[] knives) {
		if (knives == null) {
			knives = new Knife[0];
		}
		size = knives.length;
		this.knives = new Knife[size];
		System.arraycopy(knives, 0, this.knives, 0, size);
	}

	public int assignable(Beef beef, int offset, int index) {
		return ASSIGNED;
	}

	public int dissect(Collector collector, Beef beef, int offset) {
		Knife knife;
		for (int i = 0; i < size; i++) {
			knife = knives[i];
			if (ASSIGNED == knife.assignable(beef, offset, offset)) {
				int lastLimit = knife.dissect(collector, beef, offset);
				// 如果返回的下一个分词点发生了变化(可进可退)，则直接返回之，
				// 否则继续让下一个Knife有机会分词
				if (lastLimit != offset) {
					return lastLimit;
				}
			}
		}
		return ++offset;
	}

}
