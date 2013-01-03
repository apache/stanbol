package net.paoding.analysis.analyzer.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import net.paoding.analysis.Constants;
import net.paoding.analysis.dictionary.Dictionary;
import net.paoding.analysis.dictionary.Word;
import net.paoding.analysis.dictionary.support.detection.Snapshot;
import net.paoding.analysis.knife.Dictionaries;
import net.paoding.analysis.knife.DictionariesCompiler;
import net.paoding.analysis.knife.Knife;

public class SortingDictionariesCompiler implements DictionariesCompiler {
	public static final String VERSION = "2";
	
	public boolean shouldCompile(Properties p) throws Exception {
		String dicHome = p.getProperty("paoding.dic.home.absolute.path");
		File dicHomeFile = new File(dicHome);
		File compliedMetadataFile = new File(dicHomeFile, ".compiled/sorting/.metadata");
		if (compliedMetadataFile.exists() && compliedMetadataFile.isFile()) {
			// get checksum for all compiled dictionaries
			String checksum = Snapshot.flash(
					new File(dicHomeFile, ".compiled/sorting"),
					new FileFilter() {
						public boolean accept(File pathname) {
							return pathname.getPath().endsWith(".dic.compiled");
						}
					}).getCheckSum();

			Properties compiledProperties = new Properties();
			InputStream compiledPropertiesInput = new FileInputStream(compliedMetadataFile);
			compiledProperties.load(compiledPropertiesInput);
			compiledPropertiesInput.close();
			String compiledCheckSum = compiledProperties.getProperty("paoding.analysis.compiler.checksum");
			String clazz = compiledProperties.getProperty("paoding.analysis.compiler.class");
			String version = compiledProperties.getProperty("paoding.analysis.compiler.version");
			if (checksum.equals(compiledCheckSum) && this.getClass().getName().equalsIgnoreCase(clazz)
					&& VERSION.equalsIgnoreCase(version)) {
				return false;
			}
		}
		return true;
	}
	
	
	public void compile(Dictionaries dictionaries, Knife knife, Properties p) throws Exception {
		String dicHome = p.getProperty("paoding.dic.home.absolute.path");
		String noiseCharactor = getProperty(p, Constants.DIC_NOISE_CHARACTOR);
		String noiseWord = getProperty(p, Constants.DIC_NOISE_WORD);
		String unit = getProperty(p, Constants.DIC_UNIT);
		String confucianFamilyName = getProperty(p, Constants.DIC_CONFUCIAN_FAMILY_NAME);
		String combinatorics = getProperty(p, Constants.DIC_FOR_COMBINATORICS);
		String charsetName = getProperty(p, Constants.DIC_CHARSET);
		
		File dicHomeFile = new File(dicHome);
		File compiledDicHomeFile = new File(dicHomeFile, ".compiled/sorting");
		compiledDicHomeFile.mkdirs();
		
		//
		Dictionary vocabularyDictionary = dictionaries.getVocabularyDictionary();
		File vocabularyFile = new File(compiledDicHomeFile, "vocabulary.dic.compiled");
		sortCompile(vocabularyDictionary, vocabularyFile, charsetName);

		//
		Dictionary noiseCharactorsDictionary = dictionaries.getNoiseCharactorsDictionary();
		File noiseCharactorsDictionaryFile = new File(compiledDicHomeFile, noiseCharactor + ".dic.compiled");
		sortCompile(noiseCharactorsDictionary, noiseCharactorsDictionaryFile, charsetName);
		//
		Dictionary noiseWordsDictionary = dictionaries.getNoiseWordsDictionary();
		File noiseWordsDictionaryFile = new File(compiledDicHomeFile, noiseWord + ".dic.compiled");
		sortCompile(noiseWordsDictionary, noiseWordsDictionaryFile, charsetName);
		//
		Dictionary unitsDictionary = dictionaries.getUnitsDictionary();
		File unitsDictionaryFile = new File(compiledDicHomeFile, unit + ".dic.compiled");
		sortCompile(unitsDictionary, unitsDictionaryFile, charsetName);
		//
		Dictionary confucianFamilyDictionary = dictionaries.getConfucianFamilyNamesDictionary();
		File confucianFamilyDictionaryFile = new File(compiledDicHomeFile, confucianFamilyName + ".dic.compiled");
		sortCompile(confucianFamilyDictionary, confucianFamilyDictionaryFile, charsetName);
		//
		Dictionary combinatoricsDictionary = dictionaries.getCombinatoricsDictionary();
		File combinatoricsDictionaryFile = new File(compiledDicHomeFile, combinatorics + ".dic.compiled");
		sortCompile(combinatoricsDictionary, combinatoricsDictionaryFile, charsetName);

		//
		File compliedMetadataFile = new File(dicHomeFile, ".compiled/sorting/.metadata");
		if (compliedMetadataFile.exists()) {
			//compliedMetadataFile.setWritable(true);
			compliedMetadataFile.delete();
		}
		else {
			compliedMetadataFile.getParentFile().mkdirs();
		}
		OutputStream compiledPropertiesOutput = new FileOutputStream(compliedMetadataFile);
		Properties compiledProperties = new Properties();
		String lastModifiedsKey = "paoding.analysis.properties.lastModifieds";
		String filesKey = "paoding.analysis.properties.files";
		compiledProperties.setProperty(lastModifiedsKey, p.getProperty(lastModifiedsKey));
		compiledProperties.setProperty(filesKey, p.getProperty(filesKey));
		compiledProperties.setProperty("paoding.analysis.compiler.checksum",
				Snapshot.flash(
						new File(dicHomeFile, ".compiled/sorting"),
						new FileFilter() {
							public boolean accept(File pathname) {
								return pathname.getPath().endsWith(
										".dic.compiled");
							}
						}).getCheckSum());
		compiledProperties.setProperty("paoding.analysis.compiler.class", this.getClass().getName());
		compiledProperties.setProperty("paoding.analysis.compiler.version", VERSION);
		compiledProperties.store(compiledPropertiesOutput, "dont edit it! this file was auto generated by paoding.");
		compiledPropertiesOutput.close();
		compliedMetadataFile.setReadOnly();
	}

	
	
