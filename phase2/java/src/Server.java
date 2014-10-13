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
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
public class Server {

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
    private static ServerSocketChannel mySock;
    private static DatagramChannel datagramChannel;
    public static String pound = "\n\n############################################################\n\n";
    public static HashMap<String, ReqObject> history;
    private static int start_delay;
    private static int lifetime;
    private static int receive;
    private static int send;
    private static String bankName;
    
    private static int messagesS = 0;
    private static int messagesR = 0;
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

    /**
     * This function will parse the message recieved from the Sockets
     *
     * @param req This is the string representing the request, look below for
     * the format of the messages
     * deposit#clientIPPORT%bank_name%accountnum%seq#amount
     * withdrawal#clientIPPORT%bank_name%accountnum%seq#amount
     * balance#clientIPPORT%bank_name%accountnum%seq
     */
    private static void parseRequest(String req, MessagesEnum m) {
        System.out.println(req);
        String response = "reply#";
        String[] parts = req.split("#");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].replace("#", "");
        }
        response += parts[1] + "#";
        if(parts[0].equals(B) && m == MessagesEnum.SOCKCHANNELREAD){
            System.out.println("HACK");
            return;
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
                    } catch (IOException ex) {
                        System.out.println("FAILED to send response to client.");
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                else{
                    MsgSent(MessagesEnum.FWDTOSUCCESSOR, msgrecv);
                }
                System.out.println(response);
                return;
            }
            
        }catch(NullPointerException e){
            System.out.println("Request not found in history.");
        }
        
        

        String ip_port;
        String bank_name;
        String account_num= null;
        int seq_num;
        Double amount = 0.0;
        if (parts.length == 3 && parts[2] != null) {
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
        ReqObject insert = new ReqObject(parts[1], account_num, parts[0], amount, ReqObject.Outcome.Processed, ret, response);
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
            logFile = new File(fname);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            Date today = new Date();
            writeToLog(pound + today + pound);
            writeToLog(new Date() +": "+ myPosition.toString() + " Server in the chain. My IP:Port : " + myIP +":"+ myPort + ". Start delay " + start_delay + ", lifetime " +lifetime + ", receive " + receive + " messages maximum, send " + send + " messages max.");
        } catch (IOException e)  {
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
        if(myPosition == ServerStatus.HEAD || myPosition == ServerStatus.TAIL || myPosition == ServerStatus.HEAD_TAIL){
            System.out.println("Setup Datagram socket.");
            datagramChannel = DatagramChannel.open();
            datagramChannel.configureBlocking(false);
            datagramChannel.socket().bind(new InetSocketAddress(myPort));
            datagramChannel.register(selector, SelectionKey.OP_READ);
        }
        //If the server is a MIDDLE server or a tail server then connect to the Predecessor Server
        if(myPosition == ServerStatus.MIDDLE || myPosition == ServerStatus.TAIL){
            
            predecessorSock = SocketChannel.open();
            predecessorSock.configureBlocking(false);
            predecessorSock.connect(new InetSocketAddress(pServerIP,pServerPort));
            predecessorSock.register(selector, SelectionKey.OP_READ);
            System.out.println("SENT Message to Predecessor");
        }
        /*if(myPosition != ServerStatus.TAIL){
            //successorSock = SocketChannel.open();
            //successorSock.connect(new InetSocketAddress(sServerIP,sServerPort ));
        }*/
        
        //Infinite loop to recieve queries and messages from server.
        while (true) {
            int n = selector.select();
            if (n == 0) {
                //System.out.print(".");
                continue;
            }

            Iterator it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = (SelectionKey) it.next();
                if (key.isAcceptable()) {
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel channel = server.accept();
                    if (channel != null) {
                        channel.configureBlocking(false);
                        channel.register(selector, SelectionKey.OP_READ);
                        System.out.println("ACCEPTABLE");
                        String test = "Server accepted and created a socket channel...";
                        ByteBuffer buf = ByteBuffer.allocate(1000);
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
                        parseRequest(ret, MessagesEnum.SOCKCHANNELREAD);
                        System.out.println("bytes read: " + bytesRead + " " + ret);
                        if (myPosition == ServerStatus.HEAD || myPosition == ServerStatus.MIDDLE) {
                            System.out.println(sServerPort);
                            if (successorSock == null) {
                                successorSock = SocketChannel.open();
                                successorSock.connect(new InetSocketAddress("localhost", sServerPort));
                                System.out.println("Connection to successor established");
                            }
                            while (buf.hasRemaining()) {
                                successorSock.write(buf);
                            };
                            System.out.println("Sent to successor");
                        }
                        
                        it.remove();
                        continue;
                    }
                    if (ch != null) {
                        System.out.println("In Datagram");
                        ByteBuffer buf = ByteBuffer.allocate(100);
                        buf.clear();
                        ch.receive(buf);
                        String ret = new String(buf.array());
                        System.out.println("Data: " + ret);
                        parseRequest(ret, MessagesEnum.DATACHANNELREAD);
                        buf.flip();

                        if(myPosition != ServerStatus.TAIL && myPosition != ServerStatus.HEAD_TAIL){
                            System.out.println("Inside the resending to successor " +sServerPort);
                            if(successorSock == null){
                                successorSock = SocketChannel.open();
                                successorSock.connect(new InetSocketAddress("localhost",sServerPort ));
                                System.out.println("Connection to successor established");
                            }
                            while(buf.hasRemaining()){
                                successorSock.write(buf);
                            };
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

    /**
     * This is the main method which gets called in the function
     *
     * @param args will contain the IP addresses of the Master, Predecessor,
     * Successor etc
     */
    public static void main(String[] args) {
        bank = new HashMap<String, Double>();
        history = new HashMap<String, ReqObject>();
        parseArgs(args);
        createFile();
        /*writeToLog("TESTING1...");
        writeToLog("TESTING2...");
        writeToLog("TESTING3...");*/
        try {
            socketInit();
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Success");
        String depositTest = "deposit#127.0.0.1:5000%Chase%1%1#100.00";
        String depositTest2 = "deposit#127.0.0.1:5000%Chase%1%1#200.00";
        String withTest = "withdrawal#127.0.0.1:5000%Chase%1%1#50.00";
        String queryTest = "balance#127.0.0.1:5000%Chase1%1%1";
        String withTest2 = "withdrawal#127.0.0.1:5000%Chase%1%1#200.00";
        //parseRequest(depositTest, MessagesEnum.DATACHANNELREAD);
        //parseRequest(depositTest2, MessagesEnum.DATACHANNELREAD);
        /*//parseRequest(withTest);
        parseRequest(queryTest,MessagesEnum.DATACHANNELREAD);
        parseRequest(queryTest, MessagesEnum.SOCKCHANNELREAD);
        */
        //parseRequest(depositTest);
        //parseRequest(withTest2);
        System.out.println("Success2");
        /*   deposit#clientIPPORT%bank_name%accountnum%seq#amount
         *     withdrawal#clientIPPORT%bank_name%accountnum%seq#amount
         *     balance#clientIPPORT%bank_name%accountnum%seq
         */
    }
}
