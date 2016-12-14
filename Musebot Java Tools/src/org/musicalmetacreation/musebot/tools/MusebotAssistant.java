package org.musicalmetacreation.musebot.tools;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Scanner;

import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import de.sciss.net.OSCServer;

public class MusebotAssistant implements OSCListener {
	
	OSCServer server;
	InetSocketAddress controllerAddress;
	boolean debugMode;

    String clientID;
	long lastHeartbeat = System.currentTimeMillis();
	boolean alive = true;
    boolean printMessage = false;

	public interface Responder {
		public void incoming(OSCMessage msg);
		public void time(float tempo, int tickCount);
		public void gain(float level, float time);
		public void kill();
	}
	
	Responder theResponder;
	
	public MusebotAssistant() {
		this(false);
	}

	public MusebotAssistant(boolean debugMode) {
		this.debugMode = debugMode;
		//read in data from config file
		try {
			Scanner scanner = new Scanner(new File("config.txt"));
			String conductorHostname = "";
			int conductorListenPort = 0;
			int myListenPort = 7474;
			while(scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] parts = line.split("[ ]");
				if(parts[0].equals("mc_hostname")) {
					conductorHostname = parts[1];
				} else if(parts[0].equals("mc_listen_port")) {
					conductorListenPort = Integer.parseInt(parts[1]);
				} else if(parts[0].equals("my_listen_port")) {
					if(!debugMode) myListenPort = Integer.parseInt(parts[1]);
				} else if(parts[0].equals("id")) {
					clientID = parts[1].trim();
				}
			}
			scanner.close();
			if(clientID == null) {
				throw new IOException("No id parameter in config file.");
			} 
			//set up server
			server = OSCServer.newUsing(OSCServer.UDP, myListenPort);
			server.addOSCListener(this);;
			server.start();
			controllerAddress = new InetSocketAddress(conductorHostname, conductorListenPort);
			System.out.println("Musebot config: conductor=" + conductorHostname + "," + conductorListenPort);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Since you don't have the config file you're not allowed to continue.");
			System.err.println("It's for your own good. Goodbye!");
			System.exit(0);	
		}
		//now set up heartbeat sending
		new Thread() {
			public void run() {
				while(alive) {
					send("/agent/alive", new Object[] {clientID});
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
		//now set up heartbeat listening (suicide if the mc disappears after 30 seconds)
				new Thread() {
					public void run() {
						while(alive) {
                            long currentTime = System.currentTimeMillis();
                            long diff = 0;
                            synchronized (MusebotAssistant.this) {
                                diff = currentTime - lastHeartbeat;
                            }
							if(diff > 30000L) {
								System.out.println("Haven't heard from the MC so the kill message is being sent. (Time=" + currentTime + ", lastHeartbeat=" + lastHeartbeat + ", diff=" + diff + ").");
								alive = false;
								theResponder.kill();
							}
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
					}
				}.start();
	}
	
	public void send(String msgName, Object... args) {
		try {
			server.send(new OSCMessage(msgName, args), controllerAddress);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void respondWith(Responder responder) {
		theResponder = responder;
	}

	@Override
	public void messageReceived(OSCMessage msg, SocketAddress source, long timetag) {
        if(printMessage && !msg.getName().equals("/mc/time")) {
            StringBuffer fullMessage = new StringBuffer(msg.getName() + ":");
            for(int i = 0; i < msg.getArgCount(); i++) {
                fullMessage.append(" " + msg.getArg(i).toString());
            }
            System.out.println(fullMessage);
        }
		if(msg.getName().equals("/agent/off")) {
			if(theResponder != null) theResponder.kill();
		} else if(msg.getName().equals("/agent/gain")) {
			float gain = (Float)msg.getArg(0);
			float time = 10;
			if(msg.getArgCount() == 2) {
				time = (Float)msg.getArg(1);
			}
			if(theResponder != null) theResponder.gain(gain, time);
		} else if(msg.getName().equals("/mc/time")) {
			float tempo = (Float)msg.getArg(0);
			int tickCount = (Integer)msg.getArg(1);
			if(theResponder != null) theResponder.time(tempo, tickCount);
		} else {
			if(msg.getName().equals("/mc/agentList")) {
				//heartbeat from agent
                synchronized (MusebotAssistant.this) {
                    lastHeartbeat = System.currentTimeMillis();
                }
                if(printMessage) System.out.println("Hearbeat received. Time = " + lastHeartbeat);
			}
			if(theResponder != null) theResponder.incoming(msg);
		}
	}

    public String getClientID() {
        return clientID;
    }
	
	public void setPrintMessage(boolean b) {
        printMessage = b;
    }
	
}
