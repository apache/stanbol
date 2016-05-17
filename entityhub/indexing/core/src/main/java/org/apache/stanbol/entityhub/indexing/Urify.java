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
package org.apache.stanbol.entityhub.indexing;

import static java.lang.System.exit;
import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implemented to allow importing the Musicbrainz dump to Jena TDB.<p>
 * 
 * The problem is that RDF dumps with blank nodes do require to store a
 * lookup table for the black nodes IDs during import.<p> In case a dump
 * contains millions of such nodes this table does no longer fit into
 * memory. This makes importing Dumps to with the Jena RDF parser impossible.
 * <p>
 * This Utility can replaces nodes that start with "_:{id}" with
 * "<{prefix}{id}>. The prefix must be set with the "-p" parameter.
 * <p>
 * This tool supports "gz" and "bz2" compressed files. If the output will use
 * the same compression as the input. It uses two threads for "reading/processing" 
 * and "writing". It could also process multiple files in parallel. However this
 * feature is not yet activated as the Musicbrainz dump comes in a single file.
 * 
 * @author Rupert Westenthaler
 *
 */
public class Urify implements Runnable{

    private static final String EOF_INDICATOR = "__EOF_INDICATOR";
    
    private static Logger log = LoggerFactory.getLogger(Urify.class);
    
    private static final Options options;
    
    static {
        options = new Options();
        options.addOption("h", "help", false, "display this help and exit");
        options.addOption("p","prefix",true, 
            "The URI prefix used for wrapping the bNode Id");
        options.addOption("e","encoding",true, "the char encodinf (default: UTF-8)");
        options.addOption("o","outputFilePrefix",true, "The prefix to add to output files, defaults to \"uf_\"");
    }
    /**
     * @param args
     * @throws ParseException 
     */
    public static void main(String[] args) throws IOException, ParseException {
        CommandLineParser parser = new PosixParser();
        CommandLine line = parser.parse(options, args);
        args = line.getArgs();
        if (line.hasOption('h')) {
        		out.println("Processes RDF files to translate blank nodes into prefixed URI nodes.");
        		out.println("-h/--help: Print this help and exit.");
        		out.println("-p/--prefix: Required: The prefix to add to blank nodes to make them URIs.");
        		out.println("-e/--encoding: The text encoding to expect in the RDF, defaults to UTF-8.");
        		out.println("-o/--outputFilePrefix: The prefix to add to output files, defaults to \"uf_\".");
        		exit(0);
        }
        if(!line.hasOption('p')){
            log.error("Missing parameter 'prefix' ('p)!");
            exit(1);
        }
        String prefix = "<"+line.getOptionValue('p');
        log.info("Using prefix: {} ",line.getOptionValue('p'));
        Charset charset;
        if(line.hasOption('e')){
            charset = Charset.forName(line.getOptionValue('e'));
            if(charset == null){
                log.error("Unsupported encoding '{}'!",line.getOptionValue('e'));
                exit(1);
            }
        } else {
            charset = Charset.forName("UTF-8");
        }

        log.info("charset: {} ",charset.name());
		Urify urify = new Urify(Arrays.asList(args), charset, prefix,
				line.hasOption('o') ? line.getOptionValue('o') : "uf_");
		urify.run(); //TODO: this could support processing multiple files in parallel
    }

    private final Charset charset;
    private final String prefix;
    private final String outputFilePrefix;
    protected long start = System.currentTimeMillis();
    protected long uf_count = 0;

    private List<String> resources;

	public Urify(List<String> resources, Charset charset, String prefix,
			final String outputFilePrefix) throws IOException {
		this.charset = charset;
		this.prefix = prefix;
		this.outputFilePrefix = outputFilePrefix;
		this.resources = Collections.synchronizedList(new ArrayList<String>(
				resources));
	}
    
