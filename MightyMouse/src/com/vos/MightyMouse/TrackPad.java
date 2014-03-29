package com.vos.MightyMouse;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

/**
 * The hope is that this will listen for mouse events to serve as a trackpad.
 * 
 * This has to be static to inflate, but really it should be a non-static 
 * member class, as we only
 * ever want one per view, most likely. Not necessarily a huge deal, though.
 * 
 * @author sudar.sam@gmail.com
 * 
 */
public class TrackPad extends View {

  private static final String TAG = "TrackPad";

  // this is what we will scale delta by to try and get better numbers.
  // but we'll actually be dividing by this.
  private static final long DELTA_SCALE = (long) 1.0;

  // I want to store the old x and old y so that I can measure from them.
  private float mStartX;
  private float mStartY;
  private long lastTime;
  
  // These should be the sums of mouse events that are added. Be sure to access
  // them in a threadsafe manner.
  private Object mLock;
  private float mRunningTotalX;
  private float mRunningTotalY;
  // this will store the time of the first event to send.
  private long mTimeOfFirstToSend;
  private long mTimeOfLastToSend;
  // These will be the last measured so we know what to diff to and then add
  // to the running total. (eg if you get a move with x of 7, that is only 
  // interesting if you know where that 7 came from.
  private float mLastX;
  private float mLastY;
  
  // This will be the minimum movement that can occur--essentially the unit
  // of movement.
  private float mBaseUnitX;
  private float mBaseUnitY;

  // this will be if the numbers are ok. Otherwise we need to get new ones.
  private boolean isValid;

  private Context mContext;
  
  private Thread mSenderThread;
  private MouseSender mSenderRunnable;
  
  // This should be the queue that holds the mouse events. Must be thread 
  // safe for the runnable thing to use it correctly.
  private ConcurrentLinkedQueue<MotionEvent> mMouseQueue;

  private MouseAndKeyboardActivity mChat;

  // the time between events.
  private long mDelta;
  
  // The dispatcher object. Must be the one from the Activity holding this 
  // view.
  private DispatcherSingleton mDispatcher;

  // Based on the code here it looks like I need this to listen to
  // gestures?
  // http://www.anddev.org/gesturedetector_and_gesturedetectorongesturelistener-t3204.html
  private GestureDetector mGestureScanner;

  private final byte[] mouseInfoToRelay = 
      new byte[mDispatcher.numMouseBytes];

  public TrackPad(Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
    this.mContext = context;
    init();
  }

