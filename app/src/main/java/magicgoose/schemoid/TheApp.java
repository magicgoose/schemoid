package magicgoose.schemoid;

import android.app.Application;
import android.support.annotation.NonNull;

import java.io.IOException;

import magicgoose.schemoid.scheme.ISchemeRunner;
import magicgoose.schemoid.scheme.JSchemeRunner;

public class TheApp extends Application {

    private static TheApp Instance;
    private ISchemeRunner schemeRunner;

    public static TheApp getInstance() {
        return Instance;
    }

    public ISchemeRunner getSchemeRunner() {
        if (schemeRunner == null) {
            schemeRunner = createSchemeRunner();
        }
        return schemeRunner;
    }

    @NonNull
    private ISchemeRunner createSchemeRunner() {
        return new JSchemeRunner(getResources());
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
