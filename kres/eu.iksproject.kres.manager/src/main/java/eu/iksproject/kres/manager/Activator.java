package eu.iksproject.kres.manager;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Called upon OSGi bundle startup and shutdown, it constructs and releases the
 * resources required by the KReS Ontology Network Manager during its activity.
 * 
 * @author alessandro
 * 
 */
public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {

		// context.addBundleListener(new BundleListener() {
		//			
		// @Override
		// public void bundleChanged(BundleEvent event) {
		// System.err.println("BundleEvent has fired");
		//				
		// }
		// });
		// context.addFrameworkListener(new FrameworkListener() {
		//			
		// @Override
		// public void frameworkEvent(FrameworkEvent event) {
		// System.err.println("FrameworkEvent has fired");
		//				
		// }
		// });
		// context.addServiceListener(new ServiceListener() {
		//			
		// @Override
		// public void serviceChanged(ServiceEvent event) {
		// System.err.println("ServiceEvent has fired");
		//				
		// }
		// });
		// context.getBundle().getLocation();
		// Instantiate the static context for the KReS ONM

		Logger log = LoggerFactory.getLogger(this.getClass());
		log.debug("KReS :: Instantiating ONM static context...");
//		if (ONManager.get() != null) {
//			log.debug("KReS :: ONM static context instantiated.");
			log.info("KReS :: Ontology Network Manager set up.");
//		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		LoggerFactory.getLogger(this.getClass()).info(
				"KReS :: Ontology Network Manager brought down.");
	}

}
