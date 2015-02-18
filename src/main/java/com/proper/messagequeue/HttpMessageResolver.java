package com.proper.messagequeue;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
//import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proper.data.diagnostics.LogEntry;
import com.proper.data.diagnostics.WifiLogEntry;
//import com.proper.utils.StringUtils;
import com.proper.wifimonitor.AppContext;
import com.proper.wifimonitor.R;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
//import org.apache.commons.net.ftp.FTP;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * Created by Lebel on 16/05/2014.
 */
public class HttpMessageResolver {
    private String deviceIMEI = "";
    private static final String ApplicationID = "WarehouseTools";
    private Date utilDate = Calendar.getInstance().getTime();
    private java.sql.Timestamp today = null;
    //private LogHelper logger = new LogHelper();
    private String response;
    //private List<Contact> contactList;
    private AppContext appContext;

    public HttpMessageResolver(AppContext appContext) {
        this.appContext = appContext;
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

    //@Override
    public String resolveMessageQuery(Message msg) {

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
        return response;
    }

    public String resolveMessageQueue(Message msg) {
        String responseBody = "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            String input = mapper.writeValueAsString(msg);
            DefaultHttpClient http = new DefaultHttpClient();
            HttpPost httpMethod = new HttpPost();
            httpMethod.setURI(new URI(getDefaultConfig()));
            httpMethod.setHeader("Accept", "application/json");
            httpMethod.setHeader("Content-type", "application/json");
            httpMethod.setEntity(new StringEntity(input));
            HttpResponse response = http.execute(httpMethod);
            int responseCode = response.getStatusLine().getStatusCode();
            switch(responseCode)
            {
                case HttpURLConnection.HTTP_OK:
                    HttpEntity entity = response.getEntity();
                    if(entity != null)
                    {
                        responseBody = EntityUtils.toString(entity);
                    }
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            today = new java.sql.Timestamp(utilDate.getTime());
            LogEntry log = new LogEntry(1L, ApplicationID, "HttpMessageResolver - resolveMessageQueue", deviceIMEI, ex.getClass().getSimpleName(), ex.getMessage(), today);
            //logger.log(log);
        }
        return responseBody;
    }

    public boolean hasActiveInternetConnection(Context context) {
        if (isNetworkAvailable(context)) {
            try {
                HttpURLConnection urlc = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
                urlc.setRequestProperty("User-Agent", "Test");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(1500);
                urlc.connect();
                return (urlc.getResponseCode() == HttpURLConnection.HTTP_OK);
            } catch (IOException e) {
                Log.e("LOG_TAG", "Error checking internet connection", e);
                e.printStackTrace();
                today = new java.sql.Timestamp(utilDate.getTime());
                LogEntry log = new LogEntry(1L, ApplicationID, "HttpMessageResolver - hasActiveInternetConnection", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                //logger.log(log);
            }
        } else {
            Log.d("LOG_TAG", "No network available!");
        }
        return false;
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    private Boolean LogWifiReceiver(Context context, WifiLogEntry entry) {
        boolean success = false;
        List<WifiLogEntry> entryList = new ArrayList<WifiLogEntry>();
        try {
            //TODO - Use CIFS Client Library -  http://stackoverflow.com/a/10600116 <Already added in Maven>
            String fName = "smb:\\\\cinnamon\\ftpparent\\TelfordHand\\WarehouseWifiLog\\log.json";
            String domain = "PROPER.DOMAIN";
            String username = "administrator";
            String pwd = "proper2580";
            jcifs.Config.setProperty( "jcifs.netbios.wins", "192.168.10.247" );
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(domain, username, pwd);
            SmbFile file = new SmbFile(fName, auth);
            SmbFileInputStream in = new SmbFileInputStream(file);
            //Retrieve entry list from file
            ObjectMapper mapper = new ObjectMapper();
            entryList = mapper.readValue(in, new TypeReference<List<WifiLogEntry>>(){});
            entryList.add(entry);
            byte[] bytes = mapper.writeValueAsBytes(entryList);
            SmbFileOutputStream os = new SmbFileOutputStream(file);
            os.write(bytes);
            success = true;
        } catch(Exception ex) {
            String msg = "Unable to send";
            Log.d("LOG_TAG", "No network available!");
            AlertDialog.Builder builder = new AlertDialog.Builder(appContext);
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
}
