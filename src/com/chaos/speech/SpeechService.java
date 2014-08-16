package com.chaos.speech;

import java.util.Date;
import java.util.List;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.RemoteViews;

public class SpeechService extends Service implements RecognitionListener {

	// Static context reference
	public static SpeechService Context;
	
    private static final String LIVE_CARD_TAG = "speech";
    private static final String LIVE_CARD_DEFAULT_TEXT = "Listening...";
    
	// Speech recogniser
	private SpeechRecognizer recogniser;
	
	// Boolean is listening
	private boolean listening;
	
	// Static variables
	private static int RUNNABLE_INTERVAL = 1000;
	private static int RESET_INTERVAL = 15000;
	private static int LENGTH_INTERVAL = 30;
	
	// Live card
    private LiveCard liveCard;
    private RemoteViews remoteViews;

    // Speech recognition text
    String speech = LIVE_CARD_DEFAULT_TEXT;
    
    // Last updated
    Long updated = (long)0;
    
    // Handler and runnable
    private final Handler handler = new Handler( );
    private final Runnable runnable = new Runnable( ) {
        @Override public void run( ) {

        	// Reset the text if needed
        	if ( speech.length( ) > LENGTH_INTERVAL ) speech = LIVE_CARD_DEFAULT_TEXT;
        	else {
        		Long time = new Date( ).getTime( );
        		if ( time - updated > RESET_INTERVAL ) {
        			if ( !isListening( ) ) startListening( );
        			speech = LIVE_CARD_DEFAULT_TEXT;
        		}
        	}
        	
            // Update the remote view with the speech recognition
        	remoteViews.setTextViewText( R.id.text, speech );

            // Always call setViews to update the live card's RemoteViews.
            liveCard.setViews( remoteViews );

            // Queue another score update in a seconds.
            handler.postDelayed( this, RUNNABLE_INTERVAL );
            
        }
    	
    };


    /**
     * Storing a static global context of self reference class?
     * Naughty naughty...
     */
    @Override public void onCreate( ) {
    	super.onCreate( );
    	SpeechService.Context = this;
    }

    @Override public int onStartCommand( Intent intent, int flags, int startId ) {
    	
    	// Create new live card
        if ( liveCard == null ) {

            // Get an instance of a live card
            liveCard = new LiveCard( this, LIVE_CARD_TAG );
            
            // Create menu intent for live card
            Intent menuIntent = new Intent( this, MenuActivity.class );
            menuIntent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK );
            liveCard.setAction( PendingIntent.getActivity( this, 0, menuIntent, 0 ) );
            
            // Inflate a layout  and publish
            remoteViews = new RemoteViews( getPackageName( ), R.layout.card_speech );
            remoteViews.setTextViewText( R.id.text, speech );
            
            // Set view and publish
            liveCard.setViews( remoteViews );
            liveCard.publish( PublishMode.REVEAL );

            // Queue the update text runnable
            handler.post( runnable );
            
        }
        
        // Live card already exists
        else liveCard.navigate( );
        
        // Instantiate speech recogniser
        recogniser = SpeechRecognizer.createSpeechRecognizer( getApplicationContext( ) );
        recogniser.setRecognitionListener( this );
        
        // Start speech
        startListening( );
        
        return START_STICKY;
        
    }

 
    /**
     * Stop the handler from queueing more Runnable jobs
     */
    @Override public void onDestroy( ) {
    	
    	// Unpublish live card
        if ( liveCard != null && liveCard.isPublished( ) ) {
        	handler.removeCallbacks( runnable );
            liveCard.unpublish( );
            liveCard = null;
        }
        
        // Stop speech recognition
        recogniser.cancel( );
        recogniser.destroy( );
        recogniser = null;
        
        super.onDestroy( );
        
    }
    
    /**
     * Is listening?
     */
    public boolean isListening( ) { return this.listening; }
    
    /**
     * Start the speech service recogniser
     */
    public void startListening( ) {
        
    	// Check if already listening
    	if ( isListening( ) == true ) return;
    	
        // Create recogniser intent
        Intent recognizerIntent = new Intent( RecognizerIntent.ACTION_RECOGNIZE_SPEECH );        
        recognizerIntent.putExtra( RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM );
        recognizerIntent.putExtra( RecognizerIntent.EXTRA_CALLING_PACKAGE,  getPackageName( ) );
        recogniser.startListening( recognizerIntent );
        this.listening = true;
    	
    }
    
    /**
     * Stop the speech service recogniser
     */
    public void stopListening( ) {
    	if ( isListening( ) ) recogniser.stopListening( );
    	this.listening = false;
    }

    /**
     * Unused binder
     * @param intent
     */
    @Override public IBinder onBind( Intent intent ) { return null; }

	@Override public void onBeginningOfSpeech( ) { Log.i( "SpeechService", "onBeginningOfSpeech" ); speech = "..."; }
	@Override public void onBufferReceived( byte[] buffer ) { Log.i( "SpeechService", "onBufferReceived" ); }
	@Override public void onEndOfSpeech( ) { Log.i( "SpeechService", "onEndOfSpeech" ); this.listening = false; }
	@Override public void onError( int error ) { Log.i( "SpeechService", "onEvent" ); recogniser.cancel( ); this.listening = false; }
	@Override public void onEvent( int eventType, Bundle params ) { Log.i( "SpeechService", "onEvent" ); }
	@Override public void onReadyForSpeech( Bundle params ) { Log.i( "SpeechService", "onReadyForSpeech" ); }
	@Override public void onRmsChanged( float rmsdB ) { }
	
	@Override public void onPartialResults( Bundle partialResults ) {
		
		Log.i( "SpeechService", "onPartialResults" );
		
		// Store update time
		updated = new Date( ).getTime( );
		
		// Reset speech string
		speech = "";
		
		// Fetch results
		List<String> recognitionResults = partialResults.getStringArrayList( SpeechRecognizer.RESULTS_RECOGNITION );
		for( String item : recognitionResults ) speech += item + " ";
		
	}
	
	@Override public void onResults( Bundle results ) {
		
		Log.i( "SpeechService", "onResults" );
		
		// Store update time
		updated = new Date( ).getTime( );
		
		// Reset speech string
		speech = "";
		
		// Fetch results
		List<String> recognitionResults = results.getStringArrayList( SpeechRecognizer.RESULTS_RECOGNITION );
		for( String item : recognitionResults ) speech += item + " ";
		
	}
	
    
}