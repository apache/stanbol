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

import java.util.HashSet;

import net.paoding.analysis.dictionary.Dictionary;
import net.paoding.analysis.dictionary.Hit;

/**
 * 排列组合Knife。
 * <p>
 * 
 * 该Knife把遇到的非LIMIT字符视为一个单词分出。<br>
 * 同时如果有以该词语开头的字符串在x-for-combinatorics.dic出现也会切出
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 * @since 1.0
 * 
 */
public abstract class CombinatoricsKnife implements Knife, DictionariesWare {

	protected Dictionary combinatoricsDictionary;

	protected HashSet/* <String> */noiseTable;

	public CombinatoricsKnife() {
	}

	public CombinatoricsKnife(String[] noiseWords) {
		setNoiseWords(noiseWords);
	}

	public void setNoiseWords(String[] noiseWords) {
		noiseTable = new HashSet/* <String> */((int) (noiseWords.length * 1.5));
		for (int i = 0; i < noiseWords.length; i++) {
			noiseTable.add(noiseWords[i]);
		}
	}

	public void setDictionaries(Dictionaries dictionaries) {
		combinatoricsDictionary = dictionaries.getCombinatoricsDictionary();
	}

	public int dissect(Collector collector, Beef beef, int offset) {
		// 当point == -1时表示本次分解没有遇到POINT性质的字符；
		// 如果point != -1，该值表示POINT性质字符的开始位置，
		// 这个位置将被返回，下一个Knife将从point位置开始分词
		int point = -1;

		// 记录同质字符分词结束极限位置(不包括limit位置的字符)-也就是assignable方法遇到LIMIT性质的字符的位置
		// 如果point==-1，limit将被返回，下一个Knife将从limit位置开始尝试分词
		int limit = offset + 1;

		// 构建point和limit变量的值:
		// 往前直到遇到LIMIT字符；
		// 其中如果遇到第一次POINT字符，则会将它记录为point
		GO_UNTIL_LIMIT: while (true) {
			switch (assignable(beef, offset, limit)) {
			case LIMIT:
				break GO_UNTIL_LIMIT;
			case POINT:
				if (point == -1) {
					point = limit;
				}
			}
			limit++;
		}
		// 如果最后一个字符也是ASSIGNED以及POINT，
		// 且beef之前已经被分解了一部分(从而能够腾出空间以读入新的字符)，则需要重新读入字符后再分词
		if (limit == beef.length() && offset > 0) {
			return -offset;
		}

		// 检索是否有以该词语位前缀的词典词语
		// 若有，则将它解出
		int dicWordVote = -1;
		if (combinatoricsDictionary != null && beef.charAt(limit) > 0xFF) {
			dicWordVote = tryDicWord(collector, beef, offset, limit);
		}

		// 收集从offset分别到point以及limit的词
		// 注意这里不收集从point到limit的词
		// ->当然可能从point到limit的字符也可能是一个词，不过这不是本次分解的责任
		// ->如果认为它应该是个词，那么只要配置对应的其它Knife实例，该Knife会有机会把它切出来的
		// ->因为我们会返回point作为下一个Knife分词的开始。

		int pointVote = collectPoint(collector, beef, offset, point, limit,
				dicWordVote);
		int limitVote = collectLimit(collector, beef, offset, point, limit,
				dicWordVote);

		return nextOffset(beef, offset, point, limit, pointVote, limitVote,
				dicWordVote);
	}

	/**
	 * 通知收集从offset到第一个LIMIT字符的词，并投票下一个Knife开始的分词位置。如果不存在POINT字符，则Point的值为-1。
	 * <p>
	 * 
	 * 默认方法实现：如果不存在POINT性质的字符，则直接返回不做任何切词处理。
	 * 
	 * @param collector
	 * @param beef
	 * @param offset
	 *            本次分解的内容在beef中的开始位置
	 * @param point
	 *            本次分解的内容的第一个POINT性质字符的位置，-1表示不存在该性质的字符
	 * @param limit
	 *            本次分解的内容的LIMIT性质字符
	 * @return 投票下一个Knife开始分词的位置；-1表示弃权。默认方法实现：弃权。
	 */
	protected int collectPoint(Collector collector, Beef beef, int offset,
			int point, int limit, int dicWordVote) {
		if (point != -1 && dicWordVote == -1) {
			collectIfNotNoise(collector, beef, offset, point);
		}
		return -1;
	}

	/**
	 * 通知收集从offset到第一个LIMIT字符的词，并投票下一个Knife开始的分词位置。
	 * <p>
	 * 
	 * 默认方法实现：把从offset位置到limit位置止(不包含边界)的字符串视为一个词切出。
	 * 
	 * @param collector
	 * @param beef
	 * @param offset
	 *            本次分解的内容在beef中的开始位置
	 * @param point
	 *            本次分解的内容的第一个POINT性质字符的位置，-1表示不存在该性质的字符
	 * @param limit
	 *            本次分解的内容的LIMIT性质字符
	 * 
	 * @param dicWordVote 
	 * 
	 * @return 投票下一个Knife开始分词的位置；-1表示弃权。默认方法实现：弃权。
	 */
	protected int collectLimit(Collector collector, Beef beef, int offset,
			int point, int limit, int dicWordVote) {
		if (dicWordVote == -1) {
			collectIfNotNoise(collector, beef, offset, limit);
		}
		return -1;
	}

