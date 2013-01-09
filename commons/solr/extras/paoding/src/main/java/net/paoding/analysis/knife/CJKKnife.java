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

import net.paoding.analysis.dictionary.Dictionary;
import net.paoding.analysis.dictionary.Hit;
import net.paoding.analysis.dictionary.Word;

/**
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 * @since 1.0
 * 
 */
public class CJKKnife implements Knife, DictionariesWare {

	// -------------------------------------------------

	private Dictionary vocabulary;
	private Dictionary noiseWords;
	private Dictionary noiseCharactors;
	private Dictionary units;

	// -------------------------------------------------

	public CJKKnife() {
	}

	public CJKKnife(Dictionaries dictionaries) {
		setDictionaries(dictionaries);
	}

	public void setDictionaries(Dictionaries dictionaries) {
		vocabulary = dictionaries.getVocabularyDictionary();
		noiseWords = dictionaries.getNoiseWordsDictionary();
		noiseCharactors = dictionaries.getNoiseCharactorsDictionary();
		units = dictionaries.getUnitsDictionary();
	}

	// -------------------------------------------------

	/**
	 * 分解以CJK字符开始的，后可带阿拉伯数字、英文字母、横线、下划线的字符组成的语句
	 */
	public int assignable(Beef beef, int offset, int index) {
		char ch = beef.charAt(index);
		if (CharSet.isCjkUnifiedIdeographs(ch))
			return ASSIGNED;
		if (index > offset) {
			if (CharSet.isArabianNumber(ch) || CharSet.isLantingLetter(ch)
					|| ch == '-' || ch == '_') {
				return POINT;
			}
		}
		return LIMIT;
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

		// 如果从offset到beef.length()都是本次Knife的责任，则应读入更多的未读入字符，以支持一个词分在两次beef中的处理
		// 魔幻逻辑：
		// Beef承诺:如果以上GO_UNTIL_LIMIT循环最终把limit值设置为beef.length则表示还为未读入字符。
		// 因为beef一定会在文本全部结束后加入一个char='\0'的值作为最后一个char标志结束。
		// 这样以上的GO_UNTIL_LIMIT将在limit=beef.length()之前就已经break，此时limit!=beef.length
		if (offset > 0 && limit == beef.length()) {
			return -offset;
		}

		// 记录当前正在检视(是否是词典词语)的字符串在beef中的始止位置(包含开始位置，不包含结束位置)
		int curSearchOffset = offset, curSearchEnd;

		// 记录当前被检视的字符串的长度，它的值恒等于(curSearchEnd - curSearchOffset)
		int curSearchLength;

		// 当前检视的字符串的判断结果
		Hit curSearch = null;

		// 限制要判断的字符串的最大开始位置
		// 这个变量不随着程序的运行而变化
		final int offsetLimit;
		if (point != -1)
			offsetLimit = point;
		else
			offsetLimit = limit;

		// 记录到当前为止所分出的词典词语的最大结束位置
		int maxDicWordEnd = offset;

		// 记录最近的不在词典中的字符串(称为孤立字符串)在beef的位置，-1表示没有这个位置
		int isolatedOffset = -1;

		// 记录到当前为止经由词典所切出词的最大长度。
		// 用于辅助判断是否调用shouldBeWord()方法，以把前后有如引号、书名号之类的，但还没有被切出的字符串当成一个词
		// 详见本方法后面对maxDicWordLength的应用以及shouldBeWord()的实现
		int maxDicWordLength = 0;

		// 第1个循环定位被检视字符串的开始位置
		// 被检视的字符串开始位置的极限是offsetLimit，而非limit
		for (; curSearchOffset < offsetLimit; curSearchOffset++) {

			// 第二个循环定位被检视字符串的结束位置(不包含该位置的字符)
			// 它的起始状态是：被检视的字符串一长度为1，即结束位置为开始位置+1
			curSearchEnd = curSearchOffset + 1;
			curSearchLength = 1;
			for (; curSearchEnd <= limit; curSearchEnd++, curSearchLength++) {

				/*
				 * Fix issue 50: 中文数字解析问题 
				 */				
				//先搜索连续的中文数字
				curSearch = searchNumber(beef, curSearchOffset, curSearchLength);
				if (curSearch.isHit()) {
					if (isolatedOffset >= 0) {
						dissectIsolated(collector, beef, isolatedOffset,
								curSearchOffset);
						isolatedOffset = -1;
					}
					
					// trick: 用index返回中文数字实际结束位置
					int numberSearchEnd = curSearch.getIndex();
					int numberSearchLength = curSearch.getIndex() - curSearchOffset;

					// 1.2)
					// 更新最大结束位置
					if (maxDicWordEnd < numberSearchEnd) {
						maxDicWordEnd = numberSearchEnd;
					}

					// 1.3)
					// 更新词语最大长度变量的值
					if (curSearchOffset == offset
							&& maxDicWordLength < numberSearchLength) {
						maxDicWordLength = numberSearchLength;
					}

					Word word = curSearch.getWord();
					if (!word.isNoise()) {
						dissectIsolated(collector, beef, curSearchOffset,
								curSearch.getIndex());
					}
					curSearchOffset = numberSearchEnd - 1;
					break;
				}
				if (curSearch.isUnclosed()) {
					continue;
				}

				// 通过词汇表判断，返回判断结果curSearch
				curSearch = vocabulary.search(beef, curSearchOffset,
						curSearchLength);

				// ---------------分析返回的判断结果--------------------------

				// 1)
				// 从词汇表中找到了该词语...
				if (curSearch.isHit()) {

					// 1.1)
					// 确认孤立字符串的结束位置=curSearchOffset，
					// 并调用子方法分解把从isolatedOffset开始的到curSearchOffset之间的孤立字符串
					// 孤立字符串分解完毕，将孤立字符串开始位置isolatedOffset清空
					if (isolatedOffset >= 0) {
						dissectIsolated(collector, beef, isolatedOffset,
								curSearchOffset);
						isolatedOffset = -1;
					}

					// 1.2)
					// 更新最大结束位置
					if (maxDicWordEnd < curSearchEnd) {
						maxDicWordEnd = curSearchEnd;
					}

					// 1.3)
					// 更新词语最大长度变量的值
					if (curSearchOffset == offset
							&& maxDicWordLength < curSearchLength) {
						maxDicWordLength = curSearchLength;
					}

					// 1.2)
					// 通知collector本次找到的词语
					Word word = curSearch.getWord();
					if (!word.isNoise()) {
						collector.collect(word.getText(), curSearchOffset,
								curSearchEnd);
					}
				}

				// 若isolatedFound==true，表示词典没有该词语
				boolean isolatedFound = curSearch.isUndefined();

				// 若isolatedFound==false，则通过Hit的next属性检视词典没有beef的从offset到curWordEnd
				// + 1位置的词
				// 这个判断完全是为了减少一次词典检索而设计的，
				// 如果去掉这个if判断，并不影响程序的正确性(但是会多一次词典检索)
				if (!isolatedFound && !curSearch.isHit()
						&& curSearch.getNext() != null) {
					isolatedFound = curSearchEnd >= limit
							|| beef.charAt(curSearchEnd) < curSearch.getNext()
									.charAt(curSearchLength);
				}
				// 2)
				// 词汇表中没有该词语，且没有以该词语开头的词汇...
				// -->将它记录为孤立词语
				if (isolatedFound) {
					if (isolatedOffset < 0 && curSearchOffset >= maxDicWordEnd) {
						isolatedOffset = curSearchOffset;
					}
					break;
				}

				// ^^^^^^^^^^^^^^^^^^分析返回的判断结果^^^^^^^^^^^^^^^^^^^^^^^^
			} // end of the second for loop
		} // end of the first for loop

		// 上面循环分词结束后，可能存在最后的几个未能从词典检索成词的孤立字符串，
		// 此时isolatedOffset不一定等于一个有效值(因为这些孤立字虽然不是词语，但是词典可能存在以它为开始的词语，
		// 只要执行到此才能知道这些虽然是前缀的字符串已经没有机会成为词语了)
		// 所以不能通过isolatedOffset来判断是否此时存在有孤立词，判断依据转换为：
		// 最后一个词典的词的结束位置是否小于offsetLimit(!!offsetLimit, not Limit!!)
		if (maxDicWordEnd < offsetLimit) {
			dissectIsolated(collector, beef, maxDicWordEnd, offsetLimit);
		}

		// 现在是利用maxDicWordLength的时候了
		// 如果本次负责的所有字符串文本没有作为一个词被切分出(包括词典切词和孤立串切分)，
		// 那如果它被shouldBeWord方法认定为应该作为一个词切分，则将它切出来
		int len = limit - offset;
		if (len > 2 && len != maxDicWordLength
				&& shouldBeWord(beef, offset, limit)) {
			collector.collect(beef.subSequence(offset, limit).toString(),
					offset, limit);
		}

		// 按照point和limit的语义，返回下一个Knife开始切词的开始位置
		return point == -1 ? limit : point;
	}

