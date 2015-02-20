package com.proper.wifimonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.*;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proper.data.BinResponse;
import com.proper.data.LastQuery;
import com.proper.data.ServerResponseObject;
import com.proper.data.UserLoginResponse;
import com.proper.data.diagnostics.LogEntry;
import com.proper.data.helpers.ResponseHelper;
import com.proper.messagequeue.*;
import com.proper.services.WifiReportingService;
import com.proper.utils.DeviceUtils;
import com.rscja.deviceapi.Barcode1D;
import com.rscja.deviceapi.exception.ConfigurationException;

import java.io.IOException;
import java.util.AbstractMap;

public class ActMain extends Activity {
    private String TAG = ActMain.class.getSimpleName();
    private String ApplicationID = "WifiReporter";
    protected AppContext appContext;
    protected AppManager appManager;
    protected int screenSize;
    protected static final int KEY_SCAN = 139;
    protected String deviceID = "";
    protected String deviceIMEI = "";
    protected java.util.Date utilDate = java.util.Calendar.getInstance().getTime();
    protected java.sql.Timestamp today = null;
    protected DeviceUtils device = null;
    protected LogResolver logResolver = null;
    //protected HttpMessageResolver resolver = null;
    protected ResponseHelper responseHelper = null;
    protected com.proper.messagequeue.Message thisMessage = null;
    private Handler handler;
    private Button btnScan, btnLog, btnExit;
    private EditText txtBin;
    private String scanInput;
    private UserLoginResponse currentUser = null;
    private LogBinQueryTask binQryTask;
    private UserLoginTask loginTask;
    private PushLogfileTask pushLogfileTask;
    private BinResponse qryResponse = null;
    private boolean isBarcodeOpened = false;
    private boolean threadStop = true;
    protected Barcode1D mInstance = null;
    protected Thread readThread;
    protected int readerStatus = 0;
    private String currentBin = "", currentUserToken = "";
    private long startTime, endtime;
    public String getScanInput() {
        return scanInput;
    }

    public void setScanInput(String scanInput) {
        this.scanInput = scanInput;
    }

    public String getCurrentBin() {
        return currentBin;
    }

