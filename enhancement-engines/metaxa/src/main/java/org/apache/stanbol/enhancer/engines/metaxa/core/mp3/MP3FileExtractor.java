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
package org.apache.stanbol.enhancer.engines.metaxa.core.mp3;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.node.RDFTerm;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.vocabulary.RDF;
import org.semanticdesktop.aperture.extractor.AbstractFileExtractor;
import org.semanticdesktop.aperture.extractor.ExtractorException;
import org.semanticdesktop.aperture.rdf.RDFContainer;
import org.semanticdesktop.aperture.rdf.util.ModelUtil;
import org.semanticdesktop.aperture.vocabulary.NCO;
import org.semanticdesktop.aperture.vocabulary.NID3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mpatric.mp3agic.ID3Wrapper;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

/**
 *
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 * 
 */

public class MP3FileExtractor extends AbstractFileExtractor {
  
  private Logger logger = LoggerFactory.getLogger(MP3FileExtractor.class);
  
  @Override
  protected void performExtraction(URI arg0, File arg1, Charset arg2, String arg3, RDFContainer result) throws ExtractorException {
    try {
      Mp3File mp3File = new Mp3File(arg1.toString());
      ID3v1 id3v1 = mp3File.getId3v1Tag();
      ID3v2 id3v2 = mp3File.getId3v2Tag();
      ID3Wrapper wrapper = new ID3Wrapper(id3v1,id3v2);
      addId3Fields(wrapper,result);
      result.add(RDF.type, NID3.ID3Audio);
      
    } catch (UnsupportedTagException e) {
      throw new ExtractorException(e);
    } catch (InvalidDataException e) {
      throw new ExtractorException(e);
    } catch (IOException e) {
      throw new ExtractorException(e);
    }
  }

  private void addId3Fields(ID3Wrapper wrapper, RDFContainer result) {
    String value = null;
    if ((value = wrapper.getAlbum()) != null) {
      result.add(NID3.albumTitle,value);
    }
    if ((value = wrapper.getArtist()) != null) {
      addSimpleContact(NID3.originalArtist,value,result);
    }
    if ((value = wrapper.getComment()) != null) {
      result.add(NID3.comments,value);
    }
    if ((value = wrapper.getComposer())!= null) {
      addSimpleContact(NID3.composer,value,result);
    }
    if ((value  = wrapper.getCopyright()) != null) {
      result.add(NID3.copyrightMessage,value);
    }
    if ((value  = wrapper.getEncoder()) != null) {
      addSimpleContact(NID3.encodedBy, value,result);
    }
    if ((value  = wrapper.getGenreDescription()) != null) {
      result.add(NID3.contentType,value);
    }
    if ((value  = wrapper.getTitle()) != null) {
      result.add(NID3.title,value);
    }
    if ((value  = wrapper.getOriginalArtist()) != null) {
      addSimpleContact(NID3.originalArtist,value,result);
    }
    if ((value  = wrapper.getTrack()) != null) {
      addSimpleContact(NID3.trackNumber,value,result);
    }
    if ((value  = wrapper.getYear()) != null) {
      try {
        int year = Integer.parseInt(value);
        result.add(NID3.recordingYear,year);
      }
      catch(NumberFormatException e) {}
    }
  }
  
  protected void addSimpleContact(URI property, String fullname, RDFContainer container) {
    Model model = container.getModel();
    RDFTerm resource = ModelUtil.generateRandomResource(model);
    model.addStatement(resource, RDF.type, NCO.Contact);
    model.addStatement(resource, NCO.fullname, fullname);
    model.addStatement(container.getDescribedUri(), property, resource);
}


  
}
