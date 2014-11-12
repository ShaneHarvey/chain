
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
*
* @author tony
*/
enum ServerStatus {

        HEAD, TAIL, MIDDLE, HEAD_TAIL
}
class BankChain{
    String bankName;
    ArrayList<Integer> chain;
    int requestToJoinPort = -1;
    public BankChain(String bn){
        bankName = bn;
        chain = new ArrayList();
    }
    public int getTail(){
        if(chain.size()> 0){
            return (chain.get(chain.size() - 1 ));
        }
        else{
            return -1;
        }
    }
    public int getHead(){
        return chain.get(0);
    }
    public String printBankChain(){
        String output = bankName +": ";
        for(int i = 0; i < chain.size(); i++){
            if(i == 0)
                output+= chain.get(i);
            else
                output+= "->" + chain.get(i);
        }
        System.out.println(output);
        return output;
        
    }
    
    public ServerStatus getServerStatus(int port){
        for(int i = 0; i < chain.size(); i++){
            if(chain.get(i) == port){
                if(chain.size() == 1){
                    return ServerStatus.HEAD_TAIL;
                }
                else if(i == 0){
                    return ServerStatus.HEAD;
                }
                else if(i == (chain.size() - 1)){
                    return ServerStatus.TAIL;
                }
                else{
                    return ServerStatus.MIDDLE;
                }
            }
        }
        return null;
    }
    /**
     * Function returns the predecessor port number of a server
     * @param port
     * @return returns predecessor port number or -1 if not found
     */
    public int getPredecessorPort(int port){
        for(int i =0; i < chain.size(); i++){
            if(chain.get(i) == port && i > 0){
                return chain.get(i-1);
            }
        }
        return -1;
    }
    /**
     * Function returns the successor port number of a server
     * @param port
     * @return returns predecessor port number or -1 if not found
     */
    public int getSuccessorPort(int port){
        for(int i = 0; i < chain.size(); i++){
            if(chain.get(i) == port && i < (chain.size() - 1)){
                return chain.get(i+1);
            }
        }
        return -1;
    }
    public void removeServer(int port){
        for(int i =0 ; i < chain.size(); i++){
            if(chain.get(i) == port){
                chain.remove(i);
            }
        }
    }
    public boolean isServPresent(int port){
        for(int i = 0; i < chain.size(); i++){
            if(chain.get(i) == port){
                return true;
            }
        }
        return false;
    }
}
public class Master implements Runnable{
    private static HashMap<String, Integer> pings = new HashMap<String, Integer>();
    private static int myPort;
    private static ArrayList<Integer> clients = new ArrayList<Integer>();
    private static HashMap<String, BankChain> bankChains = new HashMap<String, BankChain>();
    private static File logFile;
    public static String pound = "\n\n############################################################\n\n";
    private static DatagramChannel channel;
    private static boolean joinFlag = false;
    public static void main(String[] args) throws IOException {
        createFile();
        parseArgs(args);
        //System.out.println(myPort);
        (new Thread(new Master())).start(); //start the checker thread
        listen();
        
    }
    
