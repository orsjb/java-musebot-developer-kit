package org.musicalmetacreation.musebot.tools; /**
 * Created by Ollie on 6/08/15.
 */

import de.sciss.net.OSCMessage;
import javafx.application.Application;
import javafx.stage.Stage;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.events.KillTrigger;
import net.beadsproject.beads.ugens.*;
import org.jaudiolibs.beads.AudioServerIO;

import java.net.URISyntaxException;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public abstract class OBMusebotBaseClass extends Application implements MusebotAssistant.Responder {

    private final static Logger log = Logger.getLogger(OBMusebotBaseClass.class.getName());
    static {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());
        log.addHandler(handler);
        handler.setLevel(Level.SEVERE);
        log.setLevel(Level.SEVERE);
    }

    public final String workingDir = System.getProperty("user.dir");
    public final String resDir = workingDir + "/resources";
    public final String audioDir = resDir + "/audio";

    protected MusebotAssistant assistant;

    public final Random rng = new Random();
//    public final AudioContext ac = new AudioContext(512);     //control
    public final AudioContext ac = new AudioContext(new AudioServerIO.Jack(), 512);     //control
    UGen in;                        //input ugen
    Envelope masterGainEnv;         //master gain value
    Glide tempoCtrl;                //bpm tempo
    Gain masterGain;                //plug everything into this

    @Override
    public void start(Stage primaryStage) throws URISyntaxException {
        log.info("Creating musebot");
        //core elements
        assistant = new MusebotAssistant();
        in = ac.getAudioInput(new int[]{1,2});
        //basic setup
        masterGainEnv = new Envelope(ac, 1);
        masterGain = new Gain(ac, 2, masterGainEnv);
        ac.out.addInput(masterGain);
        tempoCtrl = new Glide(ac, 2, 120);
        //good to go
        ac.start();
        assistant.respondWith(this);
        //additional init stuff by subclass
        subclassStart();
    }

    protected abstract void subclassStart();

    public void sound(UGen sound) {
        masterGain.addInput(sound);
    }

    private void feedbackTest() {
        //simple input test
        TapIn tin = new TapIn(ac, 5000);
        TapOut tout = new TapOut(ac, tin, 1000);
        ac.out.addInput(tout);
        tin.addInput(in);
    }

    private void bleep() {
        WavePlayer wp = new WavePlayer(ac, 500, Buffer.SINE);
        Envelope e = new Envelope(ac, 1);
        Gain g = new Gain(ac, 2, e);
        g.addInput(wp);
        sound(g);
        e.addSegment(0, 100, new KillTrigger(g));

    }

    @Override
    /**
     * Highly! Recommended to override this. Listen to the other people.
     */
    public void incoming(OSCMessage msg) {
        log.info(msg.getName());
    }

    @Override
    /**
     * Recommended to override this again to do your own time change and click stuff.
     */
    public void time(float tempo, int tickCount) {
        //respond to incoming time - try to match the beat
        log.info("Time message: " + tickCount +" (" + tempo + " bpm)");
        tempoCtrl.setValue(tempo);
    }

    @Override
    public void gain(float level, float time) {
        masterGainEnv.addSegment(level, time);
    }

    @Override
    public void kill() {
        masterGainEnv.lock(false);
        masterGainEnv.addSegment(0, 4000, new Bead() {
            @Override
            protected void messageReceived(Bead message) {
                super.messageReceived(message);
                System.exit(0);
            }
        });
        masterGainEnv.lock(true);
    }

}
