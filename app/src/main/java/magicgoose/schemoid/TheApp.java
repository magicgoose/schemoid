package magicgoose.schemoid;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import magicgoose.schemoid.fragment.SelectableSchemeLogItem;
import magicgoose.schemoid.scheme.ISchemeRunner;
import magicgoose.schemoid.scheme.JSchemeRunner;
import magicgoose.schemoid.scheme.SchemeLogItemKind;
import magicgoose.schemoid.scheme.parser.SimpleSchemeParser;

public class TheApp extends Application {

    private static TheApp Instance;
    private ISchemeRunner<SelectableSchemeLogItem> schemeRunner;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String VERSION_NAME;
    private int VERSION_NUMBER;
    private Toast toast;
    private static final String LogFileName = "log.bin";

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
        return new JSchemeRunner<>(getResources(), this.handler, new SimpleSchemeParser(), SelectableSchemeLogItem::new, loadLog());
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

    public void showToast(final int stringResId) {
        if (toast == null) {
            toast = Toast.makeText(this, stringResId, Toast.LENGTH_SHORT);
        } else {
            toast.setText(stringResId);
            toast.setDuration(Toast.LENGTH_SHORT);
        }
        toast.show();
    }

    @SuppressLint("NewApi")
    public void saveLog(final List<SelectableSchemeLogItem> log) throws IOException {
        try (final DataOutputStream os = new DataOutputStream(new BufferedOutputStream(openFileOutput(LogFileName, MODE_PRIVATE)))) {
            os.writeInt(log.size());
            for (final SelectableSchemeLogItem item : log) {
                os.writeInt(item.kind.ordinal());
                os.writeUTF(item.content);
            }
        }
    }

    @SuppressLint("NewApi")
    public List<SelectableSchemeLogItem> loadLog() {
        try (final DataInputStream is = new DataInputStream(new BufferedInputStream(openFileInput(LogFileName)))) {
            final int size = is.readInt();
            final ArrayList<SelectableSchemeLogItem> log = new ArrayList<>(size);
            final SchemeLogItemKind[] itemKinds = SchemeLogItemKind.values();
            for (int i = 0; i < size; i++) {
                final SchemeLogItemKind kind = itemKinds[is.readInt()];
                final String content = is.readUTF();
                log.add(new SelectableSchemeLogItem(kind, content));
            }
            return log;
        } catch (Exception e) {
            return null;
        }
    }
}
