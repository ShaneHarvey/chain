/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author tony
 */
enum ServerStatus{
    HEAD, TAIL, MIDDLE, HEAD_TAIL
}

class BankInfo2{
    String bank_name;
    ArrayList<ServerInfo> servers;
    public BankInfo2(String bn){
        bank_name = bn;
        servers = new ArrayList<ServerInfo>();
    }
}
class ServerInfoForClient{
    String bank_name;
    String HeadPort;
    String HeadIP;
    String TailPort;
    String TailIP;
    ServerInfoForClient(String hp, String hi, String tp, String ti, String bn){
        HeadPort= hp;
        HeadIP = hi;
        TailPort = tp;
        TailIP = ti;
        bank_name = bn;
    }
}
class ServerInfo{
    String IP;
    String Port;
    String Start_delay;
    String Lifetime;
    String Receive;
    String Send;
    public ServerInfo(String ip, String port, String sd, String life, String recv, String send){
        IP = ip;
        Port = port;
        Start_delay = sd;
        Lifetime = life;
        Receive = recv;
        Send = send;
    }
}
class RequestInfo{
    String request;
    String bankName;
    String accountNum;
    String amount;
    String sequenceNum;
    
    public RequestInfo(String r, String b, String a, String seq){

        request = r;
        bankName = b;
        accountNum = a;
        sequenceNum = seq;
    }
    public RequestInfo(String r, String b, String a, String amt, String seq){

        request = r;
        bankName = b;
        accountNum = a;
        amount = amt;
        sequenceNum = seq;
    }
}
class ClientInfo{
    int PortNumber;
    String reply_timeout;
    String request_retries;
    String resend_head;
    boolean isRandom;
    String seed;
    String num_requests;
    String prob_balance;
    String prob_deposit;
    String prob_withdraw;
    String prob_transfer;
    String prob_failure = "0.0";
    String msg_send_delay = "0.0";
    //String request;
    ArrayList<RequestInfo> requests;
    public ClientInfo(String t, String rr, String h){
        reply_timeout = t;
        request_retries = rr;
        resend_head = h;
        requests = new ArrayList<RequestInfo>();
    }
    public ClientInfo(String t, String rr, String h, String s, String n, String b, String d, String w, String tr){
        reply_timeout = t;
        request_retries = rr;
        resend_head = h;
        isRandom = true;
        seed = s;
        num_requests = n;
        prob_balance = b;
        prob_deposit = d;
        prob_withdraw = w;
        prob_transfer = t;
    }
}
public class ExecSimulation {

