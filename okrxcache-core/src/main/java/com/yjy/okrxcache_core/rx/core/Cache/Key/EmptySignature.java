package com.yjy.okrxcache_core.rx.core.Cache.Key;

/**
 * Created by software1 on 2018/2/2.
 */

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

/**
 * An empty key that is always equal to all other empty keys.
 */
public final class EmptySignature implements Key {
    private static final EmptySignature EMPTY_KEY = new EmptySignature();

    public static EmptySignature obtain() {
        return EMPTY_KEY;
    }

    private EmptySignature() {
        // Empty.
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) throws UnsupportedEncodingException {
        // Do nothing.
    }
}
