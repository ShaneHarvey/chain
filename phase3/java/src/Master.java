/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
    public BankChain(String bn){
        bankName = bn;
        chain = new ArrayList();
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
}
public class Master implements Runnable{
    private static HashMap<String, Integer> pings = new HashMap<String, Integer>();
    private static int myPort;
    private static ArrayList<Integer> clients = new ArrayList<Integer>();
    private static HashMap<String, BankChain> bankChains = new HashMap<String, BankChain>();
    private static File logFile;
    public static String pound = "\n\n############################################################\n\n";
    private static DatagramChannel channel;
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
            System.out.println("Interation of Checking loop");
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
        System.out.println("Received Ping from " + port);
        pings.put(port, 1);
    }
    public static synchronized void checkServersStatus(){
        Iterator<Map.Entry<String, Integer>> entries = pings.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, Integer> entry = entries.next();
            if(entry.getValue() == 1){
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
                    String newData = "MASTER#NEWPRED#50004";
                    ByteBuffer buf = ByteBuffer.allocate(48);
                    buf.clear();
                    buf.put(newData.getBytes());
                    buf.flip();
                    //int bytesSent = channel.send(buf, new InetSocketAddress("localhost", Integer.parseInt(parse[1])));
                    //System.out.println(bytesSent + " bytes sent.");
                    System.out.println("pPort = " + pPort + ", sPort = " + sPort);
                    if(pPort == -1){
                        System.out.println("NEW HEAD");
                        //Send to clients
                        String msg = "MASTER#" + parse[0] + "#HEAD#"+bankChains.get(parse[0]).chain.get(0);
                        sendNewHead(msg);
                    }
                    if(sPort == -1){
                        System.out.println("NEW TAIL");
                        //Send to clients
                        int bankSize = bankChains.get(parse[0]).chain.size();
                        String msg = "MASTER#" + parse[0] + "#HEAD#"+ bankChains.get(parse[0]).chain.get(bankSize -1);
                        sendNewTail(msg);
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
            }
        }
    }
    private static void sendNewHead(String toSend) {
        ByteBuffer buf = ByteBuffer.allocate(48);
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
        ByteBuffer buf = ByteBuffer.allocate(48);
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