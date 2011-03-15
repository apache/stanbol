package eu.iksproject.kres.semion.manager.datasources;

import java.io.InputStream;

import org.apache.stanbol.reengineer.base.IdentifiedDataSource;
import org.apache.stanbol.reengineer.base.util.ReengineerType;

public class XML extends IdentifiedDataSource {

	private InputStream in;
	
	public XML(InputStream in) {
		this.in = in;
	}
	
	@Override
	public Object getDataSource() {
		return in;
	}

	@Override
	public int getDataSourceType() {
		return ReengineerType.XML;
	}

}
