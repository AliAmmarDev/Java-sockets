import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

  private Socket socket = null;
  private PrintWriter socketOutput = null;
  private BufferedReader socketInput = null;

  public void clientServerCommunication() {
    try {
      // try and create the socket
      socket = new Socket( "localhost", 8888 );
      // chain a writing stream
      socketOutput = new PrintWriter(socket.getOutputStream(), true);
      // chain a reading stream
      socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
	  catch (UnknownHostException e) {
      System.err.println("Don't know about host.\n");
      System.exit(1);
    }
	  catch(IOException e){
      System.err.println("Couldn't get I/O for the connection to host.\n");
      System.exit(1);
    }
	  // chain a reader from the keyboard
    BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
    String fromServer;
    String fromUser;
    // communication loop
    try {
      // read from server
      while ((fromServer = socketInput.readLine()) != null) {
        // echo server string
        System.out.println("Server: " + fromServer);
        //exit if user enters bye
        if (fromServer.equals("bye")){
          break;
        }
        // client types in response
        //scanner input
        Scanner in = new Scanner(System.in);

        fromUser = in.nextLine();
        //if input exists
        if (fromUser != null) {
          String command = null;
          String parameter = null;
          int tokenAmount = 0;
          //split input using space as delimiter
          String[] inputTokens = fromUser.split(" ");
          tokenAmount = inputTokens.length;
          //set first token to command
          command = inputTokens[0];
          if(inputTokens.length > 1){
            //set second token as file parameter
            parameter = inputTokens[1];
          }
          // echo client string
          System.out.println("Client: " + fromUser);
          // write to server
          socketOutput.println(fromUser);
          //if user requests to send a file
          if(command.equals("get") && tokenAmount == 2){
            try{
              int fileBytes;
              int temp = 0;
              //open input stream
              InputStream inputStream = socket.getInputStream();
              //open file output stream
              FileOutputStream fileOutputStream = new FileOutputStream("./clientFiles/" + parameter);
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
            }
            catch (FileNotFoundException e){
              System.err.println("File not found on client");
            }
          }
          //if user requests to receive a file
          if(command.equals("put") && tokenAmount == 2){
            try{
              //create a new file
              File file = new File ("./clientFiles/" + parameter);
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
              System.out.println(parameter + " sent");
            }
            catch (FileNotFoundException e){
              System.err.println("File not found on client");
            }
          }
        }
      }
      //close streams and socket
      socketOutput.close();
      socketInput.close();
      stdIn.close();
      socket.close();
    }
    catch (IOException e) {
      System.err.println("I/O exception during execution\n");
      System.exit(1);
    }
  }
  public static void main(String[] args) {
    Client client = new Client();
    client.clientServerCommunication();
  }
}
