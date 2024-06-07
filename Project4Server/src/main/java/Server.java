import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Consumer;


import javafx.application.Platform;
import javafx.scene.control.ListView;


public class Server{


	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<>();

	ArrayList<ClientThread> waitingList = new ArrayList<>();

	ArrayList<ArrayList<ClientThread>> activeMatches = new ArrayList<>();
	TheServer server;

	public void printClients(){
		for(int i = 0; i < clients.size(); i++){
			System.out.println(clients.get(i).count);
		}
	}

	public void printWaitingList(){
		for(int i = 0; i < waitingList.size(); i++){
			System.out.println(waitingList.get(i).count);
		}
	}

	public void printActiveMatches(){
		for(int i = 0; i < activeMatches.size(); i++){

			System.out.println("Match Number " + i + ":");

			for(int j = 0; j < activeMatches.get(i).size(); j++){
				System.out.print(activeMatches.get(i).get(j).count);
			}

			System.out.println();

		}
	}

	public boolean inMatch(ClientThread client){

		for(int i = 0; i < activeMatches.size(); i++){

			if(activeMatches.get(i).get(0).count == client.count || activeMatches.get(i).get(1).count == client.count){
				return true;
			}

		}

		return false;

	}

	public boolean inWaitingList(ClientThread client){

		for(int i = 0; i < waitingList.size(); i++){

			if(waitingList.get(i).count == client.count){
				return true;
			}

		}

		return false;

	}


	public ClientThread findOpponent(ClientThread client){

		for(int i = 0; i < activeMatches.size(); i++){

			if(activeMatches.get(i).get(0).count == client.count){
				return activeMatches.get(i).get(1);
			}
			else if(activeMatches.get(i).get(1).count == client.count){
				return activeMatches.get(i).get(0);
			}

		}

		return null;

	}

	public void removeFromActiveMatch(ClientThread client){

		int matchIndexToRemove = -1;

		for(int i = 0; i < activeMatches.size(); i++){

			if(activeMatches.get(i).get(0).count == client.count || activeMatches.get(i).get(1).count == client.count){
				matchIndexToRemove = i;
			}

		}

		if(matchIndexToRemove != -1){
			activeMatches.remove(matchIndexToRemove);
		}

	}
	
	Server(){

		server = new TheServer();
		server.start();

	}

	public void matchPlayers(){

		if(waitingList.size() >= 2){

			// we can create a match.
			ClientThread player1 = waitingList.remove(0);
			ClientThread player2 = waitingList.remove(0);

			ArrayList<ClientThread> match = new ArrayList<>();
			match.add(player1);
			match.add(player2);

			activeMatches.add(match);

			// send message back to client that match was found.
			Message messageObj = new Message();
			messageObj.messageType = "MatchFound";
			messageObj.data = "";

			Message messageObj2 = new Message();
			messageObj2.messageType = "MatchFound";
			messageObj2.data = "";


			System.out.println("Match Found for: " + player1.count + " and " + player2.count);

			if(player1.out == null){
				System.out.println(player1.count + " out stream is null");
			}

			if(player2.out == null){
				System.out.println(player2.count + " out stream is null");
			}

			Message turnMessage = new Message();
			turnMessage.messageType = "FirstTurn";
			turnMessage.data = "true";

			try{

				player1.out.writeObject(messageObj);
				player2.out.writeObject(messageObj2);


				if(player1.count < player2.count){
					// player 1 goes first turn
					player1.out.writeObject(turnMessage);
				}
				else{
					// player 2 goes first turn
					player2.out.writeObject(turnMessage);
				}


			} catch(Exception e){
				System.out.println("error here " + e.getMessage());
			}

		}

	}
	
	
	public class TheServer extends Thread{
		
		public void run() {

			try(ServerSocket mySocket = new ServerSocket(5555);){

				System.out.println("Server is waiting for a client!");

				while(true){

					ClientThread newClient = new ClientThread(mySocket.accept(), count);
					newClient.start();

					while (newClient.out == null) {
						try {
							Thread.sleep(100); // Adjust the sleep duration as needed
						} catch (InterruptedException e) {
							System.out.println(e.getMessage());
						}
					}

					count++;
					clients.add(newClient);
					waitingList.add(newClient);

					System.out.println("WAITING LIST SIZE: " + waitingList.size());
					for(int i = 0; i < waitingList.size(); i++){
						System.out.println(i + " " + waitingList.get(i).count);
					}

					matchPlayers();

				}


			}catch(Exception e){}

		} // end of run

	} // end of TheServer
	

	class ClientThread extends Thread{
			
		
		Socket connection;
		int count;

		ObjectInputStream in;
		ObjectOutputStream out;

		boolean ready = false;


			
		ClientThread(Socket s, int count){
			this.connection = s;
			this.count = count;
		}

			
		public void run(){
					
			try {
				in = new ObjectInputStream(connection.getInputStream());
				out = new ObjectOutputStream(connection.getOutputStream());
				connection.setTcpNoDelay(true);
			}
			catch(Exception e) {
				System.out.println("Streams not open");
			}

			while(true) {

				try {

					Message messageObj = (Message) in.readObject();

					String messageType = messageObj.messageType;

					if(messageType.equals("Status")){
						// need to let other player know he is ready.
						Message newMessage = new Message();
						newMessage.messageType = "Status";
						newMessage.data = "ready";

						for(int i = 0; i < activeMatches.size(); i++){

							if(activeMatches.get(i).get(0).count == count){
								activeMatches.get(i).get(1).out.writeObject(newMessage);
							}
							else if(activeMatches.get(i).get(1).count == count){
								activeMatches.get(i).get(0).out.writeObject(newMessage);
							}

						}
					}
					else if(messageType.equals("TargetCheck")){

						// need to get his opponent connection socket
						ClientThread opponent = findOpponent(this);

						// ask opponent if guess is a hit.
						Message hitCheck = new Message();
						hitCheck.messageType = "HitCheck";
						hitCheck.row = messageObj.row;
						hitCheck.col = messageObj.col;

						opponent.out.writeObject(hitCheck);

					}
					else if(messageType.equals("Inform")){

						ClientThread opponent = findOpponent(this);

						Message informUserIfHitOrMiss = new Message();
						informUserIfHitOrMiss.messageType = "MoveResult";
						informUserIfHitOrMiss.data = messageObj.data;

						opponent.out.writeObject(informUserIfHitOrMiss);

					}
					else if(messageType.equals("TurnChange")){

						ClientThread opponent = findOpponent(this);

						Message yourTurn = new Message();
						yourTurn.messageType = "YourTurn";

						opponent.out.writeObject(yourTurn);

					}

				}
				catch(Exception e) {

					if(inMatch(this)){

						System.out.println("Client that left was in an active match");
						// send the player he is playing against back to main menu.
						ClientThread opponent = findOpponent(this);


						Message obj = new Message();
						obj.messageType = "OpponentDisconnect";
						obj.data = "";

						try {

							opponent.out.writeObject(obj);

						} catch (Exception exc){
							System.out.println("Here " + exc.getMessage());
						}

						// remove from active match.
						removeFromActiveMatch(this);
						clients.remove(opponent);

						System.out.println("Clients:");
						printClients();

					}

					clients.remove(this);


					break;
				}

			}
		}//end of run
			
			
	}//end of client thread
}


	
	

	
