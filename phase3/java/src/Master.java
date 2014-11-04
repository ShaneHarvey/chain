import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
/**
*
* @author tony
*/
public class Master {

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
}