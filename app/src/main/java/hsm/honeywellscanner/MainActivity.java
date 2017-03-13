package hsm.honeywellscanner;

//import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.honeywell.aidc.*;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements BarcodeReader.BarcodeListener, BarcodeReader.TriggerListener {

    final boolean bUseClientTriggerControl=true;
    static String TAG = "honeywellscanner mod: ";
    private static BarcodeReader barcodeReader;
    private AidcManager manager;
    private boolean triggerState = false;

    private Button btnAutomaticBarcode;
    boolean useTrigger=true;
    boolean btnPressed = false;

    EditText editText;
    TextView editScan;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "app onCreate...");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "AidcManager.create...");
        // create the AidcManager providing a Context and a
        // CreatedCallback implementation.
        AidcManager.create(this, new AidcManager.CreatedCallback() {
            @Override
            public void onCreated(AidcManager aidcManager) {
                manager = aidcManager;
                barcodeReader = manager.createBarcodeReader();
                Log.i(TAG, "manager.createBarcodeReader() done");

                try {
                    if(barcodeReader!=null) {
                        Log.i(TAG, "barcodereader not claimed in OnCreate()");
                        //barcodeReader.claim();
                        scannerClaimRelease(true);
                        Log.i(TAG, "barcodeReader.claim() done");
                    }
                    // apply settings

                    try {
                        if(bUseClientTriggerControl) {
                            Log.i(TAG, "### using client trigger mode");
                            // set the trigger mode to client control
                            barcodeReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE,
                                    BarcodeReader.TRIGGER_CONTROL_MODE_CLIENT_CONTROL);
                        }
                        else{
                            Log.i(TAG, "### using automatic trigger mode");
                            barcodeReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE,
                                    BarcodeReader.TRIGGER_CONTROL_MODE_AUTO_CONTROL);
                        }
                        int iTop = barcodeReader.getIntProperty(BarcodeReader.PROPERTY_DECODE_WINDOW_TOP);
                        int iBottom = barcodeReader.getIntProperty(BarcodeReader.PROPERTY_DECODE_WINDOW_BOTTOM);
                        int iLeft = barcodeReader.getIntProperty(BarcodeReader.PROPERTY_DECODE_WINDOW_LEFT);
                        int iRight = barcodeReader.getIntProperty(BarcodeReader.PROPERTY_DECODE_WINDOW_RIGHT);
                        Point p1 = new Point(iLeft,iTop);
                        Point p2= new Point(iRight,iBottom);
                        dumpDecoderWindow(barcodeReader);

                        ArrayList<Point> points=new ArrayList<Point>();
                        points.add(p1);
                        points.add(p2);
                        Log.i(TAG, "Window Props: " + getPoints(points));
                    }catch(UnsupportedPropertyException ex)
                    {}
                    /*
                    barcodeReader.setProperty(BarcodeReader.PROPERTY_CODE_39_ENABLED, false);
                    barcodeReader.setProperty(BarcodeReader.PROPERTY_DATAMATRIX_ENABLED, true);

                    // set the trigger mode to automatic control
                    barcodeReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE,
                            BarcodeReader.TRIGGER_CONTROL_MODE_AUTO_CONTROL);
                } catch (UnsupportedPropertyException e) {
                    Toast.makeText(MainActivity.this, "Failed to apply properties",
                            Toast.LENGTH_SHORT).show();
                    */
                }
                catch (ScannerUnavailableException e) {
                    Toast.makeText(MainActivity.this, "Failed to claim scanner",
                            Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "FAILED to claim scanner after createBarcodeReader()");
                    //e.printStackTrace();
                }

                Log.i(TAG, "registering BarcodeListener...");
                // register bar code event listener
                //barcodeReader.addBarcodeListener(MainActivity.this);
            }
        });

        imageView=(ImageView)findViewById(R.id.imageView);
        //
        editText=(EditText)findViewById(R.id.editText);
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                try{
                if (hasFocus) {
                    Log.i(TAG, "editText has Focus");
                    scannerClaimRelease(false);
                } else {
                    Log.i(TAG, "editText lost Focus");
                    scannerClaimRelease(true);
                }
                }catch (ScannerUnavailableException e){
                    e.printStackTrace();
                }
            }
        });


        btnAutomaticBarcode = (Button) findViewById(R.id.button1);
        btnAutomaticBarcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (barcodeReader != null) {
                    try {
                        if(!bUseClientTriggerControl)
                            barcodeReader.softwareTrigger(true);
                    } catch (ScannerNotClaimedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();

                    } catch (ScannerUnavailableException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();

                    }

                } else {
                    showToastMsg("Barcodereader not available");
                }
            }
        });

        editScan =(EditText)findViewById(R.id.editScan);
        editScan.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                btnAutomaticBarcode.setEnabled(hasFocus);
            }
        });

        editScan.requestFocus();
    }

    void playSound(){
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void scannerClaimRelease(boolean bDoClaim) throws ScannerUnavailableException{
        if(bDoClaim){

            //reset software trigger if soft button has been used in between using the wedge
            //another option will be to disable the Software Scan Button if the EditText has Focus
            if(barcodeReader!=null) {
                try{
                    if(!bUseClientTriggerControl)
                        barcodeReader.softwareTrigger(false);

                }
                catch (ScannerNotClaimedException e){
                    e.printStackTrace();
                }
            }


            if (barcodeReader != null) {
                String[] sProps={"DEC_MULTIREAD_ENABLED", "DEC_SHOW_DECODE_WINDOW", "DEC_WINDOW_MODE"};
                String sProp="";
                try {
                    barcodeReader.claim();
                    sProp=barcodeReader.PROPERTY_DECODE_WINDOW_BOTTOM;
                    //Sets the bottom edge of the scan window within the scanner's overall image window. A value of 50 is the center of the image window, and 100 is the bottom.
                    barcodeReader.setProperty(barcodeReader.PROPERTY_DECODE_WINDOW_BOTTOM, 50);

                    //############ some of the Props do not work here ############
//                    sProp=sProps[0];
//                    barcodeReader.setProperty("DEC_MULTIREAD_ENABLED", true);
//                    sProp=sProps[1];
//                    barcodeReader.setProperty("DEC_SHOW_DECODE_WINDOW", true);
//                    sProp=sProps[2];
                    //barcodeReader.setProperty("DEC_WINDOW_MODE", true); //same as PROPERTY_CENTER_DECODE=false;
                    sProp=barcodeReader.PROPERTY_CENTER_DECODE;
                    barcodeReader.setProperty(sProp, true);

//                    sProp=barcodeReader.GOOD_READ_NOTIFICATION; //just for the event
//                    barcodeReader.setProperty(sProp, true);

//                    sProp=barcodeReader.IMAGER_SAMPLE_METHOD_CENTER_WEIGHTED;
//                    barcodeReader.setProperty(sProp, true);
//                    sProp=barcodeReader.IMAGER_SAMPLE_METHOD_CENTER;
//                    barcodeReader.setProperty(sProp, true);
//                    sProp=barcodeReader.IMAGER_SAMPLE_METHOD_UNIFORM;
//                    barcodeReader.setProperty(sProp, true);

                    dumpDecoderWindow(barcodeReader);
                    setDecoderWindow(barcodeReader, getWindow(0));
                    dumpDecoderWindow(barcodeReader);

                    // register bar code event listener
                    barcodeReader.addBarcodeListener(MainActivity.this);
                    Log.d(TAG, "scanner claimed");
                    if(bUseClientTriggerControl)
                    {
                        // register trigger state change listener
                        barcodeReader.addTriggerListener(MainActivity.this);
                    }
                }
                catch(UnsupportedPropertyException e1){
                    e1.printStackTrace();
                    Log.i(TAG, "\tscanner UnsupportedPropertyException: "+sProp);
                    Toast.makeText(this, "scanner UnsupportedPropertyException: "+sProp, Toast.LENGTH_LONG );
                }
                catch (ScannerUnavailableException e) {
                    e.printStackTrace();
                    Log.i(TAG, "\tscanner claim failed in lostFocus");
                    throw e;
                }
            }else{
                Log.i(TAG, "\tbarcodereader is null");
            }
        }else {
            if (barcodeReader != null) {
                barcodeReader.removeBarcodeListener(MainActivity.this);
                barcodeReader.release();
                Log.i(TAG, "\tscanner released");
            } else {
                Log.i(TAG, "\tbarcodereader is null");
            }
        }
    }

    void setDecoderWindow(BarcodeReader br, Rect rect){
        try {
            br.setProperty(br.PROPERTY_DECODE_WINDOW_LEFT, (int) (rect.left));
            br.setProperty(br.PROPERTY_DECODE_WINDOW_TOP, (int) (rect.top));
            br.setProperty(br.PROPERTY_DECODE_WINDOW_RIGHT, (int) (rect.right));
            br.setProperty(br.PROPERTY_DECODE_WINDOW_BOTTOM, (int) (rect.bottom));
        }catch(UnsupportedPropertyException e){

        }
    }

    void dumpDecoderWindow(BarcodeReader br){
        try {
            int x = br.getIntProperty(br.PROPERTY_DECODE_WINDOW_LEFT);
            int y = br.getIntProperty(br.PROPERTY_DECODE_WINDOW_TOP);
            Point p1 = new Point(x, y);
            x = br.getIntProperty(br.PROPERTY_DECODE_WINDOW_RIGHT);
            y = br.getIntProperty(br.PROPERTY_DECODE_WINDOW_BOTTOM);
            Point p2 = new Point(x, y);
            Log.d(TAG, "Decode_Window: " + p1.toString() + "/" + p2.toString());
        }catch (UnsupportedPropertyException e){

        }
    }

    /*
        return three different center windows to scan different areas
     */
    Rect getWindow(int order){
        /*
        the standard window is 50,50,50,50 with range from 0 to 100
        000 ----- 050 ----- 100
        |                     |
        025        +          |      first (0) center
        |                     |
        050        +          |      second (1) center
        |                     |
        075        +          |      third (2) center
        |                     |
        100 ----- 050 ----- 100
         */
        Rect pRect=new Rect(0,0,100,100);
        switch (order){
            case 0:
                pRect=new Rect(50,25,50,25);    //set window
                break;
            case 1:
                pRect=new Rect(50,50,50,50);
                break;
            case 2:
                pRect=new Rect(50,75,50,75);
                break;
        }
        return  pRect;
    }

    @Override
    public void onResume(){  //will always? be called before app becomes visible?
        super.onResume();
        Log.i(TAG, "app onResume()...");
        if (barcodeReader != null) {
            try {
                barcodeReader.claim();
                Log.d(TAG, "scanner claimed");
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
                Toast.makeText(this, "Scanner unavailable", Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    public void onStop(){
        super.onStop();
        Log.i(TAG, "app onStop()...");
        if(barcodeReader!=null)
            barcodeReader.release();
    }
    /**
     * Create buttons to launch demo activities.
     */

    private void showToastMsg(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "app onDestroy()...");
        if (barcodeReader != null) {
            // close BarcodeReader to clean up resources.
            barcodeReader.close();
            barcodeReader = null;
        }

        if (manager != null) {
            // close AidcManager to disconnect from the scanner service.
            // once closed, the object can no longer be used.
            manager.close();
        }
    }

    String getPoints(List<Point> bounds){
        StringBuilder sRet=new StringBuilder();
        for (Point p:bounds) {
            sRet.append(p.x);
            sRet.append(";");
            sRet.append(p.y);
            sRet.append(", ");
        }
        return sRet.toString();
    }

    int iState=0;
    final boolean[] bReadBarcde={false,false,false};
    final String[] sBarcodes={"","",""};
    /*
    First Code.
    I 2o5
    Length 12
    Start with 3

    Second code
    I 2o5
    Length 12
    Starts with 5

    Third code
    I 2o5
    Length 8
    Starts with 5

     */
    @Override
    public void onBarcodeEvent(final BarcodeReadEvent event) {
        Log.i(TAG, "onBarcodeEvent()...");
        try {
            if(!bUseClientTriggerControl)
                barcodeReader.softwareTrigger(false);

        } catch (ScannerNotClaimedException e) {
            e.printStackTrace();
        } catch (ScannerUnavailableException e) {
            e.printStackTrace();
        }
        // TODO Auto-generated method stub
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String barcodeData = event.getBarcodeData();
                String timestamp = event.getTimestamp();
                // update UI to reflect the data
                String s = editScan.getText().toString();
                s += "\n" + barcodeData + "\n" + timestamp;
                //s += "\nwindow: " + getPoints(event.getBarcodeBounds())+"\n-----------------------------\n";
                Log.i(TAG, s);
                editScan.setText(s);
                /* the so called scanned image, does not always look like that is correct
                Bitmap bmp = barcodeReader.captureImage();
                if(bmp!=null){
                    imageView.setImageBitmap(bmp);
                }
                */
            }
        });

        //########## SIMULATE MULTICODE READING ############
        String bData=event.getBarcodeData();
        if(bData.startsWith("3") && bData.length()==12 && !bReadBarcde[0]){
            //first
            bReadBarcde[0]=true;
            sBarcodes[0]=event.getBarcodeData();
            setDecoderWindow(barcodeReader, getWindow(1));
            Log.i(TAG, "sBarcodes[0]: " + sBarcodes[0]);
        }
        //43 06407 70005
        if(bData.startsWith("5") && bData.length()==12 && !bReadBarcde[1]){
            //second
            bReadBarcde[1]=true;
            sBarcodes[1]=event.getBarcodeData();
            setDecoderWindow(barcodeReader, getWindow(2));
            Log.i(TAG, "sBarcodes[1]: " + sBarcodes[1]);
        }
        //30 90724 16703
        if(bData.startsWith("5") && bData.length()==8 && !bReadBarcde[2]){
            //third
            bReadBarcde[2]=true;
            sBarcodes[2]=event.getBarcodeData();
            setDecoderWindow(barcodeReader, getWindow(0));
            Log.i(TAG, "sBarcodes[2]: " + sBarcodes[2]);
        }
        //all read?
        if(bReadBarcde[0]==true && bReadBarcde[1]==true && bReadBarcde[2]==true ){
            Log.i(TAG, "All barcodes read");
            Message msg = uiThreadHandler.obtainMessage();
            msg.obj = sBarcodes[0]+"\n"+sBarcodes[1]+"\n"+sBarcodes[2];
            uiThreadHandler.sendMessage(msg);
            Log.i(TAG, msg.obj.toString());
            playSound();
            bReadBarcde[0]=false; bReadBarcde[1]=false; bReadBarcde[2]=false;
            sBarcodes[0]=""; sBarcodes[1]=""; sBarcodes[2]="";
            //prepare reset soft trigger count
            iState=2;
            if(bUseClientTriggerControl)
                triggerState=!triggerState; //reset trigger
        }
        else{
            //need to read more barcode
            try {
                if(!bUseClientTriggerControl) {
                    barcodeReader.softwareTrigger(true);
                }else{
                    //lit scanner
                    barcodeReader.aim(true);
                    barcodeReader.light(true);
                    barcodeReader.decode(true);
                }
            } catch (ScannerNotClaimedException e) {
                e.printStackTrace();
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
            }
        }
        iState++;
        if(iState>2){
            iState=0;
            try {
                if(!bUseClientTriggerControl)
                    barcodeReader.softwareTrigger(false);
            } catch (ScannerNotClaimedException e) {
                e.printStackTrace();
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
            }
        }
    }


    // these events can be used to implement custom trigger modes if the automatic
    // behavior provided by the scanner service is insufficient for your application.
    // the following code demonstrates a "toggle" mode implementation, where the state
    // of the scanner changes each time the scan trigger is pressed.
    @Override
    public void onTriggerEvent(TriggerStateChangeEvent event) {
        try {
            Log.d(TAG, "onTriggerEvent: " + event.getState() );
            // only handle trigger presses
            if (event.getState()) {
                // turn on/off aimer, illumination and decoding
                barcodeReader.aim(!triggerState);
                barcodeReader.light(!triggerState);
                barcodeReader.decode(!triggerState);

                triggerState = !triggerState;
            }
        } catch (ScannerNotClaimedException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Scanner is not claimed",
                    Toast.LENGTH_SHORT).show();
        } catch (ScannerUnavailableException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Scanner unavailable",
                    Toast.LENGTH_SHORT).show();
        }
    }

    Handler uiThreadHandler = new Handler() {
        public void handleMessage(Message msg) {
            Object o = msg.obj;
            if (o==null)
                o = "";
            TextView textIn = (TextView)findViewById(R.id.editScan);
            textIn.setText(o.toString());
        }
    };

    @Override
    public void onFailureEvent(BarcodeFailureEvent arg0) {
        Log.i(TAG, "onFailureEvent()...");
        // TODO Auto-generated method stub
        try {
            if(!bUseClientTriggerControl)
                barcodeReader.softwareTrigger(false);
        } catch (ScannerNotClaimedException e) {
            e.printStackTrace();
        } catch (ScannerUnavailableException e) {
            e.printStackTrace();
        }

    }

}
