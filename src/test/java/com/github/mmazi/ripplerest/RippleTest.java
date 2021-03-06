package com.github.mmazi.ripplerest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

import static org.testng.Assert.fail;

public class RippleTest {

    private static final String ADDRESS1 = "rEQVHJhVo74wX5UxoNZJTQP6iiUwNBKV8";
    private static final String ADDRESS2 = "rKvRwBxwfxjLkudrdwwRnNwpuYpb6jspfj";
    private static final String ADDRESS1_SECRET = "ssz1egtw9EoqNGswCX2bn7AiAyWwH";
    private static final String EXISTING_PAYMENT_HASH = "8B4E584DB98DAC10BC48FC387A785FB06B32B50510D0FFB88B40AC47219A0379";
    private static final Pattern UUID_PATTERN = Pattern.compile("([a-f\\d]{8}(-[a-f\\d]{4}){3}-[a-f\\d]{12}?)");
    private static final Logger log = LoggerFactory.getLogger(RippleTest.class);
    private static final Random RANDOM = new Random();
    private Ripple ripple;

    @BeforeClass
    private void createClient() {
        ripple = RippleClientFactory.createClient("https://api.altnet.rippletest.net:5990");
    }

    @Test
    public void testError() throws Exception {
        try {
            ripple.getBalances("illegalAddress");
            fail();
        } catch (RippleException e) {
            Assert.assertEquals(e.getMessage(), "Parameter is not a valid Ripple address: account");
        }
    }

    @Test
    public void testGenerateUuid() throws Exception {
        final UuidResponse uuidResponse = ripple.generateUuid();
        assertResponse(uuidResponse);
        final String uuid = uuidResponse.getUuid();
        log.info("Generated UUID: {}", uuid);
        Assert.assertTrue(UUID_PATTERN.matcher(uuid).matches());
    }

    @Test
    public void testGetBalances() throws Exception {
        final BalancesResponse balancesResponse = ripple.getBalances(ADDRESS1);
        assertResponse(balancesResponse);
        final List<Amount> balances = balancesResponse.getBalances();
        Assert.assertFalse(balances.isEmpty());
        boolean xrpFound = false;
        log.info("Balances:");
        for (Amount balance : balances) {
            log.debug("{}", balance);
            if (balance.getCurrency().equals("XRP")) {
                xrpFound = true;
                Assert.assertTrue(balance.getValue().intValue() > 0);
            }
        }
        Assert.assertTrue(xrpFound);
    }

    @Test
    public void testGetSettings() throws Exception {
        final SettingsResponse balancesResponse = ripple.getSettings(ADDRESS1);
        assertResponse(balancesResponse);
        final AccountSettings settings = balancesResponse.getSettings();
        Assert.assertTrue(settings.getTransactionSequence() >= 1);
    }

    @Test
    public void testSetSettings() throws Exception {
        AccountSettings settings = ripple.getSettings(ADDRESS1).getSettings();
        final Boolean originalValue = settings.getRequireDestinationTag();
        testSetRequireDestinationTag(settings, !originalValue);
        testSetRequireDestinationTag(settings, originalValue);
    }

    @Test
    public void testSetSettingsNew() throws Exception {
        AccountSettings settings = new AccountSettings();
        testSetRequireDestinationTag(settings, true);
    }

    private void testSetRequireDestinationTag(AccountSettings settings, Boolean set) throws IOException {
        settings.setRequireDestinationTag(set);
        settings.setNoFreeze(null);
        final String uuid = createUUID();
        final SettingsResponse result = ripple.setSettings(ADDRESS1, new SetSettingsRequest(ADDRESS1_SECRET, uuid, settings));
        assertResponse(result);
        Assert.assertEquals(result.getSettings().getRequireDestinationTag(), set);
        settings = ripple.getSettings(ADDRESS1).getSettings();
        Assert.assertEquals(settings.getRequireDestinationTag(), set);
    }

