
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientTest3{
    public static void main(String [] args) throws InterruptedException{
        int port = Integer.parseInt(args[0]);
        try {
            DatagramChannel dChannel = DatagramChannel.open();
            dChannel.connect(new InetSocketAddress("localhost", port));
            dChannel.configureBlocking(true);
            /*ByteBuffer buf = ByteBuffer.allocate(100);
            int bytesRead = dChannel.read(buf);
            //while(bytesRead != 0){
                String ret = new String(buf.array());
                System.out.println("bytes read: "+ bytesRead + " " + ret);
                //System.exit(0);
            //}*/
        int count = 0;
        while(true){
            System.out.println("Here");
            //if(count%100 == 0){
            String test = "MSG from client3...UDP..." + count;
            ByteBuffer buf2 = ByteBuffer.allocate(1000);
            buf2.clear();
            buf2.put(test.getBytes());
            buf2.flip();
            while(buf2.hasRemaining()){
                dChannel.write(buf2);
                System.out.println("Sent Data");
            }
            //}
            count++;
            Thread.sleep(5000);
            System.out.println("Here");
        }
        } catch (IOException ex) {
            Logger.getLogger(ClientTest3.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}