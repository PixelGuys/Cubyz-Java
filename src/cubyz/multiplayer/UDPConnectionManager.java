package cubyz.multiplayer;

import cubyz.utils.Logger;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

public final class UDPConnectionManager extends Thread {
	private final DatagramSocket socket;
	private final DatagramPacket receivedPacket;
	private final ArrayList<UDPConnection> connections = new ArrayList<>();
	private final ArrayList<DatagramPacket> requests = new ArrayList<>();
	private volatile boolean running = true;
	public final String externalIPPort;
	private InetAddress externalAddress = null;
	private int externalPort = 0;

	public UDPConnectionManager(int localPort) {
		// Connect
		DatagramSocket socket = null;
		//TODO: Might want to use SSL or something similar to encode the message
		while(socket == null) {
			try {
				socket = new DatagramSocket(localPort);
			} catch(SocketException e) {
				Logger.warning("Couldn't use port "+localPort+".");
				localPort++;
			}
		}
		this.socket = socket;

		receivedPacket = new DatagramPacket(new byte[65536], 65536);

		start();

		externalIPPort = STUN.requestIPPort(this);
		String[] ipPort = externalIPPort.split(":");
		try {
			externalAddress = InetAddress.getByName(ipPort[0]);
		} catch(UnknownHostException e) {
			Logger.error(e);
			throw new IllegalArgumentException("externalIPPort is invalid.");
		}
		externalPort = Integer.parseInt(ipPort[1]);
	}

	public void send(DatagramPacket packet) {
		try {
			socket.send(packet);
		} catch(IOException e) {
			Logger.error(e);
		}
	}

	public byte[] sendRequest(DatagramPacket packet, long timeout) {
		send(packet);
		byte[] request = packet.getData();
		synchronized(requests) {
			requests.add(packet);
		}
		synchronized(packet) {
			try {
				packet.wait(timeout);
			} catch(InterruptedException e) {}
		}
		synchronized(requests) {
			requests.remove(packet);
		}
		if(packet.getData() == request) {
			return null;
		} else {
			return packet.getData();
		}
	}

	public void addConnection(UDPConnection connection) {
		synchronized(connections) {
			connections.add(connection);
		}
	}

	public void removeConnection(UDPConnection connection) {
		synchronized(connections) {
			connections.remove(connection);
		}
	}

	public void cleanup() {
		while(!connections.isEmpty()) {
			connections.get(0).disconnect();
		}
		running = false;
		if(Thread.currentThread() != this) {
			interrupt();
			try {
				join();
			} catch(InterruptedException e) {
				Logger.error(e);
			}
		}
		socket.close();
	}

	private void onReceive() {
		byte[] data = receivedPacket.getData();
		int len = receivedPacket.getLength();
		InetAddress addr = receivedPacket.getAddress();
		int port = receivedPacket.getPort();
		for(UDPConnection connection : connections) {
			if(connection.remoteAddress.equals(addr) && connection.remotePort == port) {
				connection.receive(data, len);
				return;
			}
		}
		// Check if it's part of an active request:
		synchronized(requests) {
			for(DatagramPacket packet : requests) {
				if(packet.getAddress().equals(addr) && packet.getPort() == port) {
					packet.setData(Arrays.copyOf(data, len));
					synchronized(packet) {
						packet.notify();
					}
					return;
				}
			}
		}
		if(addr.equals(externalAddress) && port == externalPort) return;
		Logger.error("Unknown connection from address: " + addr+":"+port);
		Logger.debug("Message: "+Arrays.toString(Arrays.copyOf(data, len)));
	}

	@Override
	public void run() {
		assert Thread.currentThread() == this : "UDPConnectionManager.run() shouldn't be called by anyone.";
		try {
			socket.setSoTimeout(100);
			long lastTime = System.currentTimeMillis();
			while (running) {
				try {
					socket.receive(receivedPacket);
					onReceive();
				} catch(SocketTimeoutException e) {
					// No message within the last ~100 ms.
					// TODO: Add a counter that breaks connection if there was no message for a longer time.
				}

				// Send a keep-alive packet roughly every 100 ms:
				if(System.currentTimeMillis() - lastTime > 100) {
					lastTime = System.currentTimeMillis();
					for(UDPConnection connection : connections.toArray(new UDPConnection[0])) {
						connection.sendKeepAlive();
					}
					if(connections.isEmpty() && externalAddress != null) {
						// Send a message to external ip, to keep the port open:
						DatagramPacket packet = new DatagramPacket(new byte[0], 0);
						packet.setAddress(externalAddress);
						packet.setPort(externalPort);
						packet.setLength(0);
						send(packet);
					}
				}
			}
		} catch (Exception e) {
			Logger.crash(e);
		}
	}
}
