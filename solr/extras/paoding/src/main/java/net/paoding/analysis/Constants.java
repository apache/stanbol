package net.paoding.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 * @since 2.0.0
 */
public class Constants {

	/**
	 * "词典目录安装目录"配置的优先级别
	 * <p>
	 * "system-env"以及其他非"this"的配置，表示优先从环境变量PAODING_DIC_HOME的值找词典目录安装环境
	 * "this"表示优先从本配置文件的paoding.dic.home配置项找<br>
	 * 只有在高优先级没有配置，才会找低优先级的配置。 默认环境变量的优先级别高于paoding-analysis.properties属性文件配置。
	 */
	public static final String DIC_HOME_CONFIG_FIRST = "paoding.dic.home.config-first";
	public static final String DIC_HOME_CONFIG_FIRST_DEFAULT = "system-env";

	/**
	 * 词典安装目录环境变量名
	 */
	public static final String ENV_PAODING_DIC_HOME = "PAODING_DIC_HOME";

	// -------------------------------------------------------------
	/**
	 * 词典安装目录
	 * <p>
	 * 默认值为null，以在环境变量和配置文件都没有配置paoding.dic.home的情况下，让PaodingMaker尝试从当前工作目录下、类路径下探索是否存在dic目录
	 */
	public static final String DIC_HOME = "paoding.dic.home";
	public static final String DIC_HOME_DEFAULT = null;

	// -------------------------------------------------------------
	//
	public static final String DIC_CHARSET = "paoding.dic.charset";
	public static final String DIC_CHARSET_DEFAULT = "UTF-8";

	// dictionary word length limit
	public static final String DIC_MAXWORDLEN = "paoding.dic.maxWordLen";
	public static final String DIC_MAXWORDLEN_DEFAULT = "0";
	
	// -------------------------------------------------------------
	// dictionaries which are skip
	public static final String DIC_SKIP_PREFIX = "paoding.dic.skip.prefix";
	public static final String DIC_SKIP_PREFIX_DEFAULT = "x-";

	// -------------------------------------------------------------
	// chinese/cjk charactors that will not token
	public static final String DIC_NOISE_CHARACTOR = "paoding.dic.noise-charactor";
	public static final String DIC_NOISE_CHARACTOR_DEFAULT = "x-noise-charactor";

	// -------------------------------------------------------------
	// chinese/cjk words that will not token
	public static final String DIC_NOISE_WORD = "paoding.dic.noise-word";
	public static final String DIC_NOISE_WORD_DEFAULT = "x-noise-word";

	// -------------------------------------------------------------
	// unit words, like "ge", "zhi", ...
	public static final String DIC_UNIT = "paoding.dic.unit";
	public static final String DIC_UNIT_DEFAULT = "x-unit";

	// -------------------------------------------------------------
	// like "Wang", "Zhang", ...
	public static final String DIC_CONFUCIAN_FAMILY_NAME = "paoding.dic.confucian-family-name";
	public static final String DIC_CONFUCIAN_FAMILY_NAME_DEFAULT = "x-confucian-family-name";
	
	// -------------------------------------------------------------
	// like 
	public static final String DIC_FOR_COMBINATORICS = "paoding.dic.for-combinatorics";
	public static final String DIC_FOR_COMBINATORICS_DEFAULT = "x-for-combinatorics";

	// -------------------------------------------------------------
	// like 
	public static final String DIC_DETECTOR_INTERVAL = "paoding.dic.detector.interval";
	public static final String DIC_DETECTOR_INTERVAL_DEFAULT = "60";

	// -------------------------------------------------------------
	// like "default", "max", ...
	public static final String ANALYZER_MODE = "paoding.analyzer.mode";
	public static final String ANALYZER_MOE_DEFAULT = "most-words";

	// -------------------------------------------------------------
	// 
	public static final String ANALYZER_DICTIONARIES_COMPILER = "paoding.analyzer.dictionaries.compiler";
	public static final String ANALYZER_DICTIONARIES_COMPILER_DEFAULT = null;

	// -------------------------------------------------------------
	private static final Map/* <String, String> */map = new HashMap();

	static {
		map.put(DIC_HOME_CONFIG_FIRST, DIC_HOME_CONFIG_FIRST_DEFAULT);
		map.put(DIC_HOME, DIC_HOME_DEFAULT);
		map.put(DIC_CHARSET, DIC_CHARSET_DEFAULT);
		map.put(DIC_MAXWORDLEN, DIC_MAXWORDLEN_DEFAULT);
		map.put(DIC_SKIP_PREFIX, DIC_SKIP_PREFIX_DEFAULT);
		map.put(DIC_NOISE_CHARACTOR, DIC_NOISE_CHARACTOR_DEFAULT);
		map.put(DIC_NOISE_WORD, DIC_NOISE_WORD_DEFAULT);
		map.put(DIC_UNIT, DIC_UNIT_DEFAULT);
		map.put(DIC_CONFUCIAN_FAMILY_NAME, DIC_CONFUCIAN_FAMILY_NAME_DEFAULT);
		map.put(DIC_FOR_COMBINATORICS, DIC_FOR_COMBINATORICS_DEFAULT);
		map.put(DIC_DETECTOR_INTERVAL, DIC_DETECTOR_INTERVAL_DEFAULT);
		map.put(ANALYZER_MODE, ANALYZER_MOE_DEFAULT);
		map.put(ANALYZER_DICTIONARIES_COMPILER, ANALYZER_DICTIONARIES_COMPILER_DEFAULT);
	}

	//
	public static final String KNIFE_CLASS = "paoding.knife.class.";

	public static String getProperty(Properties p, String name) {
		return p.getProperty(name, (String) map.get(name));
	}
}
