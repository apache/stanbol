package org.apache.stanbol.enhancer.servicesapi;

/**
 * Exection thorwn if the an Enhancement Property parsed to an engine is
 * missing or has an invalid value
 * 
 * @since 0.12.1
 *
 */
public class EnhancementPropertyException extends EngineException {
    
    private static final long serialVersionUID = 1L;
    
    private String property;

    public EnhancementPropertyException(EnhancementEngine ee, ContentItem ci, String property, String reason, Throwable t){
        super(ee,ci, new StringBuilder("Enhancement Property '")
        .append(property).append("' - ").append(reason).toString(),t);
        this.property = property;
        
    }
    
    public String getProperty() {
        return property;
    }

    
}
