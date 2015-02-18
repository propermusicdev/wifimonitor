package com.proper.data.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.proper.data.diagnostics.WifiLogEntry;
import com.proper.wifimonitor.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Lebel on 18/02/2015.
 */
public class WifiEntryAdapter extends BaseAdapter {
    private Context context;
    private List<WifiLogEntry> entries;

    public WifiEntryAdapter(Context context, List<WifiLogEntry> entries) {
        this.context = context;
        this.entries = entries;
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public WifiLogEntry getItem(int position) {
        return entries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        /** REF: https://sriramramani.wordpress.com/2012/07/25/infamous-viewholder-pattern **/
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_wifientry, parent, false);
            holder.txtAPName = (TextView) convertView.findViewById(R.id.txtvLAPName);
            holder.txtBSSID = (TextView) convertView.findViewById(R.id.txtvLBSSID );
            holder.txtOldAPName = (TextView) convertView.findViewById(R.id.txtvLOldAPName);
            holder.txtSignalLevel = (TextView) convertView.findViewById(R.id.txtvLSignalLevel);
            holder.txtChannel = (TextView) convertView.findViewById(R.id.txtvLChannel);
            holder.txtBinScanned = (TextView) convertView.findViewById(R.id.txtvLBinScanned);
            //holder.txtPingTime = convertView.findViewById(R.id.txtvPingTime);
            holder.txtDbResponseTime = (TextView) convertView.findViewById(R.id.txtvLDbResponseTime);
            holder.txtEntryTimeStamp = (TextView) convertView.findViewById(R.id.txtvLEntryTimeStamp);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }
        WifiLogEntry entry = entries.get(pos);
        SimpleDateFormat pattern = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        holder.txtAPName.setText(entry.getAPName());
        holder.txtBSSID.setText(entry.getBSSID());
        holder.txtOldAPName.setText(entry.getOldAPName());
        holder.txtSignalLevel.setText(String.format("%s", entry.getSignalLevel()));
        holder.txtChannel.setText(String.format("%s", entry.getChannel()));
        holder.txtBinScanned.setText(entries.get(pos).getBinScanned());
        //holder.txtPingTime.setText(String.format("%s", entry.getPingTime()));
        holder.txtDbResponseTime.setText(String.format("%s", entry.getDbResponseTime()));
        holder.txtEntryTimeStamp.setText(String.format("%s", pattern.format(entry.getDateTimeStamp())));
        holder.position = pos;
        return convertView;
    }
    class ViewHolder {
        TextView txtAPName;
        TextView txtBSSID;
        TextView txtOldAPName;
        TextView txtSignalLevel;
        TextView txtChannel;
        TextView txtBinScanned;
        //TextView txtPingTime;
        TextView txtDbResponseTime;
        TextView txtEntryTimeStamp;
        int position;
    }
}
