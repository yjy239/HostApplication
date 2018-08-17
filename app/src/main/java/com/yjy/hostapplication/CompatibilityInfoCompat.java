package com.yjy.hostapplication;

import java.lang.reflect.Field;

/**
 * <pre>
 *     author : yjy
 *     e-mail : yujunyu12@gmail.com
 *     time   : 2018/08/17
 *     desc   :
 *     version: 1.0
 * </pre>
 */
public class CompatibilityInfoCompat {
    private static Class sClass;

    public static Class getMyClass() throws ClassNotFoundException {
        if (sClass == null) {
            sClass = Class.forName("android.content.res.CompatibilityInfo");
        }
        return sClass;
    }

    private static Object sDefaultCompatibilityInfo;

    public static Object DEFAULT_COMPATIBILITY_INFO() throws IllegalAccessException, ClassNotFoundException {
        if (sDefaultCompatibilityInfo==null) {
            try {
                Field defaultField = getMyClass().getField("DEFAULT_COMPATIBILITY_INFO");
                sDefaultCompatibilityInfo = defaultField.get(null);
            }catch (Exception e){
                e.printStackTrace();
            }

        }
        return sDefaultCompatibilityInfo;
    }
}
