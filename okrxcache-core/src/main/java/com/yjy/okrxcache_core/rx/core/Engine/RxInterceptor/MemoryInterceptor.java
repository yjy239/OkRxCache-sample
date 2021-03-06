package com.yjy.okrxcache_core.rx.core.Engine.RxInterceptor;


import android.util.Log;

import com.yjy.okrxcache_core.rx.core.Cache.CacheStragry;
import com.yjy.okrxcache_core.rx.core.Cache.MemoryCacheCallBack;
import com.yjy.okrxcache_core.rx.core.CacheCore;
import com.yjy.okrxcache_core.rx.core.CacheResult;
import com.yjy.okrxcache_core.rx.core.Engine.InterceptorMode;
import com.yjy.okrxcache_core.rx.core.Request.Request;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * <pre>
 *     author : yjy
 *     e-mail : yujunyu12@gmail.com
 *     time   : 2018/04/26
 *     desc   :interceptor for MemoryInterceptor
 *     version: 1.0
 * </pre>
 */

public class MemoryInterceptor<T> implements Interceptor {

    private CacheStragry mCacheStagry;
    private int mMode = 0;
    private MemoryCacheCallBack mEngine;

    private static final String TAG = "MemoryInterceptor";


    public MemoryInterceptor(MemoryCacheCallBack mMemory,CacheStragry cacheStagry){
        this.mCacheStagry = cacheStagry;
        this.mEngine = mMemory;
    }

    @Override
    public Observable intercept(Interceptor.Chain chain) {
//        Log.e("MemoryInterceptor","MemoryInterceptor");
        Request request = chain.request();

        Observable memoryObservale = loadFromMemoryCache(request);


        //判断拦截器执行模式
        if(mMode == InterceptorMode.GET){

            return Observable.concat(memoryObservale,chain.process());
        }else if(mMode == InterceptorMode.SAVE){
            return chain.process().compose(isSucessSaveFromDisk(request));
        }else if(mMode == InterceptorMode.REMOVE){
            return chain.process().compose(deleteResultIsSuccess(request));
        }else if(mMode == InterceptorMode.CLEAR){
            return chain.process().compose(clearCacheIsSuccess());
        }

        if(mMode == InterceptorMode.RUN){
            if(mCacheStagry == CacheStragry.ALL){
                //优先缓存显示,之后会显示网络
//                return getRealData(chain.process().compose(save2CacheResult(request)));
                return Observable.merge(getRealData(memoryObservale),getRealData(chain.process().compose(save2CacheResult(request))));
            }else if(mCacheStagry == CacheStragry.FIRSTCACHE){
                //优先显示缓存，找到了就不找网络
                return Observable.concat(getRealData(memoryObservale),getRealData(chain.process().compose(save2CacheResult(request))));
            }else if(mCacheStagry == CacheStragry.ONLYNETWORK){
                //只获取网络
                return getRealData(chain.process().compose(save2CacheResult(request)));
            }else if(mCacheStagry == CacheStragry.ONLYMEMORY){
                return getRealData(memoryObservale);
            }else {
                return getRealData(chain.process());
            }
        }


        return getRealData(chain.process());
    }

    @Override
    public void setMode(int mode) {
        this.mMode = mode;
    }

    /**
     * 转化为真正的result
     * @param observable
     * @return
     */
    private Observable getRealData(Observable observable){
        return observable.map(new Func1<CacheResult<T>, T>() {
            @Override
            public T call(CacheResult<T> tCacheResult) {
                return tCacheResult.getData();
            }
        });
    }

    /**
     * 从Memory中读取
     * @param request
     * @return
     */
    private Observable loadFromMemoryCache(final Request request){
        return Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                CacheResult result =null;
                //判断缓存是否存在
                result = mEngine.loadFromCache(request.getKey(),true);
//                Log.e("MemoryInterceptor","Memory");

                if(request == null){
                    //获取活跃的资源是否存在
                    result = mEngine.loadFromActiveResources(request.getKey(),true);
                }
                if(result != null){
                    if(!mCacheStagry.isOutDate() &&result.getLifeTime()+result.getCurrentTime() < System.currentTimeMillis()) {
                        Log.e(TAG,"result is outdate"+result.toString());
                        result = null;
                    }

                    if(result != null){
                        request.setHadGetCache(true);
                    }else {

                    }

//                    Log.e("MemoryInterceptor","result "+result);
                    subscriber.onNext(result);
                }

                subscriber.onCompleted();


            }
        });
    }

    /**
     * 判断上个拦截器是否保存成功
     * @param request
     * @return
     */
    private Observable.Transformer<Boolean,Boolean> isSucessSaveFromDisk(final Request request){
        return new Observable.Transformer<Boolean, Boolean>() {
            @Override
            public Observable<Boolean> call(Observable<Boolean> observable) {

                return observable.map(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean aBoolean) {
                        if(!aBoolean){
                            Log.e("MemoryInterceptor","Disk save failed");
                        }
                        return mEngine.complete(request.getKey(),request.getResult());
                    }
                });
            }
        };
    }


    /**
     * 网络拦截器返回之后，转化为cacheresult，并且保存
     * @param request
     * @param <T>
     * @return
     */
    private <T>Observable.Transformer<T,CacheResult<T>> save2CacheResult(final Request request){
        return new Observable.Transformer<T, CacheResult<T>>() {
            @Override
            public Observable<CacheResult<T>> call(Observable<T> tObservable) {

                return tObservable.map(new Func1<T, CacheResult<T>>() {
                    @Override
                    public CacheResult<T> call(T t) {
//                        Log.e("MemoryInterceptor","save2CacheResult");
                        if(t ==null){
                            throw new IllegalArgumentException(" network error");
                        }else {
                            mEngine.complete(request.getKey(),(CacheResult)t);
                            request.setResult((CacheResult)t);
                        }


                        return (CacheResult)t;
                    }
                });
            }
        };
    }

    /**
     * 删除成功的转化器
     * @param <T>
     * @return
     */
    private <T>Observable.Transformer<T,Boolean> clearCacheIsSuccess(){
        return new Observable.Transformer<T, Boolean>() {
            @Override
            public Observable<Boolean> call(Observable<T> tObservable) {

                return tObservable.map(new Func1<T, Boolean>() {
                    @Override
                    public Boolean call(T t) {
//                        Log.e("MemoryInterceptor","clearCacheIsSuccess");
                        return mEngine.clear();
                    }
                });
            }
        };
    }

    /**
     * 删除成功的转化器
     * @param request
     * @param <T>
     * @return
     */
    private <T>Observable.Transformer<T,Boolean> deleteResultIsSuccess(final Request request){
        return new Observable.Transformer<T, Boolean>() {
            @Override
            public Observable<Boolean> call(Observable<T> tObservable) {

                return tObservable.map(new Func1<T, Boolean>() {
                    @Override
                    public Boolean call(T t) {
//                        Log.e("MemoryInterceptor","deleteResultIsSuccess");
                        return mEngine.remove(request.getKey());
                    }
                });
            }
        };
    }
}
