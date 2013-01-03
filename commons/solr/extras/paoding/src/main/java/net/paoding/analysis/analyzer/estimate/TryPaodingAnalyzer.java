package net.paoding.analysis.analyzer.estimate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import net.paoding.analysis.analyzer.PaodingAnalyzer;
import net.paoding.analysis.knife.PaodingMaker;

import org.apache.lucene.analysis.Analyzer;

public class TryPaodingAnalyzer {
	private static final String ARGS_TIP = ":";
	static String input = null;
	static String file = null;
	static Reader reader = null;
	static String charset = null;
	static String mode = null;
	static String analyzerName = null;
	static String print = null;
	static String properties = PaodingMaker.DEFAULT_PROPERTIES_PATH;
	
	public static void main(String[] args) {
		try {
			resetArgs();
			
			int inInput = 0;
			for (int i = 0; i < args.length; i++) {
				if (args[i] == null || (args[i] = args[i].trim()).length() == 0) {
					continue;
				}
				if (args[i].equals("--file") || args[i].equals("-f")) {
					file = args[++i];
				} else if (args[i].equals("--charset") || args[i].equals("-c")) {
					charset = args[++i];
				} else if (args[i].equals("--mode") || args[i].equals("-m")) {
					mode = args[++i];
				} else if (args[i].equals("--properties") || args[i].equals("-p")) {
					properties = args[++i];
				} else if (args[i].equals("--analyzer") || args[i].equals("-a")) {
					analyzerName = args[++i];
				} else if (args[i].equals("--print") || args[i].equals("-P")) {
					print = args[++i];
				} else if (args[i].equals("--input") || args[i].equals("-i")) {
					inInput++;
				} else if (args[i].equals("--help") || args[i].equals("-h")
						|| args[i].equals("?")) {
					printHelp();
					return;
				} else {
					// 非选项的参数数组视为input
					if (!args[i].startsWith("-")
							&& (i == 0 || args[i - 1].equals("-i") || args[i - 1].equals("--input") || !args[i - 1].startsWith("-"))) {
						if (input == null) {
							input = args[i];// !!没有++i
						} else {
							input = input + ' ' + args[i];// !!没有++i
						}
						inInput++;
					}
				}
			}
			if (file != null) {
				input = null;
				reader = getReader(file, charset);
			}
			//
			analysing();
		} catch (Exception e1) {
			resetArgs();
			e1.printStackTrace();
		}
	}



	private static void resetArgs() {
		input = null;
		file = null;
		reader = null;
		charset = null;
		mode = null;
		print = null;
		analyzerName = null;
		properties = PaodingMaker.DEFAULT_PROPERTIES_PATH;
	}
	

	
	private static void analysing() throws Exception {
		Analyzer analyzer;
		if (analyzerName == null || analyzerName.length() == 0 || analyzerName.equalsIgnoreCase("paoding")) {
			//properties==null等同于new new PaodingAnalyzer();
			analyzer = new PaodingAnalyzer(properties);
			if (mode != null) {
				((PaodingAnalyzer) analyzer).setMode(mode);
			}
		}
		else {
			Class clz;
			if (analyzerName.equalsIgnoreCase("standard")) {
				analyzerName = "org.apache.lucene.analysis.standard.StandardAnalyzer";
			}
			else if (analyzerName.equalsIgnoreCase("cjk")) {
				analyzerName = "org.apache.lucene.analysis.cjk.CJKAnalyzer";
			}
			else if (analyzerName.equalsIgnoreCase("cn") || analyzerName.equalsIgnoreCase("chinese")) {
				analyzerName = "org.apache.lucene.analysis.cn.ChineseAnalyzer";
			}
			else if (analyzerName.equalsIgnoreCase("st") || analyzerName.equalsIgnoreCase("standard")) {
				analyzerName = "org.apache.lucene.analysis.standard.StandardAnalyzer";
			}
			clz = Class.forName(analyzerName);
			analyzer = (Analyzer) clz.newInstance();
		}
		boolean readInputFromConsle = false;
		Estimate estimate = new Estimate(analyzer);
		if (print != null) {
			estimate.setPrint(print);
		}
		while (true) {
			if (reader == null) {
				if (input == null || input.length() == 0 || readInputFromConsle) {
					input = getInputFromConsole();
					readInputFromConsle = true;
				}
				if (input == null || input.length() == 0) {
					System.out.println("Warn: none charactors you input!!");
					continue;
				}
				else if (input.startsWith(ARGS_TIP)) {
					String argsStr = input.substring(ARGS_TIP.length());
					main(argsStr.split(" "));
					continue;
				}
			}
			if (reader != null) {
				estimate.test(System.out, reader);
				reader = null;
			}
			else {
				estimate.test(System.out, input);
				input = null;
			}
			System.out.println("--------------------------------------------------");
			if (false == readInputFromConsle) {
				return;
			}
		}
	}

	private static String getInputFromConsole() throws IOException {
		printTitleIfNotPrinted("");
		String input = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		String line;
		do {
			System.out.print("paoding> ");
			line = reader.readLine();
			if (line == null || line.length() == 0) {
				continue;
			}
			if (line.equals(ARGS_TIP + "clear") || line.equals(ARGS_TIP + "c")) {
				input = null;
				System.out.println("paoding> Cleared");
				return getInputFromConsole();
			}
			else if (line.equals(ARGS_TIP + "exit") || line.equals(ARGS_TIP + "quit") || line.equals(ARGS_TIP + "e") || line.equals(ARGS_TIP + "q") ) {
				System.out.println("Bye!");
				System.exit(0);
			}
			else if (input == null && line.startsWith(ARGS_TIP)) {
				input = line;
				break;
			}
			else {
				if (line.endsWith(";")) {
					if (line.length() > ";".length()) {
						input = line.substring(0, line.length() - ";".length());
					}
					break;
				}
				else {
					if (input == null) {
						input = line;
					} else {
						input = input + "\n" + line;
					}
				}
			}
		} while (true);
		return input == null ? null : input.trim();
	}