	// -------------------------------------------------

	protected Hit searchNumber(CharSequence input, int offset, int count) {
		int endPos = -1;
		StringBuilder nums = new StringBuilder();
		for (int i = 0; i < count; i++) {
			char c = input.charAt(offset + i);
			if (CharSet.toNumber(c) < 0) {
				break;
			}
			nums.append(c);
			endPos = i;
		}
		//没有中文数字了
		if (endPos == -1) {
			return Hit.UNDEFINED;
		}
		//中文数字还没结束，后面可能还有
		if (endPos == count - 1) {
			return new Hit(Hit.UNCLOSED_INDEX, null, null);
		}
		//只有一个中文数字，不是连续的，不处理
		if (endPos == 0) {
			return Hit.UNDEFINED;
		}
		
		//部分含有中文数字，取这一部分出来
		//trick: 我们这里用index参数传递该部分中文的结束位置
		return new Hit(offset + endPos + 1, new Word(nums.toString()), null);
	}

	/**
	 * 对孤立字符串分词
	 * 
	 * @param cellector
	 * @param beef
	 * @param offset
	 * @param count
	 */
	protected void dissectIsolated(Collector collector, Beef beef, int offset,
			int limit) {
		int curSearchOffset = offset;
		int binOffset = curSearchOffset; // 进行一般二元分词的开始位置
		int tempEnd;

		while (curSearchOffset < limit) {
			// 孤立字符串如果是汉字数字，比如"五十二万"，"十三亿"，。。。
			tempEnd = collectNumber(collector, beef, curSearchOffset, limit,
					binOffset);
			if (tempEnd > curSearchOffset) {
				curSearchOffset = tempEnd;
				binOffset = tempEnd;
				continue;
			}

			// 魔幻逻辑：
			// noiseWords的词在语言学上虽然也是词，但CJKKnife不会把它当成词汇表中的正常词，
			// 有些noise词可能没有出现词汇表，则就会被视为孤立字符串在此处理(不被视为词汇、不进行二元分词)
			tempEnd = skipNoiseWords(collector, beef, curSearchOffset, limit,
					binOffset);
			if (tempEnd > curSearchOffset) {
				curSearchOffset = tempEnd;
				binOffset = tempEnd;
				continue;
			}

			// 如果当前字符是noise单字，其不参加二元分词
			Hit curSearch = noiseCharactors.search(beef, curSearchOffset, 1);
			if (curSearch.isHit()) {
				binDissect(collector, beef, binOffset, curSearchOffset);
				binOffset = ++curSearchOffset;
				continue;
			}
			curSearchOffset++;
		}

		// 
		if (limit > binOffset) {
			binDissect(collector, beef, binOffset, limit);
		}
	}

