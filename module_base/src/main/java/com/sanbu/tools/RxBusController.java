package com.sanbu.tools;

import java.util.HashMap;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subscriptions.CompositeSubscription;

/**
 * @author Tom
 *         <p>
 *         使用准则 （类结尾对应三个使用方法）
 *         1. 订阅事件监听 ——> doAsyncSubscribe()
 *         2. 发送事件 ——> doPost()
 *         3. 取消事件订阅 ——> unSubscribe()
 *         <p>
 *         compile 'io.reactivex:rxandroid:1.2.1'
 *         compile 'io.reactivex:rxjava:1.2.4'
 */
public class RxBusController {
    private static volatile RxBusController mInstance;
    private SerializedSubject<Object, Object> mSubject;
    private HashMap<String, CompositeSubscription> mSubscriptionMap;

    private RxBusController() {
        mSubject = new SerializedSubject<>(PublishSubject.create());
    }

    public static RxBusController getInstance() {
        if (mInstance == null) {
            synchronized (RxBusController.class) {
                if (mInstance == null) {
                    mInstance = new RxBusController();
                }
            }
        }
        return mInstance;
    }


    /**
     * 返回指定类型的Observable实例
     *
     * @param type
     * @param <T>
     * @return
     */
    private <T> Observable<T> tObservable(final Class<T> type) {
        return mSubject.ofType(type);
    }

    /**
     * 是否已有观察者订阅
     *
     * @return
     */
    public boolean hasObservers() {
        return mSubject.hasObservers();
    }

    /**
     * 一个默认的订阅方法
     *
     * @param type
     * @param next
     * @param error
     * @param <T>
     * @return
     */
    private <T> Subscription doAsyncSubscribe(Class<T> type, Action1<T> next, Action1<Throwable> error) {
        return tObservable(type)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(next, error);
    }

    /**
     * 保存订阅后的subscription
     *
     * @param key
     * @param subscription
     */
    private void addSubscription(String key, Subscription subscription) {
        if (mSubscriptionMap == null) {
            mSubscriptionMap = new HashMap<>();
        }
        if (mSubscriptionMap.get(key) != null) {
            mSubscriptionMap.get(key).add(subscription);
        } else {
            CompositeSubscription compositeSubscription = new CompositeSubscription();
            compositeSubscription.add(subscription);
            mSubscriptionMap.put(key, compositeSubscription);
        }
    }


    /************************************外部使用*****************************************/
    /**
     * 异步订阅事件监听
     */
    public <T> void doAsyncSubscribe(String tag, Class<T> clz, final OnCallback<T> callback) {
        Subscription subscription = RxBusController.getInstance()
                .tObservable(clz)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<T>() {
                    @Override
                    public void call(T t) {
                        callback.callback(t);
                    }
                });
        addSubscription(tag, subscription);
    }

    /**
     * 发送事件
     */
    public void doPost(Object obj) {
        mSubject.onNext(obj);
    }

    /**
     * 取消事件订阅
     */
    public void unSubscribe(String tag) {
        if (mSubscriptionMap == null) {
            return;
        }
        if (!mSubscriptionMap.containsKey(tag)) {
            return;
        }
        if (mSubscriptionMap.get(tag) != null) {
            mSubscriptionMap.get(tag).unsubscribe();
        }

        mSubscriptionMap.remove(tag);
    }

    /************************************外部使用*****************************************/

    public interface OnCallback<T> {
        void callback(T t);
    }
}
