import java.net.*;

public class Server {

    private static int myPort;
    private static int masterPort;
    private static String masterIP;
    private static boolean isHead;
    private static boolean isTail;
    private static int predecessorPort;
    private static int successorPort;

    public static boolean isHead() {
        return isHead;
    }

    public static void setHead(boolean Head) {
        isHead = Head;
    }

    public static boolean isTail() {
        return isTail;
    }

    public static void setTail(boolean Tail) {
        isTail = Tail;
    }

    public static int getPredecessorPort() {
        return predecessorPort;
    }

    public static void setPredecessorPort(int predecessorPort) {
        predecessorPort = predecessorPort;
    }

    public static int getSuccessorPort() {
        return successorPort;
    }

    public static void setSuccessorPort(int successorPort) {
        successorPort = successorPort;
    }

    public static int getMyPort() {
        return myPort;
    }

    public static void setMyPort(int myPort) {
        myPort = myPort;
    }

    /*
     * arg0 states if its head, tail or middle
     * arg1 states the port number to listen on
     * arg2 states the predecessor port number
     * arg3 states the successor port number
     */
    public static void main(String[] args) {
        System.out.println("arg0: "+ args[0] + ", args[1] : " + args[1] +  ", args[2] : " + args[2] +  ", args[3] : " + args[3]);
        if (args.length == 4) {
            if (args[0].equals("HEAD")) {
                setHead(true);
                setTail(false);
                setMyPort(Integer.parseInt(args[1]));
                setSuccessorPort(Integer.parseInt(args[3]));

            } else if (args[0].equals("TAIL")) {
                setTail(true);
                setHead(false);
                setMyPort(Integer.parseInt(args[1]));
                setPredecessorPort(Integer.parseInt(args[2]));
            } else {
                isHead = false;
                isTail = false;
                setMyPort(Integer.parseInt(args[1]));
                setPredecessorPort(Integer.parseInt(args[2]));
                setSuccessorPort(Integer.parseInt(args[2]));
            }
        }
        
        //ServerSocket serverSocket = new ServerSocket();
    }
}
