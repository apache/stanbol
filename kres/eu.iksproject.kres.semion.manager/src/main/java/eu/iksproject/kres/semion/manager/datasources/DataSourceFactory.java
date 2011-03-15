package eu.iksproject.kres.semion.manager.datasources;

import java.io.InputStream;

import org.apache.stanbol.reengineer.base.DataSource;
import org.apache.stanbol.reengineer.base.settings.ConnectionSettings;

public class DataSourceFactory {

	
	public static DataSource createDataSource(int dataSourceType, Object source) throws NoSuchDataSourceExpection, InvalidDataSourceForTypeSelectedException {
		DataSource dataSource;
		System.out.println("DATA SOURCE CLASS IS "+source.getClass().getCanonicalName());
		switch (dataSourceType) {
		case 0:
			if(source instanceof ConnectionSettings){
				dataSource = new RDB((ConnectionSettings) source);
			}
			else{
				throw new InvalidDataSourceForTypeSelectedException(source);
			}
			break;
		case 1:
			if(source instanceof InputStream){
				dataSource = new XML((InputStream) source);
				System.out.println("THE DATA SOURCE IS AN XML");
			}
			else{
				throw new InvalidDataSourceForTypeSelectedException(source);
			}
			break;

		default:
			throw new NoSuchDataSourceExpection(dataSourceType);
		}
		
		return dataSource;
	}
}
