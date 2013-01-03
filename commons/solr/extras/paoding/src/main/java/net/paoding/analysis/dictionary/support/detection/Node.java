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

/**
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 * @since 2.0.2
 * 
 */
public class Node implements Comparable<Node>{

	String path;

	boolean isFile;

	public Node() {
	}

	public Node(String path, boolean isFile) {
		this.path = path;
		this.isFile = isFile;
	}

	/**
	 * 返回结点路径
	 * <p>
	 * 如果该结点为根，则返回根的绝对路径<br>
	 * 如果该结点为根下的目录或文件，则返回其相对与根的路径<br>
	 * 
	 * @return
	 */
	public String getPath() {
		return path;
	}

	/**
	 * 该结点当时的属性：是否为文件
	 * 
	 * @return
	 */
	public boolean isFile() {
		return isFile;
	}

	public String toString() {
		return path;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Node other = (Node) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

	public int compareTo(Node o) {
		//path
		if (this.path != null && o.path != null){
			int cmp = this.path.compareTo(o.path);
			if (cmp != 0) return cmp;
		} else {
			if (this.path != null && o.path == null) return 1;
			if (this.path == null && o.path != null) return -1;
		}
		
		//isfile
		if (this.isFile && !o.isFile) return 1;
		if (!this.isFile && o.isFile) return -1;
		return 0;
	}

}