	private void sortCompile(final Dictionary dictionary, 
			File dicFile, String charsetName) throws FileNotFoundException,
			IOException, UnsupportedEncodingException {
		int wordsSize = dictionary.size();
		if (dicFile.exists()) {
			//dicFile.setWritable(true);
			dicFile.delete();
		}
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(dicFile), 1024 * 16);
		
		for (int i = 0; i < wordsSize; i++) {
			Word word = dictionary.get(i);
			out.write(word.getText().getBytes(charsetName));
			if (word.getModifiers() != Word.DEFAUL) {
				out.write("[m=".getBytes());
				out.write(String.valueOf(word.getModifiers()).getBytes());
				out.write(']');
			}
			out.write('\r');
			out.write('\n');
		}
		out.flush();
		out.close();
		dicFile.setReadOnly();
	}
	
	public Dictionaries readCompliedDictionaries(Properties p) {
		String dicHomeAbsolutePath = p.getProperty("paoding.dic.home.absolute.path");
		String noiseCharactor = getProperty(p, Constants.DIC_NOISE_CHARACTOR);
		String noiseWord = getProperty(p, Constants.DIC_NOISE_WORD);
		String unit = getProperty(p, Constants.DIC_UNIT);
		String confucianFamilyName = getProperty(p, Constants.DIC_CONFUCIAN_FAMILY_NAME);
		String combinatorics = getProperty(p, Constants.DIC_FOR_COMBINATORICS);
		String charsetName = getProperty(p, Constants.DIC_CHARSET);
		int maxWordLen = Integer.valueOf(getProperty(p, Constants.DIC_MAXWORDLEN));
		return new CompiledFileDictionaries(
				dicHomeAbsolutePath + "/.compiled/sorting",
				noiseCharactor, noiseWord, unit,
				confucianFamilyName, combinatorics, charsetName, maxWordLen);
	}
	
	private static String getProperty(Properties p, String name) {
		return Constants.getProperty(p, name);
	}
	
}
