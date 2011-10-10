package org.apache.stanbol.explanation.heuristics;


public class PersonMatcher implements Matcher {

    @Override
    public boolean matches(Entity arg0, Entity arg1) {
        for (Identifier id0 : arg0.getIDs())
            for (Identifier id1 : arg1.getIDs())
                switch (id1.getType()) {
                    case CMS_USERNAME:
                        return id0.getType() == IDTypes.CMS_USERNAME && id0.equals(id1);
                    case FOAF_ID:
                        return id0.getType() == IDTypes.FOAF_ID && id0.equals(id1);
                    case SSN:
                        return id0.getType() == IDTypes.SSN && id0.equals(id1);
                    default:
                        return false;
                }
        return false;
    }

}
