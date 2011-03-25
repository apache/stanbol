package org.apache.stanbol.rules.manager.atoms;

import org.apache.stanbol.rules.base.api.URIResource;

public class TypedLiteralAtom {

	private Object value;
	private URIResource xsdType;
	
	public TypedLiteralAtom(Object value, URIResource xsdType) {
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
