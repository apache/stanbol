package net.paoding.analysis.analyzer;

import java.io.Reader;

import net.paoding.analysis.analyzer.impl.MaxWordLengthTokenCollector;
import net.paoding.analysis.analyzer.impl.MostWordsTokenCollector;
import net.paoding.analysis.knife.Knife;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

public class PaodingAnalyzerBean extends Analyzer {

	// -------------------------------------------------

	/**
	 * 最多切分
	 */
	public static final int MOST_WORDS_MODE = 1;

	/**
	 * 按最大切分
	 */
	public static final int MAX_WORD_LENGTH_MODE = 2;

	// -------------------------------------------------
	/**
	 * 用于向PaodingTokenizer提供，分解文本字符
	 * 
	 * @see PaodingTokenizer#next()
	 * 
	 */
	private Knife knife;

	/**
	 * @see #MOST_WORDS_MODE
	 * @see #MAX_WORD_LENGTH_MODE
	 */
	private int mode = MOST_WORDS_MODE;

	/**
	 * 
	 */
	private Class modeClass;

	// -------------------------------------------------

	public PaodingAnalyzerBean() {
	}

	/**
	 * @see #setKnife(Knife)
	 * @param knife
	 */
	public PaodingAnalyzerBean(Knife knife) {
		this.knife = knife;
	}

	/**
	 * @see #setKnife(Knife)
	 * @see #setMode(int)
	 * @param knife
	 * @param mode
	 */
	public PaodingAnalyzerBean(Knife knife, int mode) {
		this.knife = knife;
		this.mode = mode;
	}

	/**
	 * @see #setKnife(Knife)
	 * @see #setMode(int)
	 * @param knife
	 * @param mode
	 */
	public PaodingAnalyzerBean(Knife knife, String mode) {
		this.knife = knife;
		this.setMode(mode);
	}

	// -------------------------------------------------

	public Knife getKnife() {
		return knife;
	}

	public void setKnife(Knife knife) {
		this.knife = knife;
	}

	public int getMode() {
		return mode;
	}

	/**
	 * 设置分析器模式.
	 * <p>
	 * 
	 * @param mode
	 */
	public void setMode(int mode) {
		if (mode != MOST_WORDS_MODE && mode != MAX_WORD_LENGTH_MODE) {
			throw new IllegalArgumentException("wrong mode:" + mode);
		}
		this.mode = mode;
		this.modeClass = null;
	}

	/**
	 * 设置分析器模式类。
	 * 
	 * @param modeClass
	 *            TokenCollector的实现类。
	 */
	public void setModeClass(Class modeClass) {
		this.modeClass = modeClass;
	}

	public void setModeClass(String modeClass) {
		try {
			this.modeClass = Class.forName(modeClass);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("not found mode class:" + e.getMessage());
		}
	}

	public void setMode(String mode) {
		if (mode.startsWith("class:")) {
			setModeClass(mode.substring("class:".length()));
		} else {
			if ("most-words".equalsIgnoreCase(mode)
					|| "default".equalsIgnoreCase(mode)
					|| ("" + MOST_WORDS_MODE).equals(mode)) {
				setMode(MOST_WORDS_MODE);
			} else if ("max-word-length".equalsIgnoreCase(mode)
					|| ("" + MAX_WORD_LENGTH_MODE).equals(mode)) {
				setMode(MAX_WORD_LENGTH_MODE);
			}
			else {
				throw new IllegalArgumentException("不合法的分析器Mode参数设置:" + mode);
			}
		}
	}

	// -------------------------------------------------

	public TokenStream tokenStream(String fieldName, Reader reader) {
		if (knife == null) {
			throw new NullPointerException("knife should be set before token");
		}
		// PaodingTokenizer是TokenStream实现，使用knife解析reader流入的文本
		return new PaodingTokenizer(reader, knife, createTokenCollector());
	}

	protected TokenCollector createTokenCollector() {
		if (modeClass != null) {
			try {
				return (TokenCollector) modeClass.newInstance();
			} catch (InstantiationException e) {
				throw new IllegalArgumentException("wrong mode class:" + e.getMessage());
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException("wrong mode class:" + e.getMessage());
			}
		}
		switch (mode) {
		case MOST_WORDS_MODE:
			return new MostWordsTokenCollector();
		case MAX_WORD_LENGTH_MODE:
			return new MaxWordLengthTokenCollector();
		default:
			throw new Error("never happened");
		}
	}
}
