package org.apache.stanbol.reasoners.servicesapi.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Documentation {
 String name();
 String description();
 String file() default "";
}
