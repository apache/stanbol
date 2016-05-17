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
package org.apache.stanbol.enhancer.engines.tika.metadata;

import static java.util.Collections.disjoint;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.ontologies.SKOS;
import org.apache.clerezza.rdf.ontologies.XSD;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.apache.tika.metadata.CreativeCommons;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Geographic;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.MSOffice;
import org.apache.tika.metadata.Message;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Office;
import org.apache.tika.metadata.OfficeOpenXMLCore;
import org.apache.tika.metadata.OfficeOpenXMLExtended;
import org.apache.tika.metadata.TIFF;
import org.apache.tika.metadata.XMPDM;
import org.apache.tika.parser.opendocument.OpenOfficeParser;

/**
 * Defines mappings for keys used by Apache Tika in the {@link Metadata} to
 * ontology properties.<p>
 * 
 * @author Rupert Westenthaler
 *
 */
public class OntologyMappings implements Iterable<Mapping>{
    
    private static OntologyMappings defaultMappings;
    
    private final Map<IRI,Collection<Mapping>> mappings = new HashMap<IRI,Collection<Mapping>>();
    /**
     * Used to protect the default mappings from modifications
     */
    private boolean readonly = false;
    /**
     * The media ontology namespace
     */
    private static String ma = "http://www.w3.org/ns/ma-ont#";
    
    public static OntologyMappings getDefaultMappings(){
        if(defaultMappings == null){
            defaultMappings = new OntologyMappings();
            //TODO: validate the defaults
            addMediaResourceOntologyMappings(defaultMappings);
            addNepomukMessageMappings(defaultMappings);
            addRdfsMappings(defaultMappings);
        }
        return defaultMappings;
    }
    
    /**
     * @param mappings
     */
    public static void addNepomukMessageMappings(OntologyMappings mappings) {
        String nmo = "http://www.semanticdesktop.org/ontologies/2007/03/22/nmo#";
        mappings.addMappings( 
            new PropertyMapping(nmo+"bbc",Message.MESSAGE_BCC));
        mappings.addMappings( 
            new PropertyMapping(nmo+"cc",Message.MESSAGE_CC));
        mappings.addMappings( 
            new PropertyMapping(nmo+"from",Message.MESSAGE_FROM));
        mappings.addMappings( 
            new PropertyMapping(nmo+"to",Message.MESSAGE_TO));
    }

    /**
     * @param mappings
     */
    public static void addGeoMappings(OntologyMappings mappings) {
        mappings.addMappings(
            new PropertyMapping(NamespaceEnum.geo+"alt",XSD.double_,Geographic.ALTITUDE.getName()));
        mappings.addMappings(
            new PropertyMapping(NamespaceEnum.geo+"lat",XSD.double_,Geographic.LATITUDE.getName()));
        mappings.addMappings(
            new PropertyMapping(NamespaceEnum.geo+"long",XSD.double_,Geographic.LONGITUDE.getName()));
    }

