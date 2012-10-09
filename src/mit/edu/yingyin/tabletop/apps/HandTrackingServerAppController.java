package mit.edu.yingyin.tabletop.apps;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.OpenNI.GeneralException;
import org.eclipse.jetty.websocket.WebSocket.Connection;

import com.google.gson.Gson;

import edu.mit.yingyin.tabletop.models.HandTracker.FingerEvent;
import edu.mit.yingyin.tabletop.models.HandTrackingEngine;
import edu.mit.yingyin.tabletop.models.HandTrackingEngine.IHandEventListener;
import edu.mit.yingyin.websocket.IInputListener;
import edu.mit.yingyin.websocket.InputServer;

/**
 * Application controller for running a handtracking server.
 * @author yingyin
 *
 */
public class HandTrackingServerAppController {
 
  private static class HandTrackingThread extends Thread {
    private static final String MAIN_DIR = 
        "/afs/csail/u/y/yingyin/research/kinect/";
    private static final String OPENNI_CONFIG_FILE = 
        MAIN_DIR + "config/config.xml";
    private static final String CALIB_FILE = MAIN_DIR + 
        "data/calibration/calibration.txt";
    private static final int DEFAULT_MAX_DEPTH = 1600;
    
    private HandTrackingEngine engine;
    
    public HandTrackingThread() {
      try {
        engine = new HandTrackingEngine(OPENNI_CONFIG_FILE, CALIB_FILE, 
            DEFAULT_MAX_DEPTH);
      } catch (GeneralException e) {
        logger.severe(e.getMessage());
        e.printStackTrace();
        System.exit(-1);
      }
    }

    @Override
    public void run() {
      while (true) {
        engine.step();
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
      HandTrackingServerAppController.class.getName());
  
  public static void main(String... args) {
    try {
      int port = 8081;
      HandTrackingThread handTrackingThread = new HandTrackingThread();
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
