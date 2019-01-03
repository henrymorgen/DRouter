package com.dovar.router_api.multiprocess;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;

import com.dovar.router_api.Debugger;
import com.dovar.router_api.ILocalRouterAIDL;
import com.dovar.router_api.router.Router;
import com.dovar.router_api.router.RouterUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * auther by heweizong on 2018/8/17
 * description: 广域路由
 * 只在{@link MultiRouterService}中被调用，由于MultiRouterService被指定在主进程中，所以MultiRouter只会被主进程访问
 */
public class MultiRouter {
    private static MultiRouter instance;
    private Application mApplication;
    private HashMap<String, ILocalRouterAIDL> mLocalRouterAIDLMap;
    private HashMap<String, ServiceConnection> mLocalRouterConnectionMap;//可用于解绑
    private static HashMap<String, Class<? extends ConnectMultiRouterService>> mLocalRouterServiceMap;

    private MultiRouter(Application mApplication) {
        if (mApplication == null)
            throw new RuntimeException("MultiRouter Init Failed:Application cannot be null! ");
        this.mApplication = mApplication;
    }

    static MultiRouter instance(Application mApplication) {
        if (instance == null) {
            synchronized (MultiRouter.class) {
                if (instance == null) {
                    instance = new MultiRouter(mApplication);
                }
            }
        }
        return instance;
    }

    public static void registerLocalRouter(Application app, String processName, Class<? extends ConnectMultiRouterService> targetClass) {
        String processAppName = RouterUtil.getProcessName(app);
        if (processAppName == null || !processAppName.equalsIgnoreCase(app.getPackageName())) {
            //非主进程时不做任何事情
            return;
        }
        if (mLocalRouterServiceMap == null) {
            mLocalRouterServiceMap = new HashMap<>();
        }
        mLocalRouterServiceMap.put(processName, targetClass);
    }

    void connectLocalRouter(final String process) {
        if (mApplication == null || mLocalRouterServiceMap == null) return;
        Class<? extends ConnectMultiRouterService> service = mLocalRouterServiceMap.get(process);
        if (service == null) return;
        Intent mIntent = new Intent();
        mIntent.setClass(mApplication, service);
        Debugger.d("connectLocalRouter\t" + process);
        mApplication.bindService(mIntent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ILocalRouterAIDL lrAIDL = ILocalRouterAIDL.Stub.asInterface(service);
                if (mLocalRouterAIDLMap == null) {
                    mLocalRouterAIDLMap = new HashMap<>();
                }
                if (mLocalRouterConnectionMap == null) {
                    mLocalRouterConnectionMap = new HashMap<>();
                }
                //新增或更新
                mLocalRouterAIDLMap.put(process, lrAIDL);
                mLocalRouterConnectionMap.put(process, this);
                Debugger.d("connectLocalRouter成功");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        }, Context.BIND_AUTO_CREATE);
    }

    @NonNull
    MultiRouterResponse route(MultiRouterRequest routerRequest) {
        String process = routerRequest.getProcess();
        //主进程
        if (process.equals(mApplication.getPackageName())) {
            return RouterUtil.createMultiResponse(Router.instance().localRoute(RouterUtil.backToRequest(routerRequest)));
        }
        //其他进程
        if (mLocalRouterAIDLMap == null) {
            mLocalRouterAIDLMap = new HashMap<>();
        }
        ILocalRouterAIDL target = mLocalRouterAIDLMap.get(process);
        if (target != null) {
            try {
                return target.route(routerRequest);
            } catch (RemoteException mE) {
                mE.printStackTrace();
                MultiRouterResponse mResponse = new MultiRouterResponse();
                mResponse.setMessage(mE.getMessage());
                return mResponse;
            }
        } else {
            //尝试重新连接
            connectLocalRouter(process);

            MultiRouterResponse mResponse = new MultiRouterResponse();
            mResponse.setMessage("广域路由服务正在启动中...");
            return mResponse;
        }
    }

    void publish(String key, Bundle bundle) {
        //主进程
        Router.instance().localPublish(key, bundle);
        //其他进程
        if (mLocalRouterAIDLMap == null) {
            mLocalRouterAIDLMap = new HashMap<>();
        }
        for (Map.Entry<String, ILocalRouterAIDL> entry : mLocalRouterAIDLMap.entrySet()
                ) {
            try {
                entry.getValue().publish(key, bundle);
            } catch (RemoteException e) {
                e.printStackTrace();
                Debugger.e(e.getMessage());
                //会不会导致foreach异常
                if (e instanceof DeadObjectException){
                    mLocalRouterAIDLMap.remove(entry.getKey());
                }
            }
        }
    }
}
