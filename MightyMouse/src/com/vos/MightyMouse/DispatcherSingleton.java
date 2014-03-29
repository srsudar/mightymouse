package com.vos.MightyMouse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This class keeps the info that must be present across the app to allow for
 * sending data. It is a singleton.
 * @author sudar.sam@gmail.com
 *
 */
public class DispatcherSingleton {
  
  private static final String TAG = DispatcherSingleton.class.getName();
  
  private static DispatcherSingleton mSingleton = null;
  
  // We need this guy to interact with the worker threads.
  private WorkerClass mChatService;
  // This is the context that last fetched the singleton. It is very important
  // that classes are good about acquiring the singleton before using any of 
  // its methods, in case they're holding the same reference but the mContext
  // saved here has changed underneath it. In this case, if the context was 
  // used for something like a toast, I believe it would throw an error as 
  // there would be no place for the context to be called.
  private Context mContext;
  
  /*
   * These should both be null if we're not currently sending a file.
   */
  private SendFileTask mActiveSendFileTask;
  private AlertDialog mFileSendingDialog;
  
  /*
   * The fixed bytes used for sending. The general method for sending a 
   * message should be thought of as:
   * --modify the appropriate bytes
   * --call the appropriate send method.
   * 
   * eg: to send a mouse movement that is a diagonal of 20 up and 10 over, 
   * and no scroll, you would set the mouse bytes to [10][20][0] and then
   * call sendMouse().
   */
  private static final byte DELIMITER[] = {(byte)255};
  private static final byte KEYBOARD[] = {(byte)'K'};
  private static final byte MOUSE[] = {(byte)'M'};
  private static final byte CLICK[] = {(byte)'C'};
  
  /*
   * Here are a number of pre-determined actions. These are the bytes for 
   * things like left click, which require the same number of bytes again and
   * again, and it really makes no sense to be new'ing the same array over and
   * over again.
   */
  private static final byte[] CLICK_LEFT = {(byte) 'L'};
  private static final byte[] CLICK_RIGHT = {(byte) 'R'};
  private static final byte[] KEYCODE_DEL = {(byte) 178};
  private static final byte[] KEYCODE_ENTER = {(byte) 176};
  
  /*
   * These are the number of bytes in the respective arrays.
   */
  public static final int numKeyBytes = 1;
  public static final int numMouseBytes = 3;
  public static final int numClickBytes = 1;
  public static final int numSendBufferBytes = 1;
  /*
   * These are the arrays to hold info that will be passed. The general
   * concept of message passing is as follows: {255}{CHAR}{[ByteArray]}.
   * 
   * The 255 is a value we will never pass, so we use it as a delimeter. If 
   * one of our methods does not begin with 255, we know we dropped a byte.
   * 
   * Below are the byte arrays.
   */
  private final byte[] keyBytes = new byte[numKeyBytes];
  private final byte[] mouseBytes = new byte[numMouseBytes];
  private final byte[] clickBytes = new byte[numClickBytes];
  
  private final byte[] sendBuffer = new byte[numSendBufferBytes];
  
  private DispatcherSingleton(Context context, WorkerClass chatService) {
    this.mChatService = chatService;
    this.mContext = context;
    this.mActiveSendFileTask = null;
    this.mFileSendingDialog = null;
  }
  
  // 
  
  /**
   * Get the singleton, replacing the old or non-existent context with the 
   * parameter context. 
   * <p>
   * It does not to the same with the chatService for the following logic. 
   * Only one activity, the MouseAndKeyboardActivity, sets up the chatService.
   * For this reason I think that we should allow other activies to rely on the
   * fact that the chatService has been set up. Only if the singleton has yet
   * to be called is the chatService used. Therefore as long as the object has
   * been initialized, it is null safe.
   * @param context
   * @param chatService
   * @return
   */
  public static DispatcherSingleton getSingleton(Context context, 
                                          WorkerClass chatService) {
    if (mSingleton == null) {
      mSingleton = new DispatcherSingleton(context, chatService);
    }
    mSingleton.mContext = context;
    return mSingleton;
  }
  
