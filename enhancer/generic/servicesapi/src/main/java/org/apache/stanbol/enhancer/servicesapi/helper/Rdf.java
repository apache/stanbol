package org.apache.stanbol.enhancer.servicesapi.helper;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

@Retention(RUNTIME)
public @interface Rdf {
    String id();
}