	/**
	 * 尝试从combinatorics字典中检索，如果存在以offset到limit位置止(不包含limit边界)字符串开始的词语，则切出该词语。
	 * <p>
	 * 如没有检索到这样的词语，则本方法返回-1弃权投票下一个Knife的开始分解位置。<br>
	 * 如果检索到这样的词语，在切出在词语的同时，投票返回这个词语的结束位置(词语本身不包含该结束位置的字符)
	 * <p>
	 * 
	 * (for version 2.0.4+):<br>
	 * 本方法目前存在的局限：<br>
	 * 如果字典中的某个词语刚好分隔在两次beef之中，比如"U"刚好是此次beef的最后字符，而"盘"是下一次beef的第一个字符，<br>
	 * 这种情况现在 {@link CombinatoricsKnife}还没机制办法识别将之处理为一个词语
	 * 
	 * @param collector
	 * @param beef
	 * @param offset
	 * @param limit
	 * @return
	 */
	protected int tryDicWord(Collector collector, Beef beef, int offset,
			int limit) {
		int ret = limit;
		for (int end = limit + 1, count = limit - offset + 1; end <= beef
				.length(); end++, count++) {
			Hit hit = combinatoricsDictionary.search(beef, offset, count);
			if (hit.isUndefined()) {
				break;
			} else if (hit.isHit()) {
				collectIfNotNoise(collector, beef, offset, end);
				// 收到词语，将ret设置为该词语的end
				ret = end;
			}
			// gotoNextChar为true表示在词典中存在以当前词为开头的词，
			boolean gotoNextChar = hit.isUnclosed() && end < beef.length()
					&& beef.charAt(end) >= hit.getNext().charAt(count);
			if (!gotoNextChar) {
				break;
			}
		}
		return ret <= limit ? -1 : ret;
		// TODO:
		// 存在的局限:
		// 刚好词语分隔在两次beef之中，比如"U"刚好是此次beef的最后字符，而"盘"是下一次beef的第一个字符
		// 这种情况现在CombinatoricsKnife还没机制办法识别将之处理为一个词语
	}

	/**
	 * 当Knife决定切出从offset始到end位置止(不包含结束位置的字符)的词语时，本方法能够过滤掉可能是noise的词，使最终不切出。
	 * 
	 * @param collector
	 * @param beef
	 * @param offset
	 * @param end
	 */
	protected void collectIfNotNoise(Collector collector, Beef beef,
			int offset, int end) {
		// 将offset和end之间的词(不包含end位置)创建出来给word
		// 如果该词语为噪音词，则重新丢弃之(设置为null)，
		String word = beef.subSequence(offset, end).toString();
		if (noiseTable != null && noiseTable.contains(word)) {
			word = null;
		}

		// 否则发送消息给collect方法，表示Knife新鲜出炉了一个内容为word的候选词语
		// 即：最终决定是否要把这个词语通知给collector的是collect方法
		if (word != null) {
			doCollect(collector, word, beef, offset, end);
		}
	}

	/**
	 * 
	 * 当Knife决定切出从offset始到end位置止(不包含结束位置的字符)的词语时，本方法直接调用{@link #doCollect(Collector, String, Beef, int, int)}切出词语(而不过滤noise词汇)
	 * 
	 * @param collector
	 * @param beef
	 * @param offset
	 * @param end
	 */
	protected void collect(Collector collector, Beef beef, int offset, int end) {
		String word = beef.subSequence(offset, end).toString();
		doCollect(collector, word, beef, offset, end);
	}

	/**
	 * 收集分解出的候选词语。 默认实现是将该候选词语通知给收集器collector。<br>
	 * 子类覆盖本方法可以更灵活地控制词语的收录，例如控制仅当word满足一些额外条件再决定是否收集，<br>
	 * 或依上下文环境收集更多的相关词语
	 * 
	 * @param collector
	 * @param word
	 * @param beef
	 * @param offset
	 * @param end
	 */
	protected void doCollect(Collector collector, String word, Beef beef,
			int offset, int end) {
		collector.collect(word, offset, end);
	}

	/**
	 * 根据字符串性质位置，以及分词结果投票，决出下一个Knife应该从哪一个位置开始探测切词
	 * 
	 * @param beef
	 * @param offset
	 *            本次分词的开始位置
	 * @param point
	 *            本次分词的第一个POINT性质的字符位置，-1表示没有该性质的字符
	 * @param limit
	 *            本次分词的第一个LIMIT性质的字符位置
	 * @param pointVote
	 *            收集从offset到第一个POINT性质字符词汇时的投票，-1表示弃权
	 * @param limitVote
	 *            收集从offset到第一个LIMIT性质字符词汇时的投票，-1表示弃权
	 * @param dicWordVote
	 *            收集combinatorics词典词语时的投票，-1表示弃权
	 * @return
	 */
	protected int nextOffset(Beef beef, int offset, int point, int limit,
			int pointVote, int limitVote, int dicWordVote) {
		int max = pointVote > limitVote ? pointVote : limitVote;
		max = max > dicWordVote ? max : dicWordVote;
		if (max == -1) {
			return point != -1 ? point : limit;
		} else if (max > limit) {
			return max;
		} else {
			return limit;
		}
	}
}
