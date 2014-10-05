import java.io.*;
public class ExecProcess{
	private static void printLines(String name, InputStream ins) throws Exception{
		String line = null;
		//InputStream a = ;
		BufferedReader in = new BufferedReader(new InputStreamReader(ins) {});
		while ((line = in.readLine()) != null){
			System.out.println(name + " " +line);
		}
                
	}
	
	private static int runProcess(String command) throws Exception {
		Process pro = Runtime.getRuntime().exec(command);
                //pro = Runtime.getRutime().exec(command);
		printLines(command + " stdout:", pro.getInputStream());
		printLines(command + " stderr:", pro.getErrorStream());
		return pro.exitValue();
	}

	public static void main(String[] args){
		try{
			//int k = runProcess("javac HelloWorld.java ");
			int k = runProcess("javac Server.java");
			if (k==0)
				k = runProcess("java Server HEAD 50001 HEAD 50002");
		}catch(Exception e){
			e.printStackTrace();
		}
	}	
}