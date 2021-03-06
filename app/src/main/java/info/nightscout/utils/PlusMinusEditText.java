package info.nightscout.utils;

import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import android.widget.ImageView;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;

/**
 * Created by mike on 28.06.2016.
 */
public class PlusMinusEditText implements View.OnKeyListener,
        View.OnTouchListener, View.OnClickListener {
    private static Logger log = LoggerFactory.getLogger(PlusMinusEditText.class);

    Integer editTextID;
    TextView editText;
    ImageView minusImage;
    ImageView plusImage;

    Double value;
    Double minValue = 0d;
    Double maxValue = 1d;
    Double step = 1d;
    NumberFormat formater;
    boolean allowZero = false;

    private Handler mHandler;
    private ScheduledExecutorService mUpdater;

    private class UpdateCounterTask implements Runnable {
        private boolean mInc;
        private int repeated = 0;
        private int multiplier = 1;

        private final int doubleLimit = 5;

        public UpdateCounterTask(boolean inc) {
            mInc = inc;
        }

        public void run() {
            Message msg = new Message();
            if (repeated % doubleLimit == 0) multiplier *= 2;
            repeated++;
            msg.arg1 = multiplier;
            msg.arg2 = repeated;
            if (mInc) {
                msg.what = MSG_INC;
            } else {
                msg.what = MSG_DEC;
            }
            mHandler.sendMessage(msg);
        }
    }

    private static final int MSG_INC = 0;
    private static final int MSG_DEC = 1;

    public PlusMinusEditText(View view, int editTextID, int plusID, int minusID, Double initValue, Double minValue, Double maxValue, Double step, NumberFormat formater, boolean allowZero) {
        editText = (TextView) view.findViewById(editTextID);
        minusImage = (ImageView) view.findViewById(minusID);
        plusImage = (ImageView) view.findViewById(plusID);

        this.value = initValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.step = step;
        this.formater = formater;
        this.allowZero = allowZero;

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_INC:
                        inc(msg.arg1);
                        return;
                    case MSG_DEC:
                        dec(msg.arg1);
                        return;
                }
                super.handleMessage(msg);
            }
        };

        minusImage.setOnTouchListener(this);
        minusImage.setOnKeyListener(this);
        minusImage.setOnClickListener(this);
        plusImage.setOnTouchListener(this);
        plusImage.setOnKeyListener(this);
        plusImage.setOnClickListener(this);
        updateEditText();
    }

    public void setValue(Double value) {
        this.value = value;
        updateEditText();
    }

    public Double getValue() {
        return value;
    }

    public String getText() {
        return editText.getText().toString();
    }

    public void setStep(Double step) {
        this.step = step;
    }

    private void inc(int multiplier) {
        value += step * multiplier;
        if (value > maxValue) {
            value = maxValue;
            ToastUtils.showToastInUiThread(MainApp.instance().getApplicationContext(), MainApp.sResources.getString(R.string.youareonallowedlimit));
            stopUpdating();
        }
        updateEditText();
    }

    private void dec( int multiplier) {
        value -= step * multiplier;
        if (value < minValue) {
            value = minValue;
            ToastUtils.showToastInUiThread(MainApp.instance().getApplicationContext(), MainApp.sResources.getString(R.string.youareonallowedlimit));
            stopUpdating();
        }
        updateEditText();
    }

    private void updateEditText() {
        if (value == 0d && !allowZero)
            editText.setText("");
        else
            editText.setText(formater.format(value));
    }

    private void startUpdating(boolean inc) {
        if (mUpdater != null) {
            log.debug("Another executor is still active");
            return;
        }
        mUpdater = Executors.newSingleThreadScheduledExecutor();
        mUpdater.scheduleAtFixedRate(new UpdateCounterTask(inc), 200, 200,
                TimeUnit.MILLISECONDS);
    }

    private void stopUpdating() {
        if (mUpdater != null) {
            mUpdater.shutdownNow();
            mUpdater = null;
        }
    }

    @Override
    public void onClick(View v) {
        if (mUpdater == null) {
            if (v == plusImage) {
                inc(1);
            } else {
                dec(1);
            }
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        boolean isKeyOfInterest = keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER;
        boolean isReleased = event.getAction() == KeyEvent.ACTION_UP;
        boolean isPressed = event.getAction() == KeyEvent.ACTION_DOWN
                && event.getAction() != KeyEvent.ACTION_MULTIPLE;

        if (isKeyOfInterest && isReleased) {
            stopUpdating();
        } else if (isKeyOfInterest && isPressed) {
            startUpdating(v == plusImage);
        }
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean isReleased = event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL;
        boolean isPressed = event.getAction() == MotionEvent.ACTION_DOWN;

        if (isReleased) {
            stopUpdating();
        } else if (isPressed) {
            startUpdating(v == plusImage);
        }
        return false;
    }

}
