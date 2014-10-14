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
                    
                    System.out.println(bankNames[i]);
                    String bname = (String)bankNames[i];
                    BankInfo2 binfo = new BankInfo2(bname);
                    JSONObject banki = (JSONObject)banks.get(bname);
                    JSONArray chain = (JSONArray)banki.get("chain");
                    int chainLength = chain.size();
                    //System.out.println(chainLength);
                    for (Object chain1 : chain) {
                        
                        JSONObject serv = (JSONObject)chain1;
                        /*String ip =(String)serv.get("ip");
                        String port ="", delay = "", lifetime ="", receive="", send="";
                        port += serv.get("port");
                        delay += serv.get("start_delay");
                        lifetime += serv.get("lifetime");
                        receive += serv.get("receive");
                        send += serv.get("send");*/
                        
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
                ArrayList<ServerInfoForClient> servInfoCli = new ArrayList<ServerInfoForClient>();
                for(int i = 0 ; i < BankArray.size(); i++){
                    BankInfo2 analyze = BankArray.get(i);
                    //System.out.println(analyze.bank_name);
                    //One server in the chain
                    String execCmd = "java Server ";
                    String hIP = "", hPort = "", tIP="", tPort="", bn="";
                    bn = analyze.bank_name;
                    
                    if(analyze.servers.size()== 1){
                        ServerInfo si = analyze.servers.get(0);
                        execCmd += "HEAD_TAIL " +si.IP + ":" + si.Port;
                        execCmd += " localhost:0 localhost:0 localhost:0 "+ si.Start_delay + " " + si.Lifetime + " " + si.Receive + " " + si.Send+ " " + analyze.bank_name;;
                        hIP = si.IP;
                        hPort = si.Port;
                        tIP = si.IP;
                        tPort = si.Port;
                        System.out.println(execCmd);
                        Thread.sleep(500);
                        Process pro = Runtime.getRuntime().exec(execCmd);
                    }
                    else{
                        for(int j = 0; j < analyze.servers.size(); j++){
                            execCmd = "java Server ";
                            ServerInfo si = analyze.servers.get(j);
                            //Head server
                            if(j == 0){
                                ServerInfo siSucc = analyze.servers.get(j+1);
                                execCmd += "HEAD " + si.IP + ":" +si.Port+ " ";
                                execCmd += "localhost:0 " + siSucc.IP +":" +siSucc.Port + " localhost:0";
                                execCmd += " "+ si.Start_delay + " " + si.Lifetime + " " + si.Receive + " " + si.Send + " " + analyze.bank_name;
                                System.out.println(execCmd);
                                hIP = si.IP;
                                hPort = si.Port;
                                
                            }
                            //Tail Server
                            else if(j == (analyze.servers.size() - 1) ){
                                ServerInfo siPred =  analyze.servers.get(j-1);
                                execCmd += "TAIL " + si.IP + ":" +si.Port+ " ";
                                execCmd +=  siPred.IP +":" +siPred.Port + " localhost:0 localhost:0";
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
                                execCmd +=  siPred.IP +":" +siPred.Port +" " +siSucc.IP+":"+ siSucc.Port +" localhost:0";
                                execCmd += " "+ si.Start_delay + " " + si.Lifetime + " " + si.Receive + " " + si.Send + " " + analyze.bank_name;
                                System.out.println(execCmd);
                            }
                            Thread.sleep(500);
                            Process pro = Runtime.getRuntime().exec(execCmd);
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
                                + " " + analyze.request_retries + " " + analyze.resend_head + " " + analyze.prob_balance
                                + "," + analyze.prob_deposit + ","+ analyze.prob_withdraw +","+ analyze.prob_transfer; 
                    }
                    else{
                       execCommand = "java Client localhost:"+p+ " "+banksCliParam+" "+requestsString+ " "+ analyze.reply_timeout 
                                + " " + analyze.request_retries + " " + analyze.resend_head;
                     
                    }
                    Thread.sleep(500);
                    //System.out.println(execCommand);
                    System.out.println("Client " + (i+1) + " started" );
                    Process serv1 = Runtime.getRuntime().exec(execCommand);
                    //System.out.println(requestsString);
                }
                
                
                
                //System.out.println("asdf");
        }
        
}
