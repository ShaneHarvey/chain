
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientTest2{
    public static void main(String [] args) throws InterruptedException{
        int port = Integer.parseInt(args[0]);
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress("localhost", port));
            ByteBuffer buf = ByteBuffer.allocate(100);
            int bytesRead = socketChannel.read(buf);
            //while(bytesRead != 0){
                String ret = new String(buf.array());
                System.out.println("bytes read: "+ bytesRead + " " + ret);
                //System.exit(0);
            //}
        int count = 0;
        while(true){
            
            //if(count%100 == 0){
            String test = "MSG from client2..." + count;
            ByteBuffer buf2 = ByteBuffer.allocate(1000);
            buf2.clear();
            buf2.put(test.getBytes());
            buf2.flip();
            while(buf2.hasRemaining()){
                socketChannel.write(buf2);
            }
            //}
            count++;
            Thread.sleep(1000);
            System.out.println("Here");
        }
        } catch (IOException ex) {
            Logger.getLogger(ClientTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}