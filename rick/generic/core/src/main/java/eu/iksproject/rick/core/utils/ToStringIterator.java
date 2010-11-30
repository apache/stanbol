package eu.iksproject.rick.core.utils;

import java.util.Iterator;

public class ToStringIterator implements Iterator<String> {

	protected final Iterator<?> it;
	public ToStringIterator(Iterator<?> it){
		if(it == null){
			throw new IllegalArgumentException("Parsed iterator MUST NOT be NULL!");
		}
		this.it = it;
	}
	@Override
	public final void remove() {
		it.remove();
	}
	
	@Override
	public final String next() {
		Object next =  it.next();
		return next != null?next.toString():null;
	}
	
	@Override
	public final boolean hasNext() {
		return it.hasNext();
	}
}
