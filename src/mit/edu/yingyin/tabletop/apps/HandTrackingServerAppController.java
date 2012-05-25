package mit.edu.yingyin.tabletop.apps;

import java.util.List;
import java.util.logging.Logger;

import org.OpenNI.GeneralException;
import org.eclipse.jetty.websocket.WebSocket.Connection;

import edu.mit.yingyin.tabletop.models.HandTracker.FingerEvent;
import edu.mit.yingyin.tabletop.models.HandTrackingEngine;
import edu.mit.yingyin.tabletop.models.HandTrackingEngine.IHandEventListener;
import edu.mit.yingyin.websocket.IInputListener;
import edu.mit.yingyin.websocket.InputServer;

public class HandTrackingServerAppController {
 
  private static class HandTrackingThread extends Thread {
    private static final String MAIN_DIR = 
        "/afs/csail/u/y/yingyin/research/kinect/";
    private static final String OPENNI_CONFIG_FILE = 
        MAIN_DIR + "config/config.xml";
    private static final String CALIB_FILE = MAIN_DIR + "data/calibration.txt";
    
    private HandTrackingEngine engine;
    
    public HandTrackingThread() {
      try {
        engine = new HandTrackingEngine(OPENNI_CONFIG_FILE, CALIB_FILE);
      } catch (GeneralException e) {
        logger.severe(e.getMessage());
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
      // TODO Auto-generated method stub
      
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
    }
  }
}