	protected int collectNumber(Collector collector, Beef beef, int offset,
			int limit, int binOffset) {

		/*
		 * Fix : "百二十回" => 1020
		 */
		
		// 当前尝试判断的字符的位置
		int curTail = offset;
		int number1 = -1;
		int number2 = -1;
		int bitValue = 0;
		int minUnit = 0;
		int number2Start = curTail;
		boolean hasDigit = false;// 作用：去除没有数字只有单位的汉字，如“万”，“千”
		for (; curTail < limit
				&& (bitValue = CharSet.toNumber(beef.charAt(curTail))) >= 0; curTail++) {
			// 
			if (bitValue == 2
					&& (beef.charAt(curTail) == '两'
							|| beef.charAt(curTail) == '俩' || beef
							.charAt(curTail) == '倆')) {
				if (curTail != offset) {
					break;
				}
			}
			// 处理连续汉字个位值的数字："三四五六" ->"3456"
			if (bitValue >= 0 && bitValue < 10) {
				hasDigit = true;
				if (number2 < 0){
					number2 = bitValue;
					number2Start = curTail;
				}
				else {
					number2 *= 10;
					number2 += bitValue;
				}
			} else {
				if (number2 < 0) {
					if (number1 < 0) {
						number1 = 1;
					} else {
						//"一百十" => "一百" "十"
						break;
					}
					if (bitValue >= minUnit) {
						if (minUnit == 0){
							number1 *= bitValue;
							minUnit = bitValue;
						} else {
							//"一千二百三十百" => "一千二百三十" "百"
							break;
						}
					} else {
						minUnit = bitValue;
					}
				} else {
					if (number1 < 0) {
						number1 = 0;
					}
					if (bitValue >= minUnit) {
						if (minUnit == 0){
							number1 += number2;
							number1 *= bitValue;
							minUnit = bitValue;
						} else {
							//"一百二千" => "一百" "二千"
							curTail = number2Start;
							number2 = -1;
							break;
						}
					} else {
						minUnit = bitValue;
						number1 += number2 * bitValue;
					}
				}
				number2 = -1;
				number2Start = -1;
			}
		}
		if (!hasDigit) {
			return offset;
		}
		if (number2 > 0) {
			if (number1 < 0) {
				number1 = number2;
			} else {
				number1 += number2;
			}
		}
		if (number1 >= 0) {
			// 二元分词先
			if (offset > binOffset) {
				binDissect(collector, beef, binOffset, offset);
			}
			collector.collect(String.valueOf(number1), offset, curTail);
			
			if (units != null) {
				// 后面可能跟了计量单位
				Hit wd = null;
				Hit wd2 = null;
				int i = curTail + 1;
				
				/*
				 * Fix issue 48: 查找计量单位引起的高亮越界错误
				 */
				while (i <= limit && (wd = units.search(beef, curTail, i - curTail)).isHit()) {
					wd2 = wd;
					i ++;
					if (!wd.isUnclosed()) {
						break;
					}
				}
				i --;
				if (wd2 != null) {
					collector.collect(wd2.getWord().getText(), curTail, i);
					return i;
				}
			}
		}

		// 返回最后一个判断失败字符的结束位置：
		// 该位置要么是offset，要么表示curTail之前的字符(不包括curTail字符)已经被认为是汉字数字
		return curTail;
	}

