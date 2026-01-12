package ru.practicum.market.integration.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PaymentUris {
    public static final String PAYMENT_BALANCE = "/api/payments/balance";
    public static final String PAYMENT_HOLD = "/api/payments/hold";
    public static final String PAYMENT_CONFIRM = "/api/payments/confirm/";
    public static final String PAYMENT_CANCEL = "/api/payments/cancel/";

}
