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


import java.util.Properties;
/**
 * 
 * @author  Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 * @since 2.0.4
 */
public interface DictionariesCompiler {

	/**
	 * 
	 * @param p
	 * @return
	 * @throws Exception
	 */
	public boolean shouldCompile(Properties p) throws Exception;
	
	/**
	 * 
	 * @param dictionaries
	 * @param knife
	 * @param p
	 * @throws Exception
	 */
	public void compile(Dictionaries dictionaries, Knife knife, Properties p) throws Exception;
	
	/**
	 * 
	 * @param p
	 * @return
	 * @throws Exception
	 */
	public Dictionaries readCompliedDictionaries(Properties p) throws Exception;
}
