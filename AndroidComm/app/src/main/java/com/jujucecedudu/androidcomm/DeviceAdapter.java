package com.jujucecedudu.androidcomm;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by rhine on 16/11/17.
 */

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceAdapterViewHolder>{

    private BluetoothDevice[] mAvailableDevices;

    public class DeviceAdapterViewHolder extends RecyclerView.ViewHolder{
        public final TextView mDeviceTextView;

        public DeviceAdapterViewHolder(View view){
            super(view);
            mDeviceTextView = (TextView) view.findViewById(R.id.tv_available_device);
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
        BluetoothDevice device = mAvailableDevices[position];
        holder.mDeviceTextView.setText(device.getName() + " " + device.getAddress());
    }

    @Override
    public int getItemCount() {
        if(null == mAvailableDevices){
            return 0;
        }
        return mAvailableDevices.length;
    }

    public void setDeviceData(BluetoothDevice[] deviceData){
        mAvailableDevices = deviceData;
        notifyDataSetChanged();
    }

    public void addDeviceData(BluetoothDevice deviceData){
        mAvailableDevices[mAvailableDevices.length] = deviceData;
        notifyDataSetChanged();
    }
}
