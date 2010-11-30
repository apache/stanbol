/**
 * 
 */
package eu.iksproject.rick.servicesapi.yard;

public enum CacheStrategy{
	/**
	 * All entities of this site should be cached
	 */
	all,
	/**
	 * Only entities mapped to symbols should be cached
	 */
	mapped,
	/**
	 * Entities of this site are not cached
	 */
	none
}