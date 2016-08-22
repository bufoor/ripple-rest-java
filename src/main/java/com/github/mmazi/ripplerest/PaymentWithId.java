package com.github.mmazi.ripplerest;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class PaymentWithId implements Serializable {

    @JsonProperty("client_resource_id")
    private String clientResourceId;

    @JsonProperty("hash")
    private String hash;

    private Payment payment;

    public String getClientResourceId() {
        return clientResourceId;
    }

    public Payment getPayment() {
        return payment;
    }

    public String getHash() {
        return hash;
    }
}