	private static void printHelp() {
		String app = System.getProperty("paoding.try.app",
				"TryPaodingAnalyzer");
		String cmd = System.getProperty("paoding.try.cmd", "java "
				+ TryPaodingAnalyzer.class.getName());
		System.out.println(app + "的用法:");
		System.out.println("\t" + cmd + " [OPTIONS] [text_content]");
		System.out.println("\nOPTIONS:");
		System.out.println("\t--file, -f:\n\t\t文章以文件的形式输入，在前缀加上\"classpath:\"表示从类路径中寻找该文件。");
		System.out.println("\t--charset, -c:\n\t\t文章的字符集编码，比如gbk,utf-8等。如果没有设置该选项，则使用Java环境默认的字符集编码。");
		System.out.println("\t--properties, -p:\n\t\t不读取默认的类路径下的庖丁分词属性文件，而使用指定的文件，在前缀加上\"classpath:\"表示从类路径中寻找该文件。");
		System.out.println("\t--mode, -m:\n\t\t强制使用给定的mode的分词器；可以设定为default,most-words,max-word-length或指定类名的其他mode(指定类名的，需要加前缀\"class:\")。");
		System.out.println("\t--input, -i:\n\t\t要被分词的文章内容；当没有通过-f或--file指定文章输入文件时可选择这个选项指定要被分词的内容。");
		System.out.println("\t--analyzer, -a:\n\t\t测试其他分词器，通过--analyzer或-a指定其完整类名。特别地，paoding、cjk、chinese、st分别对应PaodingAnalyzer、CJKAnalyzer、ChineseAnalyzer、StandardAnalyzer");
		System.out.println("\t--print, -P:\n\t\t 是否打印分词结果。默认打印前50行。规则：no表示不打印；50等价于1-50行；1-50表示打印1至50行;可以以逗号组合使用，如20,40-50表示打印1-20以及40-50行");
		System.out.println("\n示例:");
		System.out.println("\t" + cmd);
		System.out.println("\t" + cmd + " ?");
		System.out.println("\t" + cmd + " 中华人民共和国");
		System.out.println("\t" + cmd + " -m max 中华人民共和国");
		System.out.println("\t" + cmd + " -f e:/content.txt -c utf8");
		System.out.println("\t" + cmd + " -f e:/content.txt -c utf8 -m max-word-length");
		System.out.println("\t" + cmd + " -f e:/content.txt -c utf8 -a cjk");
		System.out.println("\n若是控制台进入\"paoding>\"后:");
		titlePrinted = false;
		printTitleIfNotPrinted("\t");
	}
	
	
	private static boolean titlePrinted = false;
	private static boolean welcomePrinted = false;
	private static void printTitleIfNotPrinted(String prefix) {
		if (!titlePrinted) {
			System.out.println();
			if (!welcomePrinted) {
				System.out.println("Welcome to Paoding Analyser(2.0.4-alpha2)");
				System.out.println();
				welcomePrinted = true;
			}
			System.out.println(prefix + "直接输入或粘贴要被分词的内容，以分号;结束，回车后开始分词。");
			System.out.println(prefix + "另起一行输入:clear或:c，使此次输入无效，用以重新输入。");
			System.out.println(prefix + "要使用命令行参数读入文件内容或其他参数请以冒号:开始，然后输入参数选项。");
			System.out.println(prefix + "退出，请输入:quit或:q、:exit、:e");
			System.out.println(prefix + "需要帮助，请输入:?");
			System.out.println(prefix + "注意：指定对文件分词之前要了解该文件的编码，如果系统编码和文件编码不一致，要通过-c指定文件的编码。");
			System.out.println();
			titlePrinted = true;
		}
	}
	
		
	static String getContent(String path, String encoding) throws IOException {
		return (String) read(path, encoding, true);
	}
	
	static Reader getReader(String path, String encoding) throws IOException {
		return (Reader) read(path, encoding, false);
	}
	
	static Object read(String path, String encoding, boolean return_string) throws IOException {
		InputStream in;
		if (path.startsWith("classpath:")) {
			path = path.substring("classpath:".length());
			URL url = Estimate.class.getClassLoader().getResource(path);
			if (url == null) {
				throw new IllegalArgumentException("Not found " + path
						+ " in classpath.");
			}
			System.out.println("read content from:" + url.getFile());
			in = url.openStream();
		} else {
			File f = new File(path);
			if (!f.exists()) {
				throw new IllegalArgumentException("Not found " + path
						+ " in system.");
			}
			System.out.println("read content from:" + f.getAbsolutePath());
			in = new FileInputStream(f);
		}
		Reader re;
		if (encoding != null) {
			re = new InputStreamReader(in, encoding);
		} else {
			re = new InputStreamReader(in);
		}
		if (!return_string) {
			return re;
		}
		char[] chs = new char[1024];
		int count;
		// 为兼容低版本的JDK，使用StringBuffer而不是StringBuilder
		StringBuffer content = new StringBuffer();
		while ((count = re.read(chs)) != -1) {
			content.append(chs, 0, count);
		}
		re.close();
		return content.toString();
		}
}
