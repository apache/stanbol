package org.apache.stanbol.reengineer.base;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class IdentifiedDataSource implements DataSource {

		
	protected String id;
	
	public String getID() {
        return id;
    }

    

}
