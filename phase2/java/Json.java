/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package json;

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
class BankInfo{
    String bank_name;
    ArrayList<ServerInfo> servers;
    public BankInfo(String bn){
        bank_name = bn;
        servers = new ArrayList();
    }
}
class ServerInfo{
    private String IP;
    private String Port;
    private String Start_delay;
    private String Lifetime;
    private String Receive;
    private String Send;
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
    
    public RequestInfo(String r, String b, String a){

        request = r;
        bankName = b;
        accountNum = a;
    }
    public RequestInfo(String r, String b, String a, String amt){

        request = r;
        bankName = b;
        accountNum = a;
        amount = amt;
    }
}
class ClientInfo{
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
        requests = new ArrayList();
    }
    public ClientInfo(String t, String rr, String h, String s, String n, String b, String d, String w, String tr){
        reply_timeout = t;
        request_retries = rr;
        resend_head = h;
        isRandom = true;
        seed = s;
        num_requests = n;
        prob_balance = b;
        prob_withdraw = w;
        prob_transfer = t;
    }
}
public class Json {

    /**
     * @param args the command line arguments
     */
	public static void main (String [] args) throws FileNotFoundException, IOException, ParseException {
            FileReader reader = null;
                ArrayList BankArray = new ArrayList();
                reader = new FileReader(args[0]);
                JSONParser jp = new JSONParser();
                JSONObject doc = (JSONObject) jp.parse(reader);
                JSONObject banks =(JSONObject) doc.get("banks");
                Set bankKeys =(Set) banks.keySet();
                Object [] bankNames = (Object[])bankKeys.toArray();
                
                for(int i = 0; i< bankNames.length; i++){
                    
                    System.out.println(bankNames[i]);
                    String bname = (String)bankNames[i];
                    BankInfo binfo = new BankInfo(bname);
                    JSONObject banki = (JSONObject)banks.get(bname);
                    JSONArray chain = (JSONArray)banki.get("chain");
                    int chainLength = chain.size();
                    System.out.println(chainLength);
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
                        System.out.println(serv.get("ip") + ":" + serv.get("port"));
                    }
                    BankArray.add(binfo);
                }
                System.out.println("Done Processing Servers");
                JSONArray clients = (JSONArray)doc.get("clients");
                ArrayList<ClientInfo> clientsList = new ArrayList();
                for(int i = 0; i < clients.size(); i++){
                    JSONObject client_i = (JSONObject) clients.get(i);
                    //This is for hard coded requests in the json file
                    System.out.println(client_i.getClass());
                    String typeOfClient = client_i.get("requests").getClass().toString();
                    
                    //This is for a client that has hardCoded requests
                    if (typeOfClient.equals("class org.json.simple.JSONArray")) {
                        System.out.println("JSONArray");
                        JSONArray requests = (JSONArray) client_i.get("requests");
                        ClientInfo c = new ClientInfo(client_i.get("reply_timeout").toString(), client_i.get("request_retries").toString(), client_i.get("resend_head").toString());
                        ArrayList<RequestInfo> req_list = new ArrayList();
                        for (int j = 0; j < requests.size(); j++) {
                            JSONObject request_j = (JSONObject) requests.get(j);
                            String req = request_j.get("request").toString();
                            String bank = request_j.get("bank").toString();
                            String acc = request_j.get("account").toString();
                            String amt = null;
                            try {
                                amt = request_j.get("amount").toString();
                            } catch (NullPointerException e) {
                                System.out.println("Amount not specified.");
                            }
                            RequestInfo r;
                            if (amt == null) {
                                r = new RequestInfo(req, bank, acc);
                            } else {
                                r = new RequestInfo(req, bank, acc, amt);
                            }
                            //RequestInfo r = new RequestInfo(request_j.get("request").toString(), request_j.get("bank").toString(), request_j.get("account").toString(), request_j.get("amount").toString());
                            req_list.add(r);
                        }
                        c.requests = req_list;
                        clientsList.add(c);
                        System.out.println(client_i);
                    } 
                    //This is for Random client requests
                    else if(typeOfClient.equals("class org.json.simple.JSONObject")) {
                        JSONObject randomReq = (JSONObject) client_i.get("requests");
                        String seed = randomReq.get("seed").toString();
                        String num_requests = randomReq.get("num_requests").toString();
                        String prob_balance = randomReq.get("prob_balance").toString();
                        String prob_deposit = randomReq.get("prob_deposit").toString();
                        String prob_withdraw = randomReq.get("prob_withdraw").toString();
                        String prob_transfer = randomReq.get("prob_transfer").toString();
                        //ClientInfo c = new ClientInfo(true, seed, num_requests, prob_balance, prob_deposit, prob_withdraw, prob_transfer);
                        ClientInfo c = new ClientInfo(client_i.get("reply_timeout").toString(), client_i.get("request_retries").toString(), client_i.get("resend_head").toString(), seed, num_requests, prob_balance, prob_deposit, prob_withdraw, prob_transfer);
                        clientsList.add(c);
                    }
                }    
                System.out.println(clients.size());

        }
}
