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
 * Knife规定如何分解字符串成词语，并将分解成的词语告知{@link Collector}接口。
 * <p>
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 * @see Collector
 * @see Paoding
 * @see CJKKnife
 * @see CombinatoricsKnife
 * @see NumberKnife
 * @see LetterKnife
 * 
 * @since 1.0
 * 
 */
public interface Knife {

	/**
	 * 表征 {@link #assignable(Beef beef, int offset, int indec)}对index位置字符的性质规定。
	 * ASSIGNED性质的字符表示该字符可以被Knife接受进行分词。
	 * <p>
	 * {@link KnifeBox}据此将一段由这种性质字符开始的内容(由beef和offset封装)交给Knife分解。
	 * <p>
	 * 同样的一段内容的一个位置的字符，对不同的Knife来说，往往具有不同的性质结果。
	 * <P>
	 * 
	 * @see KnifeBox#dissect(Collector, Beef, int)
	 */
	int ASSIGNED = 1;

	/**
	 * 表征 {@link #assignable(Beef beef, int offset, int indec)}对index位置字符的性质规定。
	 * POINT性质的字符表示如果给定分解的内容之前存在ASSIGNED性质的字符时，该字符可以被Knife接受进行分词。
	 * <P>
	 * {@link KnifeBox}不关心给定的文本内容是否包含POINT性质的字符。<br>
	 * 这种性质的字符的最大关心者是{@link Knife}本身。
	 * 一般情况下，如果存在POINT性质的字符，下一个合适的Knife将从第一个这样性质的字符开始分解内容<br>
	 * (这仅是一般的情况，具体是由{@link #dissect(Collector, Beef, int)}来确定)。
	 * <p>
	 * 同样的一段内容的一个位置的字符，对不同的Knife来说，往往具有不同的性质结果。
	 * <P>
	 */
	int POINT = 0;

	/**
	 * 表征 {@link #assignable(Beef beef, int offset, int indec)}对index位置字符的性质规定。
	 * LIMIT性质的字符表示给定的字符不属于此Knife的分解范畴。本Knife分解应该到此为止。 <br>
	 * 一般情况下，如果不存在POINT性质的字符，下一个合适的Knife将从这样性质的字符开始分解内容<br>
	 * (这仅是一般的情况，具体是由{@link #dissect(Collector, Beef, int)}来确定)。
	 * <p>
	 * 同样的一段内容的一个位置的字符，对不同的Knife来说，往往具有不同的性质结果。
	 * <P>
	 */
	int LIMIT = -1;

	/**
	 * 返回beef的index位置字符的性质，{@link KnifeBox}据此决定将一段文本内容“交给”一个合适的Knife切词
	 * 
	 * @param beef
	 *            要被分词的字符串
	 * @param offset
	 *            Knife开始或有可能开始切词的始发位置。
	 * @param index
	 *            被判断的字符的位置，本方法返回的即时该位置字符的性质。index>=offset。<br>
	 *            当{@link KnifeBox}根据字符的性质(是否为{@link #ASSIGNED})选择Knife分解时，index=offset。
	 * @return index位置的字符在本Knife中的性质规定 <br>
	 *         当offset==index时，仅当返回ASSIGNED时，该Knife才有机会被{@link KnifeBox}分配接收文本内容进行分词<br>
	 *         (即才有机会调用dissect方法)
	 * @see #LIMIT
	 * @see #ASSIGNED
	 * @see #POINT
	 */
	public int assignable(Beef beef, int offset, int index);

	/**
	 * 分解词语，并将分解成的词语相关信息告知{@link Collector}接口。
	 * <p>
	 * 分解从beef的offset位置开始，直至可能的结束的位置，结束时返回具有特定意义的一个非0数字。<br>
	 * 
	 * @param collector
	 *            当分解到词语时，collector将被通知接收该词语
	 * @param beef
	 *            待分解的字符串内容，这个字符串可能是所要分解的全部字符串的一部分(比如文章中的某一部分)，当beef的最后一个字符为'\0'时，表示此次分解是文章最后一段。
	 * @param offset
	 *            此次分解从beef的offset位置开始，即本此分解只需从beef.charAt(offset)开始
	 * @return 非0的整数，即正整数或负整数。<br>
	 *         正数时：表示此次分解到该结束位置(不包括该边界)，即此次成功分解了从offset到该位置的文本流。<br>
	 *         特别地，当其>=beef.lenght()表示已经把beef所有的词语分解完毕<br>
	 *         如果，当其==offset时，表示{@link KnifeBox}应该继续遍历还未遍历的Knife，确定是否有其他Knife接收分解offset位置开始的文本内容<br>
	 *         <p>
	 *         负数时：该负数的绝对值必须>=offset。这个绝对值表示此次成功分解了从offset到该绝对值的文本流，剩下的字符，该knife已经不能正确解析。(一般此时应该重新传入新的beef对象解析)
	 *         <p>
	 *         比如，有内容为"hello yang!"的文章，先读入8个字符"hello ya"，<br>
	 *         此时分解后应该返回-5，表示正确解析到5这个位置，即"hello"，但必须读入新的字符然后再继续解析。
	 *         此时beef构造者就读入剩下的字符"ng!"并与前次剩下的" ya"<br>
	 *         构成串" yang!"，这样才能继续解析，从而解析出"yang"!
	 * 
	 * 
	 */
	public int dissect(Collector collector, Beef beef, int offset);
}