    public static void parseMessage(String msg){
        String [] parts = msg.split("#");
        for(int i = 0; i < parts.length; i++){
            parts[i] = parts[i].replace("#", "");
        }
        if(parts[0].equals("PING")){
            receivePing(parts[1]);
        }
        else if( parts[0].equals("JOIN")){
            receiveJoin(msg);
        }
        else if( parts[0].equals("DONESENDING")){
            //receiveDoneSending(msg);
        }
        else if( parts[0].equals("NEWTAIL")){
            receiveNewTail(msg);
        }
    }
    public static void receiveNewTail(String msg){
        System.out.println("Done Sending. Now finally finish chain.");
        String [] parts = msg.split("#");
        for(int i = 0; i < parts.length; i++){
            parts[i] = parts[i].replace("#", "");
            parts[i] = parts[i].trim();
            System.out.println(i+ " : "+ parts[i]);
        }
        BankChain target = bankChains.get(parts[1]);
        target.chain.add(target.requestToJoinPort);
        target.requestToJoinPort = -1;
        String broadcast = "MASTER#" + target.bankName+ "#TAIL#"+ target.getTail();
        sendNewTail(broadcast);
    }
    public static void receiveDoneSending(String msg){
        System.out.println("Done Sending. Now finally finish chain.");
        String [] parts = msg.split("#");
        for(int i = 0; i < parts.length; i++){
            parts[i] = parts[i].replace("#", "");
            parts[i] = parts[i].trim();
            System.out.println(i+ " : "+ parts[i]);
        }
        BankChain target = bankChains.get(parts[1]);
        String newData="";
        if(target.chain.size()==1){
            newData = "MASTER#SERVSTATUS#HEAD";
        }
        else{
            newData = "MASTER#SERVSTATUS#MIDDLE";
        }
        ByteBuffer buf = ByteBuffer.allocate(100);
        buf.clear();
        buf.put(newData.getBytes());
        buf.flip();
        String newData2 = "MASTER#SERVSTATUS#TAIL";
        ByteBuffer buf2 = ByteBuffer.allocate(100);
        buf2.clear();
        buf2.put(newData2.getBytes());
        buf2.flip();
        try {
            int byteSent = channel.send(buf, new InetSocketAddress("localhost", target.getTail()));
            byteSent += channel.send(buf, new InetSocketAddress("localhost", target.requestToJoinPort));
        } catch (IOException ex) {
            Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        target.chain.add(target.requestToJoinPort);
        target.requestToJoinPort = -1;
        String broadcast = "MASTER#" + target.bankName+ "#TAIL#"+ target.getTail();
        sendNewTail(msg);//Broadcast Message to new tail
        
       
    }
    public static void receiveJoin(String msg){
        System.out.println("Join Received");
        String parts[] = msg.split("#");
        for(int i = 0; i < parts.length; i++){
            parts[i] = parts[i].replace("#", "");
            parts[i] = parts[i].trim();
        }
        BankChain target = bankChains.get(parts[1]);
        int tailPort = target.getTail();
        if(tailPort != -1){
            //joinFlag = true;
            target.requestToJoinPort = Integer.parseInt(parts[2]);
            String newData = "MASTER#NEWPRED#" + tailPort;
            ByteBuffer buf = ByteBuffer.allocate(100);
            buf.clear();
            buf.put(newData.getBytes());
            buf.flip();
            try {
                int byteSent = channel.send(buf, new InetSocketAddress("localhost", Integer.parseInt(parts[2])));
            } catch (IOException ex) {
                Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            String newData2 = "MASTER#NEWSERV#" + parts[2];
            ByteBuffer buf2 = ByteBuffer.allocate(100);
            buf2.clear();
            buf2.put(newData2.getBytes());
            buf2.flip();
            try {
                int byteSent = channel.send(buf2, new InetSocketAddress("localhost", tailPort));
            } catch (IOException ex) {
                Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            //Send old TAIL server that they are now MIDDLE or HEAD depends
            //Send new TAIL that they are tail
            
        }
    }
    /**
     * args[0] will be the port number that the master will run on
     * args[1] will be a list of client port numbers separated by "@"
     * args[2] will be a list of Banks and the elements in the chain
     * @param args the string array of command line arguments passed in
     */
    public static void parseArgs(String [] args){
        //Sets the port number that master should run on
        myPort = Integer.parseInt(args[0]);
        
        //get the client in the banking system
        String [] cli_split = args[1].split("@");
        for(int i = 0; i < cli_split.length; i++){
            cli_split[i] = cli_split[i].replace("@", "");
            clients.add(Integer.parseInt(cli_split[i]));
            System.out.println(clients.get(i));
        }
        
        String [] bsplit = args[2].split("@");
        for(int i = 0; i < bsplit.length; i++){
            bsplit[i] = bsplit[i].replace("@","");
            String [] binfo = bsplit[i].split("#");
            for(int j = 0; j< binfo.length; j++){
                binfo[j] = binfo[j].replace("#", "");
            }
            BankChain insertRec = new BankChain(binfo[0]);
            for(int j = 1; j < binfo.length; j++){
                insertRec.chain.add(Integer.parseInt(binfo[j]));
            }
            //insertRec.printBankChain();
            bankChains.put(binfo[0], insertRec);
        }
        writeToLog("Successfully parsed arguments passed in.");
        System.out.println("Successfully parse args");
        
    }
    public static void printBankChains(){
        Iterator<Map.Entry<String, BankChain>> entries = bankChains.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, BankChain> entry = entries.next();
            writeToLog(entry.getValue().printBankChain());
        }
    }
    public static void listen() throws SocketException, IOException{
        channel = DatagramChannel.open();
        channel.socket().bind(new InetSocketAddress(49999));

        while (true) {
            ByteBuffer buf = ByteBuffer.allocate(1000);
            channel.receive(buf);
            String read_in = new String(buf.array());
            parseMessage(read_in);
            //System.out.println("Read message on Socket: "+ read_in);   
        }
    }
    @Override
    public void run() {
        //Check the oldCount and compare it to the new count every 5 seconds;
        while(true){
            //System.out.println("Interation of Checking loop");
            try {
                Thread.sleep(5000);
                //iterating over keys only
                checkServersStatus();
                printBankChains();
                
            } catch (InterruptedException ex) {
                Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }    
    }
    public static synchronized void receivePing(String port){
        //System.out.println("Received Ping from " + port);
        pings.put(port, 1);
    }
    public static synchronized void checkServersStatus(){
        Iterator<Map.Entry<String, Integer>> entries = pings.entrySet().iterator();
        ArrayList<String> toRemove = new ArrayList<String>();
        while (entries.hasNext()) {
            Map.Entry<String, Integer> entry = entries.next();
            if(entry.getValue() == 0){//change to one for testing if you want
                try {
                    //We did not get a ping in the last 5 seconds
                    writeToLog("Server " + entry.getValue() + " is no longer alive.");
                    System.out.println("Server " + entry.getKey() + " is no longer alive.");
                    String [] parse = entry.getKey().split(":");
                    for(int i = 0; i< parse.length; i++){
                        parse[i] = parse[i].replace(":", "");
                        parse[i] = parse[i].trim();
                        System.out.println(parse[i]);
                    }
                    
                    

                    int pPort = bankChains.get(parse[0]).getPredecessorPort(Integer.parseInt(parse[1]));
                    int sPort = bankChains.get(parse[0]).getSuccessorPort(Integer.parseInt(parse[1]));
                    //bankChains.get(parse[0]).removeServer(Integer.parseInt(parse[1]));
                    
                    System.out.println("pPort = " + pPort + ", sPort = " + sPort);
                    ServerStatus loc = bankChains.get(parse[0]).getServerStatus(Integer.parseInt(parse[1]));
                    bankChains.get(parse[0]).removeServer(Integer.parseInt(parse[1]));
                    /*if(pPort == -1){
                        System.out.println("NEW HEAD");
                        //Send to clients
                        String msg = "MASTER#" + parse[0] + "#HEAD#"+bankChains.get(parse[0]).chain.get(0);
                        sendNewHead(msg);
                    }
                    if(sPort == -1){
                        System.out.println("NEW TAIL");
                        //Send to clients
                        int bankSize = bankChains.get(parse[0]).chain.size();
                        String msg = "MASTER#" + parse[0] + "#TAIL#"+ bankChains.get(parse[0]).chain.get(bankSize -1);
                        sendNewTail(msg);
                    }*/
                    if(sPort != -1 && pPort != -1){
                        //Middle server failed
                        System.out.println("INTERNAL FAILURE");
                        String newData = "MASTER#NEWPRED#"+ pPort;
                        ByteBuffer buf = ByteBuffer.allocate(48);
                        buf.clear();
                        buf.put(newData.getBytes());
                        buf.flip();
                        int bytesSent = channel.send(buf, new InetSocketAddress("localhost", sPort));
                        
                        String newData2 = "MASTER#NEWSUCC#"+ sPort;
                        ByteBuffer buf2 = ByteBuffer.allocate(48);
                        buf2.clear();
                        buf2.put(newData2.getBytes());
                        buf2.flip();
                        int bytesSent2 = channel.send(buf2, new InetSocketAddress("localhost", pPort));
                        //System.out.println(bytesSent + " bytes sent.");
                        
                    }
                    else if( sPort == -1 && pPort != -1){
                        //NEW TAIL function
                        System.out.println("TAIL");
                        String newData2 = "MASTER#SERVSTATUS#TAIL";
                        ByteBuffer buf2 = ByteBuffer.allocate(48);
                        buf2.clear();
                        buf2.put(newData2.getBytes());
                        buf2.flip();
                        int bytesSent2 = channel.send(buf2, new InetSocketAddress("localhost", bankChains.get(parse[0]).getTail()));
                    }
                    else if( (sPort != -1 && sPort != bankChains.get(parse[0]).getTail() )&& pPort == -1){
                        //NEW HEAD function 
                        System.out.println("HEAD");
                        String newData2 = "MASTER#SERVSTATUS#HEAD";
                        ByteBuffer buf2 = ByteBuffer.allocate(48);
                        buf2.clear();
                        buf2.put(newData2.getBytes());
                        buf2.flip();
                        int bytesSent2 = channel.send(buf2, new InetSocketAddress("localhost", bankChains.get(parse[0]).getHead()));
                    }
                    else if( (sPort == bankChains.get(parse[0]).getTail()) && pPort == -1){
                        //NEW HEAD_TAIL FUNCTION
                        System.out.println("HEAD_TAIL");
                        String newData2 = "MASTER#SERVSTATUS#HEAD_TAIL";
                        ByteBuffer buf2 = ByteBuffer.allocate(48);
                        buf2.clear();
                        buf2.put(newData2.getBytes());
                        buf2.flip();
                        int bytesSent2 = channel.send(buf2, new InetSocketAddress("localhost", bankChains.get(parse[0]).getHead()));
                    
                    }
                    else{
                        //Do nothing
                    }
                    toRemove.add(entry.getKey());
                    
                    boolean isHead = false;
                    boolean isTail = false;
                    if(loc == ServerStatus.HEAD){
                        System.out.println("Head Server failed of "+ parse[0]);
                        isHead = true;
                    }
                    else if(loc == ServerStatus.MIDDLE){
                        System.out.println("Middle Server failed of " + parse[0]);
                    }
                    else if(loc == ServerStatus.TAIL){
                        System.out.println("Tail Server failed of " + parse[0]);
                        isTail = true;
                    }
                    else{
                        System.out.println("HEAD_TAIL Server failed of " + parse[0]);
                    }
                    
                    
                    if(isHead){
                        String msg = "MASTER#" + parse[0] + "#HEAD#"+bankChains.get(parse[0]).getHead();
                        sendNewHead(msg);
                    }
                    if(isTail){
                        String msg = "MASTER#" + parse[0] + "#TAIL#"+bankChains.get(parse[0]).getTail();
                        sendNewHead(msg);
                    }
                    //pings.remove(entry.getKey());
                } catch (Exception ex) {
                    Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            }
            else{
                writeToLog("Server " + entry.getKey() + " is alive.");
                System.out.println("Server running on port " + entry.getKey() + " is alive.");
                pings.put(entry.getKey(), 0);//reset the ping count to zero
                String [] parse = entry.getKey().split(":");
                for(int i = 0; i< parse.length; i++){
                    parse[i] = parse[i].replace(":", "");
                    parse[i] = parse[i].trim();
                    System.out.println(parse[i]);
                }
                if(bankChains.get(parse[0]).isServPresent(Integer.parseInt(parse[1])) == false){
                    bankChains.get(parse[0]).chain.add(Integer.parseInt(parse[1]));
                }
            }
        }
        for(int i = 0; i < toRemove.size(); i++){
            pings.remove(toRemove.get(i));
        }
    }
    private static void sendNewHead(String toSend) {
        ByteBuffer buf = ByteBuffer.allocate(100);
        buf.clear();
        buf.put(toSend.getBytes());
        buf.flip();
        for(int i = 0; i  < clients.size(); i++){
            try {
                System.out.println("New Head sent to port " +clients.get(i));
                int bytesSent = channel.send(buf, new InetSocketAddress("localhost", clients.get(i)));
            } catch (IOException ex) {
                System.out.println("Failed to send client new Tail message on port "+ clients.get(i));
                Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("Message of new head sent to all clients");
    }

    private static void sendNewTail(String toSend) {
        System.out.println(toSend);
        ByteBuffer buf = ByteBuffer.allocate(1000);
        buf.clear();
        buf.put(toSend.getBytes());
        buf.flip();
        for(int i = 0; i  < clients.size(); i++){
            try {
                System.out.println("New Tail sent to port " +clients.get(i));
                int bytesSent = channel.send(buf, new InetSocketAddress("localhost", clients.get(i)));
            } catch (IOException ex) {
                System.out.println("Failed to send client new Tail message on port "+ clients.get(i));
                Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("Message of new tail sent to all clients");
    }
    private static void createFile() {
        try {
            String fname = "../logs/Master.log";
            logFile = new File(fname);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            Date today = new Date();
            BufferedWriter output = new BufferedWriter(new FileWriter(logFile, true));
            output.write(pound + today + pound);
            output.close();
        } catch (IOException e)  {
            System.out.println("Error creating log file. Exiting Program");
            System.exit(0);
        }
    }
    private static void writeToLog(String msg) {
        try {
            BufferedWriter output = new BufferedWriter(new FileWriter(logFile, true));
            output.write(new Date() + ": "+ msg + "\n");
            output.close();
        } catch (IOException ex) {
            //Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Error Writing to log file.");
        }
    }
}