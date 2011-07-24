package org.apache.stanbol.ontologymanager.registry.api.model;

/**
 * The possible policies a registry manager can adopt for distributed caching.
 */
public enum CachingPolicy {

    /**
     * A single ontology manager will be used for all known registries, which implies that only one possible
     * version of each ontology can be loaded at one time.
     */
    CROSS_REGISTRY,

    /**
     * Every registry is assigned its own ontology manager for caching ontologies once they are loaded. If a
     * library is referenced across multiple registries, an ontology set will be instantiated for each.
     */
    PER_REGISTRY;
}