    public void setCurrentBin(String currentBin) {
        this.currentBin = currentBin;
    }
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        appContext = (AppContext) getApplication();
        //authenticator = new UserAuthenticator(this);
        device = new DeviceUtils(this);
        //logger = new LogHelper();
        thisMessage = new com.proper.messagequeue.Message();
        deviceID = device.getDeviceID();
        deviceIMEI = device.getIMEI();
        //currentUser = authenticator.getCurrentUser();
        logResolver = new LogResolver(this);
        //resolver = new HttpMessageResolver(appContext);
        responseHelper = new ResponseHelper();
        //logOn();
        btnScan = (Button) this.findViewById(R.id.bnScanBin);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClicked(v);
            }
        });
        btnLog = (Button) this.findViewById(R.id.bnViewLog);
        btnLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClicked(v);
            }
        });
        btnExit = (Button) this.findViewById(R.id.bnExit);
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClicked(v);
            }
        });
        txtBin = (EditText) this.findViewById(R.id.etxtLogBin);
        txtBin.addTextChangedListener(new TextChanged());

        try {
            mInstance = Barcode1D.getInstance();
            isBarcodeOpened = mInstance.open();
            //isBarcodeOpened = true;
        } catch (SecurityException e) {
            e.printStackTrace();
            today = new java.sql.Timestamp(utilDate.getTime());
            LogEntry log = new LogEntry(1L, ApplicationID, "ActBinMain - onCreate", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
            //logger.log(log);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            today = new java.sql.Timestamp(utilDate.getTime());
            LogEntry log = new LogEntry(1L, ApplicationID, "ActBinMain - onCreate", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
            //logger.log(log);
            new AlertDialog.Builder(this).setTitle(R.string.DIA_ALERT).setMessage(R.string.DEV_OPEN_ERR).setPositiveButton(R.string.DIA_CHECK, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    finish();
                }
            }).show();
            return;
        }

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.what == 1){
                    setScanInput(msg.obj.toString());   //>>>>>>>>>>>>>>>   set scanned object  <<<<<<<<<<<<<<<<
                    if (!txtBin.getText().toString().equalsIgnoreCase("")) {
                        txtBin.setText("");
                        txtBin.setText(getScanInput());
                    }
                    else{
                        txtBin.setText(getScanInput());
                    }
                    appContext.playSound(1);
                    btnScan.setEnabled(true);
                }
            }
        };

        logOn();
    }

    private void buttonClicked(View v) {
        boolean isContinuous = false;   //continuous scan feature?
        int iBetween = 0;
        if (v == btnScan) {
            btnScan.setEnabled(false);
            txtBin.requestFocus();
            if (threadStop) {
                Log.i("Reading", "My Barcode " + readerStatus);
                readThread = new Thread(new GetBarcode(isContinuous, iBetween));
                readThread.setName("Single Barcode ReadThread");
                readThread.start();
            }else {
                threadStop = true;
            }
            btnScan.setEnabled(true);
        }
        if (v == btnLog) {
            Intent i = new Intent(this, ActLogView.class);
            startActivityForResult(i, 0);
        }
        if (v == btnExit) {
            if (pushLogfileTask != null) pushLogfileTask.cancel(true);
            pushLogfileTask = new PushLogfileTask();
            pushLogfileTask.execute();
            if (appContext.isMyServiceRunning(WifiReportingService.class)) {
                this.stopService(new Intent(this, WifiReportingService.class));
            }
        }
    }

    private void logOn() {
        if (loginTask != null) {
            loginTask.cancel(true);
            loginTask = null;
        }
        loginTask = new UserLoginTask();
        loginTask.execute();
    }

    class TextChanged implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s != null && !s.toString().equalsIgnoreCase("")) {
                //if (inputByHand == 0) {
                String binCode = s.toString().trim();
                if (binCode.length() == 5) {
                    setCurrentBin(binCode);
                    //Authenticate current user, Build Message only when all these conditions are right then proceed with asyncTask
                    //currentUser = currentUser != null ? currentUser : authenticator.getCurrentUser();  //Gets currently authenticated user
                    if (currentUser != null) {
                        binQryTask = new LogBinQueryTask();
                        binQryTask.execute(binCode);  //executes both -> Send Queue Directly AND Send queue to Service
                    }
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KEY_SCAN) {
            if (event.getRepeatCount() == 0) {
                boolean bContinuous = true;
                int iBetween = 0;
                txtBin.requestFocus();
                if (threadStop) {
                    Log.i("Reading", "My Barcode " + readerStatus);
                    readThread = new Thread(new GetBarcode(bContinuous, iBetween));
                    readThread.setName("Single Bin Code ReadThread");
                    readThread.start();
                }else {
                    threadStop = true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        threadStop = true;
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (isBarcodeOpened) {
            mInstance.close();
        }
        //soundPool.release();
        //android.os.Process.killProcess(android.os.Process.myPid()); Since it's not longer main entry then we're not killing app *LEBEL*
    }

    private static Double getDuration(long mStartTime, long mEndTime) {
        return (mEndTime - mStartTime) / 1000.0;
    }

    private class GetBarcode implements Runnable {

        private boolean isContinuous = false;
        String barCode = "";
        private long sleepTime = 1000;
        Message msg = null;

        public GetBarcode(boolean isContinuous) {
            this.isContinuous = isContinuous;
        }

        public GetBarcode(boolean isContinuous, int sleep) {
            this.isContinuous = isContinuous;
            this.sleepTime = sleep;
        }

        @Override
        public void run() {

            do {
                startTime = System.currentTimeMillis();
                barCode = mInstance.scan();

                Log.i("MY", "barCode " + barCode.trim());

                msg = new Message();

                if (barCode == null || barCode.isEmpty()) {
                    msg.what = 0;
                    msg.obj = "";
                } else {
                    msg.what = 1;
                    msg.obj = barCode;
                }

                handler.sendMessage(msg);

                if (isContinuous) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } while (isContinuous && !threadStop);

        }

    }

    private class UserLoginTask extends AsyncTask<Void, Void, UserLoginResponse> {
        private ProgressDialog lDialog;

        @Override
        protected void onPreExecute() {
            lDialog = new ProgressDialog(ActMain.this);
            CharSequence message = "Working hard...checking credentials...";
            CharSequence title = "Please Wait";
            lDialog.setCancelable(true);
            lDialog.setCanceledOnTouchOutside(false);
            lDialog.setMessage(message);
            lDialog.setTitle(title);
            lDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            lDialog.show();
        }

        @Override
        protected UserLoginResponse doInBackground(Void... input) {

            try {
                String initials = "RMC";
                int pin = 2580;
                String msg = String.format("{\"UserInitials\":\"%s\", \"UserPin\":\"%s\"}", initials, pin);
                ObjectMapper mapper = new ObjectMapper();
                com.proper.messagequeue.Message thisMessage = new com.proper.messagequeue.Message();
                today = new java.sql.Timestamp(utilDate.getTime());
                thisMessage.setSource(deviceIMEI);
                thisMessage.setMessageType("UserLogin");
                thisMessage.setIncomingStatus(1); //default value
                thisMessage.setIncomingMessage(msg);
                thisMessage.setOutgoingStatus(0);   //default value
                thisMessage.setOutgoingMessage("");
                thisMessage.setInsertedTimeStamp(today);
                thisMessage.setTTL(100);    //default value

                //currentUserToken = testResolver.resolveLogIn();       // test only
                //currentUserToken = httpResolver.resolveMessageQueue(thisMessage);
                currentUserToken = logResolver.resolveMessageQuery(thisMessage).getResponse();
                currentUser = mapper.readValue(currentUserToken, UserLoginResponse.class);
            } catch (Exception e) {
                if (lDialog != null && lDialog.isShowing()) lDialog.dismiss();
                if (!loginTask.isCancelled()) loginTask.cancel(true);
                e.printStackTrace();
                today = new java.sql.Timestamp(utilDate.getTime());
                LogEntry log = new LogEntry(1L, ApplicationID, "ActLogin - UserLoginTask - doInBackground", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                //logger.log(log);
                if (!loginTask.isCancelled()) loginTask.cancel(true);
            }
            return currentUser;
        }

        @Override
        protected void onPostExecute(UserLoginResponse userLoginResponse) {
            if (lDialog != null && lDialog.isShowing()) lDialog.dismiss();
            if (currentUserToken.isEmpty() && currentUser == null) {
                appContext.playSound(2);
                Vibrator vib = (Vibrator) ActMain.this.getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                vib.vibrate(2000);
                String mMsg = "Unable to Log you in \n...Exiting App....";
                AlertDialog.Builder builder = new AlertDialog.Builder(ActMain.this);
                builder.setMessage(mMsg)
                        .setPositiveButton(R.string.but_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        });
                builder.show();
            }else {
                Intent intent = new Intent(ActMain.this, WifiReportingService.class);
                startService(intent);
            }
        }
    }

    private class LogBinQueryTask extends AsyncTask<String, Void, AbstractMap.SimpleEntry<ServerResponseObject, BinResponse>> {
        private ProgressDialog mDialog;

        @Override
        protected void onPreExecute() {
            mDialog = new ProgressDialog(ActMain.this);
            CharSequence message = "Working hard...checking credentials...";
            CharSequence title = "Please Wait";
            mDialog.setCancelable(true);
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.setMessage(message);
            mDialog.setTitle(title);
            mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mDialog.show();
        }

        @Override
        protected AbstractMap.SimpleEntry<ServerResponseObject, BinResponse> doInBackground(String... input) {
            qryResponse = new BinResponse();
            ServerResponseObject sro =  null;
            AbstractMap.SimpleEntry<ServerResponseObject, BinResponse> resp = null;
            String bincode = input[0];
            String msg = String.format("{\"UserId\":\"%s\", \"UserCode\":\"%s\",\"BinCode\":\"%s\"}",
                    currentUser.getUserId(), currentUser.getUserCode(), bincode);
            today = new java.sql.Timestamp(utilDate.getTime());
            BinResponse msgResponse = new BinResponse();
            thisMessage.setSource(deviceIMEI);
            thisMessage.setIncomingStatus(1); //default value
            thisMessage.setOutgoingStatus(0);   //default value
            thisMessage.setOutgoingMessage("");
            thisMessage.setInsertedTimeStamp(today);
            thisMessage.setTTL(100);

            thisMessage.setMessageType("BinQuery");
            thisMessage.setIncomingMessage(msg);

            try {
                sro = logResolver.resolveMessageQuery(thisMessage);
                String response = responseHelper.refineResponse(sro.getResponse());
                sro.setResponse(response);
                if (sro.getResponse().contains("not recognised")) {
                    //manually error trap this error
                    String iMsg = "The Response object return null due to msg queue not recognising your improper request.";
                    LogEntry log = new LogEntry(1L, ApplicationID, "ActBinMain - WebServiceTask - Line:1291", deviceIMEI, RuntimeException.class.getSimpleName(), iMsg, today);
                }else {
                    ObjectMapper mapper = new ObjectMapper();
                    msgResponse = mapper.readValue(sro.getResponse(), BinResponse.class);
                    qryResponse = msgResponse;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                today = new java.sql.Timestamp(utilDate.getTime());
                LogEntry log = new LogEntry(1L, ApplicationID, "ActQueryScan - doInBackground", deviceIMEI, ex.getClass().getSimpleName(), ex.getMessage(), today);
                //logger.log(log);
            } catch (Exception ex) {
                ex.printStackTrace();
                today = new java.sql.Timestamp(utilDate.getTime());
                LogEntry log = new LogEntry(1L, ApplicationID, "ActQueryScan - doInBackground", deviceIMEI, ex.getClass().getSimpleName(), ex.getMessage(), today);
                //logger.log(log);
            }
            return new AbstractMap.SimpleEntry<ServerResponseObject, BinResponse>(sro, qryResponse);
        }

        @Override
        protected void onPostExecute(AbstractMap.SimpleEntry<ServerResponseObject, BinResponse> response) {
            super.onPostExecute(response);
            if(mDialog != null && mDialog.isShowing()) mDialog.dismiss();
            //TODO - Log something here, DB response
            txtBin.setText("");
            endtime = System.currentTimeMillis();
            LastQuery qry = new LastQuery(currentBin, getDuration(startTime, endtime));
            Intent i = new Intent(ActMain.this, WifiReportingService.class);
            i.putExtra("QUERYDURATION_EXTRA", qry);
            startService(i);
        }
    }

    private class PushLogfileTask extends AsyncTask<Void, Void, Boolean> {
        private ProgressDialog yDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            yDialog = new ProgressDialog(ActMain.this);
            CharSequence message = "Working hard...checking credentials...";
            CharSequence title = "Please Wait";
            yDialog.setCancelable(true);
            yDialog.setCanceledOnTouchOutside(false);
            yDialog.setMessage(message);
            yDialog.setTitle(title);
            yDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            yDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return logResolver.saveLogToServer(ActMain.this);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result) {
                new AlertDialog.Builder(ActMain.this).setTitle("LOG").setMessage("Log Saved Successfully !!").setPositiveButton("Finish", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        stopService(new Intent(appContext, WifiReportingService.class));
                        Intent i = new Intent();
                        setResult(RESULT_OK, i);
                        finish();
                    }
                }).show();
            }
        }
    }
}