  /**
   * This is where we should send the mouse signal.
   */
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    /*
     * Rather than trying to pass this on to the gesture listener, let's 
     * just try to compute it
     * here, as this should be all we need.
     */
    // Let's get all the Evens in the batch.
    if (event.getAction() == MotionEvent.ACTION_UP
        || event.getAction() == MotionEvent.ACTION_CANCEL) {
      Log.d(TAG, "action type was action up or action cancel");
      // In this case we don't scroll, and we invalidate the numbers.
      isValid = false;
    }
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      Log.d(TAG, "action down");
      synchronized (mLock) {
        mStartX = event.getX();
        mStartY = event.getY();
        lastTime = event.getEventTime();
        mLastX = event.getX();
        mLastY = event.getY();
      }
      // here we also need to set the base units. We can't do it in the init 
      // steps because until now we don't know what the device id is that will
      // be generating the events.
      InputDevice device = InputDevice.getDevice(event.getDeviceId());
      Log.d(TAG, "input device id: " + event.getDeviceId());
      this.mBaseUnitX = device.getMotionRange(MotionEvent.AXIS_X).getFuzz();
      this.mBaseUnitY = device.getMotionRange(MotionEvent.AXIS_Y).getFuzz();
    }
    if (event.getAction() == MotionEvent.ACTION_MOVE) {
      Log.d(TAG, "action move");
      // Take care of any batched movements.
      synchronized (mLock) {
        for (int i = 0; i < event.getHistorySize(); ++i) {
          // This is assuming we only have one pointer, which I think has
          // to be 0. (A pointer is like a finger.)
          
          // We need to init the time if there are no totals yet.
          if (mTimeOfFirstToSend == 0) {
            mTimeOfFirstToSend = event.getHistoricalEventTime(i);
          }
          mRunningTotalX += event.getHistoricalX(0, i) - mLastX;
          mRunningTotalY += event.getHistoricalY(0, i) - mLastY;
          mLastX = event.getHistoricalX(0, i);
          mLastY = event.getHistoricalY(0, i);
          // This was the old action before we did the concurrent queue:
          //computeAndSend(event.getHistoricalX(0, i), 
           // event.getHistoricalY(0, i),
            //  event.getHistoricalEventTime(i));
        }
        // do the current movement
        //computeAndSend(event.getX(), event.getY(), event.getEventTime());
        if (mTimeOfFirstToSend == 0) {
          mTimeOfFirstToSend = event.getEventTime();
        }
        mTimeOfLastToSend = event.getEventTime();
        mRunningTotalX += event.getX() - mLastX;
        mRunningTotalY += event.getY() - mLastY;
        Log.d(TAG, "last event x: " + event.getX() + "; y: " + event.getY());
        mLastX = event.getX();
        mLastY = event.getY();
      }
    }
    return true;
  }
  
  /**
   * There is a thread sending the mouse actions while this view is up and
   * running. It should be stopped (the while loop in the run function) when
   * it isn't needed.
   */
  public void stopSendingThread() {
    Log.d(TAG, "in stopSendingThread()");
    mSenderRunnable.mKeepRunning = false;
    
  }
  
  /**
   * Start the sender thread up. Will throw an exception if the thread is 
   * already running.
   */
  public void startSendingThread() {
    Log.w(TAG, "in startSendingThread()");
    mSenderRunnable.mKeepRunning = true;
    mSenderThread.start();
  }
  
  public boolean sendingThreadIsAlive() {
    return mSenderThread.isAlive();
  }

  /*
   * Does what you would need to do for a single motion event. Computes the 
   * deltas, updates the
   * member fields storing previous state, and sends the mouse info.
   */
  private void computeAndSend(float x, float y, long time) {
    float xDelta;
    float yDelta;
    // Time change
    long tDelta;
    // Delta / time change
    double xVelDbl;
    double yVelDbl;
    // values to send
    int xVel;
    int yVel;
    // a somewhat scaled time delta
    double tFloat;
    xDelta = x - mStartX;
    mStartX = x;
    yDelta = y - mStartY;
    mStartY = y;
    tDelta = time - lastTime;
    // try to scale it.
    tFloat = tDelta * 1.0;
    lastTime = time;
    xVelDbl = xDelta / (tFloat * 1.0);
    yVelDbl = yDelta / (tFloat * 1.0);
    xVel = -Math.round(xDelta);
    yVel = -Math.round(yDelta);
    if (xVel > 127) {
      xVel = 127;
    } else if (xVel < -127) {
      xVel = -127;
    }
    if (yVel > 127) {
      yVel = 127;
    } else if (yVel < -127) {
      yVel = -127;
    }
    mouseInfoToRelay[0] = (byte) -xVel;
    Log.d(TAG, "xVel = " + -xVel);
    mouseInfoToRelay[1] = (byte) -yVel;
    Log.d(TAG, "yVel = " + -yVel);
    mouseInfoToRelay[2] = (byte) 0;
    mDispatcher.setMouseBytes(mouseInfoToRelay);
    if (mChat == null) {
      Log.d(TAG, "mChat is null");
    } else {
      Log.d(TAG, "asking to send mouse");
      mDispatcher.sendMouse();
    }
  }
  
  public void setDispatcher(DispatcherSingleton dispatcher) {
    this.mDispatcher = dispatcher;
  }

  private void init() {
    if (mContext instanceof MouseAndKeyboardActivity) {
      mChat = (MouseAndKeyboardActivity) mContext;
    }
    mLock = new Object();
    synchronized (mLock) {
      mRunningTotalX = 0;
      mRunningTotalY = 0;
      mTimeOfFirstToSend = 0;
      mTimeOfLastToSend = 0;
    }
    this.mSenderRunnable = new MouseSender();
    this.mSenderThread = new Thread(this.mSenderRunnable);
    this.mSenderThread.start();
  }

  public void waitUntilStop(){
    Log.w(TAG,"Waiting for mouse sender thread to die");
    while(this.mSenderThread.isAlive()){
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    Log.w(TAG,"Mouse sender thread has died!");
  }
  
  public TrackPad(Context context, DispatcherSingleton dispatcher) {
    super(context);
    init();
    this.mContext = context;
    this.mDispatcher = dispatcher;
    // AS OF FEB 22--KNOWN ISSUE IS THAT IT DOESN'T ALWAYS CALL THIS 
    // CONSTRUCTOR--IN FACT OFTEN CALLS THE OTHER ONE AND STUFF DOESN'T GET 
    // INITED.
  }
  
  /**
   * The point of this class is to send information via the dispatcher. 
   * The TrackPad outer class is responsible for aggregating the mouse events,
   * and then this actually communicates with the dispatcher.
   * @author sudar.sam@gmail.com
   *
   */
  private class MouseSender implements Runnable {
    
    public volatile boolean mKeepRunning;
        
    public MouseSender() {
      super();
      this.mKeepRunning = true;
    }

    @Override
    public void run() {
      while (mKeepRunning) {
        Log.e(TAG, "in MouseSender.run()");
        if (mRunningTotalX != 0 || mRunningTotalY != 0) {
          Log.d(TAG, "xFuzz: " + mBaseUnitX);
          Log.d(TAG, "yFuzz: " + mBaseUnitY);
          float xDelta;
          float yDelta;
          // Time change
          long tDelta;
          // Delta / time change
          double xVelDbl;
          double yVelDbl;
          // values to send
          int xVel;
          int yVel;
          // a somewhat scaled time delta
          double tFloat;
          xDelta = mRunningTotalX - mStartX;
          //mStartX = x;
          yDelta = mRunningTotalY - mStartY;
          //mStartY = y;
          tDelta = mTimeOfLastToSend - mTimeOfFirstToSend;
          // try to scale it.
          tFloat = tDelta * 1.0;
          //lastTime = time;
          xVelDbl = xDelta / (tFloat * DELTA_SCALE);
          yVelDbl = yDelta / (tFloat * DELTA_SCALE);
          float testScaledValueX;
          float testScaledValueY;
          float testVelVector;
          testVelVector = (float) (Math.sqrt(Math.pow(mRunningTotalX, 2) + 
            Math.pow(mRunningTotalY, 2)) / tDelta);
          Log.d(TAG, "testVelVector: " + testVelVector);
          Log.d(TAG, "mRunningTotalX: " + mRunningTotalX);
          Log.d(TAG, "mRunningTotalY: " + mRunningTotalY);
          testScaledValueX = mRunningTotalX * testVelVector;
          testScaledValueY = mRunningTotalY * testVelVector;
          if (mRunningTotalX < 1 && mRunningTotalX > 0) {
            mRunningTotalX = 1;
          }
          if (mRunningTotalX > -1 && mRunningTotalX < 0) {
            mRunningTotalX = -1;
          }
          if (mRunningTotalY < 1 && mRunningTotalY > 0) {
            mRunningTotalY = 1;
          }
          if (mRunningTotalY > -1 && mRunningTotalY < 0) {
            mRunningTotalY = -1;
          }
          xVel = -Math.round(mRunningTotalX);
          yVel = -Math.round(mRunningTotalY);
          if (xVel > 127) {
            xVel = 127;
          } else if (xVel < -127) {
            xVel = -127;
          }
          if (yVel > 127) {
            yVel = 127;
          } else if (yVel < -127) {
            yVel = -127;
          }
          mouseInfoToRelay[0] = (byte) -xVel;
          Log.d(TAG, "xVel = " + -xVel);
          mouseInfoToRelay[1] = (byte) -yVel;
          Log.d(TAG, "yVel = " + -yVel);
          mouseInfoToRelay[2] = (byte) 0;
          mDispatcher.setMouseBytes(mouseInfoToRelay);
          if (mChat == null) {
            Log.d(TAG, "mChat is null");
          } else {
            Log.d(TAG, "asking to send mouse");
            Log.d(TAG, "x: " + -xVel + "; y: " + -yVel);
            mDispatcher.sendMouse();
          }
          synchronized (mLock) {
            mRunningTotalX = 0;
            mRunningTotalY = 0;
            mTimeOfFirstToSend = 0;
            mTimeOfLastToSend = 0;
            // This catches if you're moving slowly and haven't yet done a new
            // down event to reset these variables.
            mStartX = mLastX;
            mStartY = mLastY;
          }
        }
        // Then we sleep to let new events build up.
        try {
          Thread.sleep(25);
        } catch (InterruptedException e) {
          Log.e(TAG, "interrupted exception in the MouseSender's run method");
          e.printStackTrace();
        }
      }
    }
  }

}
