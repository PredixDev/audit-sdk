package com.ge.predix.audit.sdk.routing.tms;

import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TokenTest {
    Token token = new Token("access", "type", 0, "scope", "jti");

    @Test
    public void scopesWithOneStringReturnSetWithSize1() {
        Set<String> scopes = token.scopes();

        assertEquals(1, scopes.size());
        assertTrue(scopes.contains(token.getScope()));
    }

    @Test
    public void scopesWithTwoStringReturnSetWithSize2() {
        token.setScope("1 2");

        Set<String> scopes = token.scopes();

        assertEquals(2, scopes.size());
        assertEquals(Sets.newHashSet("1", "2"), scopes);
    }

    @Test
    public void scopesWithTwoStringWithSpacesReturnSetWithSize2() {
        token.setScope(" 1 2 ");

        Set<String> scopes = token.scopes();

        assertEquals(2, scopes.size());
        assertEquals(Sets.newHashSet("1", "2"), scopes);
    }

    @Test
    public void scopesWithNullStringReturnEmptySet() {
        token.setScope(null);

        Set<String> scopes = token.scopes();

        assertTrue(scopes.isEmpty());
    }

    @Test
    public void scopesWithEmptyStringReturnEmptySet() {
        token.setScope("");

        Set<String> scopes = token.scopes();

        assertTrue(scopes.isEmpty());
    }

    @Test
    public void scopesWithSpaceStringReturnEmptySet() {
        token.setScope(" ");

        Set<String> scopes = token.scopes();

        assertTrue(scopes.isEmpty());
    }

    @Test
    public void scopesWithDuplicateStringReturnDistinctSet() {
        token.setScope("1 1");

        Set<String> scopes = token.scopes();

        assertEquals(1, scopes.size());
        assertEquals(Sets.newHashSet("1"), scopes);
    }

}