    public void run() {
        String source;
        do {
            synchronized (resources) {
                if(resources.isEmpty()){
                    source = null;
                } else {
                    source = resources.remove(0);
                    try {
                        urify(source);
                    } catch (Exception e) {
                        log.error("Unable to Urify "+resources,e);
                    }
                }
            }
        } while (source != null);
    }
    private void urify(String resource) throws IOException {
        File source = new File(resource);
        if(source.isFile()){
            String path = FilenameUtils.getFullPathNoEndSeparator(resource);
            String name = FilenameUtils.getName(resource);
            File target = new File(path, outputFilePrefix + name);
            int i=0;
            while(target.exists()){
                i++;
                target = new File(path,"uf"+i+"_"+name);
            }
            InputStream is = new FileInputStream(source);
            OutputStream os = new FileOutputStream(target);
            log.info("RDFTerm: {}",resource);
            log.info("Target  : {}",target);
            if ("gz".equalsIgnoreCase(FilenameUtils.getExtension(name))) {
                is = new GZIPInputStream(is);
                os = new GZIPOutputStream(os);
                name = FilenameUtils.removeExtension(name);
                log.debug("   - from GZIP Archive");
            } else if ("bz2".equalsIgnoreCase(FilenameUtils.getExtension(name))) {
                is = new BZip2CompressorInputStream(is);
                os = new BZip2CompressorOutputStream(os);
                name = FilenameUtils.removeExtension(name);
                log.debug("   - from BZip2 Archive");
            }// TODO: No Zip File support
            //else no complression
            BlockingQueue<String> queue = new ArrayBlockingQueue<String>(1000);
            ReaderDaemon reader = new ReaderDaemon(new BufferedReader(new InputStreamReader(is, charset)), queue);
            WriterDaemon writer = new WriterDaemon(
                new BufferedWriter(new OutputStreamWriter(os, charset)), queue);
            Thread readerDaemon = new Thread(reader,name+" reader");
            Thread writerDaemon = new Thread(writer,name+" writer");
            readerDaemon.setDaemon(true);
            writerDaemon.setDaemon(true);
            writerDaemon.start();
            readerDaemon.start();
            Object notifier = writer.getNotifier();
            synchronized (notifier) { //wait until processed
               if(!writer.completed()){
                   try {
                       notifier.wait();
                   } catch (InterruptedException e) {/*ignore*/}
               }
            }
            if(reader.getError() != null){
                throw new IOException("Error while reading source "+source,reader.getError());
            }
            if(writer.getError() != null){
                throw new IOException("Error while writing resource "+target,writer.getError());
            }
            log.info(" ... completed resource {}",resource);
        } else {
            throw new FileNotFoundException("Parsed File "+resource+" does not exist or is not a File!");
        }
    }

    private class ReaderDaemon implements Runnable {
        private final BufferedReader reader;
        private final BlockingQueue<String> queue;

        private Exception error;
        
        protected ReaderDaemon(BufferedReader reader, BlockingQueue<String> queue){
            this.reader = reader;
            this.queue = queue;
        }
        
        
        public Exception getError() {
            return error;
        }


        @Override
        public void run() {
            String triple;
            try {
                while((triple = reader.readLine()) != null){
                    StringBuilder sb = new StringBuilder();
                    StringTokenizer st = new StringTokenizer(triple," \t");
                    while(st.hasMoreElements()){
                        String node = st.nextToken();
                        if(node.startsWith("_:")){ //convert to uri
                            sb.append(prefix);
                            sb.append(node.substring(2,node.length()));
                            sb.append("> ");
                            uf_count++;
                        } else {
                            sb.append(node);
                            if(node.length()>1){
                                //the final '.' is also a node
                                sb.append(" ");
                            }
                        }
                    }
                    queue.put(sb.toString());
                }
            } catch (IOException e) {
                error = e;
            } catch (InterruptedException e) {
                error = e;
            } finally {
                IOUtils.closeQuietly(reader);
                try {
                    queue.put(EOF_INDICATOR); //indicates finished
                } catch (InterruptedException e) {
                    log.error("Unable to put EOF to queue!",e);
                } 
            }
        }
    }
    
    private class WriterDaemon implements Runnable {
        private final BufferedWriter writer;
        private final BlockingQueue<String> queue;
        private final Object notifier = new Object();
        private boolean completed = false;

        private Exception error;
        
        protected WriterDaemon(BufferedWriter writer, BlockingQueue<String> queue){
            this.writer = writer;
            this.queue = queue;
        }
        
        public Exception getError() {
            return error;
        }

        public Object getNotifier() {
            return notifier;
        }

        public boolean completed() {
            return completed;
        }

        @Override
        public void run() {
            String triple;
            long count = 0;
            boolean first = true;
            try {
                while(!EOF_INDICATOR.equals((triple = queue.take()))){
                    if(count % 1000000 == 0){
                        //NOTE: urified will not be correct as it is counted
                        //      by an other thread, but for logging ...
                        long end = System.currentTimeMillis();
                        log.info("processed {} | urified: {} (batch: {}sec)",
                            new Object[]{count,uf_count,((double)(end-start))/1000});
                        start = end;
                    }
                    count++;
                    if(first){
                        first = false;
                    } else {
                        writer.write("\n");
                    }
                    writer.write(triple);
                    
                }
            } catch (InterruptedException e) {
                this.error = e;
            } catch (IOException e) {
                this.error = e;
            } finally {
                IOUtils.closeQuietly(writer);
                this.completed = true;
                synchronized (notifier) {
                    notifier.notifyAll();
                }
            }
        }
    }
}
