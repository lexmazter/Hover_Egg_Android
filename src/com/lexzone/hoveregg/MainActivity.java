package com.lexzone.hoveregg;

import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The main activity, used only as a funcitonal link between fragments
 * */
public class MainActivity extends Activity {

	private static BluetoothSocket socket;
	private static boolean BT_Menu = false;

	public static BluetoothSocket getSocket() {
		return socket;
	}

	public static void setSocket(BluetoothSocket socket) {
		MainActivity.socket = socket;
	}

	public static boolean isBT_Menu() {
		return BT_Menu;
	}

	public static void setBT_Menu(boolean bT_Menu) {
		BT_Menu = bT_Menu;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			if (!BT_Menu) {
				Fragment bt_frag = new Bluetooth_menu();
				FragmentTransaction transaction = getFragmentManager()
						.beginTransaction();
				bt_frag.setArguments(null);

				// Replace whatever is in the fragment_container view with this
				// fragment,
				// and add the transaction to the back stack
				transaction.replace(R.id.container, bt_frag);
				transaction.addToBackStack(null);

				// Commit the transaction
				transaction.commit();
			} else
				Toast.makeText(getApplicationContext(),
						"Bluetooth Menu is already showing!",
						Toast.LENGTH_SHORT).show();

			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * The main fragment used to control the Arduino
	 */
	public static class PlaceholderFragment extends Fragment implements
			OnSeekBarChangeListener {

		private TextView txtDistance;
		private SeekBar seekDistance;

		public PlaceholderFragment() {
		}

		// Here we initialiase all the UI and functional objects
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);

			// Initialiasation of UI and functional objects
			txtDistance = (TextView) rootView.findViewById(R.id.textDistance);
			seekDistance = (SeekBar) rootView.findViewById(R.id.seekDistance);

			seekDistance.setOnSeekBarChangeListener(this);

			return rootView;
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progresValue,
				boolean fromUser) {
			if (socket != null) {

				txtDistance.setText("Distance: " + progresValue + " mm" + "/"
						+ seekBar.getMax() + " mm");

				sendStringToBluetooth(progresValue+"");
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// Do something here,
			// if you want to do anything at the start of
			// touching the seekbar
			if (socket != null) {

			}
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// Display the value in textview
			if (socket != null) {

			} else
				Toast.makeText(getActivity().getApplicationContext(),
						"No device connected!", Toast.LENGTH_LONG).show();
		}
		
		private void sendStringToBluetooth(String c) {
			try {
				if (socket.isConnected()) {
					OutputStream dos = socket.getOutputStream();
					byte[] bytes;
					if (socket.isConnected()){
						
						bytes = c.getBytes("UTF-8");
						
						if(bytes.length == 2){
							byte[] aux = new byte[3];
							aux[2] = bytes[1];
							aux[1] = bytes[0];
							aux[0] = '0';
							bytes = aux;
						}
						if(bytes.length == 1){
							byte[] aux = new byte[3];
							aux[2] = bytes[0];
							aux[1] = '0';
							aux[0] = '0';
							bytes = aux;
						}
						dos.write(bytes);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}