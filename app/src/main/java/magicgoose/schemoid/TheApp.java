package magicgoose.schemoid;

import android.app.Application;

import java.io.IOException;

import magicgoose.schemoid.scheme.DummySchemeRunner;
import magicgoose.schemoid.scheme.ISchemeRunner;

public class TheApp extends Application {

    private static TheApp Instance;
    private ISchemeRunner schemeRunner;

    public static TheApp getInstance() {
        return Instance;
    }

    public ISchemeRunner getSchemeRunner() {
        if (schemeRunner == null) {
            schemeRunner = new DummySchemeRunner();
        }
        return schemeRunner;
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