    @Test
    public void testPayment() throws Exception {
        final String uuid = createUUID();
//        final String uuid = ripple.generateUuid().getUuid();
        final BigDecimal value = BigDecimal.valueOf(RANDOM.nextInt(8) + 1);
        final Amount amount = new Amount(value, "XRP");
        final CreatePaymentResponse createPaymentResponse = ripple.createPayment(ADDRESS1, new PaymentRequest(ADDRESS1_SECRET, uuid, new Payment(
                ADDRESS1, ADDRESS2, amount, null, BigDecimal.valueOf(0.02), 2L, 3L, null, false, false, null
        )));
        assertResponse(createPaymentResponse);
        Assert.assertNotNull(createPaymentResponse.getStatusUrl());
        log.info("Payment status url: {}", createPaymentResponse.getStatusUrl());
        PaymentResponse paymentResponse = ripple.getPayment(ADDRESS1, uuid);
        assertResponse(paymentResponse);
        Payment pmt = paymentResponse.getPayment();
        Assert.assertEquals(pmt.getSourceAmount().getValue(), value);
        final String pmtHash = paymentResponse.getHash();
        paymentResponse = ripple.getPayment(ADDRESS1, pmtHash);
        assertResponse(paymentResponse);
        pmt = paymentResponse.getPayment();
        Assert.assertEquals(pmt.getSourceAmount(), amount);
        Assert.assertEquals(pmt.getSourceAccount(), ADDRESS1);
        Assert.assertEquals(pmt.getDestinationAmount(), amount);
        Assert.assertEquals(pmt.getDestinationAccount(), ADDRESS2);
        Assert.assertTrue(pmt.getFee().doubleValue() > 0);
//        Assert.assertEquals(paymentResponse.getClientResourceId(), uuid); // actual: null
    }

    @Test
    public void testGetPayments() throws Exception {
        final PaymentsResponse paymentsResponse = ripple.getPayments(ADDRESS1, ADDRESS1, ADDRESS2, false, null, null, true, 10, 1);
        assertResponse(paymentsResponse);
        final List<PaymentWithId> payments = paymentsResponse.getPayments();
        log.info("Got {} payments.", payments.size());
        Assert.assertFalse(payments.isEmpty());
        for (PaymentWithId paymentWithId : payments) {
            final Payment pmt = paymentWithId.getPayment();
            log.debug("payment: {}", pmt);
            Assert.assertTrue(Arrays.asList(pmt.getSourceAccount(), pmt.getDestinationAccount()).contains(ADDRESS1));
            Assert.assertTrue(pmt.getSourceAmount().getValue().floatValue() > 0);
            assertSensiblePastTimestamp(pmt.getTimestamp());
        }
    }

    @Test(enabled = false)
    public void testTrustlines() throws Exception {
        TrustlinesResponse trustlinesResponse = ripple.getTrustlines(ADDRESS1, null, null);
        assertResponse(trustlinesResponse);
        final int initialTrustlines = trustlinesResponse.getTrustlines().size();
        log.debug("initialTrustlines = {}", initialTrustlines);

        final BigDecimal value = new BigDecimal(RANDOM.nextInt(7) + 1);
        final String clientResourceId = createUUID();
        final AddTrustlineResponse addTrustlineResponse =
                ripple.addTrustline(ADDRESS1, AddTrustlineRequest.create(ADDRESS1_SECRET, true, new Trustline(value, "USD", ADDRESS2), clientResourceId));
        assertResponse(addTrustlineResponse);

        final Trustline addedTrustline = addTrustlineResponse.getTrustline();
        Assert.assertEquals(addedTrustline.getAccount(), ADDRESS1);
        Assert.assertEquals(addedTrustline.getCounterparty(), ADDRESS2);
        Assert.assertEquals(addedTrustline.getCurrency(), "USD");
        Assert.assertEquals(addedTrustline.getLimit(), value);

        trustlinesResponse = ripple.getTrustlines(ADDRESS1, null, null);
        assertResponse(trustlinesResponse);
        final List<Trustline> finalTrustlines = trustlinesResponse.getTrustlines();

        boolean addedFound = false;
        for (Trustline finalTrustline : finalTrustlines) {
            log.debug("trustline: {}", finalTrustline);
            if (equalTrustlines(finalTrustline, addedTrustline)) {
                addedFound = true;
            }
        }
        Assert.assertTrue(addedFound);
    }

