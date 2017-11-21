package com.jujucecedudu.androidcomm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rhine on 16/11/17.
 */

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceAdapterViewHolder>{
    private static final String TAG = "BLUETOOTH_TEST_ADAPTER";

    private ArrayList<BluetoothDevice> mAvailableDevices;

    private final ListItemClickListener mOnClickListener;

    public interface ListItemClickListener{
        void onListItemClick(BluetoothDevice device);
    }

    public DeviceAdapter(ListItemClickListener listener){
        mAvailableDevices = new ArrayList<>();
        mOnClickListener = listener;
    }

    public class DeviceAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public final TextView mDeviceTextView;

        public DeviceAdapterViewHolder(View view){
            super(view);
            mDeviceTextView = (TextView) view.findViewById(R.id.tv_available_device);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mOnClickListener.onListItemClick(mAvailableDevices.get(getAdapterPosition()));
        }
    }

    @Override
    public DeviceAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.devices_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(layoutIdForListItem, parent, false);
        return new DeviceAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DeviceAdapterViewHolder holder, int position) {
        BluetoothDevice device = mAvailableDevices.get(position);
        if(device != null){
            holder.mDeviceTextView.setText(device.getName() + " " + device.getAddress());
        }
    }

    @Override
    public int getItemCount() {
        if(null == mAvailableDevices){
            return 0;
        }
        return mAvailableDevices.size();
    }

    public void setDeviceData(ArrayList deviceData){
        mAvailableDevices = deviceData;
        notifyDataSetChanged();
    }

    public void addDeviceData(BluetoothDevice deviceData){
        mAvailableDevices.add(deviceData);
        notifyDataSetChanged();
    }

    public void clearDevices(){
        mAvailableDevices.clear();
        notifyDataSetChanged();
    }
}
