package wiki.qdc.webtracking;

import static android.provider.Settings.Secure.getString;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class Logger {
    private static final String TAG = "WebTrackingLogger";
    private static final String FAIL_CACHE_KEY = "webtrack_fail_log";

    private static String api;
    private static final OkHttpClient client = new OkHttpClient();
    private static final Set<Call> callSet = new CopyOnWriteArraySet<>();
    private static LoggerConfig loggerConfig;
    private static LogBean.DeviceInfo mDeviceInfo;
    private static LogBean.AppInfo mAppInfo;
    private static Context applicationContext;

    private Logger() {
    }

    /**
     * 初始化参数
     *
     * @param context  application context
     * @param project  project名称
     * @param region   地域，例如 华南1（深圳）为 cn-shenzhen
     * @param logstore logstore名称
     */
    public static void init(Context context, String project, String region, String logstore) {
        Logger.init(context, project, region, logstore, null);
    }

    /**
     * 初始化参数
     *
     * @param context  application context
     * @param project  project名称
     * @param region   地域，例如 华南1（深圳）为 cn-shenzhen
     * @param logstore logstore名称
     * @param config   配置
     */
    public static void init(Context context, String project, String region, String logstore, LoggerConfig config) {
        applicationContext = context.getApplicationContext();
        api = "https://" + project + "." + region + ".log.aliyuncs.com/logstores/" + logstore + "/track";
        mDeviceInfo = new LogBean.DeviceInfo();
        mDeviceInfo.setSystem("Android");
        mDeviceInfo.setSystemVersion(Build.VERSION.RELEASE);
        mDeviceInfo.setDeviceName(getDeviceNameSync(context));
        mDeviceInfo.setDeviceModel(Build.MODEL);
        mDeviceInfo.setUniqueId(getString(context.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID));
        mDeviceInfo.setIsEmulator(String.valueOf(isEmulatorSync()));
        loggerConfig = config != null ? config : new LoggerConfig.Builder().build();
        if (loggerConfig.offlineMode) {
            uploadOfflineLogs();
        }
    }

    /**
     * 提交日志。调用该方法前，需要先调用 init() 方法。
     *
     * @param json json文本，其具体内容需查看阿里云文档
     */
    public static void put(String json) {
        if (loggerConfig == null) {
            throw new RuntimeException("需要先调用Logger.init()进行初始化");
        }
        JSONObject object = new JSONObject();
        try {
            JSONArray array = new JSONArray();
            array.put(new JSONObject(json));
            object.put("__logs__", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String content = object.toString();
        _put(content, json);
    }

    /**
     * 提交日志。调用该方法前，需要先调用 init() 方法。
     * @param errorType 自定义错误类型
     * @param message 错误信息
     */
    public static void log(String errorType, String message) {
        if (loggerConfig == null) {
            throw new RuntimeException("需要先调用Logger.init()进行初始化");
        }
        LogBean logBean = new LogBean();
        mDeviceInfo.setNetStatus(getConnectStatus(applicationContext));
        logBean.setDeviceInfo(GsonUtil.toJson(mDeviceInfo));
        if (mAppInfo != null) {
            logBean.setAppInfo(GsonUtil.toJson(mAppInfo));
        }
        logBean.setErrorType(errorType);
        logBean.setDetail(message);
        Logger.put(GsonUtil.toJson(logBean));
    }

    /**
     * 设置APP应用信息，在调用Logger.log()时有用
     * @param appName
     * @param appVersionName
     * @param appVersionCode
     */
    public static void setAppInfo(String appName, String appVersionName, String appVersionCode) {
        mAppInfo = new LogBean.AppInfo();
        mAppInfo.setAppName(appName);
        mAppInfo.setAppVersion(appVersionName);
        mAppInfo.setVersionCode(appVersionCode);
    }

    /**
     * 取消全部日志请求
     */
    public static synchronized void cancel() {
        for (Call call : callSet) {
            if (call.isExecuted()) {
                call.cancel();
            }
        }
        callSet.clear();
    }

    private static synchronized void saveFailLog(String json) {
        if (!loggerConfig.offlineMode || json == null) {
            return;
        }
        Set<String> local = SpUtil.getStringSet(applicationContext, FAIL_CACHE_KEY);
        if (local != null && local.size() >= loggerConfig.maxOfflineNum) {
            return;
        }
        print("save fail log to storage: " + json);
        SpUtil.putStringSet(applicationContext, FAIL_CACHE_KEY, json);
    }

    private static void uploadOfflineLogs() {
        Set<String> local = SpUtil.getStringSet(applicationContext, FAIL_CACHE_KEY);
        if (local == null) {
            return;
        }
        print("start upload fail logs. size: " + local.size());
        JSONObject object = new JSONObject();
        try {
            JSONArray array = new JSONArray();
            for (String item : local) {
                array.put(new JSONObject(item));
            }
            object.put("__logs__", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String content = object.toString();
        _put(content, null);
    }

    private static void _put(String content, String newLog) {
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), content);
        Request request = new Request.Builder()
                .url(api)
                .post(body)
                .addHeader("x-log-apiversion", "0.6.0")
                .addHeader("x-log-bodyrawsize", String.valueOf(content.length()))
                .build();
        Call call = client.newCall(request);
        callSet.add(call);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call c, IOException e) {
                print("logger error: " + e.getMessage());
                callSet.remove(call);
                saveFailLog(newLog);
            }

            @Override
            public void onResponse(Call c, Response response) throws IOException {
                callSet.remove(call);
                if (response.code() == 200) {
                    if (newLog == null) {
                        print("upload fail logs success.");
                        SpUtil.remove(applicationContext, FAIL_CACHE_KEY);
                    } else {
                        print("logger success: " + newLog);
                    }
                } else {
                    print("logger http error: " + response.message());
                    saveFailLog(newLog);
                }
            }
        });
    }

    private static String getDeviceNameSync(Context context) {
        try {
            String bluetoothName = Settings.Secure.getString(context.getApplicationContext().getContentResolver(), "bluetooth_name");
            if (bluetoothName != null) {
                return bluetoothName;
            }

            if (Build.VERSION.SDK_INT >= 25) {
                String deviceName = Settings.Global.getString(context.getApplicationContext().getContentResolver(), Settings.Global.DEVICE_NAME);
                if (deviceName != null) {
                    return deviceName;
                }
            }
        } catch (Exception e) {
            // same as default unknown return
        }
        return "unknown";
    }

    private static boolean isEmulatorSync() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.toLowerCase(Locale.ROOT).contains("droid4x")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.HARDWARE.contains("vbox86")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
                || Build.BOARD.toLowerCase(Locale.ROOT).contains("nox")
                || Build.BOOTLOADER.toLowerCase(Locale.ROOT).contains("nox")
                || Build.HARDWARE.toLowerCase(Locale.ROOT).contains("nox")
                || Build.PRODUCT.toLowerCase(Locale.ROOT).contains("nox")
                || Build.SERIAL.toLowerCase(Locale.ROOT).contains("nox")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"));
    }

    private static String getConnectStatus(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobileInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifiInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean status = false;
        if (wifiInfo != null) {
            status = wifiInfo.isConnected();
            return "wifi " + status;
        } else if (mobileInfo != null) {
            status = mobileInfo.isConnected() || status;
            return "net " + status;
        }
        return "unknown " + status;
    }

    private static void print(String value) {
        if (loggerConfig.enablePrint) {
            Log.d(TAG, value);
        }
    }
}
