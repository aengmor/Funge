package com.funge.funge;
 
import android.content.*;
import android.os.*;
import android.view.*;
import androidx.appcompat.app.*;

public class MainActivity extends AppCompatActivity {
     
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startActivity(new Intent(this, Level.class));
        
    }
	
	public void back(View view) {startActivity(new Intent(this, Level.class));}
}
