package com.altimeter.bdureau.bearconsole.Flash;

/**
 * @description: This is used to flash the altimeter firmware from the Android device using an OTG cable
 * so that the store Android application is compatible with altimeter. This works with the
 * ATMega328 based altimeters as well as the STM32 based altimeters
 * @author: boris.dureau@neuf.fr
 **/

import static android.os.Environment.getExternalStoragePublicDirectory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import android.os.Bundle;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.ImageView;

import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.R;

//import com.altimeter.bdureau.bearconsole.ShareHandler;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.physicaloid.lib.Boards;
import com.physicaloid.lib.Physicaloid;

import com.physicaloid.lib.programmer.avr.UploadErrors;
import com.physicaloid.lib.usb.driver.uart.UartConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.physicaloid.misc.Misc.toHexStr;


public class FlashFirmware extends AppCompatActivity {
    Physicaloid mPhysicaloid;
    UsbManager manager;
    UsbSerialDriver driver;
    UsbDeviceConnection connection;
    UsbSerialPort port;

    boolean recorverFirmware = false;
    Boards mSelectedBoard;
    Button btFlash;

    public Spinner spinnerFirmware;
    public ImageView imageAlti;
    TextView tvRead;
    private AlertDialog.Builder builder = null;
    private AlertDialog alert;
    private ArrayList<Boards> mBoardList;
    private UartConfig uartConfig;

    private static final String ASSET_FILE_NAME_ALTIMULTIV2 = "firmwares/2023-02-26-V1_28.altimultiV2.hex";
    private static final String ASSET_FILE_NAME_ALTIMULTI = "firmwares/2023-02-26-V1_28.altimulti.hex";
    private static final String ASSET_FILE_NAME_ALTISERVO = "firmwares/2023-02-27-AltiServoV1_6.ino.hex";
    private static final String ASSET_FILE_NAME_ALTIDUO = "firmwares/2023-02-27-AltiDuoV1_9_console.ino.hex";
    private static final String ASSET_FILE_NAME_ALTIMULTISTM32 = "firmwares/2023-02-26-V1_28.altimultiSTM32.bin";
    private static final String ASSET_FILE_NAME_ALTIGPS = "firmwares/2023-02-26-RocketGPSLoggerV1.7.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE1 = "firmwares/ESP32/boot_app0.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE2 = "firmwares/ESP32/2023-02-27-RocketFlightLoggerV1_28.ino.bootloader.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE3 = "firmwares/ESP32/2023-02-27-RocketFlightLoggerV1_28.ino.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE4 = "firmwares/ESP32/2023-02-27-RocketFlightLoggerV1_28.ino.partitions.bin";

    private static final String ASSET_FILE_RESET_ALTIDUO = "recover_firmwares/ResetAltiConfigAltiDuo.ino.hex";
    private static final String ASSET_FILE_RESET_ALTIMULTI = "recover_firmwares/ResetAltiConfigAltimulti.ino.hex";
    private static final String ASSET_FILE_RESET_ALTISERVO = "recover_firmwares/ResetAltiConfigAltiServo.ino.hex";
    private static final String ASSET_FILE_RESET_ALTISTM32 = "recover_firmwares/ResetAltiConfigAltimultiSTM32.ino.bin";

    private static final String ASSET_FILE_RESET_ALTIESP32_FILE1 = "firmwares/ESP32/boot_app0.bin";
    private static final String ASSET_FILE_RESET_ALTIESP32_FILE2 = "firmwares/old/ResetAltiConfigAltimultiESP32.ino.bootloader.bin";
    private static final String ASSET_FILE_RESET_ALTIESP32_FILE3 = "firmwares/old/ResetAltiConfigAltimultiESP32.ino.bin";
    private static final String ASSET_FILE_RESET_ALTIESP32_FILE4 = "firmwares/old/ResetAltiConfigAltimultiESP32.ino.partitions.bin";

