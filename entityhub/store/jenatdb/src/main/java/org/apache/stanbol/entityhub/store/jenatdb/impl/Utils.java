package org.apache.stanbol.entityhub.store.jenatdb.impl;

import java.io.File;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.base.file.Location;
import com.hp.hpl.jena.tdb.store.DatasetGraphTDB;

public class Utils {
    /**
     * @param modelLocation The directory with the Jena TDB model. Will be created
     * if not existent.
     * @return
     * @throws IllegalArgumentException if <code>null</code> is parsed; 
     * if the parsed {@link File} exists but is not a directory; if the parsed 
     * File does NOT exists AND can not be created.
     */
    public static Dataset initTDBDataset(File modelLocation) {
        if(modelLocation == null){
            throw new IllegalArgumentException("The parsed Jena TDB directory" +
            		"MUST NOT be NULL!");
        }
        if(modelLocation.exists() && !modelLocation.isDirectory()){
            throw new IllegalArgumentException("The configured RDF model directory "+
                modelLocation+"exists but is not a Directory");
        } else if(!modelLocation.exists()){
            if(!modelLocation.mkdirs()){
                throw new IllegalArgumentException("Unable to create the configured RDF model directory "+
                    modelLocation+"!");
            }
        }
        Location location = new Location(modelLocation.getAbsolutePath());
        return TDBFactory.createDataset(location);
    }
	/**
	 * Initializes the {@link Graph} for based on the parsed parameter. If
	 * namedGraphName is <code>null</code> the default graph of the parsed
	 * dataset is returned. Otherwise the named graph for the parsed node
	 * is created/opened. 
	 * @param dataset
	 * @param namedGraphNode
	 * @return
	 */
	public static Graph initGraph(Dataset dataset, Node namedGraphNode) {
		DatasetGraph dsGraph = dataset.asDatasetGraph();
		if(namedGraphNode != null){
			if(!dsGraph.containsGraph(namedGraphNode)){
				dsGraph.addGraph(namedGraphNode, Graph.emptyGraph);
			}
			return dsGraph.getGraph(namedGraphNode);
		} else {
			return dsGraph.getDefaultGraph();
		}
	}
	/**
	 * Removes all data from the parsed named graph. IF the parsed namedGraphNode
	 * is <code>null</code> than all data from the default graph are deleted.
	 * @param dataset the {@link Dataset}. <p>
	 * <i>NOTE</i> that Jena API requires to call 'set' with an empty grpah. 
	 * Therefore the graph instance is being replaced.
	 * Because of this callers need to replace their current reference to the
	 * Graph with the one returned by this method. 
	 * @param namedGraphNode the named graph node or <code>null</code> for the
	 * default graph
	 * @return the emptied graph. 
	 */
	public static Graph cleanGraph(Dataset dataset, Node namedGraphNode){
		DatasetGraph dsGraph = dataset.asDatasetGraph();
		if(namedGraphNode != null){
			dsGraph.addGraph(namedGraphNode, Graph.emptyGraph);
			return dsGraph.getGraph(namedGraphNode);
		} else {
			dsGraph.setDefaultGraph(Graph.emptyGraph);
			return dsGraph.getDefaultGraph();
		}
	}
}
