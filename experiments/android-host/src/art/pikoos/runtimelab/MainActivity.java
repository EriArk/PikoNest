package art.pikoos.runtimelab;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AtomicFile;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import art.pikoos.lab.core.LabCartridge;
import art.pikoos.lab.core.PicoRuntimeBackend;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class MainActivity extends Activity {
    private static final String TAG = "PikoRuntimeLab";
    private LabCartridge cart;
    private PicoRuntimeBackend backend;
    private TextView speedLabel, codeLabel, statusLabel;
    private Button testButton;
    private boolean awaitingReturn, leftForRuntime;
    private int returns;
    private SharedPreferences prefs;

    private enum Action { SLOWER, FASTER, TEST }

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = getSharedPreferences("workshop", MODE_PRIVATE);
        returns = prefs.getInt("returns", 0);
        awaitingReturn = prefs.getBoolean("awaitingReturn", false);
        leftForRuntime = awaitingReturn;
        backend = new ExternalPicoBackend(this);
        try {
            AtomicFile project = new AtomicFile(new File(getFilesDir(), "game.p8"));
            if (project.getBaseFile().exists() || new File(getFilesDir(), "game.p8.bak").exists()) {
                cart = new LabCartridge(project.readFully());
            } else {
                try (InputStream in = getAssets().open("workshop.p8")) {
                    cart = new LabCartridge(readAll(in));
                }
                saveCart(cart);
            }
            buildUi();
        } catch (Exception e) {
            TextView error = new TextView(this);
            error.setText("Не удалось открыть проект. Файл сохранён.\n" + e.getMessage());
            error.setPadding(24, 24, 24, 24);
            setContentView(error);
            Log.e(TAG, "Project load failed; original retained", e);
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
        return out.toByteArray();
    }

    private void saveCart(LabCartridge document) throws Exception {
        AtomicFile project = new AtomicFile(new File(getFilesDir(), "game.p8"));
        FileOutputStream out = null;
        try {
            out = project.startWrite();
            out.write(document.bytes());
            project.finishWrite(out);
        } catch (Exception e) {
            project.failWrite(out);
            throw e;
        }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView label(String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text); view.setTextSize(size); view.setTextColor(color);
        view.setPadding(0, dp(4), 0, dp(4));
        return view;
    }
    private Button button(String text, final Action action) {
        Button button = new Button(this);
        button.setText(text); button.setTextSize(18); button.setMinHeight(dp(56));
        button.setOnClickListener(v -> act(action));
        return button;
    }
    private void buildUi() {
        getWindow().setStatusBarColor(Color.rgb(28, 43, 42));
        getWindow().setNavigationBarColor(Color.rgb(28, 43, 42));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(22), dp(14), dp(22), dp(14));
        layout.setBackgroundColor(Color.rgb(245, 239, 220));
        scroll.addView(layout);
        layout.addView(label("PIKOOS · проба мастерской", 23, Color.rgb(28, 43, 42)));
        layout.addView(label("Измени скорость и попробуй игру", 17, Color.DKGRAY));
        speedLabel = label("", 24, Color.rgb(28, 43, 42));
        layout.addView(speedLabel);
        LinearLayout controls = new LinearLayout(this);
        controls.addView(button("− Медленнее", Action.SLOWER), new LinearLayout.LayoutParams(0, dp(60), 1));
        controls.addView(button("Быстрее +", Action.FASTER), new LinearLayout.LayoutParams(0, dp(60), 1));
        layout.addView(controls);
        codeLabel = label("", 17, Color.rgb(20, 73, 62));
        codeLabel.setTypeface(Typeface.MONOSPACE);
        codeLabel.setPadding(dp(12), dp(10), dp(12), dp(10));
        codeLabel.setBackgroundColor(Color.rgb(225, 226, 209));
        layout.addView(codeLabel);
        testButton = button("▶ Проверить в PICO-8", Action.TEST);
        layout.addView(testButton);
        layout.addView(label("После выхода из PICO-8 вернёшься сюда.\n← / → — скорость · Start — запуск", 15, Color.DKGRAY));
        statusLabel = label("Проект сохранён", 15, Color.DKGRAY);
        layout.addView(statusLabel);
        setContentView(scroll);
        refresh();
        testButton.requestFocus();
    }
    private void refresh() {
        speedLabel.setText("Скорость: " + cart.speed());
        codeLabel.setText("speed=" + cart.speed() + "\nif btn(1) then x+=speed end");
        testButton.setEnabled(backend.capabilities().canLaunch);
        if (!backend.detect().launcherPresent) statusLabel.setText("Для запуска установи обёртку PICO-8");
    }
    private void act(Action action) {
        if (cart == null) return;
        try {
            if (action == Action.TEST) {
                if (awaitingReturn) return;
                saveCart(cart);
                awaitingReturn = true;
                leftForRuntime = false;
                if (!prefs.edit().putBoolean("awaitingReturn", true).commit()) {
                    throw new IllegalStateException("Не удалось сохранить состояние запуска");
                }
                backend.launch(cart.bytes());
                Log.i(TAG, "launch_requested speed=" + cart.speed());
            } else {
                int value = Math.max(1, Math.min(4, cart.speed() + (action == Action.FASTER ? 1 : -1)));
                LabCartridge edited = cart.withSpeed(value);
                saveCart(edited);
                cart = edited;
                refresh();
                statusLabel.setText("Сохранено · скорость " + cart.speed());
                Log.i(TAG, "saved speed=" + cart.speed());
            }
        } catch (Exception e) {
            awaitingReturn = false;
            prefs.edit().putBoolean("awaitingReturn", false).commit();
            statusLabel.setText("Не удалось выполнить действие: " + e.getMessage());
            Log.e(TAG, "Action failed", e);
        }
    }
    @Override protected void onStop() {
        super.onStop();
        if (awaitingReturn) leftForRuntime = true;
    }
    @Override protected void onResume() {
        super.onResume();
        if (cart == null || statusLabel == null) return;
        refresh();
        if (awaitingReturn && leftForRuntime) {
            awaitingReturn = false;
            leftForRuntime = false;
            returns++;
            prefs.edit().putBoolean("awaitingReturn", false).putInt("returns", returns).commit();
            statusLabel.setText("Вернулись в мастерскую · скорость " + cart.speed()
                + "\nВозвратов: " + returns + " · результат игры не подтверждён");
            testButton.requestFocus();
            Log.i(TAG, "host_resumed speed=" + cart.speed() + " returns=" + returns + " result=unknown");
        }
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        Action action = keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? Action.SLOWER :
            keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? Action.FASTER :
            keyCode == KeyEvent.KEYCODE_BUTTON_START ? Action.TEST : null;
        if (action != null) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) act(action);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
