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

/**
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 */
public class CollectorStdoutImpl implements Collector {

	private static ThreadLocal/* <Integer> */tl = new ThreadLocal/* <Integer> */() {
		protected Object/* Integer */initialValue() {
			return new Integer(0);
		}
	};

	public void collect(String word, int begin, int end) {
		int last = ((Integer) tl.get()).intValue();
		Integer c = new Integer(last + 1);
		tl.set(c);
		System.out.println(c + ":\t[" + begin + ", " + end + ")=" + word);
	}

}
