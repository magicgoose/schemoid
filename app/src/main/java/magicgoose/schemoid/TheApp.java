package magicgoose.schemoid;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.io.IOException;

import magicgoose.schemoid.scheme.ISchemeRunner;
import magicgoose.schemoid.scheme.JSchemeRunner;
import magicgoose.schemoid.scheme.SchemeLogItem;

public class TheApp extends Application {

    private static TheApp Instance;
    private ISchemeRunner<SchemeLogItem> schemeRunner;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public static TheApp getInstance() {
        return Instance;
    }

    public ISchemeRunner<SchemeLogItem> getSchemeRunner() {
        if (schemeRunner == null) {
            schemeRunner = createSchemeRunner();
        }
        return schemeRunner;
    }

    @NonNull
    private ISchemeRunner<SchemeLogItem> createSchemeRunner() {
        return new JSchemeRunner<>(getResources(), this.handler, x -> x);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Instance = this;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (schemeRunner != null) {
            try {
                schemeRunner.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
