package cn.bizzan.base;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.gyf.barlibrary.ImmersionBar;
import com.umeng.analytics.MobclickAgent;

import cn.bizzan.R;
import cn.bizzan.ui.common.PermissionActivity;
import cn.bizzan.ui.lock.LockActivity;
import cn.bizzan.ui.login.LoginActivity;
import cn.bizzan.app.GlobalConstant;
import cn.bizzan.app.MyApplication;
import cn.bizzan.utils.SharedPreferenceInstance;
import cn.bizzan.utils.WonderfulKeyboardUtils;
import cn.bizzan.utils.WonderfulLogUtils;
import cn.bizzan.utils.WonderfulStringUtils;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.Unbinder;

public abstract class BaseActivity extends AppCompatActivity {
    private PopupWindow loadingPopup;
    protected View permissionView;
    protected View notLoginView;
    private Unbinder unbinder;
    protected ImmersionBar immersionBar;
    protected boolean isSetTitle = false;
    private TextView tvNote;
    private ViewGroup emptyView;
    boolean lockOpen = false;
    protected boolean isNeedChecke = true;// 解锁界面 是不需要判断的 用此变量控制
    protected boolean isNeedhide = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFlag();
        setContentView(getActivityLayoutId());
        unbinder = ButterKnife.bind(this);
        ActivityManage.addActivity(this);
        emptyView = getEmptyView();
        initLoadingPopup();
        initPermissionView();
        initNotLoginView();
        if (isImmersionBarEnabled()) initImmersionBar();
        initViews(savedInstanceState);
        obtainData();
        fillWidget();
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                loadData();
            }
        });
    }

    protected void setFlag() {

    }

    @Override
    protected void onStart() {
        super.onStart();
        WonderfulLogUtils.loge("当前Activity", this.getLocalClassName());
        if (isNeedShowLockActivity()) LockActivity.actionStart(this);
    }

    protected boolean isNeedShowLockActivity() {
        boolean isLogin = MyApplication.getApp().isLogin();//登录否？
        lockOpen = !WonderfulStringUtils.isEmpty(SharedPreferenceInstance.getInstance().getLockPwd());//开启否？
        boolean b = SharedPreferenceInstance.getInstance().getIsNeedShowLock();//是否处于后台过？
        return isLogin && isNeedChecke && lockOpen && b;
    }


    private void initPermissionView() {
        permissionView = getLayoutInflater().inflate(R.layout.permission_die, null);
        tvNote = permissionView.findViewById(R.id.tvNote);
        permissionView.findViewById(R.id.btToPermision).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PermissionActivity.actionStart(BaseActivity.this);
            }
        });
    }

    private void initNotLoginView() {
        notLoginView = getLayoutInflater().inflate(R.layout.empty_not_login, null);
        notLoginView.findViewById(R.id.tvToLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseActivity.this.startActivityForResult(new Intent(BaseActivity.this, LoginActivity.class), LoginActivity.RETURN_LOGIN);
            }
        });
    }

    /**
     * 子类重写实现扩展设置
     */
    protected void initImmersionBar() {
        immersionBar = ImmersionBar.with(this);
        immersionBar.keyboardEnable(false, WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN).statusBarDarkFont(false, 0.2f).flymeOSStatusBarFontColor(R.color.bgBlue).init();
    }

    /**
     * 获取布局ID
     */
    protected abstract int getActivityLayoutId();

    /**
     * 获取空布局的父布局
     */
    protected ViewGroup getEmptyView() {
        return null;
    }

    /**
     * 是否启用沉浸式
     */
    protected boolean isImmersionBarEnabled() {
        return true;
    }

    /**
     * 初始化工作
     *
     * @param savedInstanceState
     */
    protected abstract void initViews(Bundle savedInstanceState);

    /**
     * 获取本地或传递的数据
     */
    protected abstract void obtainData();

    /**
     * 控件填充
     */
    protected abstract void fillWidget();

    /**
     * 初始数据加载
     */
    protected abstract void loadData();

    @Override
    protected void onStop() {
        super.onStop();
        lockOpen = !WonderfulStringUtils.isEmpty(SharedPreferenceInstance.getInstance().getLockPwd());
        if (lockOpen) {
            if (!isAppOnForeground())
                SharedPreferenceInstance.getInstance().saveIsNeedShowLock(true);
            else
                SharedPreferenceInstance.getInstance().saveIsNeedShowLock(false);
        }
    }
    /**
     * 程序是否在前台运行
     */
    public boolean isAppOnForeground() {
        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        String packageName = getApplicationContext().getPackageName();
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) return false;
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(packageName) && appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
        ActivityManage.removeActivity(this);
        hideLoadingPopup();
        if (immersionBar != null) immersionBar.destroy();
    }

    /**
     * 初始化加载dialog
     */
    private void initLoadingPopup() {
        View loadingView = getLayoutInflater().inflate(R.layout.pop_loading, null);
        loadingPopup = new PopupWindow(loadingView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        loadingPopup.setFocusable(true);
        loadingPopup.setClippingEnabled(false);
        loadingPopup.setBackgroundDrawable(new ColorDrawable());
    }

    /**
     * 显示加载框
     */
    public void displayLoadingPopup() {
        loadingPopup.showAtLocation(getWindow().getDecorView(), Gravity.CENTER, 0, 0);
    }

    /**
     * 隐藏加载框
     */
    public void hideLoadingPopup() {
        if (loadingPopup != null) {
            loadingPopup.dismiss();
        }
    }

    /**
     * 显示没有权限的布局
     */
    public void showNoPermissionView(int permission) {
        if (emptyView == null) return;
        emptyView.removeAllViews();
        emptyView.addView(permissionView);
        emptyView.setVisibility(View.VISIBLE);
        switch (permission) {
            case GlobalConstant.PERMISSION_CONTACT:
                tvNote.setText("允许LIFE访问通讯录权限，可以获得更好的体验哦。");
                break;
            case GlobalConstant.PERMISSION_CAMERA:
                tvNote.setText("允许LIFE打开相机权限，可以获得更好的体验哦。");
                break;
            default:
        }
    }

    /**
     * 显示去登录的布局
     */
    public void showToLoginView() {
        if (emptyView == null) return;
        emptyView.removeAllViews();
        emptyView.addView(notLoginView);
        emptyView.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏去登录的布局
     */
    public void hideToLoginView() {
        if (emptyView == null) return;
        emptyView.removeAllViews();
        emptyView.setVisibility(View.GONE);
    }

    /**
     * 获取用户token
     */
    public String getToken() {
        return MyApplication.getApp().getCurrentUser().getToken();
    }

    public int getId() {
        return MyApplication.getApp().getCurrentUser().getId();
    }

    /**
     * 处理软件盘智能弹出和隐藏
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                View view = getCurrentFocus();
                if (isNeedhide) {
                    WonderfulKeyboardUtils.editKeyboard(ev, view, this);
                }
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }


    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }
    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }



}
