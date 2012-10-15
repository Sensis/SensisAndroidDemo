package au.com.sensis.android.demo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import au.com.sensis.android.demo.PingableService.PingableBinder;

import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.ViewById;


@EActivity(R.layout.service_demo)
public class ServiceDemo extends Activity implements ServiceConnection, SomeListener {
    
    private PingableService service;
    
    @ViewById(R.id.txtServiceData)
    TextView serviceData;
    
    @ViewById(R.id.btnPing)
    Button ping;
    
    @ViewById(R.id.btnNotification)
    Button notification;
    
    @ViewById(R.id.btnListen)
    Button listen;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, PingableService.class));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, PingableService.class), this, BIND_NOT_FOREGROUND);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        service.notification();
        service.removeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, PingableService.class));
    }

    @Click(R.id.btnPing)
    void clickPing(){
        String s = service.ping();
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
    
    @Click(R.id.btnNotification)
    void clickNotification(){
        service.notification();
    }
    
    @Click(R.id.btnListen)
    void clickListen(){
        service.addListener(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.i("ServiceDemo", "Service Connected");
        this.service = ((PingableBinder)service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i("ServiceDemo", "Service Disconnected");
    }

    @Override
    public void listenToThis(String s) {
        serviceData.setText(s);
    }
    
    

}
