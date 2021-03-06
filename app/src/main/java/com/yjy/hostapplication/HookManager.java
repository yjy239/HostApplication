package com.yjy.hostapplication;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

/**
 * <pre>
 *     author : yjy
 *     e-mail : yujunyu12@gmail.com
 *     time   : 2018/08/17
 *     desc   :
 *     version: 1.0
 * </pre>
 */
public class HookManager {
    private static Context context;
    private static final int LAUNCH_ACTIVITY         = 100;
    private static PluginClassLoader cl;
    private static ArrayList<Object> activities = new ArrayList<>();
    private static Object sActivityThread;
    private static Class<?> packageParserClass;
    private static Object mPackageParser;
    private static Class<?> mActivityThreadClazz;
    private static File apk;

    private static String PLUGIN = "com.yjy.pluginapplication.MainActivity";
    private static String REAL = "com.yjy.hostapplication.RealActivity";
    private static String dirPath;


    public static void loadPlugin(Context context){

        try {
            dirPath = context.getCacheDir().getParentFile().getAbsolutePath()
                    +File.separator+"Plugin"+File.separator+"data"+File.separator+"com.yjy.pluginapplication";

            apk = new File(dirPath,"plugin.apk");
            if(apk.exists()){
                Log.e("apk","exist");
            }else {
                Log.e("apk","not exist");
                Utils.copyFileFromAssets(context,"plugin.apk",
                        dirPath+ File.separator +"plugin.apk");

            }


            cl = new PluginClassLoader(apk.getAbsolutePath(),
                    context.getDir("plugin.dex", 0).getAbsolutePath(),null,context.getClassLoader().getParent());
            hookPackageParser(apk);


        }catch (Exception e){
            e.printStackTrace();
        }


    }

