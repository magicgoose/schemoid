package magicgoose.schemoid.scheme;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jscheme.JScheme;
import jsint.Evaluator;
import jsint.U;
import magicgoose.schemoid.util.ReactiveList;
import rx.functions.Func1;

public class JSchemeRunner<TLogItem> implements ISchemeRunner<TLogItem> {

    private final ReactiveList<TLogItem> log = new ReactiveList<>();
    private final Resources resources;
    private final Handler handler;
    private final Func1<SchemeLogItem, TLogItem> logTransform;

    private JScheme jsc = createJScheme();

    @NonNull
    private JScheme createJScheme() {
        final Evaluator e = new Evaluator();
        e.INTERRUPTABLE = true;
        return new JScheme(e);
    }

    private final ExecutorService es = Executors.newSingleThreadExecutor();
    private ArrayList<Future<?>> tasks = new ArrayList<>();

    public JSchemeRunner(Resources resources, Handler handler, Func1<SchemeLogItem, TLogItem> logTransform) {
        this.resources = resources;
        this.handler = handler;
        this.logTransform = logTransform;
        submitInitTask();
        logSysInfo("Welcome to Schemoid\nYour commands and evaluation results will appear here");
    }

    private void submitInitTask() {
        final Runnable task = () -> doInit(resources);
        submitTask(task);
    }

    private void submitTask(final Runnable task1) {
        tasks = removeCompleted(tasks);
        final Future<?> task = es.submit(task1);
        tasks.add(task);
    }

    private ArrayList<Future<?>> removeCompleted(final ArrayList<Future<?>> tasks) {
        final ArrayList<Future<?>> newTasks = new ArrayList<>();
        for (final Future<?> task : tasks) {
            if (task.isCancelled() || task.isDone())
                continue;
            newTasks.add(task);
        }
        return newTasks;
    }

    @SuppressLint("NewApi")
    private void doInit(final Resources rs) {
        try (final InputStreamReader istr = new InputStreamReader(new BufferedInputStream(rs.getAssets().open("jscheme.init")))) {
            final Object loadResult = jsc.load(istr);
            final boolean loadResultAsBool = U.to_bool(loadResult);
            if (!loadResultAsBool) {
                throw new RuntimeException("Could not load init script");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ReactiveList<TLogItem> getLog() {
        return log;
    }

    @Override
    public void pushInput(final String input) {
        appendToLog(new SchemeLogItem(SchemeLogItemKind.Input, formatInput(input), input));
        submitTask(() -> doProcessString(input));
    }

    private String formatInput(final String input) {
        final String[] lines = input.split("\\n");
        lines[0] = "> " + lines[0];
        for (int i = 1; i < lines.length; i++) {
            lines[i] = "  " + lines[i];
        }
        return TextUtils.join("\n", lines);
    }

    private void doProcessString(final String input) {
        final SchemeLogItem result = doEvalString(input);
        appendToLog(result);
    }

    private SchemeLogItem doEvalString(final String input) {
        try {
            final Object ret = jsc.eval(input);
            if (ret == jsint.Primitive.DO_NOT_DISPLAY) {
                return outputLogItem("<ok>");
            } else {
                return outputLogItem(jsint.U.stringify(ret));
            }
        } catch (final jscheme.SchemeException e) {
            return errorLogItem(e.getMessage());
        } catch (final jsint.BacktraceException e) {
            return errorLogItem(e.getMessage());
        } catch (final Exception e) {
            return errorLogItem(e.toString());
        } catch (final Error e) {
            return errorLogItem(e.toString());
        } catch (final Throwable e) {
            return errorLogItem(e.toString());
        }
    }

    private SchemeLogItem errorLogItem(final String text) {
        return new SchemeLogItem(SchemeLogItemKind.ErrorOutput, text, text);
    }

    private SchemeLogItem outputLogItem(final String text) {
        return new SchemeLogItem(SchemeLogItemKind.Output, text, text);
    }

    @Override
    public void abort() {
        final boolean didAbort = doAbort();
        logSysInfo(didAbort ? "Aborted evaluation" : "Nothing to abort");
    }

    private void logSysInfo(final String message) {
        final SchemeLogItem logItem = new SchemeLogItem(SchemeLogItemKind.SystemInfo, message, message);
        appendToLog(logItem);
    }

    private void appendToLog(final SchemeLogItem logItem) {
        handler.post(() -> log.add(transform(logItem)));
    }

    private TLogItem transform(final SchemeLogItem logItem) {
        return logTransform.call(logItem);
    }

    private boolean doAbort() {
        boolean didAbortSomething = false;
        for (final Future<?> task : tasks) {
            if (task.isDone() || task.isCancelled())
                continue;
            didAbortSomething |= task.cancel(true);
        }
        tasks.clear();
        return didAbortSomething;
    }

    @Override
    public void reset() {
        doAbort();
        jsc = createJScheme();
        submitInitTask();
        logSysInfo("Interpreter was reset");
    }

    @Override
    public void close() throws IOException {
        doAbort();
        es.shutdownNow();
    }
}