	protected int skipNoiseWords(Collector collector, Beef beef, int offset,
			int end, int binOffset) {
		Hit word;
		for (int k = offset + 2; k <= end; k++) {
			word = noiseWords.search(beef, offset, k - offset);
			if (word.isHit()) {
				// 二元分词
				if (binOffset > 0 && offset > binOffset) {
					binDissect(collector, beef, binOffset, offset);
					binOffset = -1;
				}
				offset = k;
			}
			if (word.isUndefined() || !word.isUnclosed()) {
				break;
			}
		}
		return offset;
	}

	protected void binDissect(Collector collector, Beef beef, int offset,
			int limit) {
		// 二元分词之策略：以W、X、Y、Z表示孤立字符串中的4个汉字
		// X ->X 单个字的孤立字符串作为一个词
		// XY ->XY 只有两个字的孤立字符串作为一个词
		// XYZ ->XY/YZ 多个字(>=3)的孤立字符串"两两组合"作为一个词
		// WXYZ ->WX/XY/YZ 同上

		if (limit - offset == 1) {
			collector.collect(beef.subSequence(offset, limit).toString(),
					offset, limit);
		} else {
			// 穷尽二元分词
			for (int curOffset = offset; curOffset < limit - 1; curOffset++) {
				collector.collect(beef.subSequence(curOffset, curOffset + 2)
						.toString(), curOffset, curOffset + 2);
			}
		}
	}

	protected boolean shouldBeWord(Beef beef, int offset, int end) {
		char prevChar = beef.charAt(offset - 1);
		char endChar = beef.charAt(end);
		// 中文单双引号
		if (prevChar == '“' && endChar == '”') {
			return true;
		} else if (prevChar == '‘' && endChar == '’') {
			return true;
		}
		// 英文单双引号
		else if (prevChar == '\'' && endChar == '\'') {
			return true;
		} else if (prevChar == '\"' && endChar == '\"') {
			return true;
		}
		// 中文书名号
		else if (prevChar == '《' && endChar == '》') {
			return true;
		} else if (prevChar == '〈' && endChar == '〉') {
			return true;
		}
		// 英文尖括号
		else if (prevChar == '<' && endChar == '>') {
			return true;
		}
		return false;
	}

}
