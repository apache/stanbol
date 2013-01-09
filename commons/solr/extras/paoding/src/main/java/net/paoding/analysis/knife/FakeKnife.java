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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 无实际用处的Knife。用于示例装载Knife并进行属性设置。
 * <p>
 * 
 * @see paoding-knives-user.properties
 * @author zhiliang.wang [qieqie.wang@gmail.com]
 * @since 2.0.2
 */

public class FakeKnife implements Knife, DictionariesWare {

	private Logger log = LoggerFactory.getLogger(FakeKnife.class);

	private String name;

	private int intParam;

	private Inner inner = new Inner();

	public void setName(String name) {
		this.name = name;
		log.info("set property: name=" + name);
	}

	public String getName() {
		return name;
	}

	public int getIntParam() {
		return intParam;
	}

	public void setIntParam(int intParam) {
		this.intParam = intParam;
		log.info("set property: intParam=" + intParam);
	}

	public void setInner(Inner inner) {
		this.inner = inner;
	}

	public Inner getInner() {
		return inner;
	}

	public int assignable(Beef beef, int offset, int index) {
		return LIMIT;
	}

	public int dissect(Collector collector, Beef beef, int offset) {
		throw new Error("this knife doesn't accept any beef");
	}

	public void setDictionaries(Dictionaries dictionaries) {
	}

	class Inner {
		private boolean bool;

		public void setBool(boolean bool) {
			this.bool = bool;
			log.info("set property: bool=" + bool);
		}

		public boolean isBool() {
			return bool;
		}
	}

}
