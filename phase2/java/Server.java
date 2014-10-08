import java.nio.channels.Selector;
import java.util.HashMap;

public class Server{
    public enum ServerEnum{
        ME, PREDECESSOR, SUCCESSOR, MASTER
    } 
    public enum ServerStatus{
        HEAD, TAIL, MIDDLE
    }
    public enum Outcome{
        Processed, InconsistentWithHistory, InsufficientFunds, Invalid
    }
    //enum Outcome { Processed, InconsistentWithHistory, InsufficientFunds }
    public static final String W = "withdrawal";
    public static final String D = "deposit";
    public static final String B = "balance";
    private static HashMap bank;
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
    /**
     * This method Parses the command line arguments and sets up the variables that store the predecessor, successor, and master server
     * @param arg
     * arg[0] will contain HEAD, TAIL, MIDDLE
     * arg[1] will contain my IP address and Port Number(Listening Port)
     * arg[2] will contain the IP address and PORT of the Predecessor Server EX(127.0.0.1:50000). If its the HEAD this will be zero
     * arg[3] will contain the IP address and PORT of the Successor Server EX(127.0.0.1:50000). If its the TAIL this will be zero
     * arg[4] will contain the IP address and PORT Number for the Master
     */
    private static void parseArgs(String[] arg){
        recordIPAndPort(arg[1], ServerEnum.ME);
        recordIPAndPort(arg[2], ServerEnum.PREDECESSOR);
        recordIPAndPort(arg[3], ServerEnum.SUCCESSOR);
        recordIPAndPort(arg[4], ServerEnum.MASTER);
        if(arg[0].equals("HEAD")){
            myPosition = ServerStatus.HEAD;
        }
        else if(arg[0].equals("TAIL")){
            myPosition = ServerStatus.TAIL;
        }
        else if(arg[0].equals("MIDDLE")){
            myPosition = ServerStatus.MIDDLE;
        }
        else{
            System.out.println("Error in Server position definition");
            System.exit(0);
        }
    }
    /**
     * Parses the IP and Port number string and populates the appropriate variables
     * @param ip_port String that is IP and Port number comma separated
     * @param who Indicates me, predecessor, successor, or master
     */
    private static void recordIPAndPort(String ip_port, ServerEnum who ){
        String [] parts = ip_port.split(":");
        parts[0] = parts[0].replace(":", "");
        parts[1] = parts[1].replace(":", "");
        int port = Integer.parseInt(parts[1]);
        switch (who){
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
     * @param req This is the string representing the request, look below for the format of the messages
     *     deposit#clientIPPORT%bank_name%accountnum%seq#amount
     *     withdrawal#clientIPPORT%bank_name%accountnum%seq#amount
     *     balance#clientIPPORT%bank_name%accountnum%seq
     */
    private static void parseRequest(String req){
        String response;
        String [] parts = req.split("#");
        for(int i = 0; i < parts.length; i++){
            parts[i] = parts[i].replace("#", "");
        }
        
        String [] rid= parts[1].split("%");
        for(int i = 0; i < rid.length; i++){
            rid[i] = rid[i].replace("%", "");
        }
        
        String ip_port;
        String bank_name;
        int account_num= -1;
        int seq_num;
        Double amount = 0.0;
        if(parts.length == 3){
            amount = Double.parseDouble(parts[2]);
        }
        
        if(rid.length == 4){
            ip_port = rid[0];
            bank_name = rid[1];
            account_num = Integer.parseInt(rid[2]);
            seq_num = Integer.parseInt(rid[2]);
        }
        if(account_num != -1){
            if(parts[0].equals(B)){
                getBalance(account_num);
            }
            else if(parts[0].equals(D)){
                deposit(account_num, amount);
            }
            else if(parts[0].equals(W)){
                withdrawal(account_num, amount);
            }
            else{
                
            }
        }
        // TODO HANDLE ACKING LATER
    }
    private static Double getBalance(int accountNum){
        Double balance = (Double)bank.get(accountNum);
        if(balance != null){
            return balance;
        }
        else{
            createAccount(accountNum);
            return 0.0;//getBalance(accountNum);
        }    
    }
    private static Double withdrawal(int accountNum, Double amount){
        Double currentBalance = (Double)bank.get(accountNum);
        if(currentBalance < 0.0 || currentBalance == null){
            return null;
        }
        else{
            Double postTrans = currentBalance - amount;
            bank.put(accountNum, postTrans);
            return postTrans;
        }
    }
    private static Double deposit(int accountNum, Double amount){
        Double currentBalance = (Double)bank.get(accountNum);
        Double postTrans;
        if(currentBalance == null){
            postTrans = amount;
            bank.put(accountNum, amount);
        }
        else{
            postTrans = currentBalance + amount;
            bank.put(accountNum, postTrans);
        }
        return postTrans;
    }
    private static void createAccount(int accountNum){
        bank.put(accountNum, new Double(0.0));
    }
    /**
     * This is the main method which gets called in the function
     * @param args will contain the IP addresses of the Master, Predecessor, Successor etc
     */
    public static void main (String [] args){
        bank = new HashMap();
        parseArgs(args);
        System.out.println("Success");
        String depositTest = "deposit#127.0.0.1:5000%Chase%1%1#100.00";
        String withTest = "withdrawal#127.0.0.1:5000%Chase%1%1#50.00";
        String queryTest = "balance#127.0.0.1:5000%Chase%1%1";
        parseRequest(depositTest);
        parseRequest(withTest);
        parseRequest(queryTest);
        parseRequest(depositTest);
        System.out.println("Success2");
    }
}
