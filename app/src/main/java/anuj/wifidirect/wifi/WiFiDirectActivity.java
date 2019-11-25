/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package anuj.wifidirect.wifi;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
//import com.google.api.GoogleAPI;
//import com.google.api.translate.Language;
//import com.google.api.translate.Translate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import anuj.wifidirect.R;
import anuj.wifidirect.utils.PermissionsAndroid;

/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */
public class WiFiDirectActivity extends AppCompatActivity implements ChannelListener, DeviceListFragment.DeviceActionListener {


    private Toolbar mToolbar;
    public static final String TAG = "wifidirectdemo";
    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver receiver = null;

    Spinner first_spinner,last_spinner;
    SpeechRecognizer recognizer;
    String CURRENT_LANGUAGE;
    String language="nyn";

    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocalHelper.onAttach(newBase,"en"));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //Paper.init(this);
        initViews();
        updateViews(language);


        first_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                CURRENT_LANGUAGE=first_spinner.getSelectedItem().toString();
                if(i==0){
                    last_spinner.setSelection(1);
                    language="en";
                }else{
                    last_spinner.setSelection(0);
                    language="nyn";
                }
                updateViews(language);
                DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.frag_detail);
                //fragmentDetails.sendMessage("INDEX:"+i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        last_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(i==0){
                    first_spinner.setSelection(1);
                }else{
                    first_spinner.setSelection(0);
                }
                CURRENT_LANGUAGE=first_spinner.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        loadPreferences();
        CURRENT_LANGUAGE=first_spinner.getSelectedItem().toString();



        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        checkStoragePermission();
    }

    private void updateViews(String language) {

        Context context=LocalHelper.setLocale(this,language);
        Resources resources=context.getResources();
        /*Locale locale=new Locale(language);
        Locale.setDefault(locale);
        Configuration conf=new Configuration();
        conf.locale=locale;
        getBaseContext().getResources().updateConfiguration(conf,resources.getDisplayMetrics());*/
        DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        fragmentList.peers_textview.setText(resources.getString(R.string.label_peers));

    }

    public void savePreferences(){
        SharedPreferences sharedPreferences=getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        editor.putInt("first_spinner_int",first_spinner.getSelectedItemPosition());
        editor.commit();
    }
    private void loadPreferences(){
        SharedPreferences sharedPreferences=getPreferences(MODE_PRIVATE);
        first_spinner.setSelection(sharedPreferences.getInt("first_spinner_int",0));
    }

    /*
      Ask permissions for Filestorage if device api > 23
       */
    //  @TargetApi(Build.VERSION_CODES.M)
    private void checkStoragePermission() {
        boolean isExternalStorage = PermissionsAndroid.getInstance().checkWriteExternalStoragePermission(this);
        if (!isExternalStorage) {
            PermissionsAndroid.getInstance().requestForWriteExternalStoragePermission(this);
        }
    }

    private void initViews() {
        // add necessary intent values to be matched.
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
//        getSupportActionBar().setTitle("WifiDirect");

        first_spinner=(Spinner)findViewById(R.id.first_spinner) ;
        last_spinner=(Spinner)findViewById(R.id.last_spinner) ;


        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), this);
    }

    @Override
    public void onBackPressed() {
        savePreferences();
        super.onBackPressed();
        //unregisterReceiver(receiver);
    }
    /**
     * register the BroadcastReceiver with the intent values to be matched
     */

    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    public void resetData() {
        DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.frag_detail);
        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.atn_direct_enable:
                if (manager != null && channel != null) {

                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.

                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                } else {
                    Log.e(TAG, "channel or manager is null");
                }
                return true;

            case R.id.atn_direct_discover:
                createConnection();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void createConnection(){
        if (!isWifiP2pEnabled) {
            Toast.makeText(this, "Wifi Disconnected", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
        }
        else {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            fragment.onInitiateDiscovery();
            manager.discoverPeers(channel, new ActionListener() {

                @Override
                public void onSuccess() {
                    Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated",
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reasonCode) {
                    Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    @Override
    public void showDetails(WifiP2pDevice device) {
        DeviceDetailFragment fragment = (DeviceDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);

    }

    @Override
    public void connect(WifiP2pConfig config) {
        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void disconnect() {
        final DeviceDetailFragment fragment = (DeviceDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.resetViews();
        manager.removeGroup(channel, new ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

            }

            @Override
            public void onSuccess() {
                fragment.getView().setVisibility(View.GONE);
            }

        });
    }

    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void cancelDisconnect() {

        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (manager != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            if (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED) {

                manager.cancelConnect(channel, new ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Aborting connection",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this,
                                "Connect abort request failed. Reason Code: " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        List<Fragment> listOfFragments = getSupportFragmentManager().getFragments();

        if(listOfFragments.size()>=1){
            for (Fragment fragment : listOfFragments) {
                if(fragment instanceof DeviceDetailFragment){
                    fragment.onActivityResult(requestCode, resultCode, data);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
//        (getSupportFragmentManager().findFragmentById(R.id.device_detail_container)).
//                onActivityResult(requestCode,resultCode,data);
    }

    public void initiateConnection(View view) {

        createConnection();
    }

    public void startVoiceConversation(View view) {

        CURRENT_LANGUAGE=first_spinner.getSelectedItem().toString();
        String lang=(CURRENT_LANGUAGE.equals("English"))?"en-US":"ISO 639-3";//ISO 639-3
        Log.d("Speech","Lang="+lang+", FROM:"+CURRENT_LANGUAGE);
        recognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},1);
        final Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT,"The app wants some permission");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

        recognizer.setRecognitionListener(listener);
        recognizer.startListening(recognizerIntent);
    }
    RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onResults(Bundle results) {
            ArrayList<String> data=results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            Log.d("Speech ","On results "+data.get(0));
            DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.frag_detail);
            fragmentDetails.sendMessage(data.get(0));
            translateNow(data.get(0));
        }

        @Override
        public void onReadyForSpeech(Bundle bundle) {
            Log.d("Speech: ","On ready for speech");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d("Speech: ","On beginning");
        }

        @Override
        public void onRmsChanged(float v) {

        }

        @Override
        public void onBufferReceived(byte[] bytes) {
            Log.d("Speech: ","Buffer received");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d("Speech: ","End of speech");
        }

        @Override
        public void onError(int error) {
            Log.d("Speech: ","Error "+error);
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> data=partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            int i=0;
            while(i<data.size()) {
                Log.d("Speech ", "Partial at "+i+"= " + data.get(i));
                i++;
            }

        }

        @Override
        public void onEvent(int i, Bundle bundle) {
            Log.d("Speech: ","On Event");
        }
    };

    private void translateNow(String text) {
        /*String OutputString="";
        try {
            GoogleAPI.setHttpReferrer("http://android-er.blogspot.com/");
            OutputString = Translate.DEFAULT.execute(text,
                    Language.ENGLISH, Language.FRENCH);
        } catch (Exception ex) {
            ex.printStackTrace();
            OutputString = "Error: "+ex;
        }
        Toast.makeText(this, OutputString, Toast.LENGTH_LONG).show();*/
    }
}
