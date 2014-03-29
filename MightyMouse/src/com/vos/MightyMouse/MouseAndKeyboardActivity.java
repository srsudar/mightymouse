/*
 * Copyright (C) 2009 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express
 * or implied. See the License for the specific language governing permissions 
 * and limitations under
 * the License.
 */

package com.vos.MightyMouse;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays a text box to enter messages into 
 * and a trackpad to serve
 * as a mouse.
 */
public class MouseAndKeyboardActivity extends Activity {
  // Debugging
  private static final String TAG = "MouseAndKeyboardActivity";
  private static final boolean D = true;

  // Message types sent from the WorkerClass Handler
  public static final int MESSAGE_STATE_CHANGE = 1;
  public static final int MESSAGE_READ = 2;
  public static final int MESSAGE_WRITE = 3;
  public static final int MESSAGE_DEVICE_NAME = 4;
  public static final int MESSAGE_TOAST = 5;

  // Key names received from the WorkerClass Handler
  public static final String DEVICE_NAME = "device_name";
  public static final String TOAST = "toast";

  // Intent request codes
  private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
  private static final int REQUEST_SEND_FILE = 2;
  private static final int REQUEST_ENABLE_BT = 3;

  // Layout Views
  private Button mDeleteButton;
  private EditText mOutEditText;
  private Button mSendButton;
  
  // If true, you do not need to press enter to send text. As you type it 
  // will be sent to the keyboard.
  private Boolean mAutoSend;
  private Boolean DEFAULT_AUTO_SEND = false;

  /*
   * The mouse buttons.
   */
  private Button mLeftClickButton;
  private Button mRightClickButton;

  // Name of the connected device
  private String mConnectedDeviceName = null;
  // String buffer for outgoing messages
  private StringBuffer mOutStringBuffer;
  // Local Bluetooth adapter
  private BluetoothAdapter mBluetoothAdapter = null;
  // Member object for the chat services
  private WorkerClass mChatService = null;
  // The dispatcher object. I think it is safest to re-get the singleton
  // in this activity's onResume(), just in case something happened to it in
  // another activity. I'm not positive this is actually necessary, but I
  // don't think it really matters.
  private DispatcherSingleton mDispatcher;
  private TrackPad mTrackPadView;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (D) Log.e(TAG, "+++ ON CREATE +++");

    // Set up the window layout
    setContentView(R.layout.main);

    // Get local Bluetooth adapter
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    

    // If the adapter is null, then Bluetooth is not supported
    if (mBluetoothAdapter == null) {
      Toast.makeText(this, "Bluetooth is not available", 
        Toast.LENGTH_LONG).show();
      finish();
      return;
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    if (D) Log.e(TAG, "++ ON START ++");

    // If BT is not on, request that it be enabled.
    // setupChat() will then be called during onActivityResult
    if (!mBluetoothAdapter.isEnabled()) {
      Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
      // Otherwise, setup the chat session
    } else {
      if (mChatService == null) setupChat();
    }
  }

  @Override
  public synchronized void onResume() {
    super.onResume();
    if (D) Log.e(TAG, "+ ON RESUME +");
    
    this.mAutoSend = DEFAULT_AUTO_SEND;
     
    // Performing this check in onResume() covers the case in which BT was
    // not enabled during onStart(), so we were paused to enable it...
    // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
    if (mChatService != null) {
      // Only if the state is STATE_NONE, do we know that we haven't
      // started already
      if (mChatService.getState() == WorkerClass.STATE_NONE) {
        // Start the Bluetooth chat services
        mChatService.start();
      }
    }
    // and now init the dispatcher.
    this.mDispatcher = DispatcherSingleton.getSingleton(this, mChatService);
    this.mTrackPadView = (TrackPad) findViewById(R.id.trackPad);
    this.mTrackPadView.setDispatcher(mDispatcher);
  }

