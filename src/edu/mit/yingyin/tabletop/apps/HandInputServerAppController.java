package edu.mit.yingyin.tabletop.apps;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.OpenNI.GeneralException;
import org.eclipse.jetty.websocket.WebSocket.Connection;

import com.google.gson.Gson;

import edu.mit.yingyin.tabletop.controllers.ProcessPacketController;
import edu.mit.yingyin.tabletop.models.HandTracker.FingerEvent;
import edu.mit.yingyin.tabletop.models.HandTrackingEngine;
import edu.mit.yingyin.tabletop.models.HandTrackingEngine.IHandEventListener;
import edu.mit.yingyin.websocket.IInputListener;
import edu.mit.yingyin.websocket.InputServer;

/**
 * Application controller for running a hand tracking server.
 * @author yingyin
 *
 */
public class HandInputServerAppController {
  private static final String DEFAULT_MAIN_DIR = ".";

  private static class HandTrackingThread extends Thread {
    private static final Logger logger = Logger.getLogger(
        HandTrackingThread.class.getName());
    private static final String OPENNI_CONFIG_FILE = "/config/config.xml";
    private static final String CALIB_FILE = 
        "/data/calibration/calibration.txt";
    private static final int DEFAULT_MAX_DEPTH = 1600;

    private HandTrackingEngine engine;
    private ProcessPacketController packetController; 

    /**
     * Constructor.
     *
     * @param mainDir the main directory of the configuration and data files.
     */
    public HandTrackingThread(String mainDir) {
      try {
        engine = new HandTrackingEngine(mainDir + OPENNI_CONFIG_FILE,
            mainDir + CALIB_FILE, DEFAULT_MAX_DEPTH);
        packetController = new ProcessPacketController(
            engine.depthWidth(), engine.depthHeight(), null);
        packetController.showDepthImage(false);
        packetController.showDiagnosticImage(false);
      } catch (GeneralException e) {
        logger.severe(e.getMessage());
        e.printStackTrace();
        System.exit(-1);
      }
    }

    @Override
    public void run() {
      while (!engine.isDone()) {
        engine.step();
        try {
          packetController.show(engine.packet());
        } catch (GeneralException ge) {
          logger.severe(ge.getMessage());
        }
      }
    }
    
    public void addListener(IHandEventListener l) {
      engine.addListener(l);
    }
    
    public void removeListener(IHandEventListener l) {
      engine.removeListener(l);
    }
  }
  
  private static class HandInputListener implements IInputListener, 
      IHandEventListener {

    private HandTrackingThread handTrackingThread;
    private Connection connection;
    private Gson gson = new Gson();
    
    public HandInputListener(HandTrackingThread handTrackingThread) {
      this.handTrackingThread = handTrackingThread;
    }
    
    @Override
    public void startListening(Connection c) {
      this.connection = c;
      handTrackingThread.addListener(this);
    }

    @Override
    public void stopListening() {
      handTrackingThread.removeListener(this);
    }

    @Override
    public void fingerPressed(List<FingerEvent> feList) {
      String json = gson.toJson(feList);
      try {
        connection.sendMessage(json);
      } catch (IOException e) {
        logger.severe(e.getMessage());
      }
    }
    
  }

  private static Logger logger = Logger.getLogger(
      HandInputServerAppController.class.getName());

  public static void main(String... args) {
    String mainDir = DEFAULT_MAIN_DIR;
    if (args.length == 1)
      mainDir = args[0];
    try {
      int port = 8081;
      HandTrackingThread handTrackingThread = new HandTrackingThread(mainDir);
      IInputListener inputListener = new HandInputListener(
          handTrackingThread);
      InputServer inputServer = new InputServer(port, inputListener);
      handTrackingThread.start();
      inputServer.start();
      inputServer.join();
      handTrackingThread.join();
    } catch (Exception e) {
      logger.severe(e.getMessage());
      System.exit(-1);
    }
  }
}
