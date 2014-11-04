import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
*
* @author tony
*/
public class Master implements Runnable{
    private static HashMap<String, Integer> pings = new HashMap<String, Integer>();
    //private static HashMap newCount = new HashMap<String, Integer>();

    public static void main(String[] args) throws IOException {
        (new Thread(new Master())).start(); //start the checker thread
        listen();
        
    }
    public static void listen() throws SocketException, IOException{
        DatagramChannel channel = DatagramChannel.open();
        channel.socket().bind(new InetSocketAddress(49999));

        while (true) {
            
            ByteBuffer buf = ByteBuffer.allocate(1000);
            channel.receive(buf);
            String read_in = new String(buf.array());
            receivePing(read_in);
            //System.out.println("Read message on Socket: "+ read_in);
            
        }
    }
    @Override
    public void run() {
        //Check the oldCount and compare it to the new count every 5 seconds;
        while(true){
            System.out.println("Interation of Checking loop");
            try {
                Thread.sleep(5000);
                //iterating over keys only
                checkServersStatus();
                
            } catch (InterruptedException ex) {
                Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }    
    }
    public static synchronized void receivePing(String port){
        System.out.println("Received Ping from " + port);
        pings.put(port, 1);
    }
    public static synchronized void checkServersStatus(){
        Iterator<Map.Entry<String, Integer>> entries = pings.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, Integer> entry = entries.next();
            if(entry.getValue() == 0){
                //We did not get a ping in the last 5 seconds
                System.out.println("Server running on port " + entry.getKey() + " is no longer alive.");
            }
            else{
                
                System.out.println("Server running on port " + entry.getKey() + " is alive.");
                pings.put(entry.getKey(), 0);//reset the ping count to zero
            }
        }
    }
}
/**
*
* @author tony
*/
/*public class Master {

    public static void main(String[] args) throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        channel.socket().bind(new InetSocketAddress(49999));

        while (true) {
            
            ByteBuffer buf = ByteBuffer.allocate(1000);
            channel.receive(buf);
            String ret = new String(buf.array());
            System.out.println("Read message on Socket: "+ ret);
        }
    }
}*/
