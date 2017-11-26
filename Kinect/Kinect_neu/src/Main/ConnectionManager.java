package Main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ConnectionManager {

	private static Socket client;
	private static DataOutputStream dataOutputStream;
	private static DataInputStream dataInputStream;
	private static boolean connected;

	public static void connect(String address, int port) {
		try {
			client = new Socket(address, port);
			dataOutputStream = new DataOutputStream(client.getOutputStream());
			dataInputStream = new DataInputStream(client.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void send(String s) {
		try {
			dataOutputStream.writeUTF(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void send(int i) {
		try {
			dataOutputStream.writeUTF(String.valueOf(i));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void send(double d1, double d2) {
		try {
			StringBuilder s = new StringBuilder();
			s.append(d1);
			s.append('#');
			s.append(d2);
			dataOutputStream.writeUTF(s.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void send(double d1, double d2, char seperator) {
		try {
			StringBuilder s = new StringBuilder();
			s.append(d1);
			s.append(seperator);
			s.append(d2);
			dataOutputStream.writeUTF(s.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String receiveString() {
		try {
			return dataInputStream.readUTF();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static int receiveInt() {
		try {
			return dataInputStream.readInt();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	public static boolean isConnected() {
		return connected;
	}
	
	public static DataOutputStream getDataOutputStream() {
		return dataOutputStream;
	}
	
	public static DataInputStream getDataInputStream() {
		return dataInputStream;
	}
	
	public static Socket getClient() {
		return client;
	}
	
	public static String getLocalSocketAddress() {
		return client.getLocalSocketAddress().toString();
	}
	
	public static String getRemoteSocketAddress() {
		return client.getRemoteSocketAddress().toString();
	}
	
	public int getPort() {
		return client.getPort();
	}
	
}
