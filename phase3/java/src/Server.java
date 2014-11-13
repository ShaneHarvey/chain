

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import static java.net.SocketOptions.SO_REUSEADDR;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
class ReqObject{
    static enum Outcome { Processed, InconsistentWithHistory, InsufficientFunds }
    String rid;
    String accountNum;
    String req;
    String reply;
    Double amount;
    Outcome outcome;
    Double balance;
    public ReqObject(String r, String an, String rq, Double a, Outcome o, Double b, String rep){
        rid = r;
        accountNum = an;
        req = rq;
        amount = a;
        outcome = o;
        balance = b;
        reply = rep;
       
    }
}
class WithdrawalResponse{
    Double balance;
    boolean flag = false;
    public WithdrawalResponse(Double b, boolean f){
        balance = b;
        flag = f;
    }
}
class SentPair{
   Integer seq;
   String req;
   public SentPair(Integer s, String r){
       seq = s;
       req = r;
   }
}
public class Server implements Runnable{

    enum ServerEnum {

        ME, PREDECESSOR, SUCCESSOR, MASTER
    }

    enum ServerStatus {

        HEAD, TAIL, MIDDLE, HEAD_TAIL
    }

    enum Outcome {

        Processed, InconsistentWithHistory, InsufficientFunds
    }

    enum MessagesEnum {
        ACCEPT, SOCKCHANNELREAD, DATACHANNELREAD, REPLYTOCLI, FWDTOSUCCESSOR
    }
    //enum Outcome { Processed, InconsistentWithHistory, InsufficientFunds }
    public static final int MAXBUFFER = 1000;
    public static final String W = "withdrawal";
    public static final String D = "deposit";
    public static final String B = "balance";
    private static HashMap<String, Double> bank;// = new HashMap()<int, Double>;
    private static Selector selector;
    private static String myIP;
    private static String pServerIP;
    private static String sServerIP;
    private static String masterIP;
    private static int myPort;
    private static int pServerPort;
    private static int sServerPort;
    private static int masterPort;
    private static ServerStatus myPosition;
    private static File logFile;
    private static SocketChannel predecessorSock;
    private static SocketChannel successorSock;
    private static SocketChannel masterSock;
    private static ServerSocketChannel mySock;
    private static DatagramChannel datagramChannel;
    public static String pound = "\n\n############################################################\n\n";
    public static HashMap<String, ReqObject> history;
    public static HashMap<Integer, String> sent;
    public static ArrayList<String> sentExt = new ArrayList<String> (); 
    private static int start_delay;
    private static int lifetime;
    private static int receive;
    private static int send;
    private static String bankName;
    
    private static int messagesS = 0;
    private static int messagesR = 0;
    private static boolean isExtending = false;
    /**
     * This method Parses the command line arguments and sets up the variables
     * that store the predecessor, successor, and master server
     *
     * @param arg 
     * arg[0] will contain HEAD, TAIL, MIDDLE, HEAD_TAIL 
     * arg[1] will contain my IP address and Port Number(Listening Port) 
     * arg[2] will contain the IP address and PORT of the Predecessor Server EX(127.0.0.1:50000). If its the HEAD this will be zero 
     * arg[3] will contain the IP address and PORT of the Successor Server EX(127.0.0.1:50000). If its the TAIL this will be zero 
     * arg[4] will contain the IP address and PORT Number for the Master
     * arg[5] will contain start_delay
     * arg[6] will contain lifetime
     * arg[7] will contain receive
     * arg[8] will contain  send
     * arg[9] will contain the bank name
     */
    private static void parseArgs(String[] arg) {
        bankName = arg[9];
        recordIPAndPort(arg[1], ServerEnum.ME);
        recordIPAndPort(arg[2], ServerEnum.PREDECESSOR);
        recordIPAndPort(arg[3], ServerEnum.SUCCESSOR);
        recordIPAndPort(arg[4], ServerEnum.MASTER);
        start_delay = Integer.parseInt(arg[5]);
        lifetime = Integer.parseInt(arg[6]);
        receive = Integer.parseInt(arg[7]);
        send = Integer.parseInt(arg[8]);
        if (arg[0].equals("HEAD")) {
            myPosition = ServerStatus.HEAD;
        } else if (arg[0].equals("TAIL")) {
            myPosition = ServerStatus.TAIL;
        } else if (arg[0].equals("MIDDLE")) {
            myPosition = ServerStatus.MIDDLE;
        } else if (arg[0].equals("HEAD_TAIL")){
            myPosition = ServerStatus.HEAD_TAIL;
        }else {
            System.out.println("Error in Server position definition");
            System.exit(0);
        }
        System.out.println(myPosition.toString());
    }

