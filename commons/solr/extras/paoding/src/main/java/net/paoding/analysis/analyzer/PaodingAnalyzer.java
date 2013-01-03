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
package net.paoding.analysis.analyzer;

import java.util.Properties;

import net.paoding.analysis.Constants;
import net.paoding.analysis.analyzer.estimate.TryPaodingAnalyzer;
import net.paoding.analysis.knife.Knife;
import net.paoding.analysis.knife.Paoding;
import net.paoding.analysis.knife.PaodingMaker;

/**
 * PaodingAnalyzer是基于“庖丁解牛”框架的Lucene词语分析器，是“庖丁解牛”框架对Lucene的适配器。
 * <p>
 * 
 * PaodingAnalyzer是线程安全的：并发情况下使用同一个PaodingAnalyzer实例是可行的。<br>
 * PaodingAnalyzer是可复用的：推荐多次同一个PaodingAnalyzer实例。
 * <p>
 * 
 * PaodingAnalyzer自动读取类路径下的paoding-analysis.properties属性文件，装配PaodingAnalyzer
 * <p>
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 * @see PaodingAnalyzerBean
 * 
 * @since 1.0
 * 
 */
public class PaodingAnalyzer extends PaodingAnalyzerBean {

	/**
	 * 根据类路径下的paoding-analysis.properties构建一个PaodingAnalyzer对象
	 * <p>
	 * 在一个JVM中，可多次创建，而并不会多次读取属性文件，不会重复读取字典。
	 */
	public PaodingAnalyzer() {
		this(PaodingMaker.DEFAULT_PROPERTIES_PATH);
	}

	/**
	 * @param propertiesPath null表示使用类路径下的paoding-analysis.properties
	 */
	public PaodingAnalyzer(String propertiesPath) {
		init(propertiesPath);
	}

	protected void init(String propertiesPath) {
		// 根据PaodingMaker说明，
		// 1、多次调用getProperties()，返回的都是同一个properties实例(只要属性文件没发生过修改)
		// 2、相同的properties实例，PaodingMaker也将返回同一个Paoding实例
		// 根据以上1、2点说明，在此能够保证多次创建PaodingAnalyzer并不会多次装载属性文件和词典
		if (propertiesPath == null) {
			propertiesPath = PaodingMaker.DEFAULT_PROPERTIES_PATH;
		}
		Properties properties = PaodingMaker.getProperties(propertiesPath);
		String mode = Constants
				.getProperty(properties, Constants.ANALYZER_MODE);
		Paoding paoding = PaodingMaker.make(properties);
		setKnife(paoding);
		setMode(mode);
	}

	/**
	 * 本方法为PaodingAnalyzer附带的测试评估方法。 <br>
	 * 执行之可以查看分词效果。以下任选一种方式进行:
	 * <p>
	 * 
	 * java net...PaodingAnalyzer<br>
	 * java net...PaodingAnalyzer --help<br>
	 * java net...PaodingAnalyzer 中华人民共和国<br>
	 * java net...PaodingAnalyzer -m max 中华人民共和国<br>
	 * java net...PaodingAnalyzer -f c:/text.txt<br>
	 * java net...PaodingAnalyzer -f c:/text.txt -c utf-8<br>
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (System.getProperty("paoding.try.app") == null) {
			System.setProperty("paoding.try.app", "PaodingAnalyzer");
			System.setProperty("paoding.try.cmd", "java PaodingAnalyzer");
		}
		TryPaodingAnalyzer.main(args);
	}

	// --------------------------------------------------

	/**
	 * @param knife
	 * @param default_mode
	 * @deprecated
	 */
	public PaodingAnalyzer(Knife knife, int mode) {
		super(knife, mode);
	}

	/**
	 * 等价于maxMode()
	 * 
	 * @param knife
	 * @return
	 * @deprecated
	 */
	public static PaodingAnalyzer queryMode(Knife knife) {
		return maxMode(knife);
	}

	/**
	 * 
	 * @param knife
	 * @return
	 * @deprecated
	 */
	public static PaodingAnalyzer defaultMode(Knife knife) {
		return new PaodingAnalyzer(knife, MOST_WORDS_MODE);
	}

	/**
	 * 
	 * @param knife
	 * @return
	 * @deprecated
	 */
	public static PaodingAnalyzer maxMode(Knife knife) {
		return new PaodingAnalyzer(knife, MAX_WORD_LENGTH_MODE);
	}

	/**
	 * 等价于defaultMode()
	 * 
	 * @param knife
	 * @return
	 * @deprecated
	 * 
	 */
	public static PaodingAnalyzer writerMode(Knife knife) {
		return defaultMode(knife);
	}
}
