package org.apache.stanbol.enhancer.engines.dereference;

/**
 * Exception thrown if the parsed dereference configuration is not valid.
 * Messages should indicate the {@link DereferenceContext} field as well as
 * the Dereferencer implementation
 * @author Rupert Westenthaler
 *
 */
public class DereferenceConfigurationException extends IllegalArgumentException {

    private static final long serialVersionUID = 1431844013656980310L;
    private final Class<? extends EntityDereferencer> dereferencer;
    private final String property;

    
    public DereferenceConfigurationException(String reason, 
            Class<? extends EntityDereferencer> dereferencer, String property){
        this(reason, null, dereferencer, property);
    }
    
    public DereferenceConfigurationException(Throwable cause, 
            Class<? extends EntityDereferencer> dereferencer, String property){
        this(null, cause, dereferencer, property);
    }
    
    public DereferenceConfigurationException(String reason, Throwable cause, 
            Class<? extends EntityDereferencer> dereferencer, String property){
        super(new StringBuilder("IllegalConfiguration for ")
        .append(dereferencer == null ? "Dereferencer <unkown>" : dereferencer.getClass().getSimpleName())
        .append(" and property '").append(property == null ? "<unknwon>" : property)
        .append("': ").append(reason != null ? reason : "").toString(), cause);
        this.dereferencer = dereferencer;
        this.property = property;
    }
    
    public Class<? extends EntityDereferencer> getDereferencer() {
        return dereferencer;
    }
    
    public String getProperty() {
        return property;
    }
    
}
