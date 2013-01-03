package net.paoding.analysis.analyzer.estimate;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedList;

import net.paoding.analysis.analyzer.PaodingTokenizer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

public class Estimate {
	private Analyzer analyzer;
	private String print;
	private PrintGate printGate;

	public Estimate() {
		this.setPrint("50");// 默认只打印前50行分词效果
	}

	public Estimate(Analyzer analyzer) {
		setAnalyzer(analyzer);
		this.setPrint("50");// 默认只打印前50行分词效果
	}

	public void setAnalyzer(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	public void setPrint(String print) {
		if (print == null || print.length() == 0
				|| print.equalsIgnoreCase("null")
				|| print.equalsIgnoreCase("no")) {
			printGate = null;
			this.print = null;
		} else {
			printGate = new LinePrintGate();
			printGate.setPrint(print, 10);
			this.print = print;
		}
	}

	public String getPrint() {
		return print;
	}

	public void test(String input) {
		this.test(System.out, input);
	}

	public void test(PrintStream out, String input) {
		Reader reader = new StringReaderEx(input);
		this.test(out, reader);
	}

	public void test(PrintStream out, Reader reader) {
		try {
			long begin = System.currentTimeMillis();
			
			LinkedList<CToken> list = new LinkedList<CToken>();
			int wordsCount = 0;
			
			//collect token
			TokenStream ts = analyzer.tokenStream("", reader);
			ts.reset();
			TermAttribute termAtt = (TermAttribute) ts
					.addAttribute(TermAttribute.class);
			while (ts.incrementToken()) {
				if (printGate != null && printGate.filter(wordsCount)) {
					list.add(new CToken(termAtt.term(), wordsCount));
				}
				wordsCount++;
			}
			
			long end = System.currentTimeMillis();
			int c = 0;
			if (list.size() > 0) {
				for (CToken ctoken : list) {
					c = ctoken.i;
					if (c % 10 == 0) {
						if (c != 0) {
							out.println();
						}
						out.print((c / 10 + 1) + ":\t");
					}
					out.print(ctoken.t + "/");
				}
			}
			if (wordsCount == 0) {
				System.out.println("\tAll are noise characters or words");
			} else {
				if (c % 10 != 1) {
					System.out.println();
				}
				String inputLength = "<未知>";
				if (reader instanceof StringReaderEx) {
					inputLength = "" + ((StringReaderEx) reader).inputLength;
				} else if (ts instanceof PaodingTokenizer) {
					inputLength = "" + ((PaodingTokenizer) ts).getInputLength();
				}
				System.out.println();
				System.out.println("\t分词器" + analyzer.getClass().getName());
				System.out.println("\t内容长度 " + inputLength + "字符， 分 "
						+ wordsCount + "个词");
				System.out.println("\t分词耗时 " + (end - begin) + "ms ");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
			}
		}
	}

	// -------------------------------------------

	static class CToken {
		String t;
		int i;

		CToken(String t, int i) {
			this.t = t;
			this.i = i;
		}
	}

	static interface PrintGate {
		public void setPrint(String print, int unitSize);

		boolean filter(int count);
	}

	static class PrintGateToken implements PrintGate {
		private int begin;
		private int end;

		public void setBegin(int begin) {
			this.begin = begin;
		}

		public void setEnd(int end) {
			this.end = end;
		}

		public void setPrint(String print, int unitSize) {
			int i = print.indexOf('-');
			if (i > 0) {
				int bv = Integer.parseInt(print.substring(0, i));
				int ev = Integer.parseInt(print.substring(i + 1));
				setBegin(unitSize * (Math.abs(bv) - 1));// 第5行，是从第40开始的
				setEnd(unitSize * Math.abs(ev));// 到第10行，是截止于100(不包含该边界)
			} else {
				setBegin(0);
				int v = Integer.parseInt(print);
				setEnd(unitSize * (Math.abs(v)));
			}
		}

		public boolean filter(int count) {
			return count >= begin && count < end;
		}
	}

	static class LinePrintGate implements PrintGate {

		private PrintGate[] list;

		public void setPrint(String print, int unitSize) {
			String[] prints = print.split(",");
			list = new PrintGate[prints.length];
			for (int i = 0; i < prints.length; i++) {
				PrintGateToken pg = new PrintGateToken();
				pg.setPrint(prints[i], unitSize);
				list[i] = pg;
			}
		}

		public boolean filter(int count) {
			for (int i = 0; i < list.length; i++) {
				if (list[i].filter(count)) {
					return true;
				}
			}
			return false;
		}

	}

	static class StringReaderEx extends StringReader {
		private int inputLength;

		public StringReaderEx(String s) {
			super(s);
			inputLength = s.length();
		}
	}

}
