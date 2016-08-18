package com.github.mmazi.ripplerest;

public class PaymentResponse extends RippleResponse<Payment> {

    private String hash;
    private String ledger;
    private String state;
    private Payment payment;

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getLedger() {
        return ledger;
    }

    public void setLedger(String ledger) {
        this.ledger = ledger;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Payment getPayment() {
        return payment;
    }

    @Override
    public Payment getValue() {
        return getPayment();
    }
}
