package org.apache.stanbol.explanation;

import static org.junit.Assert.*;

import org.apache.stanbol.explanation.heuristics.CurrentUser;
import org.apache.stanbol.explanation.heuristics.Entity;
import org.apache.stanbol.explanation.heuristics.IDTypes;
import org.apache.stanbol.explanation.heuristics.Identifier;
import org.apache.stanbol.explanation.heuristics.PersonMatcher;
import org.apache.stanbol.explanation.impl.AbstractIdentifier;
import org.apache.stanbol.explanation.impl.BasicEntity;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPersonMatching {

    private static Entity someGuy;

    private static Identifier ssn = new AbstractIdentifier(IDTypes.SSN, "ABC123DEF456GHI789");

    private static Identifier ssn2 = new AbstractIdentifier(IDTypes.SSN, "ABC123DEF456GHI789");

    @BeforeClass
    public static void setup() {
        someGuy = new BasicEntity(null);
        someGuy.addID(ssn);
    }

    @Test
    public void testIsYou() {
        Entity you = new CurrentUser(null);
        you.addID(ssn2);
        assertTrue(new PersonMatcher().matches(you, someGuy));
    }

}
