
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author tony
 */
class BankInfo{
    String headIP;
    int headPort;
    String tailIP;
    int tailPort;
    public BankInfo(String hIP, int hPort, String tIP, int tPort){
        headIP = hIP;
        headPort = hPort;
        tailIP = tIP;
        tailPort = tPort;
    }
}
public class Client {
    enum RequestType{
        HEAD, TAIL
    }
    private static String myIP;
    private static int myPort;
    private static int rely_timeout;
    private static int request_retries;
    private static boolean resend_head;
    private static boolean isRandom;
    private static String percentages;
    public static final int MAXBUFFER = 1000;
    private static HashMap<String, BankInfo> bankMap = new HashMap<String, BankInfo>();
    private static String[] requests;
    private static File logFile;
    private static long timeout;//= 10000; //timeout ms
    private static double dropRate;
    /**
     * Parse the command line arguments passed to the client program
     * @param args 
     *  args[0] will contain the IP and port number 
     *  args[1] will contain the Bank Names and HEAD and TAIL ip address
     *  args[2] will contain the requests
     *  args[3] will contain the rely timeout
     *  args[4] will contain the request_retries
     *  args[5] will contain the resend_head value
     *  args[6] will contain the percentages if its a random client
     */
    
    public static void parseArgs(String [] args){
        String[] modify = args[0].split(":");
        for(int i = 0; i < modify.length; i++){
            modify[i].replaceAll(":","");
        }
        myIP= modify[0];
        myPort = Integer.parseInt(modify[1]);
        
        modify = args[1].split("@");
        for(int i = 0; i < modify.length ; i++){
            modify[i] = modify[i].replace("@", "");
            String [] modify2 = modify[i].split("#");
            for(int j = 0; j < modify2.length ; j++){
                modify2[j] = modify2[j].replace("#", "");
            }
            String [] modify3 = modify2[1].split(":");
            String hP="", hI="", tP="", tI="";
            for(int j = 0 ; j < modify3.length; j++){
                modify3[j].replace(":", "");
            }
            hI = modify3[0];
            hP = modify3[1];
            modify3 = modify2[2].split(":");
            for(int j = 0 ; j < modify3.length; j++){
                modify3[j].replace(":", "");
            }
            tI = modify3[0];
            tP = modify3[1];
            BankInfo newEntry = new BankInfo(hI, Integer.parseInt(hP), tI, Integer.parseInt(tP));
            bankMap.put(modify2[0], newEntry);
        }
        requests = args[2].split("@");
        System.out.println(requests.length);
        for (int i = 0; i < requests.length; i++){
            //System.out.println(requests[i]);
            requests[i] = requests[i].replace("@", "");
        }
        rely_timeout = Integer.parseInt(args[3]);
        request_retries = Integer.parseInt(args[4]);
        if(args[5].equals("false")){
            resend_head = false;
        }
        else{
            resend_head = true;
        }
        isRandom = false;
        if(args.length == 7){
            percentages = args[6];
            isRandom = true;
        }
    }
    private static void createFile() {
        try {
            String pound = "\n\n############################################################\n\n";
            String fname = "../logs/Client_" +myPort + ".log";
            logFile = new File(fname);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            BufferedWriter output = new BufferedWriter(new FileWriter(logFile, true));
            output.write(pound + new Date() + pound);
            output.close();

            writeToLog("Client. My Host:Port numer:  " + myIP +":"+ myPort + ". Rely timeour " + rely_timeout + ", request retiries " +request_retries + ", resend  head " + resend_head + ".");
            if(isRandom){
                String [] parts = percentages.split(",");
                writeToLog("This is a random server. Here are the percentages to balances, deposits, withdrawals, and transfers in that order " + percentages + ".");
            }
            
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
    private static String getBank(String request){
        String [] parts = request.split("#");
        String [] innerparts = parts[1].split("%");
        String bn = innerparts[1].replace("%", "");
        return bn;
    }
    private static String getAccount(String request){
        String [] parts = request.split("#");
        String [] innerparts = parts[1].split("%");
        String accountNum = innerparts[2].replace("%", "");
        System.out.println(accountNum);
        return accountNum;
    }
    private static String getAmount(String request){
        String [] parts = request.split("#");
        String amt = parts[2];
        amt = amt.replace("#", "");
        return amt;
    }
    private static RequestType getRequestDest(String request){
        String [] parts = request.split("#");
        String req = parts[0];
        req = req.replace("#", "");
        RequestType ret;
        if(req.equals("balance")){
            ret = RequestType.TAIL;
        }
        else{
            ret = RequestType.HEAD;
        }
        return ret;
    }
    private static String getRequestType(String request){
        String [] parts = request.split("#");
        String req = parts[0];
        req = req.replace("#", "");
        return req;
    }
    private static void sendRequests() throws IOException{
        timeout = 1000*rely_timeout;
        DatagramChannel channel = DatagramChannel.open();
        channel.socket().bind(new InetSocketAddress(myPort));
        for(int i = 0; i < requests.length ; i ++ ){
            Selector selector = Selector.open();
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
            ByteBuffer sendbuf = ByteBuffer.allocate(MAXBUFFER);
            sendbuf.clear();
            sendbuf.put(requests[i].getBytes());
            sendbuf.flip();
            RequestType r = getRequestDest(requests[i]);
            String bank = getBank(requests[i]);
            BankInfo b = bankMap.get(bank);
            String host ="";
            int port=0;
            if(r == RequestType.HEAD){
                host = b.headIP;
                port = b.headPort;
            }
            else {
                host = b.tailIP;
                port = b.tailPort;
            }
            int retVal = 0;
            int count = 0;
            while(retVal != 1 && count < request_retries){
                System.out.println("Request: "+ requests[i]);
                if(count != 0){
                    System.out.println("Message not recieved in timely manner. Retransmitting " + count + " time.");
                    writeToLog("Message not received in timely manner. Number of times retransmit " + count);
                }
                else{
                    System.out.println("Sending Request the first time.");
                }
                
                double random = new Random().nextDouble();
                double result = 0.0 + (random * (1.0 - 0.0));
                
                int bytesSent;
                System.out.println("DropRate " + dropRate + ", random " + result);
                if(result > dropRate){
                    bytesSent= channel.send(sendbuf, new InetSocketAddress(host, port));
                }
                else{
                    System.out.println("Message drop simulated. Not sending message.");
                    writeToLog("Message drop simulated. Not sending message.");
                }
                
                int n = selector.select(timeout);
                
                /*String rtype = getRequestType(requests[i]);
                String accountNum = getAccount(requests[i]);
                String amt = getAmount(requests[i]);*/
                if(r == RequestType.HEAD){
                    writeToLog(getRequestType(requests[i])+ " request for the amount of $"
                            +getAmount(requests[i])+" on account number "+ getAccount(requests[i]) 
                            +" sent to head of "+bank+" at host "+ host + " and port "+ port +". Request number " + i );
                }
                else{
                    writeToLog(getRequestType(requests[i])+ " request on account number "
                            + getAccount(requests[i]) 
                            +" sent to tail of "+bank+" at host "+ host + " and port "+ port +". Request number " + i);
                }
                Iterator it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = (SelectionKey) it.next();
                    if (key.isReadable()) {
                        System.out.println("READABLE");
                        ByteBuffer recvBuffer = ByteBuffer.allocate(MAXBUFFER);
                        recvBuffer.clear();

                        channel.receive(recvBuffer);
                        String ret = new String(recvBuffer.array());
                        retVal = parseResponse(ret, getRequestType(requests[i]),i);
                        System.out.println(ret);
                    }
                }    
                //System.out.println(requests[i]);
                /*ByteBuffer recvBuffer = ByteBuffer.allocate(MAXBUFFER);
                recvBuffer.clear();

                channel.receive(recvBuffer);
                String ret = new String(recvBuffer.array());
                retVal = parseResponse(ret, getRequestType(requests[i]),i);
                */
                //if(retVal!= 0){
                    count++;
                //}
                //System.out.println(ret);
            }
            if(count>= request_retries){
                System.out.println("Failed to send request.");
                writeToLog("Failed to send current request. Sending next request.");
            }
        }
        int i =0;
        while(true){
            ByteBuffer recvBuffer = ByteBuffer.allocate(MAXBUFFER);
            recvBuffer.clear();

            channel.receive(recvBuffer);
            String ret = new String(recvBuffer.array());
            parseResponse(ret, "MASTER", 0);
            ret = ret.trim();
            if(ret.length() > 0){
                System.out.println(ret);    
            }
            //i++;
        }
    }
    public static void parseMasterMsg(String msg){
        String [] parts = msg.split("#");
        for(int i = 0; i < parts.length; i++){
            parts[i]= parts[i].replace("#", "");
            parts[i] = parts[i].trim();
            System.out.println(parts[i]);
        }
        //parts[1] will have bank name
        //parts[2] will have HEAD or TAIL
        //parts[3] will have new port number
        BankInfo modify = bankMap.get(parts[1]);
        if(parts[2].equals("HEAD")){
            modify.headPort = Integer.parseInt(parts[3]);
            System.out.println("NEW HEAD");
            writeToLog("Recevied new head server message from master for " + parts[1] + " bank. New port number is "+ modify.headPort);
        }
        else if(parts[2].equals("TAIL")){
            modify.tailPort = Integer.parseInt(parts[3]);
            System.out.println("NEW TAIL");
            writeToLog("Recevied new tail server message from master for " + parts[1] + " bank. New port number is "+ modify.headPort);
        }
    }
    public static int parseResponse(String response, String reqType, int requestNumber){
       //System.out.println("Response: " + response);

        String [] parts = response.split("#");
        if(parts.length <=1){
            return 0;
        }
        for(int i = 0; i < parts.length; i++){
            parts[i] = parts[i].replace("#", "");
        }
        if(parts[0].equals("MASTER")){
            parseMasterMsg(response);
            return 0;
        }
        int outcome = Integer.parseInt(parts[2]);
        Double bal = Double.parseDouble(parts[3]);
        if(outcome == 0){
            writeToLog("Reply from client from server: Processed " + reqType 
                    + " request. The current balance is $" + bal +". Response to request number " + requestNumber);
        }
        else if(outcome == 1){
            writeToLog("Reply to client from server: Dupicate "+ reqType 
                    +" request that is inconsistent with history present at server. The current balance is $" + bal
                    +". Response to request number " + requestNumber);
        }
        else{
           writeToLog("Reply from client from server: Insufficient funds on " + 
                   reqType + "request. The current balance is $" + bal
                   +". Response to request number " + requestNumber); 
        }
        return 1;
    }
    public static void main (String [] args) throws IOException{
        double random = new Random().nextDouble();
        dropRate = 0.0 + (random * (1.0 - 0.0));
        
        parseArgs(args);
        createFile();
        sendRequests();
        //System.out.println(requests[0]);
        //System.out.println(getBank(requests[0]));
        //System.out.println(getRequestDest(requests[0]));
        
        //System.out.println("Herer");
    }
}
