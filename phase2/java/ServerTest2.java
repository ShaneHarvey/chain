
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class ServerTest2{
	public static void main(String [] args) throws IOException{
		int port = Integer.parseInt(args[0]);
                int portUDP = Integer.parseInt(args[1]);
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		ServerSocket serverSocket = serverChannel.socket();
		Selector selector = Selector.open();
		serverSocket.bind(new InetSocketAddress(port));
		serverChannel.configureBlocking(false);
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		DatagramChannel datagramChannel = DatagramChannel.open();
                datagramChannel.configureBlocking(false);
                datagramChannel.socket().bind(new InetSocketAddress(portUDP));
                datagramChannel.register(selector, SelectionKey.OP_READ);
                
                while(true){
			int n = selector.select();
			if(n == 0){
				System.out.print(".");
				continue;
			}

			Iterator it = selector.selectedKeys().iterator();
			while(it.hasNext()){
				SelectionKey key = (SelectionKey) it.next();
				if(key.isAcceptable()){
					ServerSocketChannel server = (ServerSocketChannel) key.channel();
					SocketChannel channel = server.accept();
					//registerChannel(selector, channel, SelectionKey.OP_READ);
					
                                        if(channel != null){
                                            channel.configureBlocking(false);
                                            channel.register(selector, SelectionKey.OP_READ);
                                            System.out.println("ACCEPTABLE");
                                            String test = "Server accepted and created a socket channel...";
                                            ByteBuffer buf = ByteBuffer.allocate(1000);
                                            buf.clear();
                                            buf.put(test.getBytes());
                                            buf.flip();
                                            while(buf.hasRemaining()){
                                                channel.write(buf);
                                            }
                                            System.out.println("Not stuck here");
                                        }
                                        it.remove();
				}
				if(key.isReadable()){
					System.out.println("READABLE");
                                        SocketChannel channel = null;
                                        DatagramChannel ch = null;// = (DatagramChannel) key.channel();
                                        try{
                                            channel = (SocketChannel) key.channel();
                                        }catch(Exception e){
                                            //System.out.println("NOT a SocketChannel");
                                        }
                                        try{
                                            ch = (DatagramChannel) key.channel();
                                            ch.configureBlocking(true);
                                        }catch(Exception e1){
                                            //e1.printStackTrace();
                                            //System.out.println("NOT a DatagramChannel");
                                        }
                                        //channel.socket().
                                        //System.out.println(channel.isBlocking());
                                        //channel.
                                        if(channel != null){
                                            System.out.println("In SocketChannel");
                                            ByteBuffer buf = ByteBuffer.allocate(100);
                                            int bytesRead = channel.read(buf);
                                            if(bytesRead == -1){
                                                System.out.println("ConnectionClosed");
                                                channel.close();
                                            }
                                            String ret = new String(buf.array());
                                            System.out.println("bytes read: "+ bytesRead + " " + ret);
                                            buf.clear();
                                            it.remove();
                                            continue;
                                            //channel.configureBlocking()
                                        }
                                        if(ch != null){
                                            System.out.println("In Datagram");
                                            ByteBuffer buf = ByteBuffer.allocate(100);
                                            buf.clear();
                                            ch.receive(buf);
                                            //DatagramSocket temp = ch.socket();
                                            //DatagramPacket a = null;
                                            //temp.receive(a);
                                            
                                            /*buf = a.getData();
                                            int bytesRead = temp.receive(a);//.read(buf);
                                            if(bytesRead == -1){
                                                System.out.println("ConnectionClosed");
                                                ch.close();
                                            }*/
                                            String ret = new String(buf.array());
                                            System.out.println("Data: " + ret);
                                            buf.clear();
                                            it.remove();
                                            continue;
                                        }
                                        /*else{
                                            System.out.println("DatagramChannel");
                                            DatagramChannel ch = (DatagramChannel) key.channel();
                                            if(ch != null){
                                                ByteBuffer buf = ByteBuffer.allocate(100);
                                                int bytesRead = ch.read(buf);
                                                String ret = new String(buf.array());
                                                System.out.println("bytes read form datagram: "+ bytesRead+ " "+ ret);
                                                buf.clear();
                                                it.remove();
                                                continue;
                                            }
                                            else{
                                                it.remove();
                                                
                                            }
                                        }
                                        */
                                        it.remove();
                                        continue;
                                        //channel.close();
				}
                                //it.remove();
                                System.out.println("Not stuck here2");
                                
			}
		}
	}
}