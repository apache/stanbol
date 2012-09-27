package org.apache.stanbol.enhancer.nlp.morpho;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.apache.clerezza.rdf.core.UriRef;
/**
 * Defines verb tenses as defined by the
 * <a href="">OLIA</a> Ontology.<p>
 * The hierarchy is represented by this enumeration.
 * The {@link Set} of parent concepts is accessible via
 * the {@link #getParent()} and {@link #getTenses()}.
 */
public enum Tense {
    NotAnchored("NotTemporallyAnchored"),
    Absolute("AbsoluteTense"),
    CloseFuture(Absolute),
    Future(Absolute),
    HodiernalFuture(Future),
    ImmediateFuture(Future),
    NearFuture(Future),
    PostHodiernalFuture(Future),
    RemoteFuture(Future),
    SimpleFuture(Future),
    Past(Absolute),
    HesternalPast(Past),
    HodiernalPast(Past),
    ImmediatePast(Past),
    RecentPast(Past),
    RemotePast(Past),
    SimplePast(Past),
    StillPast(Past),
    Imperfect(StillPast),
    Aorist(Past),
    Perfect(Absolute),
    PreHodiernalPast(Absolute),
    Present(Absolute),
    Transgressive(Present),
    AbsoluteRelative("AbsoluteRelativeTense"),
    FutureInFuture(AbsoluteRelative),
    FutureInPast(AbsoluteRelative),
    PastPerfect("PastPerfectTense",AbsoluteRelative),
    PastInFuture(AbsoluteRelative),
    PluperfectTense(AbsoluteRelative),
    Relative("RelativeTense"),
    FuturePerfect(Relative),
    RelativePast(Relative),
    RelativePresent(Relative),
    ;
    static final String OLIA_NAMESPACE = "http://purl.org/olia/olia.owl#";
    UriRef uri;
    Set<Tense> tenses;
    Tense parent;
    
    Tense() {
        this(null,null);
    }
    Tense(Tense parent) {
        this(null,parent);
    }

    Tense(String name) {
        this(name,null);
    }
    Tense(String name,Tense parent) {
        uri = new UriRef(OLIA_NAMESPACE + (name == null ? name() : name));
        this.parent = parent;
        EnumSet<Tense> tenses  = EnumSet.of(this);
        if(parent != null){
            tenses.addAll(parent.tenses);
        }
        this.tenses = Collections.unmodifiableSet(tenses);
    }
    /**
     * Getter for the parent tense (e.g.
     * {@link Tense#Future} for {@link Tense#NearFuture})
     * @return the direct parent or <code>null</code> if none
     */
    public Tense getParent() {
        return parent;
    }
    
    /**
     * Returns the transitive closure over
     * the {@link #getParent() parent} tenses including
     * this instance (e.g.
     * [{@link Tense#Absolute}, {@link Tense#Future}, {@link Tense#NearFuture}] for
     * {@link Tense#NearFuture}).<p>
     * Implementation Note: Internally an {@link EnumSet} is used 
     * to represent the transitive closure. As the iteration order
     * of an {@link EnumSet} is based on the natural order (the
     * {@link Enum#ordinal()} values) AND the ordering of the
     * Tenses in this enumeration is from generic to specific the
     * ordering of the Tenses in the returned Set is guaranteed
     * to be from generic to specific.
     * @return the transitive closure over parent
     * tenses.
     */
    public Set<Tense> getTenses() {
        return tenses;
    }
    
    public UriRef getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return uri.getUnicodeString();
    }
}