    private static final String DOWNLOAD_NAME_FILE1 = "";
    private static final String DOWNLOAD_NAME_FILE2 = "";
    private static final String DOWNLOAD_NAME_FILE3 = "";
    private static final String DOWNLOAD_NAME_FILE4 = "";
    private static final String DOWNLOAD_NAME_FILE_VERSION = "";
    private static final String DOWNLOAD_STANDARD_LOCATION = Environment.DIRECTORY_DCIM;
    private static final String DOWNLOAD_STANDARD_FOLDER = "";
    private static final String DOWNLOAD_URL = "";

    private String[] itemsBaudRate;
    private String[] itemsFirmwares;
    private Spinner dropdownBaudRate;

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash_firmware);

        spinnerFirmware = (Spinner) findViewById(R.id.spinnerFirmware);
        itemsFirmwares = new String[]{
                "AltiMulti",
                "AltiMultiV2",
                "AltiServo",
                "AltiDuo",
                "AltiMultiSTM32",
                "AltiGPS",
                "AltiESP32"
        };

        ArrayAdapter<String> adapterFirmware = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, itemsFirmwares);
        spinnerFirmware.setAdapter(adapterFirmware);
        spinnerFirmware.setSelection(0);


        btFlash = (Button) findViewById(R.id.btFlash);
        tvRead = (TextView) findViewById(R.id.tvRead);
        imageAlti = (ImageView) findViewById(R.id.imageAlti);


        //mPhysicaloid = new Physicaloid(this);
        manager = (UsbManager) getSystemService((Context.USB_SERVICE));
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if(!availableDrivers.isEmpty()) {
            driver = availableDrivers.get(0);
            connection = manager.openDevice(driver.getDevice());
        }
        mBoardList = new ArrayList<Boards>();
        for (Boards board : Boards.values()) {
            if (board.support > 0) {
                mBoardList.add(board);
            }
        }

        mSelectedBoard = mBoardList.get(0);
        //uartConfig = new UartConfig(115200, UartConfig.DATA_BITS8, UartConfig.STOP_BITS1, UartConfig.PARITY_NONE, false, false);

        btFlash.setEnabled(true);
        if (connection != null) {
            msg("We presumably have a connection");
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
        //if (mPhysicaloid.open()) {
        //    mPhysicaloid.setConfig(uartConfig);

        } else {
            //cannot open
            Toast.makeText(this, getResources().getString(R.string.msg13), Toast.LENGTH_LONG).show();
        }


        //baud rate
        dropdownBaudRate = (Spinner) findViewById(R.id.spinnerBaud);
        itemsBaudRate = new String[]{
                "1200",
                "2400",
                "4800",
                "9600",
                "14400",
                "19200",
                "28800",
                "38400",
                "57600",
                "115200",
                "230400"};


        ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, itemsBaudRate);
        dropdownBaudRate.setAdapter(adapterBaudRate);
        dropdownBaudRate.setSelection(10);

        spinnerFirmware.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMulti"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altimultiv1_small, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiV2"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altimultiv2_small, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiServo"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altiservo_small, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiDuo"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altiduo_small, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiSTM32") ||
                        itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiGPS"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altimultistm32_small, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiESP32"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altimultiesp32_small, getApplicationContext().getTheme()));

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        builder = new AlertDialog.Builder(this);
        //Running Saving commands
        builder.setMessage(R.string.flash_firmware_msg)
                .setTitle(R.string.flash_firmware_title)
                .setCancelable(false)
                .setPositiveButton(R.string.flash_firmware_ok, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });

        alert = builder.create();
        alert.show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        close();
    }

    public void onClickDismiss(View v) {
        close();
        finish();
    }
    DownloadManager DLManager;
    public void onClickRecover(View v){
        createPopup();
    }

    private void createPopup(){
        builder = new AlertDialog.Builder(FlashFirmware.this);

        builder.setMessage("Select files to upload")
                .setTitle("")
                .setCancelable(false)
                .setNeutralButton("Download latest files", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        downloadFlashFiles();
                    }
                })
                .setPositiveButton("Select Directory", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        openFiles();
                    }
                });
        alert = builder.create();
        alert.show();
    }
    private void downloadFlashFiles(){
        String downloadDirectory = getExternalStoragePublicDirectory(DOWNLOAD_STANDARD_LOCATION) + "/UP-Stairlift/ChairSoftware/Temp";
        clearDownloadDirectory(downloadDirectory);
        DLManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        String baseURL = DOWNLOAD_URL;
        DownloadFile(DLManager, "https://www.google.com/robots.txt");
        DownloadFile(DLManager, baseURL + DOWNLOAD_NAME_FILE1);
        DownloadFile(DLManager, baseURL + DOWNLOAD_NAME_FILE2);
        DownloadFile(DLManager, baseURL + DOWNLOAD_NAME_FILE3);
        DownloadFile(DLManager, baseURL + DOWNLOAD_NAME_FILE4);

    }
    private long DownloadFile(DownloadManager manager, String fileURL) {
        Uri uri = Uri.parse(fileURL);
        String fileName = fileURL.substring(fileURL.lastIndexOf('/') + 1);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(fileName);
        request.setDestinationInExternalPublicDir(DOWNLOAD_STANDARD_LOCATION, DOWNLOAD_STANDARD_FOLDER + fileName);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        return DLManager.enqueue(request);
    }
    private void clearDownloadDirectory(String dir){
        File directory = new File(dir);
        File[] contents = directory.listFiles();
        if (contents != null && contents.length != 0) {
            for(File file : contents){
                file.delete();
            }
            directory.delete();
        }
    }

    private static final int OPEN_FILE_REQUEST_CODE = 1;

    private void openFiles() {
        openDirectory();
    }
    public void openDirectory() {
        // Choose a directory using the system's file picker.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, OPEN_FILE_REQUEST_CODE);
    }
    DocumentFile pickedDir = null;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == OPEN_FILE_REQUEST_CODE) {
                Uri treeUri = data.getData();
                pickedDir = DocumentFile.fromTreeUri(this, treeUri);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                    getContentResolver().takePersistableUriPermission(treeUri, takeFlags);

                }
            }
        }
    }

    private void docFileGetESP32Files(DocumentFile dir, byte[][] files) {
        DocumentFile[] pics = dir.listFiles();
        if (pics != null) {
            for (DocumentFile pic : pics) {
                String docName = pic.getName();
                int fileNumber = docName.charAt(0) - '0' - 1;
                try {
                    InputStream fileInputStream = getContentResolver().openInputStream(pic.getUri());
                    if(fileNumber < 4) {
                        byte[] file = readFile(fileInputStream);
                        files[fileNumber] = new byte[file.length];
                        files[fileNumber] = file;
                        tvAppend(tvRead,docName + " size: " + files[fileNumber].length + "\n");
                    }
                    else{
                        tvAppend(tvRead, "Extra files detected in directory! " + docName + "\n");
                    }
                } catch (FileNotFoundException e) {
                    tvAppend(tvRead,"Could not find: " + docName + "\n");
                }
            }
        }
        else{
            tvAppend(tvRead,"Could not find any files in directory!\n");
        }
    }



    public void onClickDetect(View v) {
        new DetectAsyc().execute();
    }

    public void onClickFirmwareInfo(View v) {
        tvRead.setText("The following firmwares are available:");
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_ALTIMULTIV2);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_ALTIMULTI);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_ALTISERVO);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_ALTIDUO);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_ALTIMULTISTM32);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_ALTIGPS);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_ALTIESP32_FILE2);
        tvRead.append("\n");
    }

    public void onClickFlash(View v) {
        String firmwareFileName;

        firmwareFileName = ASSET_FILE_NAME_ALTIMULTI;

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMulti"))
            firmwareFileName = ASSET_FILE_NAME_ALTIMULTI;
        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiV2"))
            firmwareFileName = ASSET_FILE_NAME_ALTIMULTIV2;
        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiServo"))
            firmwareFileName = ASSET_FILE_NAME_ALTISERVO;
        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiDuo"))
            firmwareFileName = ASSET_FILE_NAME_ALTIDUO;
        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiSTM32"))
            firmwareFileName = ASSET_FILE_NAME_ALTIMULTISTM32;
        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiGPS"))
            firmwareFileName = ASSET_FILE_NAME_ALTIGPS;

        tvRead.setText("");

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMulti") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiV2") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiServo") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiDuo")) {
            tvRead.setText("Loading firmware:" + firmwareFileName);
            try {
                builder = new AlertDialog.Builder(FlashFirmware.this);
                //Flashing firmware...
                builder.setMessage(getResources().getString(R.string.msg10))
                        .setTitle(getResources().getString(R.string.msg11))
                        .setCancelable(false)
                        .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                                mPhysicaloid.cancelUpload();
                            }
                        });
                alert = builder.create();
                alert.show();

                mPhysicaloid.setBaudrate(Integer.parseInt(itemsBaudRate[(int) this.dropdownBaudRate.getSelectedItemId()]));
                mPhysicaloid.upload(mSelectedBoard, getResources().getAssets().open(firmwareFileName), mUploadCallback);
            } catch (RuntimeException e) {
                //Log.e(TAG, e.toString());
            } catch (IOException e) {
                //Log.e(TAG, e.toString());
            }
        } else if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiGPS") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiSTM32")) {
            tvRead.setText("Loading firmware:" + firmwareFileName);
            recorverFirmware = false;
            new UploadSTM32Asyc().execute();
        } else if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiESP32")) {
            tvRead.setText("Loading ESP32 firmware\n");
            recorverFirmware = false;
            new UploadESP32Asyc().execute();
        }


    }

    private class DetectAsyc extends AsyncTask<Void, Void, Void>  // UI thread
    {
        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(FlashFirmware.this);
            //Attempting to detect firmware...
            builder.setMessage(getResources().getString(R.string.detect_firmware))
                    .setTitle(getResources().getString(R.string.msg_detect_firmware))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            String version = "";

            //FirmwareInfo firm = new FirmwareInfo(mPhysicaloid);
            //firm.open(38400);
            version = "esp32";//firm.getFirmwarVersion();

            tvAppend(tvRead, getString(R.string.firmware_version_not_detected) + version + "\n");

            if (version.equals("AltiMulti")) {
                spinnerFirmware.setSelection(0);
            }
            if (version.equals("AltiMultiV2")) {
                spinnerFirmware.setSelection(1);
            }
            if (version.equals("AltiServo")) {
                spinnerFirmware.setSelection(2);
            }
            if (version.equals("AltiDuo")) {
                spinnerFirmware.setSelection(3);
            }
            if (version.equals("AltiMultiSTM32")) {
                spinnerFirmware.setSelection(4);
            }
            if (version.equals("AltiGPS")) {
                spinnerFirmware.setSelection(5);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
        }
    }


    private class UploadSTM32Asyc extends AsyncTask<Void, Void, Void>  // UI thread
    {
        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(FlashFirmware.this);
            //Flashing firmware...
            builder.setMessage(getResources().getString(R.string.msg10))
                    .setTitle(getResources().getString(R.string.msg11))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (!recorverFirmware) {
                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiSTM32"))
                    uploadSTM32(ASSET_FILE_NAME_ALTIMULTISTM32, mUploadSTM32Callback);
                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiGPS"))
                    uploadSTM32(ASSET_FILE_NAME_ALTIGPS, mUploadSTM32Callback);
            } else {
                uploadSTM32(ASSET_FILE_RESET_ALTISTM32, mUploadSTM32Callback);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
        }
    }

    public void uploadSTM32(String fileName, UploadSTM32CallBack UpCallback) {
        boolean failed = false;
        InputStream is = null;

        try {
            is = getAssets().open(fileName);
        } catch (IOException e) {
            //e.printStackTrace();
            tvAppend(tvRead, "file not found: " + ASSET_FILE_NAME_ALTIMULTISTM32 + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_ALTIMULTISTM32 + "\n");
        }

        dialogAppend("Starting ...");
        CommandInterfaceSTM32 cmd;

        cmd = new CommandInterfaceSTM32(UpCallback, mPhysicaloid);

        cmd.open(Integer.parseInt(itemsBaudRate[(int) this.dropdownBaudRate.getSelectedItemId()]));
        int ret = cmd.initChip();
        if (ret == 1)
            dialogAppend(getString(R.string.chip_has_not_been_init) + ret);
        else {
            dialogAppend("Chip has not been initiated:" + ret);
            failed = true;
        }
        int bootversion = 0;
        if (!failed) {
            bootversion = cmd.cmdGet();
            //dialogAppend("bootversion:"+ bootversion);
            tvAppend(tvRead, " bootversion:" + bootversion + "\n");
            if (bootversion < 20 || bootversion >= 100) {
                tvAppend(tvRead, " bootversion not good:" + bootversion + "\n");
                failed = true;
            }
        }

        if (!failed) {
            byte chip_id[]; // = new byte [4];
            chip_id = cmd.cmdGetID();
            tvAppend(tvRead, " chip id:" + toHexStr(chip_id, 2) + "\n");
        }

        if (!failed) {
            if (bootversion < 0x30) {
                tvAppend(tvRead, "Erase 1\n");
                cmd.cmdEraseMemory();
            } else {
                tvAppend(tvRead, "Erase 2\n");
                cmd.cmdExtendedEraseMemory();
            }
        }
        if (!failed) {
            cmd.drain();
            tvAppend(tvRead, "writeMemory" + "\n");
            ret = cmd.writeMemory(0x8000000, is);

            tvAppend(tvRead, "writeMemory finish" + "\n\n\n\n");
            if (ret == 1) {
                tvAppend(tvRead, "writeMemory success" + "\n\n\n\n");
            }
        }
        if (!failed) {
            cmd.cmdGo(0x8000000);
        }
        cmd.releaseChip();
    }

    private byte[] readFile(InputStream inputStream) {
        ByteArrayOutputStream byteArrayOutputStream = null;

        int nRead;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            final int byteSize = 1024;
            byte[] received = new byte[byteSize];

            while ((nRead = inputStream.read(received, 0, received.length)) != -1) {
                byteArrayOutputStream.write(received, 0, nRead);
                if(nRead < byteSize) break;
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteArrayOutputStream.toByteArray();
    }

    private class UploadESP32Asyc extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(FlashFirmware.this);
            //Flashing firmware...
            builder.setMessage(getResources().getString(R.string.msg10))
                    .setTitle(getResources().getString(R.string.msg11))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String firmwareFileName[] = new String[4];
            if (!recorverFirmware) {
                firmwareFileName[0] = ASSET_FILE_NAME_ALTIESP32_FILE1;
                firmwareFileName[1] = ASSET_FILE_NAME_ALTIESP32_FILE2;
                firmwareFileName[2] = ASSET_FILE_NAME_ALTIESP32_FILE3;
                firmwareFileName[3] = ASSET_FILE_NAME_ALTIESP32_FILE4;
                uploadESP32(firmwareFileName, mUploadSTM32Callback);
            } else {
                firmwareFileName[0] = ASSET_FILE_RESET_ALTIESP32_FILE1;
                firmwareFileName[1] = ASSET_FILE_RESET_ALTIESP32_FILE2;
                firmwareFileName[2] = ASSET_FILE_RESET_ALTIESP32_FILE3;
                firmwareFileName[3] = ASSET_FILE_RESET_ALTIESP32_FILE4;
                uploadESP32(firmwareFileName, mUploadSTM32Callback);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
        }
    }
    private void moveFile(String inputPath, String inputFile, File outputPath) {

        InputStream in = null;
        OutputStream out = null;
        try {

            //create output directory if it doesn't exist
            File baseURLOld = outputPath;
            File dir = new File (baseURLOld.toURI());
            if (!dir.exists())
            {
                dir.mkdirs();
            }

            in = new FileInputStream(inputPath + inputFile);
            File outputFile = new File(baseURLOld, inputFile);
            if(!outputFile.exists()){
                outputFile.createNewFile();
            }
            out = new FileOutputStream(baseURLOld +"/"+ inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file
            out.flush();
            out.close();
            out = null;

            // delete the original file
            new File(inputPath + inputFile).delete();
        }
        catch (FileNotFoundException fnfe1) {
            Log.e("tag", fnfe1.getMessage());
        }
        catch (Exception e) {
            Log.e("tag", e.getMessage());
        }

    }

    public void uploadESP32(String fileName[], UploadSTM32CallBack UpCallback) {
        boolean failed = false;
        InputStream file1 = null;
        InputStream file2 = null;
        InputStream file3 = null;
        InputStream file4 = null;
        CommandInterfaceESP32 cmd;


        //cmd = new CommandInterfaceESP32(UpCallback, mPhysicaloid);
        cmd = new CommandInterfaceESP32(UpCallback, port);

        try {
            file1 = getAssets().open(fileName[0]);

        } catch (IOException e) {
            //e.printStackTrace();
            tvAppend(tvRead, "file not found: " + ASSET_FILE_NAME_ALTIESP32_FILE1 + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_ALTIESP32_FILE1 + "\n");
        }

        try {
            file2 = getAssets().open(fileName[1]);

        } catch (IOException e) {
            //e.printStackTrace();
            tvAppend(tvRead, "file not found: " + ASSET_FILE_NAME_ALTIESP32_FILE2 + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_ALTIESP32_FILE2 + "\n");
        }
        try {
            file3 = getAssets().open(fileName[2]);

        } catch (IOException e) {
            //e.printStackTrace();
            tvAppend(tvRead, "file not found: " + ASSET_FILE_NAME_ALTIESP32_FILE3 + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_ALTIESP32_FILE3 + "\n");
        }

        try {
            file4 = getAssets().open(fileName[3]);

        } catch (IOException e) {
            //e.printStackTrace();
            tvAppend(tvRead, "file not found: " + ASSET_FILE_NAME_ALTIESP32_FILE4 + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_ALTIESP32_FILE4 + "\n");
        }

        dialogAppend("Starting ...");
        //Custom files have priority, then downloaded files and lastly the included files
        boolean filesFound = false;
        byte[][] files = new byte[4][];
        if(pickedDir != null){
            docFileGetESP32Files(pickedDir,files);
            filesFound = areAllFilesFound(files);
            if(filesFound)
                tvAppend(tvRead, "Using selected files!\n");
        }
        if(!filesFound){
            files = new byte[4][];
            UseDownloadedFiles(files);
            filesFound = areAllFilesFound(files);
            if(filesFound)
                tvAppend(tvRead, "Using downloaded files!\n");
        }
        if(!filesFound){
            tvAppend(tvRead, "Using shipped files!\n");
           files = new byte[4][];//Check if this is possible, otherwise byte[][] temp
           files[0] = readFile(file1);
           files[1] = readFile(file2);
           files[2] = readFile(file3);
           files[3] = readFile(file4);
        }
        byte file1Ori[] = files[0];
        byte file2Ori[] = files[1];
        byte file3Ori[] = files[2];
        byte file4Ori[] = files[3];

        byte file1Array[] = cmd.compressBytes(file1Ori);
        byte file2Array[] = cmd.compressBytes(file2Ori);
        byte file3Array[] = cmd.compressBytes(file3Ori);
        byte file4Array[] = cmd.compressBytes(file4Ori);

        int file1size = file1Ori.length;
        int file2size = file2Ori.length;
        int file3size = file3Ori.length;
        int file4size = file4Ori.length;

        boolean ret = cmd.initChip();
        if (ret)
            dialogAppend(getString(R.string.chip_has_not_been_init) + ret);
        else {
            dialogAppend("Chip has not been initiated:" + ret);
            failed = true;
        }
        int bootversion = 0;
        if (!failed) {
            // let's detect the chip, not really required but I just want to make sure that
            // it is
            // an ESP32 because this is what the program is for
            int chip = cmd.detectChip();
            if (chip == cmd.ESP32)
                //dialogAppend("Chip is ESP32");
                tvAppend(tvRead, "Chip is ESP32\n");

            // now that we have initialized the chip we can change the baud rate to 921600
            // first we tell the chip the new baud rate


            if(cmd.isStub()) {
                dialogAppend("Uploading Stub loader");
                InputStream json_file = null;
                try {
                    json_file = getAssets().open(ASSET_FILE_NAME_ESP32_STUB);
                } catch (IOException e) {
                    //e.printStackTrace();
                }
                String json_string = new String(readFile(json_file));
                cmd.flashStub(json_string);
            }
            cmd.init();
            dialogAppend("Changing baudrate to 921600");
            cmd.changeBaudeRate();

            // Those are the files you want to flush
            dialogAppend("Flashing file 1 0xe000");
            cmd.flashData(file1Array, 0xe000, file1size);
            dialogAppend("Flashing file 2 0x1000");
            cmd.flashData(file2Array, 0x1000, file2size);

            dialogAppend("Flashing file 3 0x10000");
            cmd.flashData(file3Array, 0x10000, file3size);
            dialogAppend("Flashing file 4 0x8000");
            cmd.flashData(file4Array, 0x8000, file4size);

            // we have finish flashing lets reset the board so that the program can start
            cmd.flash_begin(0,0,0x400,0);
            cmd.flash_defl_end(1);
            cmd.reset();

            dialogAppend("done ");
            tvAppend(tvRead, "done");
        }
    }

    private boolean areAllFilesFound(byte[][] files) {
        boolean filesFound = false;
        boolean dirError = false;
        for(byte [] file : files){
            if(file == null){
                tvAppend(tvRead, "Error, File missing!\n");
                dirError = true;
            }
        }
        if(!dirError) {
            filesFound = true;
            //tvAppend(tvRead, "Using custom files");
        }
        return filesFound;
    }

    private void UseDownloadedFiles(byte[][] files) {
        String baseURL = String.valueOf(getExternalStoragePublicDirectory(DOWNLOAD_STANDARD_LOCATION));
        String newURL = DOWNLOAD_STANDARD_FOLDER;
        String downloadLocation = baseURL + newURL;
        String fileNameTest = DOWNLOAD_NAME_FILE_VERSION;
        File storageLocation = getFilesDir();

        byte[] downloadVersionFile = readOwnedFile(downloadLocation + fileNameTest);
        byte[] internalVersionFile = readOwnedFile(String.valueOf(storageLocation) + "/" + fileNameTest);
        int versionNew = 0;
        int versionOld = 0;
        if(downloadVersionFile != null)
            versionNew = getVersion(downloadVersionFile);
        if(internalVersionFile != null)
            versionOld = getVersion(internalVersionFile);
        if(versionNew >= versionOld){///*check*/ should become > when not just testing
            moveFile(downloadLocation, DOWNLOAD_NAME_FILE_VERSION, storageLocation);
            //move all downloaded files to internal
            moveFile(downloadLocation, "1" + DOWNLOAD_NAME_FILE1, storageLocation);
            moveFile(downloadLocation, "2" + DOWNLOAD_NAME_FILE2, storageLocation);
            moveFile(downloadLocation, "3" + DOWNLOAD_NAME_FILE3, storageLocation);
            moveFile(downloadLocation, "4" + DOWNLOAD_NAME_FILE4, storageLocation);

            clearDownloadDirectory(downloadLocation);
        }

        //Read in files from internal
        File[] contents = storageLocation.listFiles();
        if (contents != null && contents.length != 0) {
            for(File file : contents){
                String fileName = file.getName();
                int fileNumber = fileName.charAt(0) - '0' - 1;
                if(fileNumber < 4){
                    files[fileNumber] = readOwnedFile(String.valueOf(storageLocation) + "/" + fileName);
                    tvAppend(tvRead,fileName + " size: " + files[fileNumber].length + "\n");
                }
                else{
                    tvAppend(tvRead, "Extra files detected in directory! " + fileName + "\n");
                }
            }
        }
    }

    private static int getVersion(byte[] downloadTemp) {//will have to be changed to a json read
        return (downloadTemp[0] - '0') * 1000 +
                (downloadTemp[1] - '0') * 100 +
                (downloadTemp[2] - '0') * 10 +
                (downloadTemp[3] - '0') * 1;
    }

    private static byte[] readOwnedFile(String finalUrl) {
        byte fileContent[] = null;
        try {
            File downloadFile = new File(finalUrl);
            FileInputStream fileStream = new FileInputStream(downloadFile);
            fileContent = new byte[(int) fileStream.available()];
            fileStream.read(fileContent);
        } catch (FileNotFoundException e) {
            ;
        } catch (IOException e) {
            ;
        }
        return fileContent;
    }


    Physicaloid.UploadCallBack mUploadCallback = new Physicaloid.UploadCallBack() {

        @Override
        public void onUploading(int value) {
            dialogAppend(getResources().getString(R.string.msg12) + value + " %");
        }

        @Override
        public void onPreUpload() {
            //Upload : Start
            tvAppend(tvRead, getResources().getString(R.string.msg14));
        }

        public void info(String value) {
            tvAppend(tvRead, value);
        }

        @Override
        public void onPostUpload(boolean success) {
            if (success) {
                //Upload : Successful
                tvAppend(tvRead, getResources().getString(R.string.msg16));
            } else {
                //Upload fail
                tvAppend(tvRead, getResources().getString(R.string.msg15));
            }
            alert.dismiss();
        }

        @Override
        //Cancel uploading
        public void onCancel() {
            tvAppend(tvRead, getResources().getString(R.string.msg17));
        }

        @Override
        //Error  :
        public void onError(UploadErrors err) {
            tvAppend(tvRead, getResources().getString(R.string.msg18) + err.toString() + "\n");
        }

    };

    UploadSTM32CallBack mUploadSTM32Callback = new UploadSTM32CallBack() {

        @Override
        public void onUploading(int value) {
            dialogAppend(getResources().getString(R.string.msg12) + value + " %");
        }

        @Override
        public void onInfo(String value) {
            tvAppend(tvRead, value);
        }

        @Override
        public void onPreUpload() {
            //Upload : Start
            tvAppend(tvRead, getResources().getString(R.string.msg14));
        }

        public void info(String value) {
            tvAppend(tvRead, value);
        }

        @Override
        public void onPostUpload(boolean success) {
            if (success) {
                //Upload : Successful
                tvAppend(tvRead, getResources().getString(R.string.msg16));
            } else {
                //Upload fail
                tvAppend(tvRead, getResources().getString(R.string.msg15));
            }
            alert.dismiss();
        }

        @Override
        //Cancel uploading
        public void onCancel() {
            tvAppend(tvRead, getResources().getString(R.string.msg17));
        }

        @Override
        //Error  :
        public void onError(UploadSTM32Errors err) {
            tvAppend(tvRead, getResources().getString(R.string.msg18) + err.toString() + "\n");
        }
    };
    Handler mHandler = new Handler();

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }

    /*private void setRadioButton(RadioButton rb, boolean state) {
        final RadioButton frb = rb;
        final boolean fstate = state;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                frb.setChecked(fstate);
            }
        });
    }*/

    private void dialogAppend(CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                alert.setMessage(ftext);
            }
        });
    }

    private void close() {
        try {
            port.close();
            //if (mPhysicaloid.close()) {
        } catch (IOException e){}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_application_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //share screen
        //if (id == R.id.action_share) {
        //    ShareHandler.takeScreenShot(findViewById(android.R.id.content).getRootView(), this);
        //    return true;
        //}
        //open help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(FlashFirmware.this, HelpActivity.class);
            i.putExtra("help_file", "help_flash_firmware");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(FlashFirmware.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}