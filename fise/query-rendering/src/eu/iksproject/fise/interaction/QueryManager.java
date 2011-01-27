package org.apache.stanbol.enhancer.interaction;

/*
 * Copyright 2010
 * German Research Center for Artificial Intelligence (DFKI)
 * Department of Intelligent User Interfaces
 * Germany
 *
 *     http://www.dfki.de/web/forschung/iui
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
 *
 * Authors:
 *     Sebastian Germesin
 *     Massimo Romanelli
 *     Tilman Becker
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.apache.stanbol.enhancer.interaction.event.ClerezzaResultEvent;
import org.apache.stanbol.enhancer.interaction.event.ClerezzaServerInfoChangedEvent;
import org.apache.stanbol.enhancer.interaction.event.Event;
import org.apache.stanbol.enhancer.interaction.event.EventListener;
import org.apache.stanbol.enhancer.interaction.event.EventManager;
import org.apache.stanbol.enhancer.interaction.event.QueryEvent;
import org.apache.stanbol.enhancer.interaction.event.UploadFileEvent;
import org.apache.stanbol.enhancer.interaction.util.ClientHttpRequest;

public class QueryManager implements EventListener {

    private String serverHost;
    private int serverPort;
    private String username;
    private String password;

    public QueryManager (String serverHost, int serverPort, String username, String password) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.username = username;
        this.password = password;
    }

    public void eventOccurred(Event e) {
        if (e instanceof QueryEvent) {
            QueryEvent qe = (QueryEvent)e;

            String clerezzaResult = postData(qe.getSparqlQuery());

            if (clerezzaResult != null) {
                ClerezzaResultEvent cre = new ClerezzaResultEvent(clerezzaResult);
                EventManager.eventOccurred(cre);
            }
        }
        else if (e instanceof UploadFileEvent) {
            UploadFileEvent ufe = (UploadFileEvent)e;

            uploadFile(ufe.getFilename(), ufe.getUri());
        }
        else if (e instanceof ClerezzaServerInfoChangedEvent) {
            ClerezzaServerInfoChangedEvent csice = (ClerezzaServerInfoChangedEvent)e;
            this.serverHost = csice.getServerHost();
            this.serverPort = csice.getServerPort();
            this.username = csice.getUsername();
            this.password = csice.getPassword();
        }

    }

    private void uploadFile(String filename, String uri) {
        try {
            URL url = new URL("http://" + serverHost + ":" + serverPort + "/content");
            URLConnection conn = url.openConnection();
            ClientHttpRequest chr = new ClientHttpRequest(conn, username, password);
            chr.setParameter("uri", uri);
            chr.setParameter("content", new File(filename));

            String resultText = "";
            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(chr.post()));
            String line;
            while ((line = rd.readLine()) != null) {
                // Process line...
                resultText += line;
            }
            rd.close();
            System.out.println(resultText);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String postData (String query) {
        try {
            String resultText = "";
            // Construct data
            String data = URLEncoder.encode("query", "UTF-8") + "=" + URLEncoder.encode(query, "UTF-8");

            // Send data
            URL url = new URL("http://" + serverHost + ":" + serverPort + "/fise");

            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();

            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                // Process line...
                resultText += line;
            }
            wr.close();
            rd.close();
            return resultText;
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return null;
    }

    public byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        // Get the size of the file
        long length = file.length();
        // You cannot create an array using a long type.
        // It needs to be an int type.
        // Before converting to an int type, check
        // to ensure that file is not larger than Integer.MAX_VALUE.

        if (length > Integer.MAX_VALUE) {
            // File is too large
        }
        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];
        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }
        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        } // Close the input stream and return bytes
        is.close();
        return bytes;
    }

}
