import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.function.Consumer;



public class Client extends Thread{

	
	Socket socketClient;
	
	ObjectOutputStream out;
	ObjectInputStream in;

	private Consumer<Serializable> callback;

	Client(Consumer<Serializable> call){
		callback = call;
	}

	public void sendMessage(Message messageObj){
		try{
			out.writeObject(messageObj);
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
	}

	
	public void run() {
		
		try {
			socketClient = new Socket("127.0.0.1",5555);
	    	out = new ObjectOutputStream(socketClient.getOutputStream());
	    	in = new ObjectInputStream(socketClient.getInputStream());
	    	socketClient.setTcpNoDelay(true);
		}
		catch(Exception e) {
		}


        while(true) {
			 
			try {

				Message messageObj = (Message) in.readObject();

				if((messageObj.messageType).equals("MatchFound")){

					// server found us a match
					System.out.println("match found!");
					callback.accept("matchfound");

				}
				else if((messageObj.messageType).equals("Status")){

					if((messageObj.data).equals("ready")){
						// opponent is ready.
						callback.accept("opponentready");
					}

				}
				else if((messageObj.messageType).equals("OpponentDisconnect")){

					callback.accept("opponentdisconnected");

				}
				else if((messageObj.messageType).equals("FirstTurn")){

					if((messageObj.data).equals("true")){
						callback.accept("firstturn");
					}

				}
				else if((messageObj.messageType).equals("HitCheck")){

					// need to check opponent move.
					int row = messageObj.row;
					int col = messageObj.col;

					Message hitCheck = new Message();
					hitCheck.messageType = "HitCheck";
					hitCheck.row = row;
					hitCheck.col = col;

					callback.accept(hitCheck);

				}
				else if((messageObj.messageType).equals("MoveResult")){

					String result = messageObj.data;

					if(result.equals("hit")){
						callback.accept("HIT");
					}
					else{
						callback.accept("MISS");
					}

				}
				else if((messageObj.messageType).equals("YourTurn")){

					// this clients turn.
					callback.accept("ourturn");

				}


			}
			catch(Exception e) {
				System.out.println("Erorr in Client.java line 84");
			}
		}
	
    }



}
