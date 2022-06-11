package com.pblinov.binance.futures.api;

import junit.framework.TestCase;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static com.pblinov.binance.futures.api.SignatureUtils.sign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class SignatureUtilsTest extends TestCase {
    public void testSign() throws NoSuchAlgorithmException, InvalidKeyException {
        assertThat(sign("symbol=BTCUSDT&side=BUY&type=LIMIT&quantity=1&price=9000&timeInForce=GTC&recvWindow=5000&timestamp=1591702613943",
                "2b5eb11e18796d12d88f13dc27dbbd02c2cc51ff7059765ed9821957d82bb4d9"),
                is("3c661234138461fcc7a7d8746c6558c9842d4e10870d2ecbedf7777cad694af9"));
    }
}