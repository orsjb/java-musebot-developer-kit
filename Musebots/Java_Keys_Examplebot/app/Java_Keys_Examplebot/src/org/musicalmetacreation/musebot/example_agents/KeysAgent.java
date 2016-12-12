package org.musicalmetacreation.musebot.example_agents;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.Pitch;
import net.beadsproject.beads.events.KillTrigger;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.WavePlayer;

import org.jaudiolibs.beads.AudioServerIO;
import org.musicalmetacreation.musebot.tools.AudioErrorHandler;
import org.musicalmetacreation.musebot.tools.MusebotAssistant;
import org.musicalmetacreation.musebot.tools.MusebotAssistant.Responder;

import de.sciss.net.OSCMessage;

public class KeysAgent extends Application implements Responder {

	static final AudioContext ac = new AudioContext(new AudioServerIO.Jack("Keys Agent"));
	static {
		ac.start();
	}
	
	MusebotAssistant musebot;
	Envelope masterGainEnvelope;
	Gain masterGain;
	Text text;

	public static void main(String[] args) {
		try {
			System.out.println("Waiting...");
			Thread.sleep(1000);			//here is some kind of sick fix to avoid thread lock
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Launching");
		launch(args);	
	}

	@Override
	public void incoming(OSCMessage msg) {
		//additional responses
	}

	@Override
	public void time(float tempo, int tickCount) {
		Platform.runLater(new Runnable() {
			public void run() {
				if(text != null) text.setText("Time: " + tickCount + " (Tempo=" + tempo + ")");
			}
		});
		if(tickCount % 16 == 0) {
			//play sound
			WavePlayer wp = new WavePlayer(ac, Pitch.forceFrequencyToScale((float)Math.random() * 5000f + 100f, Pitch.pentatonic), Buffer.SINE);
			Envelope e = new Envelope(ac, 0.1f);
			Gain g = new Gain(ac, 1, e);
			g.addInput(wp);
			masterGain.addInput(g);
			e.addSegment(0, 1000, new KillTrigger(g));
		}
	} 

	@Override
	public void gain(float level, float time) {
		masterGainEnvelope.clear();
		masterGainEnvelope.addSegment(level, time);
	}

	@Override
	public void kill() {
		masterGainEnvelope.clear();
		masterGainEnvelope.addSegment(0, 5000, new Bead() {
			public void messageReceived(Bead m) {
				System.exit(0);
			}
		});
		masterGainEnvelope.lock(true);
	}

	@Override
	public void start(Stage stage) throws Exception {
		//handle audio error
    	boolean audioError = AudioErrorHandler.handleAudioError(ac);
		//basics
		musebot = new MusebotAssistant();
		masterGainEnvelope = new Envelope(ac, 1);
		masterGain = new Gain(ac, 2, masterGainEnvelope);
		ac.out.addInput(masterGain);
		musebot.respondWith(this);
		System.out.println(".....");
		//GUI
		text = new Text("Starting");
		System.out.println("Keys agent is running.");
		StackPane root = new StackPane();
		root.getChildren().add(text);
		Scene scene = new Scene(root, 300, 250);
		stage.setTitle("Musebot Keys Agent!");
		stage.setScene(scene);
		stage.show();
		stage.centerOnScreen();
		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent arg0) {
				System.exit(0);	
			}
		});
		//run audio error alert
		if(audioError) {
			AudioErrorHandler.runAudioErrorAlert("KeysAgent Musebot Example");
		}
	}



}