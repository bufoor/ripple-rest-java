package com.github.mmazi.ripplerest;

public class AddTrustlineResponse extends RippleResponse<Trustline> {

    private String hash;

    private Long ledger;

    private String state;

    private Trustline trustline;

    public Trustline getTrustline() {
        return trustline;
    }

    public String getHash() {
        return hash;
    }

    public Long getLedger() {
        return ledger;
    }

    @Override
    public Trustline getValue() {
        return getTrustline();
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