    /**
     * Maps the {@link TIFF} metadata to the Nepomuk EXIF ontology. This uses
     * the more preceise datatyped as defined by {@link TIFF} rather than
     * xsd:string as defined for most of the properites within the Nepomuk
     * ontology.
     * @param mappings
     */
    public static void addNepomukExifMappings(OntologyMappings mappings) {
        String exif = "http://www.semanticdesktop.org/ontologies/2007/05/10/nexif#";
        mappings.addMappings( 
            new PropertyMapping(exif+"bitsPerSample",XSD.int_,TIFF.BITS_PER_SAMPLE.getName()));
        mappings.addMappings(
            new PropertyMapping(exif+"make",TIFF.EQUIPMENT_MAKE.getName()));
        mappings.addMappings(
            new PropertyMapping(exif+"model",TIFF.EQUIPMENT_MODEL.getName()));
        mappings.addMappings( 
            new PropertyMapping(exif+"exposureTime",XSD.double_,TIFF.EXPOSURE_TIME.getName()));
        mappings.addMappings( 
            new PropertyMapping(exif+"fNumber",XSD.double_,TIFF.F_NUMBER.getName()));
        mappings.addMappings( 
            new PropertyMapping(exif+"flash",XSD.boolean_,TIFF.FLASH_FIRED.getName()));
        mappings.addMappings( 
            new PropertyMapping(exif+"focalLength",XSD.double_,TIFF.FOCAL_LENGTH.getName()));
        mappings.addMappings(
            new PropertyMapping(exif+"relatedImageLength",XSD.int_,TIFF.IMAGE_LENGTH.getName()));
        mappings.addMappings(
            new PropertyMapping(exif+"relatedImageWidth",XSD.int_,TIFF.IMAGE_WIDTH.getName()));
        mappings.addMappings( 
            new PropertyMapping(exif+"isoSpeedRatings",XSD.int_,TIFF.ISO_SPEED_RATINGS.getName()));
        mappings.addMappings(
            new PropertyMapping(exif+"orientation",XSD.string,TIFF.ORIENTATION.getName()));
        mappings.addMappings( 
            new PropertyMapping(exif+"dateTimeOriginal",XSD.dateTime,TIFF.ORIGINAL_DATE.getName()));
        mappings.addMappings(
            new PropertyMapping(exif+"xResolution",XSD.double_,TIFF.RESOLUTION_HORIZONTAL.getName()));
        mappings.addMappings(
            new PropertyMapping(exif+"resolutionUnit",XSD.string,TIFF.RESOLUTION_UNIT.getName()));
        mappings.addMappings(
            new PropertyMapping(exif+"yResolution",XSD.double_,TIFF.RESOLUTION_VERTICAL.getName()));
        mappings.addMappings(
            new PropertyMapping(exif+"samplesPerPixel",XSD.int_,TIFF.SAMPLES_PER_PIXEL.getName()));
        mappings.addMappings(
            new PropertyMapping(exif+"software",TIFF.SOFTWARE.getName()));
    }

