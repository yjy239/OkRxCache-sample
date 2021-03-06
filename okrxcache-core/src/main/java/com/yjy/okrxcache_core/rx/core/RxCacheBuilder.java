package com.yjy.okrxcache_core.rx.core;

import android.content.Context;

import com.yjy.okrxcache_core.rx.core.Cache.CacheStragry;
import com.yjy.okrxcache_core.rx.core.Cache.DiskCache.DiskCache;
import com.yjy.okrxcache_core.rx.core.Cache.DiskCache.InternalCacheDiskCacheFactory;
import com.yjy.okrxcache_core.rx.core.Cache.MemoryCache.ItemLruResourceCache;
import com.yjy.okrxcache_core.rx.core.Cache.MemoryCache.MemoryCache;
import com.yjy.okrxcache_core.rx.core.Convert.GsonConvert;
import com.yjy.okrxcache_core.rx.core.Convert.IConvert;
import com.yjy.okrxcache_core.rx.core.Engine.CacheEngine;
import com.yjy.okrxcache_core.rx.core.Engine.RxInterceptor.Interceptor;

import java.util.ArrayList;

/**
 * <pre>
 *     author : yjy
 *     e-mail : yujunyu12@gmail.com
 *     time   : 2018/04/28
 *     desc   :
 *     version: 1.0
 * </pre>
 */
class RxCacheBuilder {

    private String mFilePath;
    private Class<?> mUsingClass;
    private CacheCore mCore;
    private ArrayList<Interceptor> mInterceptors = new ArrayList<>();
    private DiskCache.Factory mDiskCacheFactory;
    private Context mContext;
    private int mDiskSize = 0;
    private IConvert mConvert = new GsonConvert();
    private CacheStragry mCacheStagry;
    private boolean isForce = true;
    private MemoryCache mMemoryCache;



    public RxCacheBuilder(Context context){
        this.mContext = context.getApplicationContext();
    }


    public OkRxCache createCache(){
        if(mDiskCacheFactory == null){
            mDiskCacheFactory = new InternalCacheDiskCacheFactory(mContext,
                    null,0);
        }

        if(mMemoryCache == null){
            mMemoryCache = new ItemLruResourceCache(50);
        }

        if(mConvert == null){
            mConvert = new GsonConvert();
        }

        if(mCacheStagry == null){
            mCacheStagry = CacheStragry.ALL;
        }

        CacheEngine engine = new CacheEngine(mDiskCacheFactory,mMemoryCache,mConvert,mCacheStagry);
        mCore = new CacheCore(engine);

        return new OkRxCache(mCore,engine,mDiskCacheFactory,mMemoryCache,mConvert);
    }






}
