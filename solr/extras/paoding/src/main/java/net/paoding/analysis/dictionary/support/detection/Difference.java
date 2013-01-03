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
package net.paoding.analysis.dictionary.support.detection;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 * @since 2.0.2
 * 
 */
public class Difference {

	/**
	 * 变更了的
	 * 
	 * @return
	 */
	private List/* <Node> */modified = new LinkedList/* <Node> */();

	/**
	 * 删除了的
	 * 
	 * @return
	 */
	private List/* <Node> */deleted = new LinkedList/* <Node> */();

	/**
	 * 新加的
	 * 
	 * @return
	 */
	private List/* <Node> */newcome = new LinkedList/* <Node> */();

	private Snapshot older;
	private Snapshot younger;

	public List/* <Node> */getModified() {
		return modified;
	}

	public void setModified(List/* <Node> */modified) {
		this.modified = modified;
	}

	public List/* <Node> */getDeleted() {
		return deleted;
	}

	public void setDeleted(List/* <Node> */deleted) {
		this.deleted = deleted;
	}

	public List/* <Node> */getNewcome() {
		return newcome;
	}

	public void setNewcome(List/* <Node> */newcome) {
		this.newcome = newcome;
	}

	public Snapshot getOlder() {
		return older;
	}

	public void setOlder(Snapshot older) {
		this.older = older;
	}

	public Snapshot getYounger() {
		return younger;
	}

	public void setYounger(Snapshot younger) {
		this.younger = younger;
	}

	public boolean isEmpty() {
		return deleted.isEmpty() && modified.isEmpty() && newcome.isEmpty();
	}

	public String toString() {
		String smodified = ArraysToString(modified.toArray(new Node[] {}));
		String snewcome = ArraysToString(newcome.toArray(new Node[] {}));
		String sdeleted = ArraysToString(deleted.toArray(new Node[] {}));
		return "modified=" + smodified + ";newcome=" + snewcome + ";deleted="
				+ sdeleted;
	}

	// 低于JDK1.5无Arrays.toString()方法，故有以下方法
	private static String ArraysToString(Object[] a) {
		if (a == null)
			return "null";
		int iMax = a.length - 1;
		if (iMax == -1)
			return "[]";

		StringBuffer b = new StringBuffer();
		b.append('[');
		for (int i = 0;; i++) {
			b.append(String.valueOf(a[i]));
			if (i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}
}
