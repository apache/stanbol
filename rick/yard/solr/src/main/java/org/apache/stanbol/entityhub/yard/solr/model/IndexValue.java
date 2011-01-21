package org.apache.stanbol.entityhub.yard.solr.model;

public final class IndexValue {

    private final String value;
    private final IndexDataType type;
    private final String language;
    private final boolean hasLanguage;
    /**
     * Calculate the hash only once
     */
    private final int _hash;

    public IndexValue(String value,IndexDataType type){
        this(value,type,null,false);
    }

    public IndexValue(String value,IndexDataType type,String language){
        this(value,type,language,true);
    }
    private IndexValue(String value,IndexDataType type,String language,boolean hasLanguage){
        if(value == null || value.isEmpty()){
            throw new IllegalArgumentException("The value MUST NOT be NULL nor empty!");
        }
        this.value = value;
        if(type == null){
            throw new IllegalArgumentException("The IndexType MUST NOT be NULL nor empty!");
        }
        this.type = type;
        this.hasLanguage = hasLanguage;
        if(this.hasLanguage){
            this.language = language;
        } else {
            this.language = null;
        }
        this._hash= type.hashCode()+value.hashCode()+(language!=null?language.hashCode():0);
    }
    public final String getValue(){
        return value;
    }
    public final IndexDataType getType(){
        return type;
    }
    public final String getLanguage(){
        return language;
    }
    public final boolean hasLanguage(){
        return hasLanguage;
    }
    @Override
    public int hashCode() {
        return _hash;
    }
    @Override
    public boolean equals(Object obj) {
        return obj != null &&
            obj instanceof IndexValue &&
            ((IndexValue)obj).value.equals(value) &&
            ((IndexValue)obj).type.equals(type) &&
            ((IndexValue)obj).hasLanguage == hasLanguage && (
                    (language == null && ((IndexValue)obj).language == null) ||
                    (language!=null &&language.equals(((IndexValue)obj).language))
            );
    }
    @Override
    public String toString() {
        return value+(language!=null?("@"+language):"")+"^^"+type;
    }
}
