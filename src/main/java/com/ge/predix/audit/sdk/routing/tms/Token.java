package com.ge.predix.audit.sdk.routing.tms;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Sets;
import lombok.Data;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class Token {

    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("expires_in")
    private int secondsToExpire;
    @JsonProperty("scope")
    private String scope;
    @JsonProperty("jti")
    private String jti;
    @JsonIgnore
    private long expirationTime;

    @JsonCreator
    public Token(@JsonProperty("access_token") String accessToken, @JsonProperty("token_type") String tokenType,
                 @JsonProperty("expires_in") int secondsToExpire, @JsonProperty("scope") String scope,
                 @JsonProperty("jti") String jti) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.secondsToExpire = secondsToExpire;
        this.scope = scope;
        this.jti = jti;
        this.expirationTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(this.secondsToExpire) ;
    }

    public boolean shouldExpire(long msToExpired){
        return System.currentTimeMillis() + msToExpired >= expirationTime ;
    }


    public Set<String> scopes() {
        return Optional.ofNullable(scope)
                .map(this::convertToSet)
                .orElseGet(Sets::newHashSet);
    }

    private Set<String> convertToSet(String scope) {
        return Stream.of(scope.trim().split(" "))
                .filter(str -> !str.isEmpty())
                .collect(Collectors.toSet());
    }
}
