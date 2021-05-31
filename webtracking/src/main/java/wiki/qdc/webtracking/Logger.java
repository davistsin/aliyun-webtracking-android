package wiki.qdc.webtracking;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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
            throw new RuntimeException("should call Logger.init(param...) first.");
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
        Set<String> local = SpUtil.getSet(applicationContext, FAIL_CACHE_KEY);
        if (local != null && local.size() >= loggerConfig.maxOfflineNum) {
            return;
        }
        log("save fail log to storage: " + json);
        SpUtil.addSet(applicationContext, FAIL_CACHE_KEY, json);
    }

    private static void uploadOfflineLogs() {
        Set<String> local = SpUtil.getSet(applicationContext, FAIL_CACHE_KEY);
        if (local == null) {
            return;
        }
        log("start upload fail logs. size: " + local.size());
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
                log("logger error: " + e.getMessage());
                callSet.remove(call);
                saveFailLog(newLog);
            }

            @Override
            public void onResponse(Call c, Response response) throws IOException {
                callSet.remove(call);
                if (response.code() == 200) {
                    if (newLog == null) {
                        log("upload fail logs success.");
                        SpUtil.remove(applicationContext, FAIL_CACHE_KEY);
                    } else {
                        log("logger success: " + newLog);
                    }
                } else {
                    log("logger http error: " + response.message());
                    saveFailLog(newLog);
                }
            }
        });
    }

    private static void log(String value) {
        if (loggerConfig.enablePrint) {
            Log.d(TAG, value);
        }
    }
}
