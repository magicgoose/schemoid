package magicgoose.schemoid.scheme;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
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
    private StringWriter outputWriter;
//    private StringWriter errorWriter;

    @NonNull
    private JScheme createJScheme() {
        final Evaluator e = new Evaluator();
        e.INTERRUPTABLE = true;
        outputWriter = new StringWriter();
        e.setOutput(new PrintWriter(outputWriter, false));
//        errorWriter = new StringWriter();
//        e.setError(new PrintWriter(outputWriter, false));
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
        appendToLog(new SchemeLogItem(SchemeLogItemKind.Input, input));
        submitTask(() -> doProcessString(input));
    }

    private void doProcessString(final String input) {
        final SchemeLogItem result = doEvalString(input);
        appendToLog(result);
    }

    private SchemeLogItem doEvalString(final String input) {
        try {
            final Object ret = jsc.eval(input);
            final String output = getOutput();
            if (ret == jsint.Primitive.DO_NOT_DISPLAY) {
                return outputLogItem(output, "<ok>");
            } else {
                return outputLogItem(output, jsint.U.stringify(ret));
            }
        } catch (final jscheme.SchemeException e) {
            return errorLogItem(getOutput(), e.getMessage());
        } catch (final jsint.BacktraceException e) {
            return errorLogItem(getOutput(), e.getMessage());
        } catch (final Exception e) {
            return errorLogItem(getOutput(), e.toString());
        } catch (final Error e) {
            return errorLogItem(getOutput(), e.toString());
        } catch (final Throwable e) {
            return errorLogItem(getOutput(), e.toString());
        }
    }

    private String getOutput() {
        final String result = outputWriter.toString();
        outputWriter.getBuffer().setLength(0);
        return result;
    }

    private SchemeLogItem errorLogItem(final String output, final String text) {
        return new SchemeLogItem(SchemeLogItemKind.ErrorOutput, mergeOutput(output, text));
    }

    private String mergeOutput(final String output, final String value) {
        if (output.length() == 0)
            return value;
        return output + '\n' + value;
    }

    private SchemeLogItem outputLogItem(final String output, final String text) {
        return new SchemeLogItem(SchemeLogItemKind.Output, mergeOutput(output, text));
    }

    @Override
    public void abort() {
        final boolean didAbort = doAbort();
        logSysInfo(didAbort ? "Aborted evaluation" : "Nothing to abort");
    }

    private void logSysInfo(final String message) {
        final SchemeLogItem logItem = new SchemeLogItem(SchemeLogItemKind.SystemInfo, message);
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
