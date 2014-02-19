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

import java.math.BigInteger;

import net.paoding.analysis.dictionary.Dictionary;
import net.paoding.analysis.dictionary.Hit;

/**
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 */
public class NumberKnife extends CombinatoricsKnife implements DictionariesWare {

	private Dictionary units;
	
	public NumberKnife() {
	}

	public NumberKnife(Dictionaries dictionaries) {
		setDictionaries(dictionaries);
	}

	public void setDictionaries(Dictionaries dictionaries) {
		super.setDictionaries(dictionaries);
		units = dictionaries.getUnitsDictionary();
	}
	

	public int assignable(Beef beef, int offset, int index) {
		char ch = beef.charAt(index);
		if (CharSet.isArabianNumber(ch))
			return ASSIGNED;
		if (index > offset) {
			if (CharSet.isLantingLetter(ch) || ch == '.' || ch == '-' || ch == '_') {
				if (ch == '-' || ch == '_' || CharSet.isLantingLetter(ch)
						|| !CharSet.isArabianNumber(beef.charAt(index + 1))) {
					//分词效果
					//123.456		->123.456/
					//123.abc.34	->123/123.abc.34/abc/34/	["abc"、"abc/34"系由LetterKnife分出，非NumberKnife]
					//没有或判断!CharSet.isArabianNumber(beef.charAt(index + 1))，则分出"123."，而非"123"
					//123.abc.34	->123./123.abc.34/abc/34/
					return POINT;
				}
				return ASSIGNED;
			}
		}
		return LIMIT;
	}
	
	protected int collectLimit(Collector collector, Beef beef,
			int offset, int point, int limit, int dicWordVote) {
		// "123abc"的直接调用super的
		if (point != -1) {
			return super.collectLimit(collector, beef, offset, point, limit, dicWordVote);
		}
		// 
		// 2.2两
		//    ^=_point
		//     
		final int _point = limit;
		// 当前尝试判断的字符的位置
		int curTail = offset;
		/*
		 * Fix issue 56: 中文数字解析问题后续
		 */				
		BigInteger number1 = BigInteger.valueOf(-1);
		BigInteger number2 = BigInteger.valueOf(-1);
		int bitValue = 0;
		int maxUnit = 0;
		//TODO:这里又重复从curTail(其值为offset)判断，重新遍历判断是否为数字，算是一个重复计算
		//但考虑这个计算对中文分词性能影响微乎其微暂时先不优化
		for (; (bitValue = CharSet.toNumber(beef.charAt(curTail))) >= 0; curTail++) {
			// 
			if (bitValue == 2
					&& (beef.charAt(curTail) == '两' || beef.charAt(curTail) == '俩' || beef
							.charAt(curTail) == '倆')) {
				if (curTail != offset) {
					break;
				}
			}
			// 处理连续汉字个位值的数字："三四五六"	->"3456"
			if (bitValue >= 0 && bitValue < 10) {
				if (number2.compareTo(BigInteger.ZERO) < 0)
					number2 = BigInteger.valueOf(bitValue);
				else {
					number2 = number2.multiply(BigInteger.valueOf(10));
					number2 = number2.add(BigInteger.valueOf(bitValue));
				}
			} else {
				if (number2.compareTo(BigInteger.ZERO) < 0) {
					if (number1.compareTo(BigInteger.ZERO) < 0) {
						number1 = BigInteger.ONE;
					}
					number1 = number1.multiply(BigInteger.valueOf(bitValue));
				} else {
					if (number1.compareTo(BigInteger.ZERO) < 0) {
						number1 = BigInteger.ZERO;
					}
					if (bitValue >= maxUnit) {
						number1 = number1.add(number2);
						number1 = number1.multiply(BigInteger.valueOf(bitValue));
						maxUnit = bitValue;
					} else {
						number1 = number1.add(number2.multiply(BigInteger.valueOf(bitValue)));
					}
				}
				number2 = BigInteger.valueOf(-1);
			}
		}
		if (number2.compareTo(BigInteger.ZERO) > 0) {
			if (number1.compareTo(BigInteger.ZERO) < 0) {
				number1 = number2;
			} else {
				number1 = number1.add(number2);
			}
		}
		if (number1.compareTo(BigInteger.ZERO) >= 0 && curTail > _point) {
			doCollect(collector, String.valueOf(number1), beef, offset, curTail);
		}
		else {
			super.collectLimit(collector, beef, offset, point, limit, dicWordVote);
		}
		
		curTail = curTail > limit ? curTail : limit;
		
		//
		// 后面可能跟了计量单位
		if (units != null && CharSet.isCjkUnifiedIdeographs(beef.charAt(curTail))) {
			Hit wd = null;
			Hit wd2 = null;
			int i = curTail + 1;
			
			/*
			 * Fix issue 48: 查找计量单位引起的高亮越界错误
			 */
			while (i <= limit && (wd = units.search(beef, curTail, i - curTail)).isHit()) {
				wd2 = wd;
				i++;
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
		//
		
		return curTail > limit ? curTail : -1;
	}


}
