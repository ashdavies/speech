package com.chaos.speech;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.lang.Runnable;

/**
 * Activity showing the speech options menu.
 */
public class MenuActivity extends Activity {

    private final Handler handler = new Handler( );
    
    @Override public void onAttachedToWindow( ) {
        super.onAttachedToWindow( );
        openOptionsMenu( );
    }

    @Override public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.speech, menu );
        return true;
    }

    @Override public boolean onOptionsItemSelected( MenuItem item ) {
    	
        // Handle item selection.
        switch ( item.getItemId( ) ) {
        
        	// Start listening
        	case R.id.start:
        		
        		if ( SpeechService.Context == null ) return false;
        		SpeechService.Context.startListening( );
        		return true;
        		
        	// Stop the service at the end of the message queue for proper options menu animation.
            case R.id.stop:
            	
                post( new Runnable( ) {
                    @Override public void run( ) {
                        stopService( new Intent( MenuActivity.this, SpeechService.class ) );
                    }
                } );
                
                return true;
                
            default:
                return super.onOptionsItemSelected( item );
                
        }
    }

    // Nothing else to do so close activity
    @Override public void onOptionsMenuClosed( Menu menu ) { finish( ); }

    // Posts a {@link Runnable} at the end of the message loop, overridable for testing.
    protected void post( Runnable runnable ) { handler.post( runnable ); }

}
