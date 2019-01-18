package cn.iricbing.xprinter;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.widget.Toast;
import android.net.Uri;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import net.posprinter.utils.BitmapToByteData;

import static net.posprinter.utils.DataForSendToPrinterPos80.printRasterBmp;
import static net.posprinter.utils.DataForSendToPrinterPos80.selectAlignment;
import static net.posprinter.utils.DataForSendToPrinterPos80.selectCharacterSize;

/**
 * This class echoes a string called from JavaScript.
 */
public class XPrinter extends CordovaPlugin {
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice mBluetoothDevice = null;
    private BluetoothSocket mBluetoothSocket = null;
    private OutputStream mOutputStream = null;
    private Bitmap logoBmp = null;

    private static final Integer SMALL_SIZE = 0;
    private static final Integer NORMAL_SIZE = 17;
    private static final Integer MEDIAL_SIZE = 25;
    private static final Integer LARGE_SIZE = 34;

    private static final Integer ALIGN_LEFT = 0;
    private static final Integer ALIGN_CENTER = 1;
    private static final Integer ALIGN_RIGHT = 2;

    private static final Float LOGO_WIDTH = 500f;

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch (action) {
        case "coolMethod": {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
        }
            break;
        case "connect": {
            String macAddress = args.getString(0);
            this.connect(macAddress, callbackContext);
        }
            break;
        case "print": {
            String title = args.getString(0);
            String serial = args.getString(1);
            String body = args.getString(2);
            String timestamp = args.getString(3);
            this.print(title, serial, body, timestamp, callbackContext);
        }
            break;
        case "printTest": {
            String title = args.getString(0);
            String body = args.getString(1);
            String footer = args.getString(2);
            this.printTest(title, body, footer, callbackContext);
        }
            break;
        case "printImage": {
            String uri = args.getString(0);
            this.printImage(uri, callbackContext);
        }
            break;
        }
        return true;
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private void connect(String macAddress, CallbackContext callbackContext) {
        if (bluetoothAdapter == null) {
            callbackContext.error("The current device does not support bluetooth");
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            callbackContext.error("Bluetooth is not enabled");
            return;
        }
        if (macAddress == null || macAddress.length() == 0) {
            callbackContext.error("Invalidate MAC address " + macAddress);
            return;
        }
        try {
            mBluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress);
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID);
            mBluetoothSocket.connect();
            callbackContext.success("Connect success");
        } catch (Exception e) {
            callbackContext.error("Connect error: " + e.toString());
        }
    }

    private void print(String title, String serial, String body, String timestamp, CallbackContext callbackContext) {
        try {
            printTitle(title);
            printSerial(serial);
            printBody(body);
            printTimestamp(timestamp);
            cutPaper();
            callbackContext.success("Print success");
        } catch (Exception e) {
            callbackContext.error("Print error: " + e.toString());
        }
    }

    private void printTest(String title, String body, String footer, CallbackContext callbackContext) {
        try {
            printTitle(title);
            printBody(body);
            printFooter(footer);
            cutPaper();
            callbackContext.success("Test print success");
        } catch (Exception e) {
            callbackContext.error("Test print error: " + e.toString());
        }
    }

    private void printTitle(String title) throws IOException {
        mOutputStream = mBluetoothSocket.getOutputStream();
        byte[] sizeByte = selectCharacterSize(MEDIAL_SIZE);
        mOutputStream.write(sizeByte);
        byte[] aliginByte = selectAlignment(ALIGN_CENTER);
        mOutputStream.write(aliginByte);
        mOutputStream.write((title + "\n\n\n").getBytes("GBK"));
        mOutputStream.flush();
    }

    private void printSerial(String serial) throws IOException {
        mOutputStream = mBluetoothSocket.getOutputStream();
        byte[] sizeByte = selectCharacterSize(LARGE_SIZE);
        mOutputStream.write(sizeByte);
        byte[] aliginByte = selectAlignment(ALIGN_CENTER);
        mOutputStream.write(aliginByte);
        mOutputStream.write((serial + "\n").getBytes("GBK"));
        mOutputStream.flush();
    }

    private void printBody(String body) throws IOException {
        mOutputStream = mBluetoothSocket.getOutputStream();
        byte[] sizeByte = selectCharacterSize(NORMAL_SIZE);
        mOutputStream.write(sizeByte);
        byte[] aliginByte = selectAlignment(ALIGN_CENTER);
        mOutputStream.write(aliginByte);
        mOutputStream.write(("\n" + body + "\n").getBytes("GBK"));
        mOutputStream.flush();
    }

    private void printTimestamp(String timestamp) throws IOException {
        mOutputStream = mBluetoothSocket.getOutputStream();
        byte[] sizeByte = selectCharacterSize(SMALL_SIZE);
        mOutputStream.write(sizeByte);
        byte[] aliginByte = selectAlignment(ALIGN_CENTER);
        mOutputStream.write(aliginByte);
        mOutputStream.write(("\n" + timestamp + "\n").getBytes("GBK"));
        mOutputStream.flush();
    }

    private void printFooter(String footer) throws IOException {
        mOutputStream = mBluetoothSocket.getOutputStream();
        byte[] sizeByte = selectCharacterSize(SMALL_SIZE);
        mOutputStream.write(sizeByte);
        byte[] aliginByte = selectAlignment(ALIGN_LEFT);
        mOutputStream.write(aliginByte);
        mOutputStream.write(footer.getBytes("GBK"));
        mOutputStream.flush();
    }

    private void printImage(String uri, CallbackContext callbackContext) {
        try {
            createLogoBmp(uri);
            printLogo();
            cutPaper();
            callbackContext.success("Print logo success");
        } catch (Exception e) {
            callbackContext.error("Print logo error: " + e.toString());
        }
    }

    private void createLogoBmp(String uri) throws IOException {
        ContentResolver cr = this.cordova.getActivity().getContentResolver();
        logoBmp = BitmapFactory.decodeStream(cr.openInputStream(Uri.parse(uri)));
        if (logoBmp.getWidth() > LOGO_WIDTH) {
            Float scale = LOGO_WIDTH / logoBmp.getWidth();
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            logoBmp = Bitmap.createBitmap(logoBmp, 0, 0, logoBmp.getWidth(), logoBmp.getHeight(), matrix, true);
        }
    }

    private void printLogo() throws IOException {
        if (logoBmp == null) {
            throw new IOException("logo is not exist");
        }
        mOutputStream = mBluetoothSocket.getOutputStream();
        byte[] bmpByte = printRasterBmp(0, logoBmp, BitmapToByteData.BmpType.Threshold,
                BitmapToByteData.AlignType.Center, 568);
        mOutputStream.write(bmpByte);
        mOutputStream.flush();
    }

    private void cutPaper() throws IOException {
        mOutputStream = mBluetoothSocket.getOutputStream();
        mOutputStream.write("\n\n\n\n\n\n".getBytes("GBK"));
        mOutputStream.write(new byte[] { 0x0a, 0x0a, 0x1d, 0x56, 0x01 });
        mOutputStream.flush();
    }
}
