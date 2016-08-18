package com.github.mmazi.ripplerest;

public class SettingsResponse extends RippleResponse<AccountSettings> {

    private AccountSettings settings;

    private String hash;

    private Long ledger;

    private String state;

    public AccountSettings getSettings() {
        return settings;
    }

    @Override
    public AccountSettings getValue() {
        return settings;
    }

    public String getHash() {
        return hash;
    }

    public Long getLedger() {
        return ledger;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
