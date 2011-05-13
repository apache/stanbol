package org.apache.stanbol.rengineer.base;

import static org.junit.Assert.fail;

import java.util.Hashtable;

import org.apache.stanbol.reengineer.base.api.DataSource;
import org.apache.stanbol.reengineer.base.api.ReengineeringException;
import org.apache.stanbol.reengineer.base.api.ReengineerManager;
import org.apache.stanbol.reengineer.base.api.Reengineer;
import org.apache.stanbol.reengineer.base.api.util.ReengineerType;
import org.apache.stanbol.reengineer.base.api.util.UnsupportedReengineerException;
import org.apache.stanbol.reengineer.base.impl.ReengineerManagerImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;


public class ReengineerManagerTest {

	public static Reengineer semionReengineer;

	
	@BeforeClass
	public static void setup(){
		semionReengineer = new Reengineer() {
			
			@Override
			public OWLOntology schemaReengineering(String graphNS, IRI outputIRI,
					DataSource dataSource) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public OWLOntology reengineering(String graphNS, IRI outputIRI,
					DataSource dataSource) throws ReengineeringException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public int getReengineerType() {
				// TODO Auto-generated method stub
				return ReengineerType.XML;
			}
			
			@Override
			public OWLOntology dataReengineering(String graphNS, IRI outputIRI,
					DataSource dataSource, OWLOntology schemaOntology)
					throws ReengineeringException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public boolean canPerformReengineering(String dataSourceType)
					throws UnsupportedReengineerException {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean canPerformReengineering(OWLOntology schemaOntology) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean canPerformReengineering(int dataSourceType) {
				if(dataSourceType == ReengineerType.XML){
					return true;
				}
				return false;
			}
			
			@Override
			public boolean canPerformReengineering(DataSource dataSource) {
				// TODO Auto-generated method stub
				return false;
			}
		};
	}
	
	@Test
	public void bindTest(){
		ReengineerManager semionManager = new ReengineerManagerImpl(new Hashtable<String,Object>());
		if(!semionManager.bindReengineer(semionReengineer)){
			fail("Bind test failed for SemionManager");
		}
	}
	
	@Test
	public void unbindByReengineerTypeTest(){
		ReengineerManager semionManager = new ReengineerManagerImpl(new Hashtable<String,Object>());
		semionManager.bindReengineer(semionReengineer);
		if(!semionManager.unbindReengineer(ReengineerType.XML)){
			fail("Unbind by reengineer type test failed for SemionManager");
		}
	}
	
	@Test
	public void unbindByReengineerInstanceTest(){
		ReengineerManager semionManager = new ReengineerManagerImpl(new Hashtable<String,Object>());
		semionManager.bindReengineer(semionReengineer);
		if(!semionManager.unbindReengineer(semionReengineer)){
			fail("Unbind by reengineer instance test failed for SemionManager");
		}
	}
}