    /**
     * @param args the command line arguments
     */
    public static void main (String [] args) throws InterruptedException, FileNotFoundException, IOException, ParseException {
            FileReader reader = null;
                ArrayList<BankInfo2> BankArray = new ArrayList<BankInfo2>();
                reader = new FileReader(args[0]);
                JSONParser jp = new JSONParser();
                JSONObject doc = (JSONObject) jp.parse(reader);
                JSONObject banks =(JSONObject) doc.get("banks");
                //Set bankKeys = banks.keySet();
                //Object [] bankNames = bankKeys.toArray();
                Object [] bankNames = banks.keySet().toArray();
                for(int i = 0; i< bankNames.length; i++){
                    
                    //System.out.println(bankNames[i]);
                    String bname = (String)bankNames[i];
                    BankInfo2 binfo = new BankInfo2(bname);
                    JSONObject banki = (JSONObject)banks.get(bname);
                    JSONArray chain = (JSONArray)banki.get("chain");
                    int chainLength = chain.size();
                    //System.out.println(chainLength);
                    for (Object chain1 : chain) {
                        
                        JSONObject serv = (JSONObject)chain1;
                        ServerInfo sinfo = new ServerInfo((String)serv.get("ip"),serv.get("port").toString(), serv.get("start_delay").toString(), serv.get("lifetime").toString(), serv.get("receive").toString(), serv.get("send").toString());
                        binfo.servers.add(sinfo);
                        //System.out.println(serv.get("ip") + ":" + serv.get("port"));
                    }
                    BankArray.add(binfo);
                }
                //System.out.println("Done Processing Servers");
                JSONArray clients = (JSONArray)doc.get("clients");
                ArrayList<ClientInfo> clientsList = new ArrayList<ClientInfo>();
                for(int i = 0; i < clients.size(); i++){
                    JSONObject client_i = (JSONObject) clients.get(i);
                    //This is for hard coded requests in the json file
                    //System.out.println(client_i);
                    //System.out.println(client_i.getClass());
                    String typeOfClient = client_i.get("requests").getClass().toString();
                    
                    //This is for a client that has hardCoded requests
                    if (typeOfClient.equals("class org.json.simple.JSONArray")) {
                        //System.out.println("JSONArray");
                        JSONArray requests = (JSONArray) client_i.get("requests");
                        ClientInfo c = new ClientInfo(client_i.get("reply_timeout").toString(), client_i.get("request_retries").toString(), client_i.get("resend_head").toString());
                        c.prob_failure = client_i.get("prob_failure").toString();
                        c.msg_send_delay = client_i.get("msg_delay").toString();
                        System.out.println("Successfully added prob failure and msg_send "+ c.prob_failure+ ","+ c.msg_send_delay);
                        ArrayList<RequestInfo> req_list = new ArrayList<RequestInfo>();
                        for (int j = 0; j < requests.size(); j++) {
                            JSONObject request_j = (JSONObject) requests.get(j);
                            String req = request_j.get("request").toString();
                            String bank = request_j.get(""
                                    + "bank").toString();
                            String acc = request_j.get("account").toString();
                            String seq = request_j.get("seq_num").toString();
                            String amt = null;
                            try {
                                amt = request_j.get("amount").toString();
                            } catch (NullPointerException e) {
                                //System.out.println("Amount not specified.");
                            }
                            RequestInfo r;
                            if (amt == null) {
                                r = new RequestInfo(req, bank, acc, seq);
                            } else {
                                r = new RequestInfo(req, bank, acc, amt, seq);
                            }
                            //RequestInfo r = new RequestInfo(request_j.get("request").toString(), request_j.get("bank").toString(), request_j.get("account").toString(), request_j.get("amount").toString());
                            req_list.add(r);
                        }
                        c.requests = req_list;
                        c.PortNumber = 60000+i;
                        clientsList.add(c);
                        //System.out.println(client_i);
                    } 
                    //This is for Random client requests
                    else if(typeOfClient.equals("class org.json.simple.JSONObject")) {
                        JSONObject randomReq = (JSONObject) client_i.get("requests");
                        String seed = randomReq.get("seed").toString();
                        String num_requests = randomReq.get("num_requests").toString();
                        String prob_balance = randomReq.get("prob_balance").toString();
                        String prob_deposit = randomReq.get("prob_deposit").toString();
                        String prob_withdraw = randomReq.get("prob_withdrawal").toString();
                        String prob_transfer = randomReq.get("prob_transfer").toString();
                        //ClientInfo c = new ClientInfo(true, seed, num_requests, prob_balance, prob_deposit, prob_withdraw, prob_transfer);
                        ClientInfo c = new ClientInfo(client_i.get("reply_timeout").toString(), client_i.get("request_retries").toString(), client_i.get("resend_head").toString(), seed, num_requests, prob_balance, prob_deposit, prob_withdraw, prob_transfer);
                        c.PortNumber = 60000+i;
                        clientsList.add(c);
                    }
                }    
                //System.out.println(clients.size());
                double lowerPercent = 0.0;
                double upperPercent = 1.0;
                double result;
                String bankChainInfoMaster="";
                for(int x = 0; x < BankArray.size(); x++){
                    BankInfo2 analyze = BankArray.get(x);
                    String chain= analyze.bank_name+"#";
                    //analyze.servers
                    for(int j = 0; j < analyze.servers.size(); j++){
                        if(analyze.servers.get(j).Start_delay.equals("0")){
                            if(j == 0){
                                chain+=analyze.servers.get(j).Port;
                            }
                            else{
                                chain+="#"+analyze.servers.get(j).Port;
                            }
                        }
                    }
                    if(x == 0){
                        bankChainInfoMaster+= chain;
                    }
                    else{
                        bankChainInfoMaster += "@"+chain;
                    }
                }
                //System.out.println("CHAIN: "+ bankChainInfoMaster);
                
                String clientInfoMaster ="";
                for(int x = 0; x < clientsList.size(); x++){
                    ClientInfo analyze = clientsList.get(x);
                    if(x ==0){
                        clientInfoMaster+= analyze.PortNumber;
                    }
                    else{
                        clientInfoMaster+="#"+ analyze.PortNumber;
                    }
                    
                }
                //System.out.println("Clients: "+ clientInfoMaster);
                
                //RUN MASTER HERE 
                String MasterPort = "49999";
                String masterExec = "java Master "+MasterPort +" "+ clientInfoMaster +" "+ bankChainInfoMaster;
                Process masterProcess = Runtime.getRuntime().exec(masterExec);
                System.out.println(masterExec);
                ArrayList<ServerInfoForClient> servInfoCli = new ArrayList<ServerInfoForClient>();

                // List of all servers is saved so that we can wait for them to exit.
                ArrayList<Process> serverPros = new ArrayList<Process>();
                //ArrayList<String> execServs = new ArrayList<String>();
                for(int i = 0 ; i < BankArray.size(); i++){
                    BankInfo2 analyze = BankArray.get(i);
                    //System.out.println(analyze.bank_name);
                    //One server in the chain
                    String execCmd = "java Server ";
                    String hIP = "", hPort = "", tIP="", tPort="", bn="";
                    bn = analyze.bank_name;
                    boolean joinFlag = false;
                    if(analyze.servers.size() == 2 && analyze.servers.get(1).Start_delay.equals("0")){
                        joinFlag = false;
                    }
                    else{
                        joinFlag = true;
                    }
                    
                    if(analyze.servers.size()== 1 && joinFlag == false ){
                        //if(analyze.servers.size() == 1){
                            ServerInfo si = analyze.servers.get(0);
                            execCmd += "HEAD_TAIL " +si.IP + ":" + si.Port;
                            execCmd += " localhost:0 localhost:0 localhost:"+ MasterPort +" " + si.Start_delay + " " + si.Lifetime + " " + si.Receive + " " + si.Send+ " " + analyze.bank_name;;
                            hIP = si.IP;
                            hPort = si.Port;
                            tIP = si.IP;
                            tPort = si.Port;
                            System.out.println(execCmd);
                            Thread.sleep(500);
                            Process pro = Runtime.getRuntime().exec(execCmd);
                            serverPros.add(pro);
                        //}
                    }
                    else if(analyze.servers.size()== 2 && joinFlag == true){
                        ServerInfo si = analyze.servers.get(0);
                        execCmd += "HEAD_TAIL " +si.IP + ":" + si.Port;
                        execCmd += " localhost:0 localhost:0 localhost:"+ MasterPort +" " + si.Start_delay + " " + si.Lifetime + " " + si.Receive + " " + si.Send+ " " + analyze.bank_name;;
                        hIP = si.IP;
                        hPort = si.Port;
                        tIP = si.IP;
                        tPort = si.Port;
                        System.out.println(execCmd);
                        Thread.sleep(500);
                        Process pro = Runtime.getRuntime().exec(execCmd);
                        serverPros.add(pro);
                        
                        execCmd = "java Server ";
                        ServerInfo si2 = analyze.servers.get(1);
                        execCmd += "TAIL " +si2.IP + ":" + si2.Port;
                        execCmd += " localhost:0 localhost:0 localhost:"+ MasterPort +" " +si2.Start_delay + " " + si2.Lifetime + " " + si2.Receive + " " + si2.Send+ " " + analyze.bank_name;;
                        hIP = si.IP;
                        hPort = si.Port;
                        tIP = si.IP;
                        tPort = si.Port;
                        System.out.println(execCmd);
                        Thread.sleep(500);
                        Process pro2 = Runtime.getRuntime().exec(execCmd);
                        serverPros.add(pro2);
                    }
                    else{
                        int icount=0;
                        for(int x = 0 ; x < analyze.servers.size(); x++){
                            ServerInfo si = analyze.servers.get(x);
                            if(si.Start_delay.equals("0")){
                                icount++;
                            }
                        }
                        System.out.println("icount:" + icount );
                        for(int j = 0; j < icount; j++){
                        //for(int j = 0; j < analyze.servers.size(); j++){
                            execCmd = "java Server ";
                            ServerInfo si = analyze.servers.get(j);
                            //Head server
                            if(j == 0 ){
                                ServerInfo siSucc = analyze.servers.get(j+1);
                                execCmd += "HEAD " + si.IP + ":" +si.Port+ " ";
                                execCmd += "localhost:0 " + siSucc.IP +":" +siSucc.Port + " localhost:"+MasterPort;
                                execCmd += " "+ si.Start_delay + " " + si.Lifetime + " " + si.Receive + " " + si.Send + " " + analyze.bank_name;
                                System.out.println(execCmd);
                                hIP = si.IP;
                                hPort = si.Port;
                                
                            }
                            //Tail Server
                            else if(j == (icount -1)){//analyze.servers.size() - 1) ){
                                ServerInfo siPred =  analyze.servers.get(j-1);
                                execCmd += "TAIL " + si.IP + ":" +si.Port+ " ";
                                execCmd +=  siPred.IP +":" +siPred.Port + " localhost:0 localhost:"+MasterPort;
                                execCmd += " "+ si.Start_delay + " " + si.Lifetime + " " + si.Receive + " " + si.Send+ " " + analyze.bank_name;
                                tIP = si.IP;
                                tPort = si.Port;
                                System.out.println(execCmd);
                            }
                            //Middle Server
                            else{
                                ServerInfo siSucc =  analyze.servers.get(j+1);
                                ServerInfo siPred =  analyze.servers.get(j-1);
                                execCmd += "MIDDLE " + si.IP + ":" +si.Port+ " ";
                                execCmd +=  siPred.IP +":" +siPred.Port +" " +siSucc.IP+":"+ siSucc.Port +" localhost:"+ MasterPort;
                                execCmd += " "+ si.Start_delay + " " + si.Lifetime + " " + si.Receive + " " + si.Send + " " + analyze.bank_name;
                                System.out.println(execCmd);
                            }
                            Thread.sleep(500);
                            Process pro = Runtime.getRuntime().exec(execCmd);
                            serverPros.add(pro);
                        }
                        for(int j = icount; j < analyze.servers.size(); j++){
                            execCmd = "java Server ";
                            ServerInfo si = analyze.servers.get(j);
                            ServerInfo siPred =  analyze.servers.get(j-1);
                            execCmd += "TAIL " + si.IP + ":" +si.Port+ " ";
                            execCmd +=  siPred.IP +":" +siPred.Port + " localhost:0 localhost:"+MasterPort;
                            execCmd += " "+ si.Start_delay + " " + si.Lifetime + " " + si.Receive + " " + si.Send+ " " + analyze.bank_name;
                            tIP = si.IP;
                            tPort = si.Port;
                            System.out.println(execCmd);
                            Thread.sleep(500);
                            Process pro = Runtime.getRuntime().exec(execCmd);
                            serverPros.add(pro);
                        }
                    }
                    ServerInfoForClient newServInfoForCli = new ServerInfoForClient(hPort, hIP, tPort, tIP, bn);
                    servInfoCli.add(newServInfoForCli);
                }
                String banksCliParam = "";
                for(int i = 0 ; i < servInfoCli.size(); i++){
                    ServerInfoForClient temp = servInfoCli.get(i);
                    String add = "@"+ temp.bank_name + "#"+ temp.HeadIP + ":" + temp.HeadPort +"#" +temp.TailIP+":"+temp.TailPort; 
                    banksCliParam += add;
                }
                banksCliParam = banksCliParam.replaceFirst("@", "");
                //System.out.println(banksCliParam);

                // List of clients is saved so that we can wait for them to exit.
                ArrayList<Process> clientPros = new ArrayList<Process>();
                for(int i  = 0 ; i < clientsList.size(); i++){
                    ClientInfo analyze = clientsList.get(i);
                    String requestsString= "";
                    if(analyze.isRandom){
                        double balance = Double.parseDouble(analyze.prob_balance);
                        //System.out.println(analyze.prob_balance);
                        double deposit = Double.parseDouble(analyze.prob_deposit);
                        double withdraw = Double.parseDouble(analyze.prob_withdraw);
                        int numRequests = Integer.parseInt(analyze.num_requests);
                        for (int j = 0; j < numRequests; j++) {
                            result = Math.random() * (1.0 - 0.0) + 0.0;
                            int randAccount = (int) (Math.random() * (10001 - 0) + 0);
                            double randAmount = Math.random() * (10001.00 - 0.0) + 0;
                            int adjustMoney = (int) randAmount*100;
                            randAmount = (double) adjustMoney/100.00;
                            int randBank = (int) (Math.random() * (bankNames.length - 0) + 0);
                            if (result < balance) {
                                //withdrawal#clientIPPORT%bank_name%accountnum%seq#amount
                                requestsString += "@balance#localhost:"+analyze.PortNumber+"%"+bankNames[randBank]+"%"+randAccount+"%"+j;
                            } else if (result < (deposit+balance)) {
                                requestsString += "@deposit#localhost:"+analyze.PortNumber+"%"+bankNames[randBank]+"%"+randAccount+"%"+j+"#"+randAmount;
                            } else {
                                requestsString += "@withdrawal#localhost:"+analyze.PortNumber+"%"+bankNames[randBank]+"%"+randAccount+"%"+j+"#"+randAmount;
                            }
                        }
                        
                    }
                    else{
                        for(int j = 0; j < analyze.requests.size(); j++){
                            
                            RequestInfo req = analyze.requests.get(j);
                            //System.out.println("Sequence ###" + req.sequenceNum);
                            if(req.request.equals("balance")){
                                requestsString += "@"+req.request + "#localhost:" + analyze.PortNumber + "%" + req.bankName + "%" + req.accountNum + "%" + req.sequenceNum;
                            }
                            else{
                                requestsString += "@"+req.request + "#localhost:" + analyze.PortNumber + "%" + req.bankName + "%" + req.accountNum + "%" + req.sequenceNum+"#"+req.amount;
                            }
                            
                        }
                    }
                    requestsString = requestsString.replaceFirst("@","");
                    String execCommand;
                    int p = 60000+i;
                    if(analyze.isRandom){
                         execCommand = "java Client localhost:"+p+ " "+banksCliParam+" "+requestsString+ " "+ analyze.reply_timeout 
                                + " " + analyze.request_retries + " " + analyze.resend_head + " " +analyze.prob_failure + " "+ analyze.msg_send_delay +" " +analyze.prob_balance
                                + "," + analyze.prob_deposit + ","+ analyze.prob_withdraw +","+ analyze.prob_transfer; 
                    }
                    else{
                       execCommand = "java Client localhost:"+p+ " "+banksCliParam+" "+requestsString+ " "+ analyze.reply_timeout 
                                + " " + analyze.request_retries + " " + analyze.resend_head + " " +analyze.prob_failure + " "+ analyze.msg_send_delay;
                     
                    }
                    Thread.sleep(500);
                    System.out.println(execCommand);
                    System.out.println("Client " + (i+1) + " started" );
                    Process cliPro = Runtime.getRuntime().exec(execCommand);
                    clientPros.add(cliPro);
                    //System.out.println(requestsString);
                }
                // Wait for all the clients to terminate
                for(Process clientPro: clientPros) {
                    try {
                        clientPro.waitFor();
                        System.out.println("Client process finished.");
                    } catch (InterruptedException e) {
                        System.out.println("Interrupted while waiting for client.");
                    }
                }
                // Sleep for two seconds
                Thread.sleep(2000);
                // Force termination of the servers
                for(Process serverPro: serverPros) {
                    serverPro.destroy();
                    System.out.println("Killed server.");
                }
                masterProcess.destroy();
                System.out.println("Killed Master");
                //System.out.println("asdf");
        }
        
}
