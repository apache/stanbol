package eu.iksproject.fise.servicesapi.helper;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

@Retention(RUNTIME)
public @interface Rdf {
	String id();
}