    @Test
    public void testGetNotification() throws Exception {
        final NotificationResponse notificationResponse = ripple.getNotifictaion(ADDRESS2, EXISTING_PAYMENT_HASH);
        assertResponse(notificationResponse);
        final Notification notification = notificationResponse.getNotification();
        Assert.assertEquals(notification.getHash(), EXISTING_PAYMENT_HASH);
        Assert.assertEquals(notification.getAccount(), ADDRESS2);
        assertSensiblePastTimestamp(notification.getTimestamp());
        Assert.assertTrue(notification.getTransactionUrl().contains(EXISTING_PAYMENT_HASH));
    }

    @Test
    public void testFindPaths() throws Exception {
        final Currencies sourceCurrencies = Currencies.of(CurrencyAndIssuer.XRP, CurrencyAndIssuer.instance("USD"));
        final Amount amount = new Amount(BigDecimal.ONE, "USD");
        final PathsResponse paths = ripple.findPaths(ADDRESS2, ADDRESS1, amount, sourceCurrencies);
        assertResponse(paths);
        final List<Payment> payments = paths.getPayments();
        Assert.assertFalse(payments.isEmpty());
        log.info("Path payments:");
        for (Payment payment : payments) {
            log.debug("{}", payment);
            Assert.assertEquals(payment.getSourceAmount(), amount);
            Assert.assertEquals(payment.getSourceAccount(), ADDRESS2);
            Assert.assertEquals(payment.getDestinationAmount(), amount);
            Assert.assertEquals(payment.getDestinationAccount(), ADDRESS1);
        }
    }

    @Test
    public void testGetTransactionDetails() throws Exception {
        final TransactionResponse transactionDetails = ripple.getTransactionDetails(EXISTING_PAYMENT_HASH);
        assertResponse(transactionDetails);
        final Transaction tx = transactionDetails.getTransaction();
        Assert.assertNotNull(tx);
        Assert.assertNotNull(tx.getAccount());
        Assert.assertNotNull(tx.getAmount());
        Assert.assertNotNull(tx.getDestination());
        Assert.assertNotNull(tx.getFee());
        Assert.assertNotNull(tx.getFlags());
        Assert.assertNotNull(tx.getLastLedgerSequence());
        Assert.assertNotNull(tx.getLedgerIndex());
        Assert.assertNotNull(tx.getSigningPubKey());
        Assert.assertNotNull(tx.getTransactionType());
        Assert.assertNotNull(tx.getTxnSignature());
        Assert.assertNotNull(tx.getHash());
        Assert.assertNotNull(tx.getInLedger());
        Assert.assertNotNull(tx.getLedgerIndex());
        Assert.assertNotNull(tx.getMeta());
        Assert.assertNotNull(tx.getValidated());
        Assert.assertNotNull(tx.getDate());
        Assert.assertTrue(tx.getDate().before(new Date()));
    }

    @Test
    public void testGetServerInfo() throws Exception {
        final ServerInfoResponse serverInfoResponse = ripple.getServerInfo();
        assertResponse(serverInfoResponse);
        Assert.assertTrue(serverInfoResponse.getRippledServerUrl().contains("://"));
        final RippledServerStatus rippledServerStatus = serverInfoResponse.getRippledServerStatus();
        Assert.assertTrue(rippledServerStatus.getLastClose().getConvergeTimeS() < 100);
        Assert.assertTrue(rippledServerStatus.getValidatedLedger().getSeq() >= 6404992);
    }

