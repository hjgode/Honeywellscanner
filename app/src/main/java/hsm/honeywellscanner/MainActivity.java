package hsm.honeywellscanner;

//import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.honeywell.aidc.*;

public class MainActivity extends Activity implements BarcodeReader.BarcodeListener {

    private static BarcodeReader barcodeReader;
    private AidcManager manager;

    private Button btnAutomaticBarcode;
    boolean useTrigger=true;
    boolean btnPressed = false;

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView=(TextView)findViewById(R.id.textView);
        // create the AidcManager providing a Context and a
        // CreatedCallback implementation.
        AidcManager.create(this, new AidcManager.CreatedCallback() {
            @Override
            public void onCreated(AidcManager aidcManager) {
                manager = aidcManager;
                barcodeReader = manager.createBarcodeReader();

                try {
                    if(barcodeReader!=null) {
                        Log.d("honeywellscanner: ", "barcodereader not claimed in OnCreate()");
                        barcodeReader.claim();
                    }
                    // apply settings
                    barcodeReader.setProperty(BarcodeReader.PROPERTY_CODE_39_ENABLED, false);
                    barcodeReader.setProperty(BarcodeReader.PROPERTY_DATAMATRIX_ENABLED, true);

                    // set the trigger mode to automatic control
                    barcodeReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE,
                            BarcodeReader.TRIGGER_CONTROL_MODE_AUTO_CONTROL);
                } catch (UnsupportedPropertyException e) {
                    Toast.makeText(MainActivity.this, "Failed to apply properties",
                            Toast.LENGTH_SHORT).show();
                } catch (ScannerUnavailableException e) {
                    Toast.makeText(MainActivity.this, "Failed to claim scanner",
                            Toast.LENGTH_SHORT).show();
                    //e.printStackTrace();
                }

                // register bar code event listener
                barcodeReader.addBarcodeListener(MainActivity.this);
            }
        });

        ActivitySetting();
    }

    @Override
    public void onResume(){  //will always? be called before app becomes visible?
        super.onResume();
        if (barcodeReader != null) {
            try {
                barcodeReader.claim();
                Log.d("noneywellscanner: ", "scanner claimed");
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
                Toast.makeText(this, "Scanner unavailable", Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    public void onStop(){
        super.onStop();
        if(barcodeReader!=null)
            barcodeReader.release();
    }
    /**
     * Create buttons to launch demo activities.
     */
    public void ActivitySetting() {
        btnAutomaticBarcode = (Button) findViewById(R.id.button1);
        btnAutomaticBarcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(barcodeReader!=null){
                    try {

                        barcodeReader.softwareTrigger(true);
                    } catch (ScannerNotClaimedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();

                    } catch (ScannerUnavailableException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();

                    }

                }
                else{
                    showToastMsg("Barcodereader not available");
                }
            }
        });

    }

    private void showToastMsg(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

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
    @Override
    public void onBarcodeEvent(final BarcodeReadEvent event) {
        // TODO Auto-generated method stub
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String barcodeData = event.getBarcodeData();
                String timestamp = event.getTimestamp();
                // update UI to reflect the data
                String s = (String) textView.getText();
                s+="\n"+barcodeData+"\n"+timestamp;
                textView.setText(s);
            }
        });

    }
    @Override
    public void onFailureEvent(BarcodeFailureEvent arg0) {
        // TODO Auto-generated method stub

    }

}