  /**
   * Sends the message char by char.
   * @param message
   */
  public void sendString(String message) {
    if (message.length() > 0) {
      for (int i = 0; i < message.length(); i++) {
        keyBytes[0] = (byte)message.charAt(i);
        sendKeyboard();
      }
    }
  }
  
  /**
   * Convenience method. Equivalent to calling {@link setKeyBytes} and
   * then {@link sendKeyboard}.
   * @param keyBytes
   */
  public void sendKeyBytes(byte[] keyBytes) {
    setKeyBytes(keyBytes);
    sendKeyboard();
  }
  
  /**
   * Call this to send a character using the keyboard protocol.
   * @param character
   */
  public void sendKeyboard() {
    sendDelimiter();
    sendByte(KEYBOARD);
    sendByte(keyBytes);
  }
  
  public void sendMouse() {
    sendDelimiter();
    sendByte(MOUSE);
    sendByte(mouseBytes);
  }
  
  public void sendClick() {
    sendDelimiter();
    sendByte(CLICK);
    sendByte(clickBytes);
  }
  
  /**
   * Modify the clickBytes array in place. Only the number of 
   * {@link numClickBytes} are set in the array.
   * @param bytes
   */
  public void setClickBytes(byte[] bytes) {
    for (int i = 0; i < numClickBytes; i++) {
      clickBytes[i] = bytes[i];
    }
  }
  
  /**
   * Set the mouseBytes to the passed in bytes. In the interest of speed, no
   * check on length is performed. Only the first {@link numMouseBytes} bytes
   * of the passed in array is set.
   * @param mouseInfo
   */
  public void setMouseBytes(byte[] mouseInfo) {
    for (int i = 0; i < numMouseBytes; i++) {
      mouseBytes[i] = mouseInfo[i];
    }
  }
  
  /**
   * Set the key bytes to be sent. Only the number of {@link numKeyBytes} is
   * changed to the array.
   * @param keyInfo
   */
  public void setKeyBytes(byte[] keyInfo) {
    for (int i = 0; i < numKeyBytes; i++) {
      keyBytes[i] = keyInfo[i];
    }
  }
  
  /**
   * Send a left click.
   */
  public void sendLeftClick() {
    setClickBytes(CLICK_LEFT);
    sendClick();
  }
  
  /**
   * Send a right click.
   */
  public void sendRightClick() {
    setClickBytes(CLICK_RIGHT);
    sendClick();
  }
  
  /**
   * Write the delimiter to the pipe.
   */
  private void sendDelimiter() {
    sendByte(DELIMITER);
  }
  
  /**
   * Sends a byte across the wire. Based on the original sendMessage().
   * @param toSend
   */
  private void sendByte(byte[] toSend) {
    // Check that we're actually connected before trying anything
    if (mChatService.getState() != WorkerClass.STATE_CONNECTED) {
      Log.e(TAG, "i'm so broke (and can't sendByte())");
      Log.e(TAG, "WorkerClass state: " + mChatService.getState());
//        Toast.makeText(mContext, R.string.not_connected, 
//          Toast.LENGTH_SHORT).show();
        return;
    }
    mChatService.write(toSend);
  }
  
  /**
   * Send a delete key press. Equivalent to calling {@link setKeyBytes} with 
   * the code for a delete key and calling {@link sendKeybaord}.
   */
  public void sendDelete() {
    setKeyBytes(KEYCODE_DEL);
    sendKeyboard();
  }
  
  /**
   * Send an enter key press. Equivalent to calling {@link setKeyBytes} with 
   * the code for an enter key and calling {@link sendKeybaord}.
   */
  public void sendEnter() {
    setKeyBytes(KEYCODE_ENTER);
    sendKeyboard();
  }
  
