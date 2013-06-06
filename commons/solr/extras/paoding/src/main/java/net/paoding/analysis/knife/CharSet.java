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
 * @since 1.0
 * 
 */
public class CharSet {
	
	public static boolean isArabianNumber(char ch) {
		return ch >= '0' && ch <= '9';
	}

	public static boolean isLantingLetter(char ch) {
		return ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z';
	}

	public static boolean isCjkUnifiedIdeographs(char ch) {
		return ch >= 0x4E00 && ch < 0xA000;
	}
	
	public static boolean isBom(char ch) {
		// ref:http://www.w3.org/International/questions/qa-utf8-bom
		return ch == 0xFEFF || ch == 0xFFFE;
	}
	
	public static int toNumber(char ch) {
		switch (ch) {
		case '0':
		case '零':
		case '〇':
			return 0;
		case '1':
		case '一':
		case '壹':
			return 1;
		case '2':
		case '二':
		case '两':
		case '俩':
		case '貳':
			return 2;
		case '3':
		case '三':
		case '叁':
			return 3;
		case '4':
		case '四':
		case '肆':
			return 4;
		case '5':
		case '五':
		case '伍':
			return 5;
		case '6':
		case '六':
		case '陆':
			return 6;
		case '7':
		case '柒':
		case '七':
			return 7;
		case '8':
		case '捌':
		case '八':
			return 8;
		case '9':
		case '九':
		case '玖':
			return 9;
		case '十':
		case '什':
			return 10;
		case '百':
		case '佰':
			return 100;
		case '千':
		case '仟':
			return 1000;
		/*
		 * Fix issue 12: 溢出bug
		 */
		/*
		case '万':
		case '萬':
			return 10000;
		case '亿':
		case '億':
			return 100000000;
		*/
		default:
			return -1;
		}
	}

}
