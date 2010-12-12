package eu.iksproject.rick.servicesapi.query;

import java.util.Arrays;

import eu.iksproject.rick.servicesapi.defaults.DataTypeEnum;


public class ReferenceConstraint extends ValueConstraint {


    public ReferenceConstraint(String reference) {
        super(reference,Arrays.asList(DataTypeEnum.Reference.getUri()));
        if(reference == null){
            throw new IllegalArgumentException("Parsed Reference MUST NOT be NULL");
        }
    }

    /**
     * Getter for the Reference
     * @return the reference
     */
    public String getReference() {
        return (String)getValue();
    }


}