  /**
   * Send the contents of a file over the keyboard. The final name should be
   * ...canonical, I think? Where all the '.' and '..' are replace? I'm not 
   * actually positive about this. But I think as long as this handles what is 
   * returned by FileListActivity, everything should be ok.
   * @param filename
   */
  public void sendFile(String filename) {
    if (mActiveSendFileTask != null) {
      // don't start another one if one is ongoing.
      Log.e(TAG, "tried to send file: " + filename + ", while a file task " +
      		"was already active");
      return;
    }
    mActiveSendFileTask = new SendFileTask();
    View dialogView = LayoutInflater.from(mContext).inflate(
      R.layout.sending_file_dialog,
      null);
    TextView cancelMessage = 
        (TextView) dialogView.findViewById(R.id.sending_file_dialog_text);
    Button cancelButton = 
        (Button) dialogView.findViewById(R.id.cancel_file_send_button);
    cancelButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Log.d(TAG, "user canceled the file send");
        mActiveSendFileTask.canceled = true;
      }
    });
    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
    builder.setView(dialogView);
    mFileSendingDialog = builder.create();
    mFileSendingDialog.show();
    mActiveSendFileTask.execute(filename);
  }
  
  /**
   * Async task used to send a file.
   * 
   * @author sudars
   * 
   */
  public class SendFileTask extends AsyncTask<Object, Integer, Integer> {
    
    private final String TAG = SendFileTask.class.getName();

    // Whether or not the user has canceled the send.
    boolean canceled = false;

    // Return codes from doInBackground.
    final Integer SENT_SUCCESSFULLY = 1;
    final Integer EXCEPTION_CAUGHT = 2;
    final Integer USER_CANCELED = 3;
    
    // This is just a local buffer we'll be changing in place to avoid having
    // to malloc an array each time we send a character. In the case of 
    // something like Hamlet, this would be a whole heap of bytes.
    private byte[] keyBytesBuffer; 
    
    //private SendFileTaskCallbacks mCaller;

    String filename = null;

    /**
     * params expected to be [Integer].
     */
    @Override
    protected Integer doInBackground(Object... params) {
      keyBytesBuffer = new byte[DispatcherSingleton.numKeyBytes];
      filename = (String) params[0];
      File toSend = new File(filename);
      try {
        FileReader fr = new FileReader(toSend);
        byte charToSend;
        do {
          charToSend = (byte) fr.read();
          // NB: if numKeyBytes is ever > 1, we'll have to be sure to wipe out
          // the old state before sending.
          keyBytesBuffer[0] = charToSend;
          setKeyBytes(keyBytesBuffer);
          sendKeyboard();
          Thread.sleep(25);
        } while (!canceled && charToSend != (int) -1);
        fr.close();
        if (canceled) {
          return USER_CANCELED;
        }
        return SENT_SUCCESSFULLY;
      } catch (FileNotFoundException e) {
        Log.e(TAG, "file not found to read");
        e.printStackTrace();
        return EXCEPTION_CAUGHT;
      } catch (IOException e) {
        Log.e(TAG, "ioexception while reading file");
        e.printStackTrace();
        return EXCEPTION_CAUGHT;
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        return EXCEPTION_CAUGHT;
      }
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {

    }

    @Override
    protected void onPostExecute(Integer result) {
      if (mFileSendingDialog != null) {
        mFileSendingDialog.dismiss();
        Log.d(TAG, "tried to dismiss null fileSendingDialog");
      }
      if (result == SENT_SUCCESSFULLY) {
        Toast.makeText(mContext, 
          mContext.getString(R.string.sent_file_successfully),
            Toast.LENGTH_LONG).show();
      } else if (result == EXCEPTION_CAUGHT) {
        Toast.makeText(mContext, 
          mContext.getString(R.string.exception_sending_file),
            Toast.LENGTH_LONG).show();
      } else if (result == USER_CANCELED) {
        Toast.makeText(mContext, 
          mContext.getString(R.string.user_canceled_send),
            Toast.LENGTH_LONG).show();
      } else {
        Log.e(TAG, "onPostExecute received unrecognized return param: " + result);
      }
      // reset the state so we can start another task.
      mActiveSendFileTask = null;
      mFileSendingDialog = null;
    }

  }

}
