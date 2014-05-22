package com.lexzone.hoveregg;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The bluetooth menu that allows to scand and connect to the Arduino via BT
 * */
public class Bluetooth_menu extends Fragment {

	// Fragment's variables - Bluetooth related
	private static final int REQUEST_ENABLE_BT = 1;
	private static final String DELIMITER = "\n";
	private Button list;
	private BluetoothAdapter BA;
	private ArrayAdapter<String> btArrayAdapter;
	private ListView listDevicesFound;
	TextView stateBluetooth;
	private String bluetoothAdress;
	
	private OnFragmentInteractionListener mListener;

	public static Bluetooth_menu newInstance() {
		Bluetooth_menu fragment = new Bluetooth_menu();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	public Bluetooth_menu() {
		// Required empty public constructor
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	// Here we initialiase all our UI and functional elements
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		// Get the view for the fragment, as this is not an activity
		View v = inflater.inflate(R.layout.fragment_bluetooth_menu, container, false);

		// Tell the MainActivity that we have shown this menu
		MainActivity.setBT_Menu(true);
		
		// Initialise all the UI and functional elements
		list = (Button) v.findViewById(R.id.button_list);
		listDevicesFound = (ListView) v.findViewById(R.id.listView_bt);
		btArrayAdapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_list_item_1);
		listDevicesFound.setAdapter(btArrayAdapter);
		stateBluetooth = (TextView) v.findViewById(R.id.state_bt);
		BA = BluetoothAdapter.getDefaultAdapter();

		// Action when we press the List Devices button
		list.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
	    		btArrayAdapter.clear();
	    		BA.startDiscovery();
			}
		});
		
		// Action when we press an item from the list of detected devices
		listDevicesFound.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View arg1,
					int position, long arg3)
			{
				String selectedItem = (String) adapter
						.getItemAtPosition(position);
				String[] deviceInfo = selectedItem.split(DELIMITER);
				bluetoothAdress = deviceInfo[1];

				new ConnectTask().execute();
			}
		});
		
		// Check the state of the BT
		CheckBlueToothState();

		// Add the scanned devices to the list
		getActivity().registerReceiver(ActionFoundReceiver,
				new IntentFilter(BluetoothDevice.ACTION_FOUND));
		
		// Inflate the layout for this fragment
		return v;
	}

	// TODO: Rename method, update argument and hook method into UI event
	public void onButtonPressed(Uri uri) {
		if (mListener != null) {
			mListener.onFragmentInteraction(uri);
		}
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		getActivity().unregisterReceiver(ActionFoundReceiver);
		
		// Set this property to false, as we leave this fragment
		MainActivity.setBT_Menu(false);
	}
	

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}
	
	public interface OnFragmentInteractionListener {
		// TODO: Update argument type and name
		public void onFragmentInteraction(Uri uri);
	}
	
	// Checks the state of the BT and displays it to the user
	private void CheckBlueToothState()
		{
			if( BA == null )
			{
				stateBluetooth.setText("Bluetooth NOT support");
			}
			else
			{
				if( BA.isEnabled() )
				{
					if( BA.isDiscovering() )
					{
						stateBluetooth
								.setText("Bluetooth is currently in device discovery process.");
					}
					else
					{
						stateBluetooth.setText("Bluetooth is Enabled.");
						list.setEnabled(true);
					}
				}
				else
				{
					stateBluetooth.setText("Bluetooth is NOT Enabled!");
					Intent enableBtIntent = new Intent(
							BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
				}
			}
		}
	
	// We populate the list with the name and BT id's
	private final BroadcastReceiver ActionFoundReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			if( BluetoothDevice.ACTION_FOUND.equals(action) )
			{
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if( btArrayAdapter.getPosition(device.getName() + "\n"
						+ device.getAddress()) < 0 )
				{
					btArrayAdapter.add(device.getName() + "\n"
							+ device.getAddress());
				}
				btArrayAdapter.notifyDataSetChanged();
			}
		}
	};
	
	// The function that connects (pairs) with the selected device
	private void pairDevice(BluetoothDevice device)
	{
		try
		{
			Log.d("pairDevice()", "Start Pairing...");
			Method m = device.getClass()
					.getMethod("createBond", (Class[]) null);
			m.invoke(device, (Object[]) null);
			Log.d("pairDevice()", "Pairing finished.");
		}
		catch (Exception e)
		{
			Log.e("pairDevice()", e.getMessage());
		}
	}
	
	// Async task that allows us to set a waiting dialog while we connect to the
	// device
	private class ConnectTask extends AsyncTask<Void, Void, Boolean>
	{
		private final ProgressDialog dialog = new ProgressDialog(getActivity());

		@Override
		protected void onPreExecute()
		{
			this.dialog.setMessage("Connecting! Please wait..");
			this.dialog.setIndeterminate(true);
			this.dialog.setCancelable(false);
			this.dialog.show();
		}

		@Override
		protected Boolean doInBackground(Void... params)
		{
			if( bluetoothAdress == null || bluetoothAdress.isEmpty() )
				return false;
			BluetoothDevice device = BA
					.getRemoteDevice(bluetoothAdress);
			pairDevice(device);

			try
			{
				BluetoothSocket socket = null;
				Method m;
				try
				{
					m = device.getClass().getMethod("createRfcommSocket",
							new Class[] { int.class });
					socket = (BluetoothSocket) m.invoke(device, 1);
				}
				catch (NoSuchMethodException e)
				{
					e.printStackTrace();
				}
				catch (IllegalAccessException e)
				{
					e.printStackTrace();
				}
				catch (IllegalArgumentException e)
				{
					e.printStackTrace();
				}
				catch (InvocationTargetException e)
				{
					e.printStackTrace();
				}
				socket.connect();
				if( socket.isConnected() )
				{
					Log.d("BT", "Device connected!");
					MainActivity.setSocket(socket);
					return true;
				}
				Log.d("BT", "Connection Failed!");
				return false;
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			if( this.dialog.isShowing() )
				this.dialog.dismiss();

			if( result )
			{
				Toast.makeText(getActivity(), "Device connected!",
						Toast.LENGTH_LONG).show();
				stateBluetooth.setText("Device connected!");
			}
			else
			{
				Toast.makeText(getActivity(),
						"Connection failed! \n Please try again.",
						Toast.LENGTH_LONG).show();
			}
		}
	}
	
}
