package com.example.module_b;

import android.app.Application;

import com.dovar.router_annotation.Module;
import com.dovar.router_api.router.BaseAppInit;

@Module
public class BInit extends BaseAppInit {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static Application instance() {
        return mApplication;
    }
}
