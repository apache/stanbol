package org.apache.stanbol.contenthub.test;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Map;

import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.contenthub.servicesapi.ldpath.LDPathException;
import org.apache.stanbol.contenthub.servicesapi.ldpath.SemanticIndexManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SlingAnnotationsTestRunner.class)
public class LDPathTest {
	@TestReference
	SemanticIndexManager semanticIndexManager;
	
/*
ClerezzaBackend.java
LDPathUtils.java	
*/
	@Before
	public void before(){
		if(semanticIndexManager.isManagedProgram(TestVocabulary.programName)){
			semanticIndexManager.deleteProgram(TestVocabulary.programName);
		}
	}
	@Test
	public void testSubmitProgram() throws LDPathException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
		semanticIndexManager.submitProgram(TestVocabulary.programName, TestVocabulary.ldPathProgram);
		Field field = semanticIndexManager.getClass().getDeclaredField("nameProgramMap");
		field.setAccessible(true);
		Map<String,String> programMap = (Map<String, String>) field.get(semanticIndexManager);
		assertEquals(TestVocabulary.ldPathProgram,programMap.get(TestVocabulary.programName));
		semanticIndexManager.deleteProgram(TestVocabulary.programName);
	}
	
//	@Test
//	public void testGetProgramByName() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, LDPathException{
//		semanticIndexManager.submitProgram(TestVocabulary.programName, TestVocabulary.ldPathProgram);
//		Field field = semanticIndexManager.getClass().getDeclaredField("nameProgramMap");
//		field.setAccessible(true);
//		Map<String,String> programMap = (Map<String, String>) field.get(semanticIndexManager);
//		assertEquals(TestVocabulary.ldPathProgram,programMap.get(TestVocabulary.programName));
//		semanticIndexManager.deleteProgram(TestVocabulary.programName);
//	}
	
	@Test
	public void testDeleteProgram() throws LDPathException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
		semanticIndexManager.submitProgram(TestVocabulary.programName, TestVocabulary.ldPathProgram);
		semanticIndexManager.deleteProgram(TestVocabulary.programName);
		Field field = semanticIndexManager.getClass().getDeclaredField("nameProgramMap");
		field.setAccessible(true);
		Map<String,String> programMap = (Map<String, String>) field.get(semanticIndexManager);
		assertNotSame(TestVocabulary.ldPathProgram,programMap.get(TestVocabulary.programName));
	}
	
	@Test
	public void testIsManagedProgram() throws LDPathException{
		semanticIndexManager.submitProgram(TestVocabulary.programName, TestVocabulary.ldPathProgram);
		assertTrue(semanticIndexManager.isManagedProgram(TestVocabulary.programName));
		semanticIndexManager.deleteProgram(TestVocabulary.programName);
	}
	
	@Test
	public void testRetrieveAllPrograms() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
		Field field = semanticIndexManager.getClass().getDeclaredField("nameProgramMap");
		field.setAccessible(true);
		Map<String,String> programMap = (Map<String, String>) field.get(semanticIndexManager);
		
		assertEquals(programMap, semanticIndexManager.retrieveAllPrograms().asMap());
	}
	
//	@Test
//	public void testExecuteProgram(){
//		
//	}
}
