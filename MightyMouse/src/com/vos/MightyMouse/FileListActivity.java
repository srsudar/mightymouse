package com.vos.MightyMouse;

import java.io.File;

import com.vos.MightyMouse.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * The idea here is that we want to save some files that we will then send
 * over to the computer. Clicking on one of these files will either 
 * immediately send or give the option to send it. 
 * <p>
 * "Send it" means to type it out via the keyboard as if you'd typed the whole
 * thing. The choosing of the files isn't as obvious...
 * @author sudar.sam@gmail.com
 *
 */
public class FileListActivity extends Activity {

  private static final String TAG = "FileListActivity";
  private static final boolean D = true;
  
  public static final String EXTRA_FILE_NAME = "FILE_NAME";
  
  private static final String FOLDER = "files";
  
  // Member fields.
  private ArrayAdapter<File> mFilesArrayAdapter;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Set the layout
    setContentView(R.layout.file_list);
    // Hmm, interesting. DeviceListActivity is saying you should set the result
    // to be canceled, in case the user quits.
    setResult(Activity.RESULT_CANCELED);
    
    // Now init the array adapter.
    mFilesArrayAdapter = new ArrayAdapter<File>(this, R.layout.file_name);
    
    // Now create the folder to save the files in. 
    File extPath = Environment.getExternalStorageDirectory();
    String fileFolderPath = extPath.getAbsolutePath() + "/" + FOLDER;
    File fileFolder = new File(fileFolderPath);
    if (!fileFolder.exists()) {
      boolean madeFolder = fileFolder.mkdir();
      if (!madeFolder) {
        Log.e(TAG, "could not create folder like you would expect");
      }
    }
    
    File[] files = fileFolder.listFiles();
    for (int i = 0; i < files.length; i++) {
      mFilesArrayAdapter.add(files[i]);
    }
    
    ListView fileListListView = (ListView) findViewById(R.id.file_list_view);
    fileListListView.setAdapter(mFilesArrayAdapter);
    fileListListView.setOnItemClickListener(mFileClickListener);
    
  }
  
  // The on-click listener for the files. 
  private OnItemClickListener mFileClickListener = 
      new OnItemClickListener() {
    public void onItemClick(AdapterView<?> av, View v, int position, 
        long itemId) {
      Intent result = new Intent();
      result.putExtra(EXTRA_FILE_NAME, 
          ((File)av.getItemAtPosition(position)).getAbsolutePath());
      setResult(RESULT_OK, result);
      finish();
    }
  };
}

