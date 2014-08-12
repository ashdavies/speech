package com.chaos.speech;

import java.util.Date;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.speech.SpeechRecognizer;
import android.widget.RemoteViews;

public class SpeechService extends Service {

	// Speech recogniser
	SpeechRecognizer recogniser;
	
	// Static variables
	private static int RUNNABLE_INTERVAL = 1000;
	private static int RESET_INTERVAL = 8000;
	private static int LENGTH_INTERVAL = 30;
	
	// Live card
    private LiveCard liveCard;
    private RemoteViews remoteViews;

    // Speech recognition text
    String speech = "...";
    
    // Last updated
    Long updated;
    
    // Handler and runnable
    private final Handler handler = new Handler( );
    private final Runnable runnable = new Runnable( ) {
        @Override public void run( ) {

        	// Reset the text if needed
        	if ( speech.length( ) > LENGTH_INTERVAL ) speech = "...";
        	else {
        		Long time = new Date( ).getTime( );
        		if ( time - updated > RESET_INTERVAL ) speech = "...";
        	}
        	
            // Update the remote view with the speech recognition
        	remoteViews.setTextViewText( R.id.text, speech );

            // Always call setViews() to update the live card's RemoteViews.
            liveCard.setViews( remoteViews );

            // Queue another score update in a seconds.
            handler.postDelayed( this, RUNNABLE_INTERVAL );
            
        }
    	
    };


    @Override public void onCreate( ) { super.onCreate( ); }

    @Override public int onStartCommand( Intent intent, int flags, int startId ) {
    	
    	// Create new live card
        if ( liveCard == null ) {

            // Get an instance of a live card
            liveCard = new LiveCard( this, this.getClass( ).getName( ) );

            // Inflate a layout  and publish
            remoteViews = new RemoteViews( getPackageName( ), R.layout.card_speech );
            liveCard.publish( PublishMode.REVEAL );

            // Queue the update text runnable
            handler.post( runnable );
        }
        
        // Instantiate speech recogniser
        recogniser = SpeechRecognizer.createSpeechRecognizer( this );
        
        return START_STICKY;
        
    }

 
    /**
     * Stop the handler from queueing more Runnable jobs
     */
    @Override public void onDestroy( ) {
        if ( liveCard != null && liveCard.isPublished( ) ) {
        	handler.removeCallbacks( runnable );
            liveCard.unpublish( );
            liveCard = null;
        }
        super.onDestroy( );
    }

    /**
     * Unused binder
     * @param intent
     */
    @Override public IBinder onBind( Intent intent ) { return null; }
    
}