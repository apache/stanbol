package eu.iksproject.kres.rules.atoms;

import eu.iksproject.kres.api.rules.URIResource;

public class KReSTypedLiteral {

	private Object value;
	private URIResource xsdType;
	
	public KReSTypedLiteral(Object value, URIResource xsdType) {
		this.value = value;
		this.xsdType = xsdType;
	}
	
	
	public Object getValue() {
		return value;
	}
	
	public URIResource getXsdType() {
		return xsdType;
	}
	
	
	@Override
	public String toString() {
		
		if(xsdType != null){
			return value + "^^" + xsdType.toString();
		}
		else{
			return value.toString();
		}
	}
}
