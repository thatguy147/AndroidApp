package com.javacodegeeks.android.androidsocketserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.view.View.OnClickListener;

public class Server extends Activity {

	private ServerSocket serverSocket;

	Handler updateConversationHandler;
	Handler sendAMessage;

	Thread serverThread = null;

	private TextView text;
	private Button sendCANMsgbtn;
	private Button addCANIDbtn;
	private Button remCANIDbtn;
	private Button configModebtn;
	
	private EditText canIDTextField;
	
	private BufferedReader input;
	private PrintWriter output;
	private int messageCount = 10;
	
	private boolean _configMode = false;
	
	public static final int SERVERPORT = 6001;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		text = (TextView) findViewById(R.id.text2);
		canIDTextField = (EditText)findViewById(R.id.CANIDInput);
		//addListenerOnButton();
		addListenerToAddCANIDbtn();
		addListenerToRemCANIDbtn();
		addListenerToConfigModebtn();
		addListenerToSendCANMsgbtn();

		updateConversationHandler = new Handler();
		sendAMessage = new Handler();

		this.serverThread = new Thread(new ServerThread());
		this.serverThread.start();

	}
	
	public void addListenerToAddCANIDbtn(){
		addCANIDbtn = (Button) findViewById(R.id.AddCANIDbtn);
		addCANIDbtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0){
				String canIDText = getCANIDFromTextField();
				int canIDHexValue = Integer.parseInt(canIDText, 16);
				String canIDHexToDecString = String.format("%04d", canIDHexValue);
				String addIDCommand = "{SI"+canIDHexToDecString+"}";
				output.write(addIDCommand);
				output.flush();
			}
		});
	}
	
	
	
	public void addListenerToRemCANIDbtn(){
		remCANIDbtn = (Button) findViewById(R.id.RemoveIDbtn);
		remCANIDbtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0){
				String canIDText = getCANIDFromTextField();
				int canIDHexValue = Integer.parseInt(canIDText, 16);
				String canIDHexToDecString = String.format("%04d", canIDHexValue);
				String remIDCommand = "{RI"+canIDHexToDecString+"}";
				output.write(remIDCommand);
				output.flush();
				//String remIDCommand = "{RIxxx}";
				
			}
		});
	}
	
	public void addListenerToConfigModebtn(){
		configModebtn = (Button) findViewById(R.id.ConfigModebtn);
		configModebtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0){
				String configModeText = "{EC}";
				output.write(configModeText);
				output.flush();

			}
		});
	}
	
	public void addListenerToSendCANMsgbtn(){
		sendCANMsgbtn = (Button) findViewById(R.id.sendCANMessageBtn);
		sendCANMsgbtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0){
				String messageToSend = "[ID 002 "+ messageCount + " FF FF FF FF FF FF FF]";
				if(output != null){
 					output.write(messageToSend);
 					output.flush();
 				}
				if(messageCount > 19){
 					messageCount = 10;
 				}
			}
		});
	}
	
	public String getCANIDFromTextField(){
		return canIDTextField.getText().toString();
	}
	
	/*public void addListenerOnButton() {
		
		sendMessage = (Button) findViewById(R.id.sendCANMessageBtn);
		sendMessage.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				//String messageToSend = "[ID 002 "+ messageCount + " FF FF FF FF FF FF FF]";
				String messageToSend = "{SI005 }";
 				if(output != null){
 					output.write(messageToSend);
 					output.flush();
 				}
 				else{
 					
 					System.out.println(messageToSend);
 				}
 				messageCount++;
 				if(messageCount > 19){
 					messageCount = 10;
 				}
			}
				
		});
	}*/

	@Override
	protected void onStop() {
		super.onStop();
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	class ServerThread implements Runnable {

		private CommunicationThread commThread;
		
		public void run() {
			Socket socket = null;
			try {
				serverSocket = new ServerSocket(SERVERPORT);
			} catch (IOException e) {
				e.printStackTrace();
			}
			while (!Thread.currentThread().isInterrupted()) {

				try {

					socket = serverSocket.accept();

					commThread = new CommunicationThread(socket);
					new Thread(commThread).start();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		public CommunicationThread getTheThread(){
			return (CommunicationThread) commThread;
		}

	}

	class CommunicationThread implements Runnable {

		private Socket clientSocket;

		//private BufferedReader input;
		//private PrintWriter output;

		public CommunicationThread(Socket clientSocket) {

			this.clientSocket = clientSocket;

			try {

				input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
				output = new PrintWriter(clientSocket.getOutputStream());

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public PrintWriter getOutputStream()
		{
			return output;
		}
		
		public void sendMessage(String messageToSend)
		{
			if(messageToSend.length() > 40){
				System.out.println("Larger than 40");
			}
			output.write(messageToSend);
			output.flush();
		}

		public void run() {
			

			while (!Thread.currentThread().isInterrupted()) {

				try {

					String read = input.readLine();

					updateConversationHandler.post(new updateUIThread(read));
					//output.write("Just Testing");
					//output.flush();
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	class updateUIThread implements Runnable {
		private String msg;

		public updateUIThread(String str) {
			this.msg = str;
		}

		@Override
		public void run() {
			
			
			//if(msg.charAt(0) == '{'){
			//	int num = msg.indexOf("}");
			//Toast.makeText(getApplicationContext(), 
            //           msg.subSequence(1,num), Toast.LENGTH_LONG).show();
			//}
			//else{
					String currentDateandTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
					if(text.getText().toString().length() > 900)
					{
						text.setText(/*text.getText().toString()+*/"RX: "+ currentDateandTime +" " + msg + "\n");
					}
					else{
					text.setText(text.getText().toString() + "RX: "+ currentDateandTime +" " + msg + "\n");
					}	
			//}
			
			
		}

	}

}