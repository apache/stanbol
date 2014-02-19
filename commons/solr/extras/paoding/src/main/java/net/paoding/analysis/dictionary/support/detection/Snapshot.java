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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 * @since 2.0.2
 * 
 */
public class Snapshot {

	// 此次快照版本，使用时间表示
	private long version;

	// 根地址，绝对地址，使用/作为目录分隔符
	private String root;

	// String为相对根的地址，使用/作为目录分隔符
	private Map/*<String, InnerNode>*/ nodesMap = new HashMap/*<String, InnerNode>*/();

	//
	private InnerNode[] nodes;
	
	//checksum of this snapshot
	private String checksum;

	private Snapshot() {
	}

	public static Snapshot flash(String root, FileFilter filter) {
		return flash(new File(root), filter);
	}
	
	public static Snapshot flash(File rootFile, FileFilter filter) {
		Snapshot snapshot = new Snapshot();
		snapshot.implFlash(rootFile, filter);
		return snapshot;
	}
	
	private void implFlash(File rootFile, FileFilter filter) {
		version = System.currentTimeMillis();
		root = rootFile.getAbsolutePath().replace('\\', '/');
		if (!rootFile.exists()) {
			// do nothing, maybe the file has been deleted
			nodes = new InnerNode[0];
		} else {
			InnerNode rootNode = new InnerNode();
			rootNode.path = root;
			rootNode.isFile = rootFile.isFile();
			rootNode.lastModified = rootFile.lastModified();
			nodesMap.put(root, rootNode);
			if (rootFile.isDirectory()) {
				LinkedList/*<File>*/ files = getPosterity(rootFile, filter);
				nodes = new InnerNode[files.size()];
				Iterator/*<File>*/ iter = files.iterator();
				for (int i = 0; i < nodes.length; i++) {
					File f = (File) iter.next();
					String path = f.getAbsolutePath().substring(
							this.root.length() + 1);
					path = path.replace('\\', '/');
					InnerNode node = new InnerNode();
					node.path = path;
					node.isFile = f.isFile();
					node.lastModified = f.lastModified();
					int index = path.lastIndexOf('/');
					node.parent = index == -1 ? root : path.substring(0, index);
					nodes[i] = node;
					nodesMap.put(path, node);
				}
			}
		}
		
		//sort node for checksum
		Arrays.sort(nodes);
		checksum = null;
	}
	
	/**
	 * build checksum of snapshot
	 * 
	 * @return checksum of current snapshot
	 */
	private void buildCheckSum() {
		short checksum = -631;
		short multiplier = 1;
		String ENCODING = "UTF-8";
		
		StringBuilder value = new StringBuilder();
		for(int i = 0; i < nodes.length; i++){
			value.append(nodes[i].path);
			value.append(nodes[i].isFile);
			value.append(nodes[i].parent);
			value.append(nodes[i].lastModified);
		}

		try {
			byte[] data = value.toString().getBytes(ENCODING);
			for (int b = 0; b < data.length; ++b)
				checksum += data[b] * multiplier++;
		} catch (java.io.UnsupportedEncodingException ex) {

		}

		this.checksum = String.valueOf(checksum);
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public String getRoot() {
		return root;
	}

	public void setRoot(String root) {
		this.root = root;
	}

	//get checksum in lazy mode
	public String getCheckSum() {
		if (checksum == null) buildCheckSum();
		return checksum;
	}

	public Difference diff(Snapshot that) {
		Snapshot older = that;
		Snapshot younger = this;
		if (that.version > this.version) {
			older = this;
			younger = that;
		}
		Difference diff = new Difference();
		if (!younger.root.equals(older.root)) {
			throw new IllegalArgumentException("the snaps should be same root");
		}
		for (int i = 0; i < older.nodes.length; i ++) {
			InnerNode olderNode = older.nodes[i];
			InnerNode yongerNode = (InnerNode) younger.nodesMap.get((String) olderNode.path);
			if (yongerNode == null) {
				diff.getDeleted().add(olderNode);
			} else if (yongerNode.lastModified != olderNode.lastModified) {
				diff.getModified().add(olderNode);
			}
		}

		for (int i = 0; i < younger.nodes.length; i ++) {
			InnerNode yongerNode = younger.nodes[i];
			InnerNode olderNode = (InnerNode) older.nodesMap.get((String) yongerNode.path);
			if (olderNode == null) {
				diff.getNewcome().add(yongerNode);
			}
		}
		diff.setOlder(older);
		diff.setYounger(younger);
		return diff;
	}
	
	public static void main(String[] args) throws InterruptedException {
		File f = new File("dic");
		Snapshot snapshot1 = Snapshot.flash(f, null);
		System.out.println("----");
		Thread.sleep(3000);
		System.out.println("----");
		Thread.sleep(3000);
		System.out.println("----");
		Snapshot snapshot2 = Snapshot.flash(f, null);
		Difference diff = snapshot2.diff(snapshot1);
		String deleted = ArraysToString(diff.getDeleted().toArray(
				new Node[] {}));
		System.out.println("deleted: " + deleted);
		String modified = ArraysToString(diff.getModified().toArray(
				new Node[] {}));
		System.out.println("modified: " + modified);
		String newcome = ArraysToString(diff.getNewcome().toArray(
				new Node[] {}));
		System.out.println("newcome: " + newcome);
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

	// --------------------------------------------

	private LinkedList/*<File>*/ getPosterity(File root, FileFilter filter) {
		ArrayList/*<File>*/ dirs = new ArrayList/*<File>*/();
		LinkedList/*<File>*/ files = new LinkedList/*<File>*/();
		dirs.add(root);
		int index = 0;
		while (index < dirs.size()) {
			File cur = (File) dirs.get(index++);
			File[] children = cur.listFiles();
			for (int i = 0; i < children.length; i ++) {
				File f = children[i];
				if (filter == null || filter.accept(f)) {
					if (f.isDirectory()) {
						dirs.add(f);
					} else {
						files.add(f);
					}
				}
			}
		}
		return files;
	}

	class InnerNode extends Node {
		String parent;
		long lastModified;
		
		@Override
		public int compareTo(Node o) {
			// super compare
			int result = super.compareTo(o);
			if (result != 0)
				return result;

			if (o instanceof InnerNode) {
				InnerNode node = (InnerNode) o;
	
				// parent
				if (this.parent != null && node.parent != null) {
					int cmp = this.parent.compareTo(node.parent);
					if (cmp != 0)
						return cmp;
				} else {
					if (this.parent != null && node.parent == null)
						return 1;
					if (this.parent == null && node.parent != null)
						return -1;
				}
	
				// lastModified
				if (this.lastModified > node.lastModified)
					return 1;
				if (this.lastModified < node.lastModified)
					return -1;
			}
			return 0;
		}
	}
}
