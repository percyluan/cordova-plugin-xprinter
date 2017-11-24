package com.hengan.Xprinter;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.UUID;

/**
 * 蓝牙打印适配器
 *
 * @author hao.jiang
 * @create 2017-10-24
 **/
public class PrinterAdapter extends Activity{

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_CODE = 2;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDevice mBluetoothDevice=null;
    private BluetoothSocket mBluetoothSocket=null;

    private OutputStream mOutputStream = null;

    PrinterAdapter(){
        init();
    }

    /**
     * 初始化蓝牙设备
     */
    public void init(){
        if(mBluetoothAdapter == null) mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();     //初始化蓝牙适配器
        // 判断手机是否支持蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "此设备不支持蓝牙或没有开启蓝牙权限", Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    /**
     * 打开蓝牙设备
     */
    public void open(){
        // 判断是否打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
    }

    /**
     * 获取配对设备的姓名列表
     */
    public String[] makePair(){
        if(!mBluetoothAdapter.isEnabled()) this.open();

        Set<BluetoothDevice> pList=mBluetoothAdapter.getBondedDevices();
        String[] name = new String[pList.size()];

        if(pList!=null&&pList.size()>0){
            int i = 0;
            for (BluetoothDevice bluetoothDevice : pList) {
                name[i++] = bluetoothDevice.getName();
                /*if(1664 == bluetoothDevice.getBluetoothClass().getDeviceClass()){

                }*/

            }
        }else{
            Toast.makeText(this, "没找到匹配的蓝牙打印机", Toast.LENGTH_SHORT).show();
        }

        return name;
    }

    /**
     * 获得地址
     * @param name
     * @return
     */
    private String getAddress(String name){
        Set<BluetoothDevice> pList = mBluetoothAdapter.getBondedDevices();

        if(pList!=null&&pList.size()>0){
            for (BluetoothDevice bluetoothDevice : pList) {
                if(bluetoothDevice.getName().equals(name)){
                    return bluetoothDevice.getAddress();
                }
            }
        }

        return null;
    }

    /**
     * 连接指定设备
     */
    public String connect(String name){

        String success = "success";
        String address = this.getAddress(name);

        if (address == null) {
            success = "无法连接，设备不存在";
        }else{
            success = success + address;
            try {
                mBluetoothDevice=mBluetoothAdapter.getRemoteDevice(address);
                mBluetoothSocket=mBluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID);
                mBluetoothSocket.connect();
            } catch (Exception e) {
                success = address;
            }
        }

        return success;
    }

    /**
     * 打印
     *
     * @param message
     * @return
     */
    public boolean printer(String message) throws IOException {
        boolean success = true;
        try {
            mOutputStream=mBluetoothSocket.getOutputStream();
            mOutputStream.write(message.getBytes("GBK"));
            mOutputStream.flush();
        } catch (IOException e) {
            success = false;
            e.printStackTrace();
        }finally {
            /*if (mOutputStream!=null){
                mOutputStream.close();
            }*/

            return success;
        }
    }

    /**
     * 打印
     *
     * @param command
     * @return
     */
    public boolean selectCommand(byte[] command) throws IOException {
        boolean success = true;
        try {
            mOutputStream=mBluetoothSocket.getOutputStream();
            mOutputStream.write(command);
            mOutputStream.flush();
        } catch (IOException e) {
            success = false;
            e.printStackTrace();
        }finally {
            /*if (mOutputStream!=null){
                mOutputStream.close();
            }*/

            return success;
        }
    }

    public InputStream getResource(String path){
        InputStream is = null;
        try {
            URL uri = new URL(path);
            // 3、获取连接对象、此时还没有建立连接
            HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
            // 4、初始化连接对象
            // 设置请求的方法，注意大写
            connection.setRequestMethod("GET");
            // 读取超时
            connection.setReadTimeout(5000);
            // 设置连接超时
            connection.setConnectTimeout(5000);
            // 5、建立连接
            connection.connect();
            // 6、获取成功判断,获取响应码
            if (connection.getResponseCode() == 200) {
                // 7、拿到服务器返回的流，客户端请求的数据，就保存在流当中
                is = connection.getInputStream();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return is;
    }

}