    @Test
    public void testIsServerConnected() throws Exception {
        final ConnectedResponse serverConnected = ripple.isServerConnected();
        assertResponse(serverConnected);
        Assert.assertTrue(serverConnected.isConnected());
    }

    private void assertSensiblePastTimestamp(Date date) {
        Assert.assertTrue(date.getTime() < System.currentTimeMillis());
        Assert.assertTrue(date.getTime() > 1399046367);
    }

    private String createUUID() {
        return UUID.randomUUID().toString();
    }

    private void assertResponse(RippleResponse response) {
        Assert.assertNotNull(response);
        final Boolean success = response.getSuccess();
        if (!success) {
            log.error("Request failed; error: {}; message: {}", response.getError(), response.getMessage());
        }
        Assert.assertTrue(success, "Request failed: " + response.getError() + ": " + response.getMessage());
        Assert.assertNull(response.getError());
        Assert.assertNull(response.getMessage());
        Assert.assertTrue(response.getAdditionalProperties().isEmpty());

        final Object value = response.getValue();
        log.debug("Parsed response payload: {}", value);
        Assert.assertNotNull(value);
        if (value instanceof HasAdditionalProperties) {
            final Map<String, Object> additionalProperties = ((HasAdditionalProperties) value).getAdditionalProperties();
            Assert.assertTrue(additionalProperties.isEmpty(), "Any additional json properties should probably be mapped to bean properties: " + additionalProperties.toString());
        }
    }

    private static boolean equalTrustlines(Trustline tl1, Trustline tl2) {
        if (tl1 == tl2) return true;
        if (tl2 == null) return false;

        if (tl1.getAccount() != null ? !tl1.getAccount().equals(tl2.getAccount()) : tl2.getAccount() != null) return false;
        if (tl1.getAccountAllowsRippling() != null ? !tl1.getAccountAllowsRippling().equals(tl2.getAccountAllowsRippling()) : tl2.getAccountAllowsRippling() != null)
            return false;
        if (tl1.getAuthorizedByAccount() != null ? !tl1.getAuthorizedByAccount().equals(tl2.getAuthorizedByAccount()) : tl2.getAuthorizedByAccount() != null)
            return false;
        if (tl1.getAuthorizedByCounterparty() != null ? !tl1.getAuthorizedByCounterparty().equals(tl2.getAuthorizedByCounterparty()) : tl2.getAuthorizedByCounterparty() != null)
            return false;
        if (tl1.getCounterparty() != null ? !tl1.getCounterparty().equals(tl2.getCounterparty()) : tl2.getCounterparty() != null)
            return false;
//        if (counterpartyAllowsRippling != null ? !counterpartyAllowsRippling.equals(trustline.counterpartyAllowsRippling) : trustline.counterpartyAllowsRippling != null)
//            return false;
        if (tl1.getCurrency() != null ? !tl1.getCurrency().equals(tl2.getCurrency()) : tl2.getCurrency() != null) return false;
        if (tl1.getHash() != null ? !tl1.getHash().equals(tl2.getHash()) : tl2.getHash() != null) return false;
        if (tl1.getLedger() != null ? !tl1.getLedger().equals(tl2.getLedger()) : tl2.getLedger() != null) return false;
        if (tl1.getLimit() != null ? !tl1.getLimit().equals(tl2.getLimit()) : tl2.getLimit() != null) return false;
        if (tl1.getPrevious() != null ? !equalTrustlines(tl1.getPrevious(), tl2.getPrevious()) : tl2.getPrevious() != null) return false;
        // different for some reason (0 != null)
//        if (tl1.getReciprocatedLimit() != null ? !tl1.getReciprocatedLimit().equals(tl2.getReciprocatedLimit()) : tl2.getReciprocatedLimit() != null)
//            return false;
        return true;
    }
}