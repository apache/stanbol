/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.stanbol.commons.testing.it;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;

/** Start a runnable jar by forking a JVM process,
 *  and terminate the process when this VM exits.
 *
 *  TODO: move to a commons/testing library?
 */
public class JarExecutor {
    private static JarExecutor instance;
    private final File jarToExecute;
    private final String javaExecutable;
    private final int serverPort;

    public static final int DEFAULT_PORT = 8765;
    public static final String PROP_PREFIX = "jar.executor.";
    public static final String PROP_SERVER_PORT = PROP_PREFIX + "server.port";

    @SuppressWarnings("serial")
    public static class ExecutorException extends Exception {
        ExecutorException(String reason) {
            super(reason);
        }
        ExecutorException(String reason, Throwable cause) {
            super(reason, cause);
        }
    }
    
    public int getServerPort() {
        return serverPort;
    }

    public static JarExecutor getInstance(Properties config) throws ExecutorException {
        if(instance == null) {
            synchronized (JarExecutor.class) {
                if(instance == null) {
                    instance = new JarExecutor(config);
                }
            }
        }
        return instance;
    }

    /** Build a JarExecutor, locate the jar to run, etc */
    private JarExecutor(Properties config) throws ExecutorException {
        final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");

        String str = config.getProperty(PROP_SERVER_PORT);
        serverPort = str == null ? DEFAULT_PORT : Integer.valueOf(str);

        // TODO make those configurable
        javaExecutable = isWindows ? "java.exe" : "java";
        final File jarFolder = new File("./target/dependency");
        final Pattern jarPattern = Pattern.compile("org.apache.stanbol.*full.*jar$");

        // Find executable jar
        final String [] candidates = jarFolder.list();
        File f = null;
        if(candidates != null) {
            for(String filename : candidates) {
                if(jarPattern.matcher(filename).matches()) {
                    f = new File(jarFolder, filename);
                    break;
                }
            }
        }

        if(f == null) {
            throw new ExecutorException("Executable jar matching " + jarPattern
                    + " not found in " + jarFolder.getAbsolutePath()
                    + ", candidates are " + Arrays.asList(candidates));
        }
        jarToExecute = f;
    }

    /** Start the jar if not done yet, and setup runtime hook
     *  to stop it.
     */
    public void start() throws Exception {
        final ExecuteResultHandler h = new ExecuteResultHandler() {
            @Override
            public void onProcessFailed(ExecuteException ex) {
                info("Process execution failed:" + ex);
            }

            @Override
            public void onProcessComplete(int result) {
                info("Process execution complete, exit code=" + result);
            }
        };

        final String vmOptions = System.getProperty("jar.executor.vm.options"); 
        final Executor e = new DefaultExecutor();
        final CommandLine cl = new CommandLine(javaExecutable);
        if(vmOptions != null && vmOptions.length() > 0) {
            cl.addArgument(vmOptions);
        }
        cl.addArgument("-jar");
        cl.addArgument(jarToExecute.getAbsolutePath());
        cl.addArgument("-p");
        cl.addArgument(String.valueOf(serverPort));
        info("Executing " + cl);
        e.setProcessDestroyer(new ShutdownHookProcessDestroyer());
        e.execute(cl, h);
    }

    protected void info(String msg) {
        System.out.println(getClass().getName() + ": " + msg);
    }
}