    /**
     * Parses the IP and Port number string and populates the appropriate
     * variables
     *
     * @param ip_port String that is IP and Port number comma separated
     * @param who Indicates me, predecessor, successor, or master
     */
    private static void recordIPAndPort(String ip_port, ServerEnum who) {
        String[] parts = ip_port.split(":");
        parts[0] = parts[0].replace(":", "");
        parts[1] = parts[1].replace(":", "");
        int port = Integer.parseInt(parts[1]);
        switch (who) {
            case ME:
                myIP = parts[0];
                myPort = port;
                break;
            case PREDECESSOR:
                pServerIP = parts[0];
                pServerPort = port;
                break;
            case SUCCESSOR:
                sServerIP = parts[0];
                sServerPort = port;
                break;
            case MASTER:
                masterIP = parts[0];
                masterPort = port;
                break;
            default:
                System.out.println("NOT A VAILD ENUM");
                break;
        }
    }
    public static void parseMasterRequest(String req) {
        System.out.println("In Master parsing method");
        String [] parts = req.split("#");
        for(int i = 0; i < parts.length ; i++){
            parts[i] = parts[i].replace("#", "");
            parts[i] = parts[i].trim();
        }
        if(parts[1].equals("JOINFAIL")){
            writeToLog(new Date()+ ": Server that wanted to join chain failed. Clearing sent. Grasefully aborting chain extension.");
            System.out.println("Server that wanted to join chain failed.");
            isExtending = false;
            sent.clear();
        }
        else if(parts[1].equals("NEWSUCC")){
            System.out.println("New successor");
            sServerPort = Integer.parseInt(parts[2]);
            try {
                //successorSock.close();
                successorSock = SocketChannel.open();
                successorSock.connect(new InetSocketAddress(sServerIP,sServerPort ));
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Successfully relinked to successor");
            writeToLog( new Date() + ": New successor message received. Successfull relinked to port " + sServerPort);
        }
        else if(parts[1].equals("NEWPRED")){
            System.out.println("New predecessor");
            pServerPort = Integer.parseInt(parts[2]);
            try {
                //predecessorSock.close();
                predecessorSock = SocketChannel.open();
                predecessorSock.connect(new InetSocketAddress(pServerIP,pServerPort ));
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
            System.out.println("Successfully relinked to predecessor");
            writeToLog( new Date() + ": New predecessor message received. Successfull relinked to port " + pServerPort);
        }
        else if(parts[1].equals("SERVSTATUS")){
            //writeToLog();
            System.out.println("New server position");
            
            myPosition = ServerStatus.valueOf(parts[2]);
            writeToLog(new Date() +": Master sent me new position: "+ myPosition.toString());
            ServerStatus s = ServerStatus.valueOf(parts[2]);
            if(s == ServerStatus.TAIL){
                //clear sent set.
                sent.clear();
                writeToLog(new Date() + ": Became new tail. Clearing sent set.");
            }
            
            System.out.println(myPosition.toString());
            return;
        }
        else if(parts[1].equals("NEWSERV")){
            System.out.println("New server wants to join");
            writeToLog(new Date() + ": New server wants to join the chain. Port :" +parts[2] +" messagesR: " +messagesR);
            if(messagesR >= receive){
                writeToLog(new Date() + ": Received maximum numeber messages. Exiting...");
                System.exit(0);
            }
            isExtending = true;
            sServerPort = Integer.parseInt(parts[2]);
            try {
                successorSock = SocketChannel.open();
                //successorSock.configureBlocking(false);
                successorSock.connect(new InetSocketAddress("localhost",sServerPort ));
            } catch (IOException ex) {
                writeToLog("Exception connection to new server. port" + sServerPort );
                return;
                //Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Successfully relinked to successor");
            //writeToLog(new Date() + ": New server wants to join the chain. Port :" +parts[2]);
            //try{
                //sendBank();
                writeToLog(new Date() + ": Sent new server the bank state.");
                //sendHistory();
                writeToLog(new Date() + ": Sent new server the history of requests");
                //sendSent();
                writeToLog(new Date() + ": Sent new server the sent set");
           // }catch(IOException e){
                //writeToLog(new Date() + ": New server socket error.");
            //}
            ServerStatus oldStatus = myPosition;
            if(myPosition == ServerStatus.HEAD_TAIL){
                myPosition = ServerStatus.HEAD;
            }
            else{
                myPosition = ServerStatus.MIDDLE;
            }
            isExtending = false;
            sentExt.clear();
            String newData = "DONESENDING#"+bankName;
            ByteBuffer buf = ByteBuffer.allocate(1000);
            buf.clear();
            buf.put(newData.getBytes());
            buf.flip();
            //int bytesSent = successorSock.write(buf);
            //successorSock.close();
            
            try{
                //successorSock = SocketChannel.open();
                //successorSock.connect(new InetSocketAddress("localhost", sServerPort));
                System.out.println("Connection to successor established");
                messagesS++;
                writeToLog(new Date() + ": Before sending Done to new server.");
                while (buf.hasRemaining()) {
                    successorSock.write(buf);
                }
                System.out.println("Done adding new chain.");
                writeToLog(new Date() + ": Done adding new server. I am no longer the Tail.");
                //int bytes2 = datagramChannel.send(buf, new InetSocketAddress("localhost", Integer.parseInt(parts[2])));
            }catch(IOException e){
                System.out.println("Error connection to successor sock");
                writeToLog(new Date() + ": Error connection to new joining server's socket");
                myPosition = oldStatus;
            }
            
            /*try {
            int byteSent = datagramChannel.send(buf, new InetSocketAddress("localhost", masterPort));
            } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }*/
            
        }
        else{
            
        }
        return;
    }
    public static void sendBank(){
        Iterator<Map.Entry<String, Double>> entries = bank.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, Double> entry = entries.next();
            String newData = "SETUP#"+ entry.getKey() + "#" + entry.getValue();
            ByteBuffer buf = ByteBuffer.allocate(1000);
            buf.clear();
            buf.put(newData.getBytes());
            buf.flip();
            //if (successorSock == null) {
                //successorSock.close();
                //successorSock = SocketChannel.open();
                //successorSock.connect(new InetSocketAddress("localhost", sServerPort));
                System.out.println("Connection to successor established");
            //}
            messagesS++;
            while (buf.hasRemaining()) {
                try {
                    successorSock.write(buf);
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            };
            writeToLog(new Date() + ": Bank Account sent " + entry.getKey());
        }
    }
    public static void sendSent(){
       if(sentExt.isEmpty()){
           System.out.println("sendExt is empty. Nothing to forward to client");
           writeToLog(new Date() + ": sent set with extending change is empty. Noting to forward to new tail.");
           return;
       }
       for(int i = 0; i < sentExt.size(); i++){
          String req = sentExt.get(i);
          req = req.replaceAll("#", "|");
          String toSend = "SETUP3#"+ req;
          System.out.println("sendSent: "+ toSend);
          ByteBuffer buf = ByteBuffer.allocate(1000);
            buf.clear();
            buf.put(toSend.getBytes());
            buf.flip();
            
            //successorSock.close();
            //successorSock = SocketChannel.open();
            //successorSock.connect(new InetSocketAddress("localhost", sServerPort));
            System.out.println("Connection to successor established");

            messagesS++;
            while (buf.hasRemaining()) {
              try {
                  successorSock.write(buf);
              } catch (IOException ex) {
                  Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
              }
            }
            writeToLog(new Date() + ": sent while extending sent.");
       }
    }
    public static void sendHistory(){
       writeToLog(new Date() + ": Sending history.");
       Iterator<Map.Entry<String, ReqObject>> entries = history.entrySet().iterator();
       while(entries.hasNext()){
            Map.Entry<String, ReqObject> entry = entries.next();
            writeToLog(new Date() + ": Sending history entry.");
            ReqObject target = entry.getValue();
            String request = target.req;
            request = request.replaceAll("#", "|");
            String reply = target.reply;
            reply = reply.replaceAll("#", "|");
            String toFwd = "SETUP2#" + target.rid + "#"+ target.accountNum+"#"+target.amount+ "#"+target.outcome.toString()+"#"+target.balance + "#" + request +"#"+ reply; 
            System.out.println("History: " + toFwd);
            ByteBuffer buf = ByteBuffer.allocate(10000);
            buf.clear();
            buf.put(toFwd.getBytes());
            buf.flip();
            
            //successorSock.close();
                //successorSock = SocketChannel.open();
                //successorSock.connect(new InetSocketAddress("localhost", sServerPort));
                
                System.out.println("Connection to successor established");

                messagesS++;
                while (buf.hasRemaining()) {
                    try {
                        successorSock.write(buf);
                    } catch (IOException ex) {
                        writeToLog(new Date() +": Failed to send history entry. Write failed.");
                        //Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                writeToLog(new Date() + ": Bank History entry sent");
       }
    }
    public static void processDoneSending(){
        String newData = "NEWTAIL#"+bankName;
        ByteBuffer buf = ByteBuffer.allocate(100);
        buf.clear();
        buf.put(newData.getBytes());
        buf.flip();
        try {
            int byteSent = datagramChannel.send(buf, new InetSocketAddress("localhost", masterPort));
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        writeToLog(new Date() + ": New tail message sent to Master. I am the new tail now.");
    }
    public static void processAck(int ACK){
        if(myPosition == ServerStatus.TAIL || myPosition == ServerStatus.HEAD_TAIL){
            return;
        }
        System.out.println(sent.size());
        Iterator<Map.Entry<Integer, String>> entries = sent.entrySet().iterator();
        ArrayList<Integer> toRemove = new ArrayList<Integer>();
        while (entries.hasNext()) {
            Map.Entry<Integer, String> entry = entries.next();
            if(entry.getKey()<= ACK){
                System.out.println("Removed ACK " + entry.getKey());
                writeToLog(new Date() + ": Received ACK: " + ACK + " removing SEQ from sent " + entry.getKey());
                toRemove.add(entry.getKey());
                //sent.remove(entry.getKey());
            }
            //writeToLog(entry.getValue().printBankChain());
        }
        for(int i = 0; i< toRemove.size(); i++){
            sent.remove(toRemove.get(i));
        }
        System.out.println(sent.size());
        if(myPosition!= ServerStatus.HEAD && myPosition!= ServerStatus.HEAD_TAIL){
            try {
                String toSend = "ACK#"+ACK;
                ByteBuffer buf = ByteBuffer.allocate(1000);
                buf.clear();
                buf.put(toSend.getBytes());
                buf.flip();
                //if (predecessorSock == null) {
                    predecessorSock = SocketChannel.open();
                    predecessorSock.connect(new InetSocketAddress("localhost", pServerPort));
                    System.out.println("Connection to successor established");
                //}
                while (buf.hasRemaining()) {
                    predecessorSock.write(buf);
                }
                 writeToLog(new Date() + ": Sending ACK: " + ACK + " to predecessor");
                //predecessorSock.close();
                //predecessorSock.write(buf);
            } catch (IOException ex) {
                System.out.println("Acking to predecessor failed.");
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    public static void parseSetup2(String msg){
        //writeToLog("hey1" + msg);
        String [] parts = msg.split("#");
        for(int i = 0; i < parts.length; i++){
            parts[i] = parts[i].replace("#", "");
            parts[i] = parts[i].trim();
        }
        if(parts.length==8){
            parts[6] = parts[6].replaceAll("|", "#");
            parts[7] = parts[7].replaceAll("|", "#");
        }
        else{
            return;
        }
        
        //writeToLog("hey");
        ReqObject.Outcome a = ReqObject.Outcome.valueOf(parts[4]);
        //public ReqObject(String r, String an, String rq, Double a, Outcome o, Double b, String rep)
        ReqObject toAdd = new ReqObject(parts[1], parts[2], parts[6], Double.parseDouble(parts[3]), a, Double.parseDouble(parts[5]), parts[7]);
        //writeToLog("hey2");
        history.put(parts[1], toAdd);
        System.out.println("Processes new History element");
        //writeToLog(new Date() + ": History entry processed");
    }
    public static void parseSetup3(String msg) throws IOException{
        String [] parts = msg.split("#");
        for(int i = 0; i < parts.length; i++){
            parts[i] = parts[i].replace("#", "");
            parts[i] = parts[i].trim();
        }
        parts[1] = parts[1].replace("|", "#");
        parseRequest(parts[1], MessagesEnum.FWDTOSUCCESSOR );
    }
    /**
     * This function will parse the message recieved from the Sockets
     *
     * @param req This is the string representing the request, look below for
     * the format of the messages
     * deposit#clientIPPORT%bank_name%accountnum%seq#amount
     * withdrawal#clientIPPORT%bank_name%accountnum%seq#amount
     * balance#clientIPPORT%bank_name%accountnum%seq
     */
    private static int parseRequest(String req, MessagesEnum m) throws IOException {
        //writeToLog("request:" + req);
        System.out.println("request:" + req);
        String response = "reply#";
        String[] parts = req.split("#");
        if(parts.length<=1){
            //System.out.println("ASS");
            return -1;
        }
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].replace("#", "");
            parts[i] = parts[i].trim();
        }
        if(parts[0].equals(B) && m == MessagesEnum.SOCKCHANNELREAD){
            System.out.println("HACK");
            return 0;
        }
        
        if(parts[0].equals("MASTER")){
            System.out.println("Master Message");
            parseMasterRequest(req);
            return -1;
        }
        if(parts[0].equals("SETUP")){
            System.out.println("SETUP");
            double bal = Double.parseDouble(parts[2]);
            bank.put(parts[1],bal);
            writeToLog(new Date() + ": Setup Account number " + parts[1] + " balance $" + bal);
            return -1;
        }
        if(parts[0].equals("SETUP2")){
            System.out.println("SETUP2");
            //writeToLog("in setup2");
            parseSetup2(req);
            //writeToLog("in setup2 -2");
            return -1;
        }
        if(parts[0].equals("DONESENDING")){
            System.out.println("Done sending History received");
            myPosition = ServerStatus.TAIL;
            writeToLog(new Date() + " Received Done sending from old tail. Becoming new tail");
            processDoneSending();
            return -1;
            
        }
        
        response += parts[1] + "#";
        if(parts[0].equals("ACK")){
            System.out.println("ACK received");
            parts[1] = parts[1].trim();
            processAck(Integer.parseInt(parts[1]));
            return -1;
        }
        String[] rid = parts[1].split("%");
        for (int i = 0; i < rid.length; i++) {
            rid[i] = rid[i].replace("%", "");
            System.out.println(i + " , "+ rid[i]);
        }
        String msgrecv;
        if(parts[0].equals("balance")){
            System.out.println("rid " + rid[2]);
            msgrecv = parts[0] + " request on account number " + rid[2]+ ". From client "+rid[0]+".";
        }
        else{
            System.out.println("rid " + rid[2]);
            msgrecv = parts[0] + " request on account number " + rid[2] + " for the amount " + Double.parseDouble(parts[2]) +". From client "+rid[0]+".";
            if(myPosition != ServerStatus.TAIL && myPosition!= ServerStatus.HEAD_TAIL){
                System.out.println("Put into sent SEQ = " + parts[3]);
                writeToLog(new Date() + ": Putting SEQ = " + parts[3].trim() + " into sent.");
                parts[3] = parts[3].trim();
                sent.put(Integer.parseInt(parts[3]), req);
            }
            else if(isExtending){
                System.out.println("Put into sentExt SEQ = " + parts[3]);
                writeToLog(new Date() + ": Putting SEQ = " + parts[3].trim() + " into sent because we are extending the chain.");
                parts[3] = parts[3].trim();
                SentPair s = new SentPair(Integer.parseInt(parts[3]), req);
                sentExt.add(req);
                //sent.put(Integer.parseInt(parts[3]), req);
            }
            else if(myPosition == ServerStatus.TAIL){
                try {
                    String toSend = "ACK#"+parts[3];
                    System.out.println("Sending ACK " + toSend);
                    writeToLog(new Date() + ":  I am tail ACKing back = " + parts[3].trim() + " to predecessor.");
                    ByteBuffer buf = ByteBuffer.allocate(1000);
                    buf.clear();
                    buf.put(toSend.getBytes());
                    buf.flip();
                    System.out.println("Testing" + buf.asCharBuffer().toString());
                    //if (predecessorSock == null) {
                        predecessorSock = SocketChannel.open();
                        predecessorSock.connect(new InetSocketAddress("localhost", pServerPort));
                        System.out.println("Connection to successor established");
                    //}
                    while (buf.hasRemaining()) {
                        predecessorSock.write(buf);
                    }
                    //predecessorSock.close();
                } catch (IOException ex) {
                    System.out.println("Tail didnt successfull send ACK on update request.");
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else{
                //Do nothing if head 
                System.out.println("DO NOTHING");
            }
        }
        
        MsgRecieved(m, msgrecv);
        //Check the Request ID in the History First
        try{
            ReqObject check = history.get(parts[1]);
            if (check != null) {
                System.out.println("Request Found in history.");
                if (check.req.equals(parts[0]) && check.req.equals(B)) {
                    String logMsg = new Date() + ": Duplicate Request. Sending processed Reply";
                    response += "0#"+check.balance;
                    System.out.println(logMsg);
                    writeToLog(logMsg);
                    //return;
                } else if (check.req.equals(parts[0]) && check.amount.equals((Double) Double.parseDouble(parts[2]))) {
                    String logMsg = new Date() + ": Duplicate Request. Sending processed Reply";
                    response += "0#"+check.balance;
                    System.out.println(logMsg);
                    writeToLog(logMsg);
                    //return;
                } else {
                    //INCONSISTENT with history
                    String logMsg = new Date() + ": Duplicate Request. Request is Inconsistent with history. Aborting request.";
                    response += "1#"+ bank.get(check.accountNum);
                    System.out.println(logMsg);
                    writeToLog(logMsg);
                    
                }
                if(myPosition == ServerStatus.TAIL || myPosition == ServerStatus.HEAD_TAIL){
                    MsgSent(MessagesEnum.REPLYTOCLI, msgrecv);
                    ByteBuffer sendBuf = ByteBuffer.allocate(MAXBUFFER);
                    sendBuf.clear();
                    sendBuf.put(response.getBytes());
                    sendBuf.flip();
                    String [] host_port = rid[0].split(":");
                    host_port[0] = host_port[0].replaceAll(":", "");
                    host_port[1] = host_port[1].replaceAll(":", "");
                    int c_port = Integer.parseInt(host_port[1]);
                    try {
                        int byteSent = datagramChannel.send(sendBuf, new InetSocketAddress(host_port[0],c_port));
                        //messagesS++;
                    } catch (IOException ex) {
                        System.out.println("FAILED to send response to client.");
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                else{
                    MsgSent(MessagesEnum.FWDTOSUCCESSOR, msgrecv);
                }
                System.out.println(response);
                return 0;
            }
            
        }catch(NullPointerException e){
            System.out.println("Request not found in history.");
        }
        
        

        String ip_port;
        String bank_name;
        String account_num= null;
        int seq_num;
        Double amount = 0.0;
        if (parts.length >=3 && parts[2] != null) {
            System.out.println(parts[2]);
            amount = Double.parseDouble(parts[2]);
        }

        if (rid.length == 4) {
            ip_port = rid[0];
            bank_name = rid[1];
            account_num = rid[2];
            seq_num = Integer.parseInt(rid[2]);
        }
        Double ret = null;
        if (account_num != null) {
            if (parts[0].equals(B)) {
                ret = getBalance(account_num);
                response += "0#"+ret;
            } else if (parts[0].equals(D)) {
                ret = deposit(account_num, amount);
                response += "0#"+ret;
            } else if (parts[0].equals(W)) {
                WithdrawalResponse r = withdrawal(account_num, amount);
                System.out.println("Withdrawal " + r.balance + " " + r.flag); 
                ret = r.balance;
                if(r.flag == true){
                    response += "0#"+ret;
                }
                else{
                    response += "2#"+ret;
                }
            } else {

            }
        }
        ReqObject insert = new ReqObject(parts[1], account_num, req, amount, ReqObject.Outcome.Processed, ret, response);
        history.put(parts[1], insert);
        
        if(myPosition == ServerStatus.TAIL || myPosition == ServerStatus.HEAD_TAIL){
            MsgSent(MessagesEnum.REPLYTOCLI, msgrecv);
            ByteBuffer sendBuf = ByteBuffer.allocate(MAXBUFFER);
            sendBuf.clear();
            sendBuf.put(response.getBytes());
            sendBuf.flip();
            String [] host_port = rid[0].split(":");
            host_port[0] = host_port[0].replaceAll(":", "");
            host_port[1] = host_port[1].replaceAll(":", "");
            int c_port = Integer.parseInt(host_port[1]);
            try {
                int byteSent = datagramChannel.send(sendBuf, new InetSocketAddress(host_port[0],c_port));
                //messagesS++;
            } catch (IOException ex) {
                System.out.println("FAILED to send response to client.");
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        else{
            MsgSent(MessagesEnum.FWDTOSUCCESSOR, msgrecv);
            
        }
        System.out.println(response);
        // TODO HANDLE ACKING LATER
        return 0;
    }

    private static Double getBalance(String accountNum) {
        Double balance = (Double) bank.get(accountNum);
        if (balance != null) {
            writeToLog(new Date() + ": Balance request on Account Number " + accountNum + " processed. Balance: $" + balance);
            return balance;
        } else {
            createAccount(accountNum);
            writeToLog(new Date() + ": Balance request on Account Number " + accountNum+ " processed. New account created. Balance: $0.00");
            return 0.0;//getBalance(accountNum);
        }
    }

    private static WithdrawalResponse withdrawal(String accountNum, Double amount) {
        Double currentBalance = (Double) bank.get(accountNum);
        boolean valid = true;
        Double ret;
        if(currentBalance == null){
            bank.put(accountNum, 0.0);
            writeToLog(new Date() + ": Account not present in the bank. New account created with account numebr " + accountNum + ". Currecnt Balance is $0.00");
            ret = 0.0;
            valid = false;
        }
        else if (currentBalance <=0.0) {
            ret = currentBalance;
            writeToLog(new Date() + ": Insufficient Funds to withdraw $"+ amount+ " from account number "+ accountNum + ". Currecnt Balance: $" + currentBalance);
            valid = false;
            //return null;
        } 
        else {
            Double postTrans = currentBalance - amount;
            if(postTrans <0.0){
                writeToLog(new Date()+ ": Insufficient Funds to withdraw $"+ amount+ " from account number "+ accountNum + ". Currecnt Balance: $" + currentBalance);
                ret = currentBalance;
                valid = false;
            }
            else{
                writeToLog(new Date() + ": Processed withdrawal of $" + amount+ " from account number " + accountNum + ". Current balance: $"+postTrans);
                bank.put(accountNum, postTrans);
                ret = postTrans;
                valid = true;
            }
        }
        WithdrawalResponse resp = new WithdrawalResponse(ret, valid);
        return resp;
    }

    private static Double deposit(String accountNum, Double amount) {
        Double currentBalance = (Double) bank.get(accountNum);
        Double postTrans;
        if (currentBalance == null) {
            postTrans = amount;
            bank.put(accountNum, amount);
        } else {
            postTrans = currentBalance + amount;
            bank.put(accountNum, postTrans);
        }
        writeToLog(new Date() + ": Processed Deposit to account number:"+ accountNum + ". New Balance: $" + postTrans);
        return postTrans;
    }

    private static void createAccount(String accountNum) {
        bank.put(accountNum, new Double(0.0));
    }

    private static void createFile() {
        try {
            //String fname = "./Server_" +myPort + ".log";
            String fname="../logs/"+bankName+"_Server_" +myPort + ".log";
            //String fname=bankName+"_Server_" +myPort + ".log";
            logFile = new File(fname);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            Date today = new Date();
            writeToLog(pound + today + pound);
            writeToLog(new Date() +": "+ myPosition.toString() + " Server in the chain. My IP:Port : " + myIP +":"+ myPort + ". Start delay " + start_delay + ", lifetime " +lifetime + ", receive " + receive + " messages maximum, send " + send + " messages max.");
        } catch (IOException e)  {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, e);
            System.out.println("Error creating log file. Exiting Program");
            System.exit(0);
        }
    }

    private static void writeToLog(String msg) {
        try {
            BufferedWriter output = new BufferedWriter(new FileWriter(logFile, true));
            output.write(msg + "\n");
            output.close();
        } catch (IOException ex) {
            //Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Error Writing to log file.");
        }
    }
    private static void MsgRecieved(MessagesEnum m, String msg){
        switch (m){
            case ACCEPT:
                writeToLog(new Date()+ ": Successor server connected to the listening socket.");
                break;
            case SOCKCHANNELREAD:
                messagesR++;
                writeToLog(new Date()+ ": Recieved Message on Socket channel from successor server. Message contents: "+ msg +" Messages number " + messagesR);
                break;
            case DATACHANNELREAD:
                messagesR++;
                writeToLog(new Date()+ ": Recieved Message on Datagram channel from client. Message contents: "+msg+" Message number " + messagesR);
                break;
            default:
                break;
        }
    }
    private static void MsgSent(MessagesEnum m, String msg){
        switch(m){
            case REPLYTOCLI:
                messagesS++;
                writeToLog(new Date()+ ": Sent reply message to client. Message contents: "+ msg +"Message number " + messagesS);
                break;
            case FWDTOSUCCESSOR:
                messagesS++;
                writeToLog(new Date()+ ": Sent messges to successor server. Message contents: "+ msg +" Message number " + messagesS);
                break;
            default:
                break;
        }
    }
    private static void socketInit() throws IOException {
        mySock = ServerSocketChannel.open();
        ServerSocket serverSocket = mySock.socket();
        Selector selector = Selector.open();
        serverSocket.bind(new InetSocketAddress(myPort));
        mySock.configureBlocking(false);
        mySock.register(selector, SelectionKey.OP_ACCEPT);
        
        //If the server is a HEAD or Tail server start the Datagram channel for the clients to talk on
       // if(myPosition == ServerStatus.HEAD || myPosition == ServerStatus.TAIL || myPosition == ServerStatus.HEAD_TAIL){
            System.out.println("Setup Datagram socket.");
            datagramChannel = DatagramChannel.open();
            datagramChannel.configureBlocking(false);
            datagramChannel.socket().bind(new InetSocketAddress(myPort));
            datagramChannel.register(selector, SelectionKey.OP_READ);
            (new Thread(new Server())).start();
        //}
        //If the server is a MIDDLE server or a tail server then connect to the Predecessor Server
        if(myPosition == ServerStatus.MIDDLE || myPosition == ServerStatus.TAIL){
            
            predecessorSock = SocketChannel.open();
            predecessorSock.configureBlocking(false);
            predecessorSock.connect(new InetSocketAddress(pServerIP,pServerPort));
            predecessorSock.register(selector,SelectionKey.OP_CONNECT);
            System.out.println("SENT Message to Predecessor");
        }
        /*if(myPosition != ServerStatus.TAIL){
            //successorSock = SocketChannel.open();
            //successorSock.connect(new InetSocketAddress(sServerIP,sServerPort ));
        }*/
        
        //Infinite loop to recieve queries and messages from server.
        //System.out.println("send = "+ send + ", receive ="+ receive);
        writeToLog(new Date() + ": Ready to Listen on socket.");
        while (true) {
            //System.out.println("send = "+ send + ", receive ="+ receive);
            //writeToLog("Message R" + messagesR);
            if(messagesR > receive ){
                System.out.println("Server is exiting. Maximum recevied messages reached. messagesR=" + messagesR + ",  receive="+ receive);
                writeToLog(new Date() + " Received maximum number of message. Server is exiting...");
                System.exit(0);
            }
            if(messagesS > send){
                System.out.println("Server is exiting. Maximum send messages reached. messagesS=" + messagesS + ",  send="+ send);
                writeToLog(new Date() + " Received maximum number of message. Server is exiting...");
                System.exit(0);
            }
            int n = selector.select();
            if (n == 0) {
                //System.out.print(".");
                continue;
            }
            Iterator it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                //writeToLog("iterator read something. Inner loop");
                SelectionKey key = (SelectionKey) it.next();
                if (key.isAcceptable()) {
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel channel = server.accept();
                    if (channel != null) {
                        channel.configureBlocking(false);
                        channel.register(selector, SelectionKey.OP_READ);
                        System.out.println("ACCEPTABLE");
                        //writeToLog("ACCEPTABLE");
                        String test = "Server accepted and created a socket channel...";
                        ByteBuffer buf = ByteBuffer.allocate(10000);
                        buf.clear();
                        buf.put(test.getBytes());
                        buf.flip();
                        while (buf.hasRemaining()) {
                            channel.write(buf);
                        }
                        //System.out.println("Not stuck here");
                    }
                    it.remove();
                }
                if (key.isReadable()) {
                    messagesR++;
                    if(messagesR > receive ){
                        System.out.println("Server is exiting. Maximum recevied messages reached. messagesR=" + messagesR + ",  receive="+ receive);
                        writeToLog(new Date() + " Received maximum number of message. Server is exiting...");
                        System.exit(0);
                    }
                    if(messagesS > send){
                        System.out.println("Server is exiting. Maximum send messages reached. messagesS=" + messagesS + ",  send="+ send);
                        writeToLog(new Date() + " Received maximum number of message. Server is exiting...");
                        System.exit(0);
                    }
                    System.out.println("READABLE");
                    SocketChannel channel = null;
                    DatagramChannel ch = null;// = (DatagramChannel) key.channel();
                    try {
                        channel = (SocketChannel) key.channel();
                    } catch (Exception e) {
                        //System.out.println("NOT a SocketChannel");
                    }
                    try {
                        ch = (DatagramChannel) key.channel();
                        ch.configureBlocking(true);
                    } catch (Exception e1) {
                        //System.out.println("NOT a DatagramChannel");
                    }
                    if (channel != null) {
                        System.out.println("In SocketChannel");
                        ByteBuffer buf = ByteBuffer.allocate(MAXBUFFER);
                        buf.clear();
                        try{
                            int bytesRead = channel.read(buf);
                            if (bytesRead == -1) {
                                System.out.println("ConnectionClosed");
                                channel.close();
                            }
                            String ret = new String(buf.array());
                            //System.out.println(ret);
                            buf.clear();
                            buf.flip();
                            buf.clear();
                            int pReqRet = parseRequest(ret, MessagesEnum.SOCKCHANNELREAD);
                            System.out.println("bytes read: " + bytesRead + " " + ret);
                            if(pReqRet == -1){
                                System.out.println("ACK or Master message");
                                //writeToLog("TESTING IF I GET HERE Sock");
                                it.remove();
                                continue;
                            }
                            if (myPosition == ServerStatus.HEAD || myPosition == ServerStatus.MIDDLE) {
                                if(myPosition == ServerStatus.HEAD){
                                    System.out.println("Sock Ch Request: "+ret);
                                    ret+="#"+messagesS;
                                    ret = ret.trim();
                                    System.out.println("Sock Ch Request: "+ ret);
                                }
                                System.out.println(sServerPort);
                                if (successorSock == null) {
                                    successorSock = SocketChannel.open();
                                    successorSock.connect(new InetSocketAddress("localhost", sServerPort));
                                    System.out.println("Connection to successor established");
                                }
                                messagesS++;
                                sent.put(messagesS, ret);
                                while (buf.hasRemaining()) {
                                    successorSock.write(buf);
                                };
                                
                                System.out.println("Sent to successor");
                            }

                            it.remove();
                            continue;        
                        }catch(IOException e){
                            //channel.close();
                        }
                    }
                    if (ch != null) {
                        System.out.println("In Datagram");
                        ByteBuffer buf = ByteBuffer.allocate(100);
                        buf.clear();
                        ch.receive(buf);
                        String ret = new String(buf.array());
                        System.out.println("Data: " + ret);
                        //writeToLog(new Date()+ ": read on sock: " + ret+" status: " +myPosition.toString());
                        if(myPosition == ServerStatus.HEAD){
                            ret = ret.trim();
                            System.out.println("Datagram Ch Request: "+ret);
                            ret= ret + "#"+messagesS;
                            //ret = ret.trim();
                            System.out.println("Datagram Ch Request: "+ ret);
                        }
                        int pReqRet = parseRequest(ret, MessagesEnum.DATACHANNELREAD);
                        buf.flip();
                        // parseRequest(ret, MessagesEnum.SOCKCHANNELREAD);
                        //System.out.println("bytes read: " + bytesRead + " " + ret);
                        if(pReqRet == -1){
                            System.out.println("ACK or Master message");
                            //writeToLog("TESTING IF I GET HERE Data");
                            it.remove();
                            continue;
                        }
                        if(myPosition != ServerStatus.TAIL && myPosition != ServerStatus.HEAD_TAIL){
                            
                            System.out.println("Inside the resending to successor " +sServerPort);
                            if(successorSock == null){
                                successorSock = SocketChannel.open();
                                successorSock.connect(new InetSocketAddress("localhost",sServerPort ));
                                System.out.println("Connection to successor established");
                            }
                            buf.clear();
                            buf.put(ret.getBytes());
                            buf.flip();
                            while(buf.hasRemaining()){
                                successorSock.write(buf);
                            };
                            //messagesS++;
                            System.out.println("Sent to successor"); 
                        }
                        
                        it.remove();
                        continue;
                        
                    }
                    it.remove();
                    continue;
                }
                //it.remove();
                //System.out.println("Not stuck here2");

            }
        }
        
    }
    @Override
    public void run() {
        try {
            //datagramChannel = DatagramChannel.open();
           // datagramChannel.configureBlocking(false);
           // datagramChannel.socket().bind(new InetSocketAddress(myPort));
            while(true){
                Thread.sleep(1000);
                String newData = "PING#"+ bankName.trim()+":"+myPort;
                ByteBuffer buf = ByteBuffer.allocate(48);
                buf.clear();
                buf.put(newData.getBytes());
                buf.flip();

                int bytesSent = datagramChannel.send(buf, new InetSocketAddress("localhost", masterPort));
                //System.out.println("Ping Sent.");
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        

    }
    /**
     * This is the main method which gets called in the function
     *
     * @param args will contain the IP addresses of the Master, Predecessor,
     * Successor etc
     */
    public static void main(String[] args) {
        parseArgs(args);
        createFile();
        bank = new HashMap<String, Double>();
        history = new HashMap<String, ReqObject>();
        sent = new HashMap<Integer, String>();
        if(start_delay>0 ){
            System.out.println("START UP DELAY");
            try {
                Thread.sleep(start_delay * 1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            String newData = "JOIN#"+ bankName.trim()+ "#" + myPort;
            ByteBuffer buf = ByteBuffer.allocate(100);
            buf.clear();
            buf.put(newData.getBytes());
            buf.flip();
            messagesS++;
            System.out.println("send: "+ send+ " messagesS: "+ messagesS);
            try {
                datagramChannel = DatagramChannel.open();
                int byteSent = datagramChannel.send(buf, new InetSocketAddress("localhost", masterPort));
                //byteSent += datagramChannel.send(buf, null)
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            if(messagesS >= send){
                System.out.println("Exiting Sending limit reached.");
                System.exit(0);
            }
        }
        System.out.println("lifetime :" + lifetime);
        writeToLog(new Date() + ": Lifetime = " + lifetime);
        if(lifetime > 0){
           
            System.out.println("Setup lifetime timeout");
            Timer timer = new Timer();
              timer.schedule(new TimerTask() {
                public void run() {
                  System.out.println("Exiting because lifetime reached");
                  writeToLog(new Date() + ": Exiting because lifetime reached");
                  System.exit(0);
                }
            }, lifetime*1000);
        }
        //(new Thread(new Server())).start();
        
        try{
            socketInit();
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        /*sent.put(1, "asdf");
        sent.put(2, "asdf2");
        
        sent.put(3, "asdf2");
        sent.put(4, "asdf2");
        parseRequest("withdrawal#localhost:60000%Chase%0001%2#50.00#5", MessagesEnum.FWDTOSUCCESSOR);
        processAck(3);
        System.out.println("Success");
        String depositTest = "deposit#127.0.0.1:5000%Chase%1%1#100.00";
        String depositTest2 = "deposit#127.0.0.1:5000%Chase%1%1#200.00";
        String withTest = "withdrawal#127.0.0.1:5000%Chase%1%1#50.00";
        String queryTest = "balance#127.0.0.1:5000%Chase1%1%1";
        String withTest2 = "withdrawal#127.0.0.1:5000%Chase%1%1#200.00";
        parseRequest(depositTest, MessagesEnum.DATACHANNELREAD);
        //parseRequest(depositTest2, MessagesEnum.DATACHANNELREAD);
        //parseRequest(withTest);
        //parseRequest(queryTest,MessagesEnum.DATACHANNELREAD);
        //parseRequest(queryTest, MessagesEnum.SOCKCHANNELREAD);
        
        //parseRequest(depositTest);
        //parseRequest(withTest2);
        //System.out.println("Success2");
        
        /*   deposit#clientIPPORT%bank_name%accountnum%seq#amount
         *     withdrawal#clientIPPORT%bank_name%accountnum%seq#amount
         *     balance#clientIPPORT%bank_name%accountnum%seq
         */
    }
}



