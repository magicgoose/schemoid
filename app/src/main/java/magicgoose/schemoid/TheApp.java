package magicgoose.schemoid;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.io.IOException;

import magicgoose.schemoid.fragment.SelectableSchemeLogItem;
import magicgoose.schemoid.scheme.ISchemeRunner;
import magicgoose.schemoid.scheme.JSchemeRunner;

public class TheApp extends Application {

    private static TheApp Instance;
    private ISchemeRunner<SelectableSchemeLogItem> schemeRunner;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String VERSION_NAME;
    private int VERSION_NUMBER;

    public static TheApp getInstance() {
        return Instance;
    }

    public ISchemeRunner<SelectableSchemeLogItem> getSchemeRunner() {
        if (schemeRunner == null) {
            schemeRunner = createSchemeRunner();
        }
        return schemeRunner;
    }

    @NonNull
    private ISchemeRunner<SelectableSchemeLogItem> createSchemeRunner() {
        return new JSchemeRunner<>(getResources(), this.handler, SelectableSchemeLogItem::new);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            VERSION_NAME = pInfo.versionName;
            VERSION_NUMBER = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
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

    public String getVersionName() {
        return VERSION_NAME;
    }

    public int getVersionNumber() {
        return VERSION_NUMBER;
    }
}
