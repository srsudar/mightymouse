package com.vos.MightyMouse;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.DisplayMetrics;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;

public class TenKeyActivity extends Activity {
  
  // The buttons.
  private Button m1;
  private Button m2;
  private Button m3;
  private Button m4;
  private Button m5;
  private Button m6;
  private Button m7;
  private Button m8;
  private Button m9;
  private Button m0;
  private Button mDecimal;
  private Button mDelete;
  private Button mEnter;
  private Button mPlus;
  private Button mTimes;
  private Button mMinus;
  private Button mDivide;
  
  private DispatcherSingleton mDispatcher;
  
  /*
   * Here are the values we'll be sending.
   */
  private static final byte[] STR_ONE = {(byte)'1'};
  private static final byte[] STR_TWO = {(byte)'2'};
  private static final byte[] STR_THREE = {(byte)'3'};
  private static final byte[] STR_FOUR = {(byte)'4'};
  private static final byte[] STR_FIVE = {(byte)'5'};
  private static final byte[] STR_SIX = {(byte)'6'};
  private static final byte[] STR_SEVEN = {(byte)'7'};
  private static final byte[] STR_EIGHT = {(byte)'8'};
  private static final byte[] STR_NINE = {(byte)'9'};
  private static final byte[] STR_ZERO = {(byte)'0'};
  private static final byte[] STR_DECIMAL = {(byte)'.'};
  private static final byte[] STR_SPACE = {(byte)' '};
  private static final byte[] STR_MINUS = {(byte)'-'};
  private static final byte[] STR_PLUS = {(byte)'+'};
  private static final byte[] STR_DIVIDE = {(byte)'/'};
  private static final byte[] STR_TIMES = {(byte)'*'};
  
  
  

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ten_key_activity, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onResume() {
      super.onResume();
      View tenKey = LayoutInflater.from(this).inflate(R.layout.ten_key, null);
      m1 = (Button) tenKey.findViewById(R.id.button_one);
      m2 = (Button) tenKey.findViewById(R.id.button_two);
      m3 = (Button) tenKey.findViewById(R.id.button_three);
      m4 = (Button) tenKey.findViewById(R.id.button_four);
      m5 = (Button) tenKey.findViewById(R.id.button_five);
      m6 = (Button) tenKey.findViewById(R.id.button_six);
      m7 = (Button) tenKey.findViewById(R.id.button_seven);
      m8 = (Button) tenKey.findViewById(R.id.button_eight);
      m9 = (Button) tenKey.findViewById(R.id.button_nine);
      m0 = (Button) tenKey.findViewById(R.id.button_zero);
      mDecimal = (Button) tenKey.findViewById(R.id.button_decimal);
      mPlus = (Button) tenKey.findViewById(R.id.button_plus);
      mDivide = (Button) tenKey.findViewById(R.id.button_divide);
      mMinus = (Button) tenKey.findViewById(R.id.button_minus);
      mTimes = (Button) tenKey.findViewById(R.id.button_times);
      mEnter = (Button) tenKey.findViewById(R.id.button_enter);
      mDelete = (Button) tenKey.findViewById(R.id.button_delete);
      initButtonListeners();
      this.mDispatcher = DispatcherSingleton.getSingleton(this, null);
      // do a check to see if we need to make the layout a certain size, eg
      // if we're on a tablet and don't want to take up the whole screen.
      WindowManager wm = (WindowManager) this.getSystemService(WINDOW_SERVICE);
      DisplayMetrics metrics = new DisplayMetrics();
      wm.getDefaultDisplay().getMetrics(metrics);
      double x = metrics.widthPixels/metrics.xdpi;
      if (x > 3) {
        // then we're wider than four inches. Calculate the appropriate size
        // in pixels. The test is supposedly wider than four inches, then make
        // it x = 4 inches, y = 5 inches. This is just a ballpark of course.
        int xPixels = Math.round(3 * metrics.xdpi);
        int yPixels = Math.round(3.75f * metrics.ydpi);
        this.getWindow().setLayout(xPixels, yPixels);
      }
      setContentView(tenKey);
    }
    
    /**
     * Init all the listeners for the buttons.
     */
    private void initButtonListeners() {
      m1.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View arg0) {
          mDispatcher.sendKeyBytes(STR_ONE);
          m1.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        
      });
      m2.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          mDispatcher.sendKeyBytes(STR_TWO);
          m2.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        
      });
      m3.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          mDispatcher.sendKeyBytes(STR_THREE);
          m3.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        
      });
      m4.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          mDispatcher.sendKeyBytes(STR_FOUR);
          m4.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        
      });
      m5.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          mDispatcher.sendKeyBytes(STR_FIVE);
          m5.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        
      });
      m6.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          mDispatcher.sendKeyBytes(STR_SIX);
          m6.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        
      });
      m7.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          mDispatcher.sendKeyBytes(STR_SEVEN);
          m7.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        
      });
      m8.setOnClickListener(new OnClickListener() {
        
        @Override
        public void onClick(View v) {
          mDispatcher.sendKeyBytes(STR_EIGHT);
          m8.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
      });
      m9.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          mDispatcher.sendKeyBytes(STR_NINE);
          m9.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        
      });
      m0.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          mDispatcher.sendKeyBytes(STR_ZERO);
          m0.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        
      });
      mDecimal.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          mDispatcher.sendKeyBytes(STR_DECIMAL);
          mDecimal.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        
      });
      mDelete.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          mDispatcher.sendDelete();
          mDelete.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        
      });
      mPlus.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          mDispatcher.sendKeyBytes(STR_PLUS);
          mPlus.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        
      });
      mMinus.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          mDispatcher.sendKeyBytes(STR_MINUS);
          mMinus.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        
      });
      mTimes.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          mDispatcher.sendKeyBytes(STR_TIMES);
          mTimes.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
       }
        
      });
      mDivide.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          mDispatcher.sendKeyBytes(STR_DIVIDE);
          mDivide.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        
      });
      mEnter.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          mDispatcher.sendEnter();
          mEnter.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        
      });
    }

}
