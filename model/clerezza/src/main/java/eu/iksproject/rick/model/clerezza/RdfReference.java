/**
 * 
 */
package eu.iksproject.rick.model.clerezza;

import org.apache.clerezza.rdf.core.UriRef;

import eu.iksproject.rick.servicesapi.model.Reference;

public class RdfReference implements Reference,Cloneable {
	private final UriRef uri;
	protected RdfReference(String reference){
		this.uri = new UriRef(reference);
	}
	protected RdfReference(UriRef uri){
		this.uri = uri;
	}
	@Override
	public String getReference() {
		return uri.getUnicodeString();
	}
	public UriRef getUriRef(){
		return uri;
	}
	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new RdfReference(new UriRef(uri.getUnicodeString()));
	}
	@Override
	public int hashCode() {
		return uri.getUnicodeString().hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		return obj != null &&obj instanceof Reference && uri.getUnicodeString().equals(((Reference)obj).getReference());
	}
	@Override
	public String toString() {
		return uri.getUnicodeString();
	}
	
}