package com.github.mmazi.ripplerest;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class PaymentClient {
    private static final String USER_ADDRESS = "rNsa4RDNCwEPUcST8zMTQhqBrfABdWkzQF";
    private static final String USER_ADDRESS_SECRET = "snJTPWZCkoW753VuFZT6SqhWhch9w";
    public static final String IPE_ADDRESS = "rUurnm8KNwggqTWHoS4nkhBuXqQR2PggXW";

    public static void main(String[] args) throws IOException {
        System.out.println(System.getProperty("java.version"));
        sendPayment(105);
//        processIncome();
//        receivePayments();
    }

    private static void processIncome() throws IOException {
        //        String ibLastTransferIn = "1471934111000";
        String ibLastTransferIn = "1471952171000";
        String lastTransactionId = ibLastTransferIn;
        Ripple ripple = RippleClientFactory.createClient("https://api.altnet.rippletest.net:5990");
        PaymentsResponse paymentResponse = ripple.getPayments(
            IPE_ADDRESS,
                null,
                null,
                true,
                0,
                null,
                false,
                10000,
                1
        );
        Collections.reverse(paymentResponse.getPayments());
        for (PaymentWithId item: paymentResponse.getPayments()) {
            System.out.println(item.getHash() + "  " + item.getPayment().getTimestamp() + "  " + item.getPayment().getTimestamp().getTime());
        }

        System.out.println("0----------------------" + ibLastTransferIn);
        ibLastTransferIn = processing(ibLastTransferIn, paymentResponse);

        System.out.println("1---------------------- " + ibLastTransferIn);
        ibLastTransferIn = processing(ibLastTransferIn, paymentResponse);

        System.out.println("2---------------------- " + ibLastTransferIn);
        ibLastTransferIn = processing(ibLastTransferIn, paymentResponse);
    }

    private static String processing(String ibLastTransferIn, PaymentsResponse paymentResponse) {
        String lastTransactionId = ibLastTransferIn;
        for (PaymentWithId item: paymentResponse.getPayments()) {
            if(paymentExists(ibLastTransferIn, item)) {
                System.out.println("skipped: " + item.getHash());
                continue;
            }
            lastTransactionId = getTimestampAsString(item);
            System.out.println("processing: " + item.getHash());
        }
        return lastTransactionId;
    }

    private static String getTimestampAsString(PaymentWithId item) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(item.getPayment().getTimestamp());
        return String.valueOf(cal.getTimeInMillis());
    }

    private static boolean paymentExists(String ibLastTransferIn, PaymentWithId item) {
        if(ibLastTransferIn == null) {
            return false;
        }
        Long current = prepareUTCLong(item.getPayment().getTimestamp());
        boolean b = !(current.compareTo(new Long(ibLastTransferIn)) > 0);
        return b;
    }

    public static long prepareUTCLong(Date completed) {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTime(completed);
        return calendar.getTimeInMillis();
    }



    private static void receivePayments() throws IOException {
        Ripple ripple = RippleClientFactory.createClient("https://api.altnet.rippletest.net:5990");
        final PaymentsResponse paymentsResponse = ripple.getPayments(IPE_ADDRESS, null, null, true, null, null, true, 10, 1);
        final List<PaymentWithId> payments = paymentsResponse.getPayments();
        System.out.println("Got " + payments.size() + " payments.");

        System.out.println("----------------------------");
        for (PaymentWithId payment : payments) {
            System.out.println("hash: " + payment.getHash());
            System.out.println("result: " + payment.getPayment().getResult());
            System.out.println("value: " + payment.getPayment().getDirection());
            System.out.println("timestamp: " + payment.getPayment().getTimestamp());
        }
    }

    private static void sendPayment(int amount1) throws IOException {
        Ripple ripple = RippleClientFactory.createClient("https://api.altnet.rippletest.net:5990");
        final String uuid = createUUID();
        final BigDecimal value = BigDecimal.valueOf(amount1);
        final Amount amount = new Amount(value, "XRP");
        final CreatePaymentResponse createPaymentResponse = ripple.createPayment(
            USER_ADDRESS,
                new PaymentRequest(USER_ADDRESS_SECRET, uuid,
                         new Payment(
                             USER_ADDRESS,
                                IPE_ADDRESS,
                                amount,
                                null,
                                BigDecimal.valueOf(0.02),
                                2L,
                                3L,
                                null,
                                false,
                                false,
                                null
        )));
        System.out.println("Payment status url: " + createPaymentResponse.getStatusUrl());
        PaymentResponse paymentResponse = ripple.getPayment(USER_ADDRESS, uuid);
        Payment payment = paymentResponse.getPayment();
        final String pmtHash = paymentResponse.getHash();
        paymentResponse = ripple.getPayment(USER_ADDRESS, pmtHash);
        payment = paymentResponse.getPayment();

        System.out.println("amount: " + payment.getSourceAmount());
        System.out.println("sourceAccount: " + payment.getSourceAccount());
        System.out.println("sourceAmount: " + payment.getSourceAccount());
        System.out.println("destinationAccount: " + payment.getDestinationAccount());
        System.out.println("paymentFee: " + payment.getFee().doubleValue());
        System.out.println("paymentHash: " + payment.getHash());
        System.out.println("responseHash: " + createPaymentResponse.getValue());

    }

    private static String createUUID() {
        return UUID.randomUUID().toString();
    }

}
