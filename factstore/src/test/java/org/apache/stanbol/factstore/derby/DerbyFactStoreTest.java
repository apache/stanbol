package org.apache.stanbol.factstore.derby;

import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.stanbol.commons.jsonld.JsonLdProfile;
import org.apache.stanbol.factstore.model.FactSchema;
import org.junit.Assert;
import org.junit.Test;

public class DerbyFactStoreTest {

    @Test
    public void testToSQLFromProfile() throws Exception {
        DerbyFactStore fs = new DerbyFactStore();
        JsonLdProfile jsonLd = new JsonLdProfile();
        jsonLd.addNamespacePrefix("http://iks-project.eu/ont/", "iks");
        
        jsonLd.addType("person", "iks:person");
        jsonLd.addType("organization", "iks:organization");
        
        String profile = "http://iks-project.eu/ont/employeeOf";
        String profileB64 = Base64.encodeBase64URLSafeString(profile.getBytes());
        
        String expected = "CREATE TABLE aHR0cDovL2lrcy1wcm9qZWN0LmV1L29udC9lbXBsb3llZU9m (id INT GENERATED ALWAYS AS IDENTITY, person VARCHAR(1024), organization VARCHAR(1024))";
        List<String> sqls = fs.toSQLfromSchema(profileB64, FactSchema.fromJsonLdProfile(profile, jsonLd));
        Assert.assertEquals(1, sqls.size());
        Assert.assertEquals(expected, sqls.get(0));
    }

    @SuppressWarnings("unused")
    private void toConsole(String actual) {
        System.out.println(actual);
        String s = actual;
        s = s.replaceAll("\\\\", "\\\\\\\\");
        s = s.replace("\"", "\\\"");
        s = s.replace("\n", "\\n");
        System.out.println(s);
    }
}