    /**
     * Adds the Mappings for {@link DublinCore}<p>
     * Two mappings are added for each property<ul>
     * <li> <a href="http://dublincore.org/documents/dcmi-terms/">Dublin Core Terms</a>
     * <li> The <a href="http://www.w3.org/TR/mediaont-10/#dc-table">DC terms 
     * mappings</a> of the Media Annotation Ontology.
     * </ul>
     * @param mappings The ontology mappings to add the DC mappings
     */
    public static void addDcMappings(OntologyMappings mappings) {
        String dc = NamespaceEnum.dc.getNamespace();
        mappings.addMapping(
            new PropertyMapping(dc+"contributor",
                DublinCore.CONTRIBUTOR.getName(),Office.LAST_AUTHOR.getName()));
        mappings.addMapping(
            new PropertyMapping(dc+"coverage",DublinCore.COVERAGE.getName()));
        mappings.addMappings(
            new PropertyMapping(dc+"creator",
                DublinCore.CREATOR.getName(),Office.AUTHOR.getName(),"initial-creator"));
        mappings.addMappings( 
            new PropertyMapping(dc+"description",DublinCore.DESCRIPTION.getName()));
        mappings.addMappings( 
            new PropertyMapping(dc+"format",
                DublinCore.FORMAT.getName(),HttpHeaders.CONTENT_TYPE));
        mappings.addMappings( 
            new PropertyMapping(dc+"identifier",DublinCore.IDENTIFIER.getName()));
        mappings.addMappings(
            new PropertyMapping(dc+"language",
                DublinCore.LANGUAGE.getName(),HttpHeaders.CONTENT_LANGUAGE));
        mappings.addMappings(
            new PropertyMapping(dc+"modified",XSD.dateTime,
                DublinCore.MODIFIED.getName(),"Last-Modified"));
        mappings.addMappings( 
            new PropertyMapping(dc+"publisher",
                DublinCore.PUBLISHER.getName(),MSOffice.COMPANY));
        mappings.addMappings( 
            new PropertyMapping(dc+"relation",DublinCore.RELATION.getName()));
        mappings.addMappings(
            new PropertyMapping(dc+"rights",DublinCore.RIGHTS.getName()));
        mappings.addMappings( 
            new PropertyMapping(dc+"source",DublinCore.SOURCE.getName()));
        mappings.addMappings( 
            new PropertyMapping(dc+"subject",
                DublinCore.SUBJECT.getName(),Office.KEYWORDS.getName()));
        mappings.addMappings( 
            new PropertyMapping(dc+"title",DublinCore.TITLE.getName()));
        mappings.addMappings( 
            new PropertyMapping(dc+"type",DublinCore.TYPE.getName()));
        mappings.addMappings( 
            new PropertyMapping(dc+"date",XSD.dateTime,DublinCore.DATE.getName()));
        mappings.addMappings(
            new PropertyMapping(dc+"created",XSD.dateTime,
                DublinCore.CREATED.getName(),"created"));
        //MS Office -> DC
        mappings.addMappings( 
            new PropertyMapping(dc+"title",OfficeOpenXMLCore.SUBJECT.getName()));
        mappings.addMappings(
            new PropertyMapping(dc+"created",XSD.dateTime,
                Office.CREATION_DATE.getName(),"created"));
        
    }
    public static void addMediaResourceOntologyMappings(OntologyMappings mappings){
        mappings.addMappings(
            new PropertyMapping(ma+"hasContributor",
                DublinCore.CONTRIBUTOR.getName(),XMPDM.ARTIST.getName(),XMPDM.COMPOSER.getName()));
        mappings.addMapping( 
            new ResourceMapping(ma+"hasLocation",
                new TypeMapping(ma+"Location"),
                new PropertyMapping(ma+"locationName",DublinCore.COVERAGE.getName())));
        mappings.addMappings( 
            new PropertyMapping(ma+"hasCreator",
                DublinCore.CREATOR.getName(),MSOffice.AUTHOR,"initial-creator"));
        mappings.addMappings( 
            new PropertyMapping(ma+"description",DublinCore.DESCRIPTION.getName()));
        mappings.addMappings( 
            new PropertyMapping(ma+"hasFormat",
                DublinCore.FORMAT.getName(),HttpHeaders.CONTENT_TYPE));
        /*
         * Excerpt of the MA recommendation:
         *   The identifier of a media resource is represented in RDF by the URI 
         *   of the node representing that media resource. If a resource is 
         *   identified by several URI, owl:sameAs should be used.
         */
        mappings.addMappings( 
            new PropertyMapping(OWL.sameAs,RDFS.Resource,DublinCore.IDENTIFIER.getName()));
        mappings.addMappings( 
            new PropertyMapping(ma+"hasLanguage",
                DublinCore.LANGUAGE.getName(),HttpHeaders.CONTENT_LANGUAGE));
        mappings.addMappings( 
            new PropertyMapping(ma+"editDate",XSD.dateTime,
                DublinCore.MODIFIED.getName(),MSOffice.LAST_SAVED.getName()));
        mappings.addMappings(
            new PropertyMapping(ma+"hasPublisher",DublinCore.PUBLISHER.getName()));
        mappings.addMappings( 
            new PropertyMapping(ma+"hasRelatedResource",DublinCore.RELATION.getName()));
        mappings.addMappings( 
            new PropertyMapping(ma+"copyright",RDFS.Resource,
                //DC:rights and cc:license
                DublinCore.RIGHTS.getName(),CreativeCommons.LICENSE_LOCATION, CreativeCommons.LICENSE_URL,
                XMPDM.COPYRIGHT.getName()));
        mappings.addMappings( 
            new PropertyMapping(ma+"isMemberOf",DublinCore.SOURCE.getName()));
        mappings.addMappings( 
            new PropertyMapping(ma+"hasKeyword",
                DublinCore.SUBJECT.getName(),MSOffice.KEYWORDS));
        mappings.addMappings( 
            new PropertyMapping(ma+"title",
                DublinCore.TITLE.getName(),XMPDM.SCENE.getName(),XMPDM.TAPE_NAME.getName(),
                XMPDM.SHOT_NAME.getName()));
        mappings.addMapping(
            new PropertyMapping(ma+"alternativeTitle", XMPDM.ALT_TAPE_NAME.getName()));
        mappings.addMapping(
            new PropertyMapping(ma+"mainOriginalTitle", XMPDM.ALBUM.getName()));
        mappings.addMappings( 
            new PropertyMapping(ma+"hasGenre",
                DublinCore.TYPE.getName(),XMPDM.GENRE.getName()));
        mappings.addMappings(
            new PropertyMapping(ma+"creationDate",XSD.dateTime,
                DublinCore.DATE.getName(),MSOffice.CREATION_DATE.getName(),"created"));
        mappings.addMapping(
            new PropertyMapping(ma+"description", 
                DublinCore.DESCRIPTION.getName(),MSOffice.COMMENTS));
        
        mappings.addMappings( 
            new PropertyMapping(ma+"hasContributor",
                MSOffice.LAST_AUTHOR,MSOffice.AUTHOR,XMPDM.ENGINEER.getName()));

        
        //other properties -> Media Ontology
        mappings.addMappings(
            new PropertyMapping(ma+"hasCreator","producer","initial-creator"));

        //EXIF -> Media Ontology
        mappings.addMappings(
            new PropertyMapping(ma+"frameHeight",XSD.int_,TIFF.IMAGE_LENGTH.getName()));
        mappings.addMappings(
            new PropertyMapping(ma+"frameWidth",XSD.int_,TIFF.IMAGE_WIDTH.getName()));
        mappings.addMappings( 
            new PropertyMapping(ma+"creationDate",XSD.dateTime,
                TIFF.ORIGINAL_DATE.getName(),XMPDM.SHOT_DATE.getName()));

        //XMP -> Media Ontology
        //here we need to split up the metadata for the audio and video
        mappings.addMapping(
            new PropertyMapping(ma+"releaseDate", XSD.dateTime,XMPDM.RELEASE_DATE.getName()));
        mappings.addMapping(new ResourceMapping(ma+"hasTrack", 
            new Mapping[]{ /* no required */},
            new Mapping[]{//optional
                new PropertyMapping(ma+"hasFormat",XSD.string,XMPDM.AUDIO_CHANNEL_TYPE.getName()),
                new PropertyMapping(ma+"hasCompression",XSD.string,XMPDM.AUDIO_COMPRESSOR.getName()),
                new PropertyMapping(ma+"editDate", XMPDM.AUDIO_MOD_DATE.getName()),
                new PropertyMapping(ma+"samplingRate", XSD.int_,XMPDM.AUDIO_SAMPLE_RATE.getName())
            }, new Mapping[]{
                new TypeMapping(ma+"MediaFragment"),
                new TypeMapping(ma+"Track"),
                new TypeMapping(ma+"AudioTrack"),
            }
            ));
        mappings.addMapping(new ResourceMapping(ma+"hasTrack", 
            new Mapping[]{ /* no required */},
            new Mapping[]{//optional
                new PropertyMapping(ma+"hasCompression",XSD.string,XMPDM.VIDEO_COMPRESSOR.getName()),
                new PropertyMapping(ma+"editDate", XMPDM.VIDEO_MOD_DATE.getName()),
                new PropertyMapping(ma+"frameRate", XSD.double_,XMPDM.VIDEO_FRAME_RATE.getName())
            },
            new Mapping[]{ //additioanl
                new TypeMapping(ma+"MediaFragment"),
                new TypeMapping(ma+"Track"),
                new TypeMapping(ma+"VideoTrack"),
                new PropertyMapping(ma+"frameHeight",XSD.int_,TIFF.IMAGE_LENGTH.getName()),
                new PropertyMapping(ma+"frameWidth",XSD.int_,TIFF.IMAGE_WIDTH.getName())
            }));
        mappings.addMapping(
            new PropertyMapping(ma+"numberOfTracks",XSD.int_,XMPDM.TRACK_NUMBER.getName()));
        mappings.addMapping(
            new PropertyMapping(ma+"averageBitRate",XSD.double_,
                new Mapping.Converter(){//we need to convert from MByte/min to kByte/sec
                    @Override
                    public RDFTerm convert(RDFTerm value) {
                        if(value instanceof Literal &&
                                XSD.double_.equals(((Literal)value).getDataType())){
                            LiteralFactory lf = LiteralFactory.getInstance();
                            double mm = lf.createObject(Double.class, (Literal)value);
                            return lf.createTypedLiteral(Double.valueOf(
                                mm*1024/60));
                        } else {
                            return value; //do not convert
                        }
                    }
                
            },XMPDM.FILE_DATA_RATE.getName()));

        //GEO -> Media RDFTerm Ontology
        mappings.addMapping(new ResourceMapping(ma+"hasLocation", 
            new Mapping[]{ //required
                new PropertyMapping(ma+"locationLatitude", XSD.double_,Geographic.LATITUDE.getName()),
                new PropertyMapping(ma+"locationLongitude", XSD.double_,Geographic.LONGITUDE.getName())          
            },new Mapping[]{ //optional
                new PropertyMapping(ma+"locationAltitude", XSD.double_,Geographic.ALTITUDE.getName())          
            },new Mapping[]{//additional
                new TypeMapping(ma+"Location")
            }));
    }
//TODO
//    public static void addNepomukId3Mappings(OntologyMappings mappings){
//        XMPDM.ABS_PEAK_AUDIO_FILE_PATH;
//        XMPDM.ALBUM;
//        XMPDM.ALT_TAPE_NAME;
//        XMPDM.ARTIST;
//        XMPDM.AUDIO_CHANNEL_TYPE;
//        XMPDM.AUDIO_COMPRESSOR;
//        XMPDM.AUDIO_MOD_DATE;
//        XMPDM.AUDIO_SAMPLE_RATE;
//        XMPDM.AUDIO_SAMPLE_TYPE;
//        XMPDM.COMPOSER;
//        XMPDM.COPYRIGHT;
//        XMPDM.ENGINEER;
//        XMPDM.FILE_DATA_RATE;
//        XMPDM.GENRE;
//        XMPDM.INSTRUMENT;
//        XMPDM.KEY;
//        XMPDM.LOG_COMMENT;
//        XMPDM.LOOP;
//        XMPDM.METADATA_MOD_DATE;
//        XMPDM.NUMBER_OF_BEATS;
//        XMPDM.PULL_DOWN;
//        XMPDM.RELATIVE_PEAK_AUDIO_FILE_PATH;
//        XMPDM.RELEASE_DATE;
//        XMPDM.SCALE_TYPE;
//        XMPDM.SCENE;
//        XMPDM.SHOT_DATE;
//        XMPDM.SHOT_LOCATION;
//        XMPDM.SHOT_NAME;
//        XMPDM.SPEAKER_PLACEMENT;
//        XMPDM.STRETCH_MODE;
//        XMPDM.TAPE_NAME;
//        XMPDM.TEMPO;
//        XMPDM.TIME_SIGNATURE;
//        XMPDM.TRACK_NUMBER;
//        XMPDM.VIDEO_ALPHA_MODE;
//        XMPDM.VIDEO_ALPHA_UNITY_IS_TRANSPARENT;
//        XMPDM.VIDEO_COLOR_SPACE;
//        XMPDM.VIDEO_COMPRESSOR;
//        XMPDM.VIDEO_FIELD_ORDER;
//        XMPDM.VIDEO_FRAME_RATE;
//        XMPDM.VIDEO_MOD_DATE;
//        XMPDM.VIDEO_PIXEL_ASPECT_RATIO;
//        XMPDM.VIDEO_PIXEL_DEPTH;
//    }
    public static void addSkosMappings(OntologyMappings mappings){
        //DC -> SKOS
        mappings.addMappings( 
            new PropertyMapping(SKOS.prefLabel,
                DublinCore.TITLE.getName()));
        mappings.addMappings( 
            new PropertyMapping(SKOS.definition,
                DublinCore.DESCRIPTION.getName()));
        mappings.addMappings(
            new PropertyMapping(SKOS.notation,
                DublinCore.IDENTIFIER.getName()));
        //MS Office -> SKOS
        mappings.addMappings( 
            new PropertyMapping(SKOS.note,MSOffice.COMMENTS));
        mappings.addMappings( 
            new PropertyMapping(SKOS.editorialNote,
                MSOffice.NOTES,XMPDM.LOG_COMMENT.getName()));
    }
    
