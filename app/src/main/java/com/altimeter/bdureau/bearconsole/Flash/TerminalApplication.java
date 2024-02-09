package com.altimeter.bdureau.bearconsole.Flash;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
//import android.text.method.BaseMovementMethod;
//import android.text.method.ScrollingMovementMethod;
//import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

//import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Flash.CommandInterfaceESP32;
//import com.altimeter.bdureau.bearconsole.Flash.FlashFirmware;
import com.altimeter.bdureau.bearconsole.Flash.UploadSTM32CallBack;
import com.altimeter.bdureau.bearconsole.R;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
//import java.nio.charset.StandardCharsets;
import java.util.List;

public class TerminalApplication extends AppCompatActivity {
    Button btnDismiss;
//    TextView txtViewVersion;
//    ConsoleApplication myBT;
    TextView tvRead;
    UsbManager manager;
    UsbSerialDriver driver;
    UsbDeviceConnection connection;
    UsbSerialPort port;


    private boolean startedTerminal = false;
    @Override
    protected void onDestroy() {
        super.onDestroy();
        startedTerminal = false;
        close();

    }
    private void close() {
        try {
            if(port != null)
                port.close();
        } catch (IOException e){}
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
//        myBT = (ConsoleApplication) getApplication();
        setContentView(R.layout.activity_terminal);

        btnDismiss = findViewById(R.id.butDismiss);
        tvRead = findViewById(R.id.tvRead);
        btnDismiss.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();      //exit the about activity
            }
        });


        manager = (UsbManager) getSystemService((Context.USB_SERVICE));
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if(!availableDrivers.isEmpty()) {
            driver = availableDrivers.get(0);
            connection = manager.openDevice(driver.getDevice());
        }
        if (connection != null) {
            tvRead.setText("Connection made!\n");
            port = driver.getPorts().get(0); // Most devices have just one port (port 0)
            try {
                port.open(connection);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                port.setDTR(false);
                port.setRTS(false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } else {
            //cannot open
            Toast.makeText(this, getResources().getString(R.string.msg13), Toast.LENGTH_LONG).show();
        }

    }

    private void terminalAddText(String data){
        tvRead.append(data);
        //tvRead.append("\n");
    }

    private void clearTerminal (){
        tvRead.setText("");
    }

    public void onClickTest(View v) {
        if(startedTerminal) {
            startedTerminal = false;
            terminalAddText("Terminal stopping!\n");
            return;
        }

        if(connection != null){
            startedTerminal = true;
            tvRead.setText("Terminal starting\n");
//            new TerminalESP32Asyc().execute();
            terminalESP32Thread();
        }
        else
            tvRead.setText("USB not connected or permission was not granted!!!\n");
    }
    private void terminalESP32Thread(){
        //onPreExecute

        //Background and post execute
        new Thread(new Runnable() {
            @Override
            public void run() {
                doInBackground();
                onPostExecute();
            }

            private void doInBackground() {
                while(startedTerminal)
                    terminalESP32(mUploadSTM32Callback);
            }
            private void onPostExecute(){

            }
        }).start();
    }
    private class TerminalESP32Asyc extends AsyncTask<Void, Void, Void>  // UI thread
    {
        @Override
        protected Void doInBackground(Void... voids) {
            while(startedTerminal)
                terminalESP32(mUploadSTM32Callback);
            return null;
        }
    }
    public void terminalESP32(UploadSTM32CallBack UpCallback) {
        CommandInterfaceESP32 cmd;
        cmd = new CommandInterfaceESP32(UpCallback, port);
        //terminalAddText("Terminal started");
        CommandInterfaceESP32.cmdRet retVal = new CommandInterfaceESP32.cmdRet();

        int len = cmd.recieveDebug(retVal.retValue, retVal.retValue.length, 100);
        if(len > 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Your task code goes here
                    terminalAddText(new String(retVal.retValue));
                }
            });
        }
    }
    UploadSTM32CallBack mUploadSTM32Callback = new UploadSTM32CallBack() {
        @Override
        public void onUploading(int value) {
        }
        @Override
        public void onInfo(String value) {
            terminalAddText(value);
        }
        @Override
        public void onPreUpload() {
        }
        public void info(String value) {
        }
        @Override
        public void onPostUpload(boolean success) {
        }
        @Override
        //Cancel uploading
        public void onCancel() {
        }
        @Override
        //Error  :
        public void onError(UploadSTM32Errors err) {
        }
    };
}