  private void setupChat() {
    Log.d(TAG, "setupChat()"); 
    
    // Set up the delete button.
    mDeleteButton = (Button) findViewById(R.id.button_edit_text_delete);
    mDeleteButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        mDispatcher.sendDelete();
      }
      
    });

    // Initialize the compose field with a listener for the return key
    mOutEditText = (EditText) findViewById(R.id.edit_text_out);
    mOutEditText.setOnEditorActionListener(mWriteListener);
    mOutEditText.setOnKeyListener(mInterceptKeyboard);
    mOutEditText.addTextChangedListener(new TextWatcher() {

      @Override
      public void afterTextChanged(Editable s) {
        // So here we want to intercept the edit text.
        if (s.length() > 0 && mAutoSend) {
          String message = mOutEditText.getText().toString();
          mDispatcher.sendString(message);
          mOutStringBuffer.setLength(0);
          mOutEditText.setText(mOutStringBuffer.toString());
        }
        if (s.length() > 0) {
          mDeleteButton.setVisibility(View.GONE);
        } else {
          mDeleteButton.setVisibility(View.VISIBLE);
        }
        if (mAutoSend) {
          mDeleteButton.setVisibility(View.VISIBLE);
        }
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, 
                                    int after) {
        // do nothing
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, 
                                int count) {
        // do nothing
      }
      
    });


    // Initialize the send button with a listener that for click events
    mSendButton = (Button) findViewById(R.id.button_send);
    mSendButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        // Send a message using content of the edit text widget
        TextView view = (TextView) findViewById(R.id.edit_text_out);
        String message = view.getText().toString();

        mDispatcher.sendString(message);
        // Reset out string buffer to zero and clear the edit text field
        mOutStringBuffer.setLength(0);
        mOutEditText.setText(mOutStringBuffer);

      }
    });

    // Initialize the WorkerClass to perform bluetooth connections
    mChatService = new WorkerClass(this, mHandler);

    // Initialize the buffer for outgoing messages
    mOutStringBuffer = new StringBuffer("");

    mLeftClickButton = (Button) findViewById(R.id.leftClick);
    mLeftClickButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        mDispatcher.sendLeftClick();
      }
    });

    mRightClickButton = (Button) findViewById(R.id.rightClick);
    mRightClickButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        mDispatcher.sendRightClick();
      }
    });
  }



  private View.OnKeyListener mInterceptKeyboard = new View.OnKeyListener() {

    /**
     * Sadly this doesn't work in all cases. On JellyBean and above it seems 
     * that it won't work at all. So, we need to add a delete button. And we
     * won't be able to reliably do this stuff. Huge bummer, unless we make our
     * own keyboard.
     */
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
      TextView view = (TextView) findViewById(R.id.edit_text_out);
      // if the view is empty we might want to send enter or del.
      // or even another special character. The characters we send should
      // be able to be handled by the arduino keyboard. The codes are
      // located here:
      // http://arduino.cc/en/Reference/KeyboardModifiers
      if (view.getText().length() == 0 
          && event.getAction() == KeyEvent.ACTION_UP) {
        switch (keyCode) {
          case KeyEvent.KEYCODE_DEL:
            mDispatcher.sendDelete();
            return true;
          case KeyEvent.KEYCODE_ENTER:
            mDispatcher.sendEnter();
            return true;
          default: // was empty but nothing we wanted
            return false;
        }
      } else {
        return false;
      }
    };
  };

  @Override
  public synchronized void onPause() {
    super.onPause();
    if (D) Log.e(TAG, "- ON PAUSE -");
  }

  @Override
  public void onStop() {
    super.onStop();
    // We need to be sure to stop the thread sending the mouse.
    mTrackPadView.stopSendingThread();
    mTrackPadView.waitUntilStop();
    if (D) Log.e(TAG, "-- ON STOP --");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    // Double check stopping this thread, just in case.
    mTrackPadView.stopSendingThread();
    mTrackPadView.waitUntilStop();
    // Stop the Bluetooth chat services
    if (mChatService != null) mChatService.stop();
    if (D) Log.e(TAG, "--- ON DESTROY ---");
  }

  private void ensureDiscoverable() {
    if (D) Log.d(TAG, "ensure discoverable");
    if (mBluetoothAdapter.getScanMode() != 
        BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
      Intent discoverableIntent = 
          new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
      discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
        300);
      startActivity(discoverableIntent);
    }
  }

  // The action listener for the EditText widget, to listen for the return key
  private TextView.OnEditorActionListener mWriteListener = 
      new TextView.OnEditorActionListener() {
    public boolean onEditorAction(TextView view, int actionId, 
                                  KeyEvent event) {
      // If the action is a key-up event on the return key, send the message

      if (actionId == EditorInfo.IME_NULL 
          && event.getAction() == KeyEvent.ACTION_UP) {
        // If we're empty, we're going to send the a return key.
        if (view.getText().length() == 0) {
          mDispatcher.sendEnter();
        } else {
          String message = view.getText().toString();
          mDispatcher.sendString(message);
          view.setText("");
        }
      } 
      if (D) Log.i(TAG, "END onEditorAction");

     return true;
    }
  };

  private final void setStatus(int resId) {
    final ActionBar actionBar = getActionBar();
    actionBar.setSubtitle(resId);
  }

  private final void setStatus(CharSequence subTitle) {
    final ActionBar actionBar = getActionBar();
    actionBar.setSubtitle(subTitle);
  }

  // The Handler that gets information back from the WorkerClass
  private final Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MESSAGE_STATE_CHANGE:
          if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
          switch (msg.arg1) {
            case WorkerClass.STATE_CONNECTED:
              setStatus(getString(R.string.title_connected_to, 
                mConnectedDeviceName));
              break;
            case WorkerClass.STATE_CONNECTING:
              setStatus(R.string.title_connecting);
              break;
            case WorkerClass.STATE_LISTEN:
            case WorkerClass.STATE_NONE:
              setStatus(R.string.title_not_connected);
              break;
          }
          break;
        case MESSAGE_WRITE:
          byte[] writeBuf = (byte[]) msg.obj;
          // construct a string from the buffer
          String writeMessage = new String(writeBuf);
          break;
        case MESSAGE_READ:
          byte[] readBuf = (byte[]) msg.obj;
          // construct a string from the valid bytes in the buffer
          String readMessage = new String(readBuf, 0, msg.arg1);
          break;
        case MESSAGE_DEVICE_NAME:
          // save the connected device's name
          mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
          Toast.makeText(getApplicationContext(), "Connected to " + 
              mConnectedDeviceName, Toast.LENGTH_SHORT).show();
          break;
        case MESSAGE_TOAST:
          Toast.makeText(getApplicationContext(), 
            msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
          break;
      }
    }
  };

  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (D) Log.d(TAG, "onActivityResult " + resultCode);
    switch (requestCode) {
      case REQUEST_CONNECT_DEVICE_SECURE:
        // When DeviceListActivity returns with a device to connect
        if (resultCode == Activity.RESULT_OK) {
          connectDevice(data, true);
        }
        break;
      case REQUEST_SEND_FILE:
        // So, we've come back and we just finished sending a file. So,
        // there should be some information...aha, it's in the intent!
        // It's in an extra.
        Log.d(TAG, "requesting send file");
        if (resultCode == Activity.RESULT_OK) {
          // we need to get the info.
          String fileName = 
              data.getExtras().getString(FileListActivity.EXTRA_FILE_NAME);
          Log.d(TAG, "file to send: " + fileName);
          mDispatcher.sendFile(fileName);
        }
        break;
      case REQUEST_ENABLE_BT:
        // When the request to enable Bluetooth returns
        if (resultCode == Activity.RESULT_OK) {
          // Bluetooth is now enabled, so set up a chat session
          setupChat();
        } else {
          // User did not enable Bluetooth or an error occurred
          Log.d(TAG, "BT not enabled");
          Toast.makeText(this, R.string.bt_not_enabled_leaving, 
            Toast.LENGTH_SHORT).show();
          finish();
        }
    }
  }

  private void connectDevice(Intent data, boolean secure) {
    // Get the device MAC address
    String address = 
        data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
    // Get the BluetoothDevice object
    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
    // Attempt to connect to the device
    mChatService.connect(device, secure);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.option_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent serverIntent = null;
    switch (item.getItemId()) {
      case R.id.secure_connect_scan:
        // Launch the DeviceListActivity to see devices and do scan
        serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
        return true;
      case R.id.open_file_list:
        // Launch the DeviceListActivity to see devices and do scan
        serverIntent = new Intent(this, FileListActivity.class);
        startActivityForResult(serverIntent, REQUEST_SEND_FILE);
        return true;
      case R.id.discoverable:
        // Ensure this device is discoverable by others
        ensureDiscoverable();
        return true;
      case R.id.check_box_auto_send:
        // invert the mAutoSend boolean and the checked state of the item.
        mAutoSend = (mAutoSend) ? false : true;
        item.setChecked(mAutoSend);
        if (D) Log.d(TAG, "mAutoSend state changed to: " + mAutoSend);
        return true;
      case R.id.launch_ten_key:
        Intent tenKeyIntent = new Intent(this, TenKeyActivity.class);
        startActivity(tenKeyIntent);
        return true;
      default:
        Log.e(TAG, "unrecognized menu item: " + item);
        return false;
    }
  }
  
}
