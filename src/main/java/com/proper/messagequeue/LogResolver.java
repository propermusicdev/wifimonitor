package com.proper.messagequeue;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proper.data.ServerResponseObject;
import com.proper.data.diagnostics.LogEntry;
import com.proper.data.diagnostics.WifiLogEntry;
import com.proper.wifimonitor.AppContext;
import com.proper.wifimonitor.R;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;
import org.apache.commons.net.ftp.FTP;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Lebel on 17/02/2015.
 */
public class LogResolver {
    private String TAG = LogResolver.class.getSimpleName();
    private String deviceIMEI = "";
    private static final String ApplicationID = "WarehouseTools";
    private Date utilDate = Calendar.getInstance().getTime();
    private java.sql.Timestamp today = null;
    private Context context = null;
    private String response;

    public LogResolver(Context context) {
        this.context = context;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getDefaultConfig() {
        return setConfig(R.integer.CONFIG_TESTSERVER);
    }

    public String setConfig(int configurqation) {
        String newConfig = "";
        switch(configurqation) {
            case R.integer.CONFIG_TESTSERVER:
                newConfig = "http://192.168.10.248:9090/samplews/api/messages/queue";
                break;
            case R.integer.CONFIG_LIVESERVER:
                newConfig = "http://192.168.10.246:9090/samplews/api/messages/queue";
                break;
            case R.integer.CONFIG_LIVESERVER_EXTERNAL:
                newConfig = "http://89.248.28.82:9090/samplews/api/messages/queue";
                break;
            case R.integer.CONFIG_TESTSERVER_EXTERNAL:
                newConfig = "http://89.248.28.81:9090/samplews/api/messages/queue";
                break;
        }
        return newConfig;
    }

    public ServerResponseObject resolveMessageQuery(Message msg) {
        long startTime = System.currentTimeMillis();
        long endTime = 0L;
        try {
            URL url = new URL(getDefaultConfig());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setUseCaches(false); // new line
            conn.setDoInput(true); //new line
            conn.setDoOutput(true);

            ObjectMapper mapper = new ObjectMapper();
            String input = mapper.writeValueAsString(msg);

            OutputStream os = conn.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os);
            osw.write(input);
            osw.flush();
            osw.close();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }
            //Serialise the returned entity
            //LogEntry newEntry = mapper.readValue(conn.getInputStream(), LogEntry.class);
            //Or Get the string returned
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;

            // Read Server Response
            while((line = reader.readLine()) != null)
            {
                // Append server response string
                sb.append(line + "");
            }

            reader.close(); // new line added !
            // Append Server Response To Content String but do nothing with it - for now...
            setResponse(sb.toString().trim());
            conn.disconnect();
            endTime = System.currentTimeMillis();
        } catch(MalformedURLException ex) {
            ex.printStackTrace();
            today = new java.sql.Timestamp(utilDate.getTime());
            LogEntry log = new LogEntry(1L, ApplicationID, "HttpMessageResolver - resolveMessageQuery", deviceIMEI, ex.getClass().getSimpleName(), ex.getMessage(), today);
            //logger.log(log);
        } catch (IOException ex) {
            ex.printStackTrace();
            today = new java.sql.Timestamp(utilDate.getTime());
            LogEntry log = new LogEntry(1L, ApplicationID, "HttpMessageResolver - resolveMessageQuery", deviceIMEI, ex.getClass().getSimpleName(), ex.getMessage(), today);
            //logger.log(log);
        } catch (Exception ex) {
            ex.printStackTrace();
            today = new java.sql.Timestamp(utilDate.getTime());
            LogEntry log = new LogEntry(1L, ApplicationID, "HttpMessageResolver - resolveMessageQuery", deviceIMEI, ex.getClass().getSimpleName(), ex.getMessage(), today);
            //logger.log(log);
        }
        return new ServerResponseObject(response, (endTime - startTime) / 1000.0);
    }


    public Boolean LogWifiReceiver(Context context, WifiLogEntry entry) {
        boolean success = false;
        List<WifiLogEntry> entryList = new ArrayList<WifiLogEntry>();
        try {
            //TODO - Use CIFS Client Library -  http://stackoverflow.com/a/10600116 <Already added in Maven>
            String fName = "\\\\cinnamon\\ftpparent\\TelfordHand\\WarehouseWifiLog\\log.json";
            String domain = "PROPER.DOMAIN";
            String username = "administrator";
            String pwd = "Proper2580";
            jcifs.Config.setProperty( "jcifs.netbios.wins", "192.168.10.247" );
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(domain, username, pwd);
            SmbFile file = new SmbFile(fName, auth);
            if (file != null && file.exists()) {
                InputStream is = file.getInputStream();
                //Retrieve entry list from file
                ObjectMapper mapper = new ObjectMapper();
                entryList = mapper.readValue(is, new TypeReference<List<WifiLogEntry>>(){});
                entryList.add(entry);
                byte[] bytes = mapper.writeValueAsBytes(entryList);
                SmbFileOutputStream os = new SmbFileOutputStream(file);
                os.write(bytes);
                success = true;
            }
        } catch(Exception ex) {
            String msg = "Unable to send";
            Log.d("LOG_TAG", "No network available!");
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(msg)
                    .setPositiveButton(R.string.but_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //do nothing
                        }
                    });
            builder.show();
        }
        return success;
    }

    public Boolean LogWifiReceiverByFTP(Context context, WifiLogEntry entry) {
        boolean success = false;
        List<WifiLogEntry> entryList = new ArrayList<WifiLogEntry>();
        Resources res = context.getResources();
        try {
            org.apache.commons.net.ftp.FTPClient ftp = new org.apache.commons.net.ftp.FTPClient();
            String host = res.getString(R.string.FTP_HOST_EXTERNAL);
            String user = res.getString(R.string.FTP_DEFAULTUSER);
            String pass = res.getString(R.string.FTP_PASSWORD);
            ftp.connect(host);
            ftp.login(user, pass);
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            ftp.setFileTransferMode(FTP.BINARY_FILE_TYPE); //new
            ftp.setBufferSize(3774873); //3.6MB - ftp.setBufferSize(0)// new line improve speed
            ftp.enterLocalPassiveMode();
            //ftp.connect(host);
            String imagesDir = "/WarehouseWifiLog/";
            //change directory
            ftp.changeWorkingDirectory(imagesDir);
            InputStream is = ftp.retrieveFileStream("log.json");
            int nBytes = is.available();
            if (is != null && nBytes > 0) {
                ObjectMapper mapper = new ObjectMapper();
                entryList = mapper.readValue(is, new TypeReference<List<WifiLogEntry>>(){});
                entryList.add(entry);
                byte[] bytes = mapper.writeValueAsBytes(entryList);
                ftp.appendFile("log.json", new ByteArrayInputStream(bytes));
                success = true;
            }
        } catch(Exception ex) {
            //String msg = "Unable to send";
            Log.d("LOG_TAG", "No network available!");
            if (ex.getMessage().contains("EOFException")) {
                return false;
            }
        }
        return success;
    }

    public Boolean LogWifiReceiverLocally(Context context, WifiLogEntry entry) {
        boolean result = false;
        String logString = "";
        SharedPreferences prefs = context.getSharedPreferences("WIFILOGS", Context.MODE_PRIVATE);
        //getSharedPreferences("WIFILOGS", MODE_PRIVATE);
        ObjectMapper mapper = new ObjectMapper();
        if (prefs.contains("LOG")){
            logString = prefs.getString("LOG", "");
        }
        if (logString.isEmpty()) {
            //do create a new list and save
            List<WifiLogEntry> entries = new ArrayList<WifiLogEntry>();
            try {
                entries.add(entry);
                String newValue = mapper.writeValueAsString(entries);
                SharedPreferences.Editor editor = context.getSharedPreferences("WIFILOGS", Context.MODE_PRIVATE).edit();
                editor.putString("LOG", newValue);
                editor.commit();
                result = true;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }else {
            //do, load list and add then save
            List<WifiLogEntry> entries = new ArrayList<WifiLogEntry>();
            try {
                entries = mapper.readValue(logString, new TypeReference<List<WifiLogEntry>>(){});
                entries.add(entry);
                String newValue = mapper.writeValueAsString(entries);
                SharedPreferences.Editor editor = context.getSharedPreferences("WIFILOGS", Context.MODE_PRIVATE).edit();
                editor.putString("LOG", newValue);
                editor.apply();
                result = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public Boolean saveLogToServer(Context context) {
        boolean success =  false;
        try{
            String logString = "";
            SharedPreferences prefs = context.getSharedPreferences("WIFILOGS", Context.MODE_PRIVATE);
            if (prefs.contains("LOG")) {
                logString = prefs.getString("LOG", "");
            }
            if (!logString.isEmpty()) {
                Resources res = context.getResources();
                org.apache.commons.net.ftp.FTPClient ftp = new org.apache.commons.net.ftp.FTPClient();
                String host = res.getString(R.string.FTP_HOST_EXTERNAL);
                String user = res.getString(R.string.FTP_DEFAULTUSER);
                String pass = res.getString(R.string.FTP_PASSWORD);
                ftp.connect(host);
                ftp.login(user, pass);
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                ftp.setFileTransferMode(FTP.BINARY_FILE_TYPE); //new
                ftp.setBufferSize(3774873); //3.6MB - ftp.setBufferSize(0)// new line improve speed
                ftp.enterLocalPassiveMode();
                //ftp.connect(host);
                String logsDir = "/WarehouseWifiLog/";
                SimpleDateFormat pattern = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String newFileName = String.format("log_%s.json", pattern.format(new Date()));
                //change directory
                ftp.changeWorkingDirectory(logsDir);
                InputStream is = new ByteArrayInputStream(logString.getBytes());
                success = ftp.storeFile(newFileName, is);
                if (success) {
                    removeLogSession();
                }
            }
        }catch (Exception ex) {
            ex.toString();
        }
        return success;
    }

    private boolean removeLogSession(){
        boolean result = false;
        SharedPreferences prefs = context.getSharedPreferences("WIFILOGS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = context.getSharedPreferences("WIFILOGS", Context.MODE_PRIVATE).edit();
        if (prefs.contains("LOG")) {
            editor.remove("LOG");
            result = true;
        }
        return result;
    }
}