    public static void addRdfsMappings(OntologyMappings mappings){
        //DC
        mappings.addMappings( 
            new PropertyMapping(RDFS.label,DublinCore.TITLE.getName()));
        mappings.addMappings( 
            new PropertyMapping(RDFS.comment,DublinCore.DESCRIPTION.getName(),MSOffice.COMMENTS));
    }
    
    /**
     * Maps only {@link CreativeCommons#LICENSE_URL} to cc:license
     * @param mappings
     */
    public static void addCreativeCommonsMappings(OntologyMappings mappings){
        mappings.addMapping( 
            new PropertyMapping("http://creativecommons.org/ns#license",RDFS.Resource,
                CreativeCommons.LICENSE_URL,CreativeCommons.LICENSE_LOCATION));

    }
    
    
    public void addMappings(Mapping...mappings){
        if(mappings == null || mappings.length > 1){
            return; //nothing to do
        }
        for(Mapping m : mappings){
            addMapping(m);
        }
    }
    public void addMapping(Mapping mapping){
        if(readonly){
            throw new IllegalStateException("This "+getClass().getSimpleName()+" instance is read only!");
        }
        if(mapping == null){
            return; //nothing to do
        }
        Collection<Mapping> propMappings = this.mappings.get(mapping.getOntologyProperty());
        if(propMappings == null){
            propMappings = new HashSet<Mapping>();
            this.mappings.put(mapping.getOntologyProperty(), propMappings);
        }
        propMappings.add(mapping);
    }
    public void removePropertyMappings(IRI property){
        if(readonly){
            throw new IllegalStateException("This "+getClass().getSimpleName()+" instance is read only!");
        }
        this.mappings.remove(property);
    }
    
