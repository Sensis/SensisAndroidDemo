package au.com.sensis.android.demo;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class PingableService extends Service {
    
    private final int NOTIFY_ID = 111;
    private final IBinder binder = new PingableBinder();
    
    private SomeListener listener;

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("PingableService", "onBind"); 
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("PingableService", "onUnbind");
        notification();
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_REDELIVER_INTENT;
    }
    
    public String ping(){
        return "Service was pinged";
    }
    
    public void notification(){
        NotificationCompat.Builder b=new NotificationCompat.Builder(this);
        b.setAutoCancel(true).setWhen(System.currentTimeMillis());
        
        b.setContentTitle("Pingable Service")
        .setContentText("Pingable Service is still running")
        .setSmallIcon(android.R.drawable.stat_sys_warning)
        .setTicker("Pingable Service is still running");
        
        Intent outbound=new Intent(this, ServiceDemo_.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        b.setContentIntent(PendingIntent.getActivity(this, 0, outbound, 0));
        
        NotificationManager mgr= (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mgr.notify(NOTIFY_ID, b.getNotification());
    }
    
    public void addListener(SomeListener listener){
        new DataGeneratorTask().execute();
        this.listener = listener;
    }
    
    public void removeListener(SomeListener listener){
        this.listener = null;
    }
    
    class PingableBinder extends Binder {
        public PingableService getService(){
            return PingableService.this;
        }
    }
    
    class DataGeneratorTask extends AsyncTask<Void, String, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            
            for (int i = 1; i <= 1000; i++){
                synchronized (this){ try {wait(1000);} catch (InterruptedException e) {} }
                publishProgress("Pingable update #" + i);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (listener != null){
                listener.listenToThis(values[0]);
            }
        }
        
    }
}
