import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.nio.file.FileAlreadyExistsException;

public class Server {
  public static void main(String[] args) throws IOException {
    ServerSocket server = null;
    ExecutorService service = null;
      // Try to open up the listening port on no. 8888
      try {
        server = new ServerSocket(8888);
      }
      catch (IOException e) {
        System.err.println("Could not listen on port: 8888.");
        System.exit(-1);
      }
      // Initialise the executor.
      //create a max threadpool of 10 clients.
      service = Executors.newFixedThreadPool(10);
      // For each new client, submit a new handler to the thread pool.
      while( true ){
        Socket client = server.accept();
        service.submit( new ClientHandler(client) );
      }
    }
}

class ClientHandler extends Thread {
  private Socket socket = null;
  public ClientHandler(Socket socket) {
    super("ClientHandler");
    this.socket = socket;
  }
  public void run() {
    try {
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
      BufferedReader in = new BufferedReader(
                          new InputStreamReader(
                          socket.getInputStream()));
      InetAddress inet = socket.getInetAddress();
      Date date = new Date();
      System.out.println("\nDate " + date.toString() );
      System.out.println("Connection made from " + inet.getHostName() );
      String inputLine, outputLine;

      //create new protocal instance
      ServerProtocol serverProtocol = new ServerProtocol();
      outputLine = serverProtocol.processInput(null, socket);
      out.println(outputLine);
      //format of date for logging
      DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
      //while user input exists
      while ((inputLine = in.readLine()) != null) {
        try {
          //log information to text file
          FileWriter writer = new FileWriter("log.txt", true);
          writer.write(dateFormat.format(date) + " : ");
          writer.write(inet.getHostAddress() + " : ");
          writer.write(inputLine);
          writer.write("\n");
          //close writer
          writer.close();
        }
        catch (IOException e) {
          System.err.println("error logging to file");
        }
        //exit if user enters bye
	      if(outputLine.equals("bye")){
          break;
        }
        //process user input
        outputLine = serverProtocol.processInput(inputLine, socket);
        out.println(outputLine);
      }
      //close streams and socket
      out.close();
      in.close();
      socket.close();
    }
    catch (IOException e) {
      System.err.println("error in client request");
    }
  }
}


class ServerProtocol {
  private static final int WAITING = 0;
  private static final int COMMANDS = 1;
  private static final int LEAVING = 2;
  private int state = WAITING;
  String fromServer;
  String fromUser;
  int bytesRead;
  int current = 0;

  public String processInput(String theInput, Socket socket) throws IOException {
    String theOutput = null;
    String command = null;
    String parameter = null;
    int tokenAmount = 0;
    if(theInput != null){
      String[] inputTokens = theInput.split(" ");
      tokenAmount = inputTokens.length;
      command = inputTokens[0];
      if(inputTokens.length > 1){
        parameter = inputTokens[1];
      }
    }
	  if (state == WAITING) {
      theOutput = "Connection active";
      state = COMMANDS;
    }
	  else if (state == COMMANDS){
      //if user requests list
      if (command.equalsIgnoreCase("list")) {
        File directory = new File("serverFiles");
        File[] fileList = directory.listFiles();
        int fileAmount = fileList.length;
        String fileListString = "";
        //iterate through files in directory
        for (int i = 0; i < fileAmount; i++){
          //if item is a directory
          if (fileList[i].isDirectory()){
            //change color of the text to identify its a folder
            fileListString += "\033[01;32m" + fileList[i].getName() + "\033[0m ";
          }
          //for files
          else{
            fileListString += fileList[i].getName() + " ";
          }
        }
        theOutput = fileListString;
      }
      //if user requests to receive file
      else if(command.equalsIgnoreCase("get") && tokenAmount == 2){
        try{
          //create a new file
          File file = new File ("./serverFiles/" + parameter);
          //create buffer array
          byte [] bufferArray  = new byte [(int)file.length()];
          //open file output stream
          FileInputStream fileInputStream = new FileInputStream(file);
          //open buffer input stream
          BufferedInputStream bufferInStream = new BufferedInputStream(fileInputStream);
          bufferInStream.read(bufferArray,0,bufferArray.length);
          //open output stream
          OutputStream outputStream = socket.getOutputStream();
          //write content to buffer
          outputStream.write(bufferArray,0,bufferArray.length);
          //clear buffer
          outputStream.flush();
          return parameter + " sent";
        }
        catch (FileNotFoundException e){
          System.err.println("File not found on server");
          return "File not found on server";
        }
      }
      //if user requests to send file
      else if(command.equalsIgnoreCase("put") && tokenAmount == 2){
        try{
          int fileBytes;
          int temp = 0;
          //open input stream
          InputStream inputStream = socket.getInputStream();
          //open file output stream
          FileOutputStream fileOutputStream = new FileOutputStream("./serverFiles/" + parameter);
          //create buffer array
          byte [] bufferArray  = new byte [32 * 1024];
          //open buffer output stream
          BufferedOutputStream bufferOutputStream = new BufferedOutputStream(fileOutputStream);
          fileBytes = inputStream.read(bufferArray,0,bufferArray.length);
          //wrtie content to butter
          bufferOutputStream.write(bufferArray, 0 , fileBytes);
          //clear buffer output stream
          bufferOutputStream.flush();
          //close stream
          fileOutputStream.close();
          return parameter + " received";
        }
        catch (FileAlreadyExistsException e){
          System.err.println("File already exists");
          return "File already exists";
        }
      }
      //if user requests to leave the program
      else if(command.equalsIgnoreCase("bye")){
        state = LEAVING;
        theOutput = "bye";
      }
      //any other command that is not recognised
      else{
        theOutput = "request not recognised";
      }
    }
	  return theOutput;
  }
}