    /**
     * Applies the registered Ontology Mappings to the parsed metadata and
     * context. Mappings are added to the parsed ImmutableGraph
     * @param graph
     * @param context
     * @param metadata
     * @return Set containing the names of mapped keys
     */
    public Set<String> apply(Graph graph, IRI context, Metadata metadata){
        Set<String> keys = new HashSet<String>(Arrays.asList(metadata.names()));
        Set<String> mappedKeys = new HashSet<String>();
        for(Mapping mapping : this){
            if(mapping.getMappedTikaProperties().isEmpty() ||
                    !disjoint(keys, mapping.getMappedTikaProperties())){
                mapping.apply(graph, context, metadata);
                mappedKeys.addAll(mapping.getMappedTikaProperties());
            }
        }
        return mappedKeys;
    }
    @Override
    public Iterator<Mapping> iterator() {
        return new Iterator<Mapping>() {
            Iterator<Collection<Mapping>> mappingsIt = OntologyMappings.this.mappings.values().iterator();
            Iterator<Mapping> mappingIt = Collections.EMPTY_LIST.iterator();
            @Override
            public boolean hasNext() {
                //assumes no empty lists as values of OntologyMappings.this.mappings
                return mappingIt.hasNext() || mappingsIt.hasNext();
            }

            @Override
            public Mapping next() {
                //assumes no empty lists as values of OntologyMappings.this.mappings
                if(!mappingIt.hasNext()){
                    mappingIt = mappingsIt.next().iterator();
                }
                return mappingIt.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Removal not Supported!");
            }
            
        };
    }

}