    public static Class<?> findClass(String path){
        try {
             Class<?> clazz = cl.loadClass(path);
             return clazz;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


    /**
     * hook ActivityManagerNative
     * @param context
     */
    public static void init(Context context){
        HookManager.context = context;
        try {
            Class<?> amnClazz = Class.forName("android.app.ActivityManagerNative");
            Field defaultField = amnClazz.getDeclaredField("gDefault");
            defaultField.setAccessible(true);
            Object gDefaultObj = defaultField.get(null);

            Class<?> singletonClazz = Class.forName("android.util.Singleton");
            Field amsField = singletonClazz.getDeclaredField("mInstance");
            amsField.setAccessible(true);
            Object amsObj = amsField.get(gDefaultObj);


            amsObj = Proxy.newProxyInstance(context.getClass().getClassLoader(),
                    amsObj.getClass().getInterfaces(),new HookHandler(amsObj));

            amsField.set(gDefaultObj,amsObj);





        }catch (Exception e){
            e.printStackTrace();
        }

    }


    /**
     * hook登录
     */
    public static void hookLaunchActivity(){
        try {
            mActivityThreadClazz = Class.forName("android.app.ActivityThread");
            Field sActivityThreadField = mActivityThreadClazz.getDeclaredField("sCurrentActivityThread");
            sActivityThreadField.setAccessible(true);
            sActivityThread = sActivityThreadField.get(null);

            Field mHField = mActivityThreadClazz.getDeclaredField("mH");
            mHField.setAccessible(true);
            Handler mH = (Handler)mHField.get(sActivityThread);

            Field callback = Handler.class.getDeclaredField("mCallback");
            callback.setAccessible(true);
            callback.set(mH,new ActivityThreadCallBack());



        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * 解析包
     * @param apk
     */
    public static void hookPackageParser(File apk){
        try {
            packageParserClass = Class.forName("android.content.pm.PackageParser");
            mPackageParser = packageParserClass.newInstance();

            //先解析一次整个包名
            Method paresPackageMethod = packageParserClass.getDeclaredMethod("parsePackage",File.class,int.class);
            //Package.paresPackage（File，flag）
            Object mPackage = (Object) paresPackageMethod.invoke(mPackageParser,apk,0);

            //解析完整个包，获取Activity的集合,保存起来
            Field mActivitiesField = mPackage.getClass().getDeclaredField("activities");
            activities = (ArrayList<Object>) mActivitiesField.get(mPackage);
            Log.e("activites",activities.toString());


        }catch (Exception e){
            e.printStackTrace();
        }

    }





    static class HookHandler implements InvocationHandler {

        private Object amsObj;


        public HookHandler(Object amsObj){
            this.amsObj = amsObj;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Log.e("method",method.getName());
            if(method.getName().equals("startActivity")){
                // 启动Activity的方法,找到原来的Intent
                Intent proxyIntent = (Intent) args[2];
                // 代理的Intent
                Intent realIntent = new Intent();
                realIntent.setComponent(new ComponentName("com.yjy.pluginapplication",PLUGIN));
                // 把原来的Intent绑在代理Intent上面
                proxyIntent.putExtra("realIntent",realIntent);
                // 让proxyIntent去晒太阳，借尸
                args[2] = proxyIntent;
            }
            return method.invoke(amsObj,args);
        }
    }




    static class ActivityThreadCallBack implements Handler.Callback{

        @Override
        public boolean handleMessage(Message msg) {
            if(msg.what == LAUNCH_ACTIVITY){
                handleLaunchActivity(msg);
            }
            return false;
        }
    }

    //    UserHandle.getCallingUserId()
    @TargetApi(17)
    public static int getCallingUserId() {
        try {
            Method getCallingUserIdMethod = UserHandle.class.getDeclaredMethod("getCallingUserId");
            return (int) getCallingUserIdMethod.invoke(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void hookGetPackageInfoNoCheck(Object mActivityClientRecordObj,Intent intent){
        //获取ActivityInfo
        try {
            Class<?> sPackageUserStateClass = Class.forName("android.content.pm.PackageUserState");
            Object mPackageUserState = sPackageUserStateClass.newInstance();
            Class<?> sActivityClass = Class.forName("android.content.pm.PackageParser$Activity");
            Method generateActivityInfoMethod = packageParserClass.getDeclaredMethod("generateActivityInfo",sActivityClass,int.class,sPackageUserStateClass,int.class);
            ComponentName name = intent.getComponent();
            Log.e("ComponentName",name.getClassName());

            //获取activityInfo
            //已经知道我们插件中的Activity信息只有一条，就没必要筛选了。作者本人懒了
            ActivityInfo activityInfo  = (ActivityInfo) generateActivityInfoMethod.invoke(mPackageParser,
                    activities.get(0),0,mPackageUserState, getCallingUserId());

            //有了activityInfo，再获取sDefaultCompatibilityInfo,调用getPackageInfoNoCheck方法
            Method getPackageInfoNoCheckMethod = mActivityThreadClazz.getDeclaredMethod("getPackageInfoNoCheck",ApplicationInfo.class,
                    CompatibilityInfoCompat.getMyClass());

            fixApplicationInfo(activityInfo,apk);

            //获取到LoadApk实例
            Object LoadApk = getPackageInfoNoCheckMethod.invoke(sActivityThread,activityInfo.applicationInfo,CompatibilityInfoCompat.DEFAULT_COMPATIBILITY_INFO());

            //把LoadApk中的classloader切换为我们的classloader
            Field mClassLoaderField = LoadApk.getClass().getDeclaredField("mClassLoader");
            mClassLoaderField.setAccessible(true);
            mClassLoaderField.set(LoadApk,cl);


            //把这个loadApk放到mPackages中
            Field LoadApkMapField = mActivityThreadClazz.getDeclaredField("mPackages");
            LoadApkMapField.setAccessible(true);

            Map LoadApkMap = (Map)LoadApkMapField.get(sActivityThread);


            //调用Map的put方法 mPackages.put(String,LoadApk)
            LoadApkMap.put(activityInfo.applicationInfo.packageName,new WeakReference<Object>(LoadApk));


            //设置回去
            LoadApkMapField.set(sActivityThread,LoadApkMap);

            Field activityInfoField = mActivityClientRecordObj.getClass().getDeclaredField("activityInfo");
            activityInfoField.setAccessible(true);
            activityInfoField.set(mActivityClientRecordObj,activityInfo);

            Thread.currentThread().setContextClassLoader(cl);



        }catch (Exception e){
            e.printStackTrace();
        }


    }

    private static void fixApplicationInfo(ActivityInfo activityInfo,File mPluginFile){
        ApplicationInfo applicationInfo = activityInfo.applicationInfo;
        if (applicationInfo.sourceDir == null) {
            applicationInfo.sourceDir = mPluginFile.getPath();
        }
        if (applicationInfo.publicSourceDir == null) {
            applicationInfo.publicSourceDir = mPluginFile.getPath();
        }


        if (applicationInfo.dataDir == null) {
            String dirPath = context.getCacheDir().getParentFile().getAbsolutePath()
                    +File.separator+"Plugin"+File.separator+"data"+File.separator+applicationInfo.packageName;
            File dir = new File(dirPath);
            if(!dir.exists()){
                dir.mkdirs();
            }

            applicationInfo.dataDir = dirPath;
        }

        try {
            Field scanDirField = applicationInfo.getClass().getDeclaredField("scanSourceDir");
            scanDirField.setAccessible(true);
            scanDirField.set(applicationInfo,applicationInfo.dataDir);
        }catch (Exception e){
            e.printStackTrace();
        }


        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Field PublicSourceDirField = applicationInfo.getClass().getDeclaredField("scanPublicSourceDir");
                PublicSourceDirField.setAccessible(true);
                PublicSourceDirField.set(applicationInfo,applicationInfo.dataDir);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        try {
            PackageInfo mHostPackageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            applicationInfo.uid = mHostPackageInfo.applicationInfo.uid;
        }catch (Exception e){
            e.printStackTrace();
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (applicationInfo.splitSourceDirs == null) {
                applicationInfo.splitSourceDirs = new String[]{mPluginFile.getPath()};
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (applicationInfo.splitPublicSourceDirs == null) {
                applicationInfo.splitPublicSourceDirs = new String[]{mPluginFile.getPath()};
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    if (Build.VERSION.SDK_INT < 26) {
                        Field deviceEncryptedDirField = applicationInfo.getClass().getDeclaredField("deviceEncryptedDataDir");
                        deviceEncryptedDirField.setAccessible(true);
                        deviceEncryptedDirField.set(applicationInfo,applicationInfo.dataDir);


                        Field credentialEncryptedDirField = applicationInfo.getClass().getDeclaredField("credentialEncryptedDataDir");
                        credentialEncryptedDirField.setAccessible(true);
                        credentialEncryptedDirField.set(applicationInfo,applicationInfo.dataDir);
                    }

                    Field deviceProtectedDirField = applicationInfo.getClass().getDeclaredField("deviceProtectedDataDir");
                    deviceProtectedDirField.setAccessible(true);
                    deviceProtectedDirField.set(applicationInfo,applicationInfo.dataDir);

                    Field credentialProtectedDirField = applicationInfo.getClass().getDeclaredField("credentialProtectedDataDir");
                    credentialProtectedDirField.setAccessible(true);
                    credentialProtectedDirField.set(applicationInfo,applicationInfo.dataDir);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (TextUtils.isEmpty(applicationInfo.processName)) {
                applicationInfo.processName = applicationInfo.packageName;
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private static void handleLaunchActivity(Message msg) {
        try {
            //msg.obj ActivityClientRecord
            Object mActivityClientRecordObj = msg.obj;
            Field intentField = mActivityClientRecordObj.getClass().getDeclaredField("intent");
            intentField.setAccessible(true);
            Intent proxy = (Intent) intentField.get(mActivityClientRecordObj);
            Intent orgin = proxy.getParcelableExtra("realIntent");
            if(orgin != null){
                intentField.set(mActivityClientRecordObj,orgin);
            }
            if(orgin != null){
                hookGetPackageInfoNoCheck(mActivityClientRecordObj,orgin);
            }


        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
