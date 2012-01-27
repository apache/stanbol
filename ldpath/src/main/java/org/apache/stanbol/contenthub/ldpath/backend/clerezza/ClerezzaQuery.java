/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.contenthub.ldpath.backend.clerezza;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.newmedialab.ldpath.LDPath;
import at.newmedialab.ldpath.exception.LDPathParseException;

public class ClerezzaQuery {
    private static final Logger logger = LoggerFactory.getLogger(ClerezzaQuery.class);

    private static Parser clerezzaRDFParser;

    public static void main(String[] args) {
        Options options = buildOptions();

        CommandLineParser parser = new PosixParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            ClerezzaBackend clerezzaBackend = null;
            if (cmd.hasOption("filePath")) {
                clerezzaRDFParser = new Parser();
                clerezzaRDFParser.bindParsingProvider(new JenaParserProvider());
                MGraph mGraph = new SimpleMGraph(clerezzaRDFParser.parse(
                    new FileInputStream(cmd.getOptionValue("filePath")), SupportedFormat.RDF_XML));
                clerezzaBackend = new ClerezzaBackend(mGraph);
            }

            Resource context = null;
            if (cmd.hasOption("context")) {
                context = clerezzaBackend.createURI(cmd.getOptionValue("context"));
            }

            if (clerezzaBackend != null && context != null) {
                LDPath<Resource> ldpath = new LDPath<Resource>(clerezzaBackend);

                if (cmd.hasOption("path")) {
                    String path = cmd.getOptionValue("path");

                    for (Resource r : ldpath.pathQuery(context, path, null)) {
                        System.out.println(r.toString());
                    }
                } else if (cmd.hasOption("program")) {
                    File file = new File(cmd.getOptionValue("program"));

                    Map<String,Collection<?>> result = ldpath.programQuery(context, new FileReader(file));

                    for (String field : result.keySet()) {
                        StringBuilder line = new StringBuilder();
                        line.append(field);
                        line.append(" = ");
                        line.append("{");
                        for (Iterator<?> it = result.get(field).iterator(); it.hasNext();) {
                            line.append(it.next().toString());
                            if (it.hasNext()) {
                                line.append(", ");
                            }
                        }
                        line.append("}");
                        System.out.println(line);

                    }
                }
            }

        } catch (ParseException e) {
            logger.error("invalid arguments");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("LDQuery", options, true);
        } catch (LDPathParseException e) {
            logger.error("path or program could not be parsed");
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            logger.error("file or program could not be found");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("LDQuery", options, true);
        }

    }

    private static Options buildOptions() {
        Options result = new Options();

        OptionGroup query = new OptionGroup();
        OptionBuilder.withArgName("path");
        OptionBuilder.hasArg();
        OptionBuilder
                .withDescription("LD Path to evaluate on the file starting from the context");
        Option path = OptionBuilder.create("path");
        OptionBuilder.withArgName("file");
        OptionBuilder.hasArg();
        OptionBuilder
                .withDescription("LD Path program to evaluate on the file starting from the context");
        Option program = OptionBuilder
                .create("program");
        query.addOption(path);
        query.addOption(program);
        query.setRequired(true);
        result.addOptionGroup(query);

        OptionBuilder.withArgName("filePath");
        OptionBuilder.hasArg();
        OptionBuilder
                .withDescription("File system path of the file holding RDF data");
        Option filePath = OptionBuilder.create("filePath");
        filePath.setRequired(true);
        result.addOption(filePath);

        OptionBuilder.withArgName("uri");
        OptionBuilder.hasArg();
        OptionBuilder
                .withDescription("URI of the context node to start from");
        Option context = OptionBuilder.create("context");
        context.setRequired(true);
        result.addOption(context);

        return result;
    }
}
