
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GuiClient extends Application{


	Label mainMenuLabel, waitingRoomLabel, placeholder, onlineOrSingleLabel, opponentBoardTopLabel, opponentBoardBottomLabel;
	Button mainMenuPlayButton, onlineButton, singleplayerButton, switchToOpponentBoard, fireButton, backButton;
	Button[][] gridButtons;
	Button[][] opponentGridButtons;
	VBox mainMenuVBox, gameTypeVBox, topSideGameBoard, rightSideGameBoard, opponentBoardTopVBox, opponentBoardRightVBox, opponentBoardBottomVBox;
	HBox onlineOrSingleBox;
	HashMap<String, Scene> sceneMap;
	GridPane gameBoardGrid, opponentGrid;
	BorderPane gameBoardPane, opponentBoardPane, mainMenuPane, onlineOrSinglePane, waitingScenePane;
	Client clientConnection;

	Integer[][] shipBoard;

	boolean canPlace, opponentReady, playerReady, myTurn, targetSelected;

	ArrayList<Integer> shipSizeLeft = new ArrayList<>();


	public boolean checkIfHit(int row, int col){

		Button button = gridButtons[row][col];
		String currStyle = button.getStyle();

		if(currStyle.contains("-fx-background-color: grey")){
			return true;
		}
		else{
			return false;
		}

	}

	public void updateHit(int row, int col){

		Button button = opponentGridButtons[row][col];
		button.setStyle("-fx-background-color: orange");

	}

	public void updateMiss(int row, int col){

		Button button = opponentGridButtons[row][col];
		button.setStyle("-fx-background-color: yellow");

	}

	public void updatePersonalHit(int row, int col){

		Button button = gridButtons[row][col];
		button.setText("HIT!");
		button.setStyle("-fx-border-color: red;");
		button.setFont(Font.font("Impact", FontWeight.SEMI_BOLD , 15));

	}

	public void updatePersonalMiss(int row, int col){

		Button button = gridButtons[row][col];
		button.setStyle("-fx-background-color: yellow");

	}

	public void setupShipArray(){

		shipSizeLeft.add(2);
		shipSizeLeft.add(3);
		shipSizeLeft.add(3);
		shipSizeLeft.add(4);
		shipSizeLeft.add(5);

	}
	public boolean targetSelected(){

		for(int i = 0; i < 10; i++){
			for(int j = 0; j < 10; j++){

				Button currButton = opponentGridButtons[i][j];
				String currStyle = currButton.getStyle();

				if(currStyle.contains("-fx-background-color: red")){
					return true;
				}

			}
		}

		return false;

	}

	public void clearTarget(){
		for(int i = 0; i < 10; i++){
			for(int j = 0; j < 10; j++){
				Button currButton = opponentGridButtons[i][j];
				String currStyle= currButton.getStyle();
				if(currStyle.contains("-fx-background-color: red")){
					currButton.setStyle("-fx-background-color: lightblue; -fx-border-color: black;");
				}
			}
		}
	}

	public Message getTarget(){

		int row = -1;
		int col = -1;

		for(int i = 0; i < 10; i++){
			for(int j = 0; j < 10; j++){

				Button currButton = opponentGridButtons[i][j];
				String currStyle = currButton.getStyle();

				if(currStyle.contains("-fx-background-color: red")){
					row = i;
					col = j;
				}

			}
		}

		Message targetCheck = new Message();
		targetCheck.messageType = "TargetCheck";
		targetCheck.row = row;
		targetCheck.col = col;

		return targetCheck;
	}

	public void resetClientSetup(){

		// gridButtons, opponentGridButtons, shipBoard, shipsizeLeft
		shipSizeLeft.clear();
		setupShipArray();

		for(int i = 0; i < gridButtons.length; i++){
            Arrays.fill(gridButtons[i], null);
		}

		for(int i = 0; i < opponentGridButtons.length; i++){
			Arrays.fill(opponentGridButtons[i], null);
		}

		for(int i = 0; i < shipBoard.length; i++){
			Arrays.fill(shipBoard[i], null);
		}

	}
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		// setup
		setupShipArray();
		opponentReady = false;
		playerReady = false;
		myTurn = false;
		targetSelected = false;


		// Setting up Scene HashMap
		sceneMap = new HashMap<String, Scene>();
		sceneMap.put("MainMenu",  createMainMenuGui());
		sceneMap.put("OnlineOrSingle", createOnlineOrSingleScene());
		sceneMap.put("WaitingRoom", createWaitingScene());
		sceneMap.put("GameBoard", createGameBoardGui());
		sceneMap.put("OpponentBoard", createOpponentBoardGui());

		primaryStage.setScene(sceneMap.get("MainMenu"));
		primaryStage.setTitle("Battleship");
		primaryStage.show();

		fireButton.setOnAction(e -> {

			// check if a target is selected. -> already makes sure its valid
			if(targetSelected()){

				// shift turn to the opponent
				myTurn = false;
				fireButton.setDisable(true);
				opponentBoardTopLabel.setText("Waiting for opponent");
				placeholder.setText("WAITING FOR OPPONENT");

				Message changeTurn = new Message();
				changeTurn.messageType = "TurnChange";

				clientConnection.sendMessage(changeTurn);


				// check with the server if it is a hit or not
				Message checkHit = getTarget();
				clientConnection.sendMessage(checkHit);

			}
			else{
				opponentBoardTopLabel.setText("select a target before firing");
			}

		});

		switchToOpponentBoard.setOnAction(e -> {

			primaryStage.setScene(sceneMap.get("OpponentBoard"));

		});

		backButton.setOnAction(e -> {

			primaryStage.setScene(sceneMap.get("GameBoard"));

		});

		mainMenuPlayButton.setOnAction(e -> {

			primaryStage.setScene(sceneMap.get("OnlineOrSingle"));

		});

		onlineButton.setOnAction(e -> {

			// shift to waiting room scene
			primaryStage.setScene(sceneMap.get("WaitingRoom"));

			// connect to the server now & wait for another client.
			clientConnection = new Client(data->{
				Platform.runLater(()->{

					if(data instanceof  Message){

						if(((Message) data).messageType.equals("HitCheck")){

							int row = ((Message) data).row;
							int col = ((Message) data).col;

							boolean hit = checkIfHit(row, col);

							Message informServerIfHit = new Message();
							informServerIfHit.messageType = "Inform";

							if(hit){
								informServerIfHit.data = "hit";
								updatePersonalHit(row, col);
							}
							else{
								informServerIfHit.data = "miss";
								updatePersonalMiss(row, col);
							}

							clientConnection.sendMessage(informServerIfHit);

						}

					}

					if((data.toString()).equals("matchfound")){
						// change scene.
						System.out.println("Changing to gameboard");
						primaryStage.setScene(sceneMap.get("GameBoard"));
					}
					else if((data.toString()).equals("opponentready")){

						opponentReady = true;

						if(playerReady){
							// need to transition to new screen?
							if(myTurn){
								placeholder.setText("CHOOSE A TARGET ON OPPONENT BOARD");
							}
							else{
								placeholder.setText("WAITING FOR OPPONENT");
							}

							switchToOpponentBoard.setDisable(false);
							switchToOpponentBoard.setVisible(true);

						}

					}
					else if((data.toString()).equals("opponentdisconnected")){

						// go back to main menu & reset board
						resetClientSetup();
						primaryStage.setScene(sceneMap.get("MainMenu"));


					}
					else if((data.toString()).equals("firstturn")){

						myTurn = true;
						fireButton.setDisable(false);
						opponentBoardTopLabel.setText("Select a target and fire");

					}
					else if((data.toString()).equals("HIT")){
						Message temp = new Message();
						temp = getTarget();
						int row = temp.row;
						int col = temp.col;

						// mark hit
						updateHit(row, col);


					}
					else if((data.toString()).equals("MISS")){
						Message temp = new Message();
						temp = getTarget();
						int row = temp.row;
						int col = temp.col;

						// mark miss
						updateMiss(row, col);

						// shift turn to opponent.

					}
					else if((data.toString()).equals("ourturn")){

						myTurn = true;
						fireButton.setDisable(false);
						opponentBoardTopLabel.setText("Select a target and fire");
						placeholder.setText("CHOOSE A TARGET ON OPPONENT BOARD");

					}

				});
			});

			clientConnection.start();

		});
		
	}


	public class GridButtonClick implements EventHandler<ActionEvent>{

		@Override
		public void handle(ActionEvent actionEvent) {

			Button clickedButton = (Button) actionEvent.getSource();


			if(canPlace){

				int currShipSize = shipSizeLeft.remove(0);

				int rowIndex = GridPane.getRowIndex(clickedButton);
				int columnIndex = GridPane.getColumnIndex(clickedButton);

				for(int i = columnIndex; i < (currShipSize + columnIndex); i++){

					Button currentButton = gridButtons[rowIndex][i];
					currentButton.setStyle("-fx-background-color: grey; -fx-border-color: black;");
					shipBoard[rowIndex][i] = 1;

				}

				if(shipSizeLeft.isEmpty()){
					// all ships are placed.
					playerReady = true;
					placeholder.setText("Waiting for opponent..");

					Message myObj = new Message();
					myObj.messageType = "Status";
					myObj.data = "ready";

					clientConnection.sendMessage(myObj);

					if(opponentReady){
						if(myTurn){
							placeholder.setText("CHOOSE A TARGET ON OPPONENT BOARD");
						}
						else{
							placeholder.setText("WAITING FOR OPPONENT");
						}
						switchToOpponentBoard.setDisable(false);
						switchToOpponentBoard.setVisible(true);
					}


				}

			}


		}

	}

	public class GridMouseEnter implements EventHandler<MouseEvent> {

		@Override
		public void handle(MouseEvent mouseEvent) {

			Button hoveredButton = (Button) mouseEvent.getSource();
			int currentShipSize = -1;

			if(!shipSizeLeft.isEmpty()){
				currentShipSize = shipSizeLeft.get(0);
			}

			if(currentShipSize != -1){

				int rowIndex = GridPane.getRowIndex(hoveredButton);
				int columnIndex = GridPane.getColumnIndex(hoveredButton);

				canPlace = true;

				if(columnIndex + currentShipSize > 10){
					canPlace = false;
				}
				else{

					// Check if we can place here.
					for(int i = columnIndex; i < (currentShipSize + columnIndex); i++){

						Button currentButton = gridButtons[rowIndex][i];
						String style = currentButton.getStyle();

						if(style.contains("-fx-background-color: grey; -fx-border-color: black;")){

							canPlace = false;

						}

					}

				}


				if(canPlace){
					// make it green
					for(int i = columnIndex; i < (currentShipSize + columnIndex); i++){
						Button currentButton = gridButtons[rowIndex][i];
						currentButton.setStyle("-fx-background-color: lightgreen; -fx-border-color: black;");
					}

				}
				else{
					// make it red
					for(int i = columnIndex; i < (currentShipSize + columnIndex); i++){
						Button currentButton = gridButtons[rowIndex][i];
						currentButton.setStyle("-fx-background-color: red; -fx-border-color: black;");
					}

				}


			}


		}
	}

	public class GridMouseLeave implements EventHandler<MouseEvent>{

		@Override
		public void handle(MouseEvent mouseEvent) {
			Button clickedButton = (Button) mouseEvent.getSource();

			for(int i = 0; i < 10; i++){
				for(int j = 0; j < 10; j++){

					Button currButton = gridButtons[i][j];
					String style = currButton.getStyle();

					if(shipBoard[i][j] == 1){
						// ship goes here.
						if((currButton.getText()).equals("HIT!")){
							currButton.setStyle("-fx-background-color: grey; -fx-border-color: red;");
						}
						else{
							currButton.setStyle("-fx-background-color: grey; -fx-border-color: black;");
						}
					}
					else if(!style.contains("-fx-background-color: yellow")){
						currButton.setStyle("-fx-background-color: lightblue; -fx-border-color: black;");
					}

				}
			}


		}
	}



	public class OpponentGridMouseClick implements EventHandler<ActionEvent>{
		@Override
		public void handle(ActionEvent actionEvent) {

			Button button = (Button) actionEvent.getSource();
			String currStyle = button.getStyle();

			// already guessed it
			if(currStyle.contains("-fx-background-color: orange") || currStyle.contains("-fx-background-color: yellow")){
				opponentBoardTopLabel.setText("Already guessed that target");
				return;
			}

			if(targetSelected()){

				// clear that target and choose another one.
				clearTarget();
				button.setStyle("-fx-background-color: red; -fx-border-color: black;");

			}
			else{

				button.setStyle("-fx-background-color: red; -fx-border-color: black;");

			}

		}
	}

	public class OpponentGridMouseEnter implements EventHandler<MouseEvent>{
		@Override
		public void handle(MouseEvent mouseEvent) {

			Button button = (Button) mouseEvent.getSource();

			String currStyle = button.getStyle();


			if(!currStyle.contains("-fx-background-color: red") && !currStyle.contains("-fx-background-color: orange") && !currStyle.contains("-fx-background-color: yellow")){
				button.setStyle("-fx-background-color: grey; -fx-border-color: black;");
			}

		}
	}

	public class OpponentGridMouseLeave implements EventHandler<MouseEvent>{
		@Override
		public void handle(MouseEvent mouseEvent) {

			Button button = (Button) mouseEvent.getSource();
			String currStyle = button.getStyle();


			if(!currStyle.contains("-fx-background-color: red") && !currStyle.contains("-fx-background-color: orange") && !currStyle.contains("-fx-background-color: yellow")){
				button.setStyle("-fx-background-color: lightblue; -fx-border-color: black;");
			}

		}
	}


	public Scene createOpponentBoardGui(){

		opponentGrid = new GridPane();
		opponentGrid.setPadding(new Insets(10));
		opponentGrid.setHgap(5);
		opponentGrid.setVgap(5);


		opponentGridButtons = new Button[10][10];

		for(int i = 0; i < 10; i++){

			for(int j = 0; j < 10; j++){

				Button button = new Button();
				button.setPrefSize(50, 50);
				button.setStyle("-fx-background-color: lightblue; -fx-border-color: black;");

				opponentGridButtons[i][j] = button;
				opponentGrid.add(button, j, i);

				button.setOnAction(new OpponentGridMouseClick());
				button.setOnMouseEntered(new OpponentGridMouseEnter());
				button.setOnMouseExited(new OpponentGridMouseLeave());

			}

		}

		opponentGrid.setAlignment(Pos.CENTER);

		fireButton = new Button("FIRE");
		fireButton.setMinWidth(150);
		fireButton.setMinHeight(50);
		fireButton.setFont(Font.font("Impact", FontWeight.SEMI_BOLD , 25));
		fireButton.setStyle("-fx-base: Orange");
		fireButton.setAlignment(Pos.CENTER);

		backButton = new Button("BACK");
		backButton.setMinWidth(150);
		backButton.setMinHeight(50);
		backButton.setFont(Font.font("Impact", FontWeight.SEMI_BOLD , 25));
		backButton.setStyle("-fx-base: yellow");
		backButton.setAlignment(Pos.CENTER);

		opponentBoardRightVBox = new VBox(30, fireButton, backButton);
		opponentBoardRightVBox.setAlignment(Pos.CENTER);

		System.out.println("MyTurn: " + myTurn);

		opponentBoardTopLabel = new Label("");
		opponentBoardTopLabel.setAlignment(Pos.CENTER);
		opponentBoardTopLabel.setStyle("-fx-text-fill: white");
		opponentBoardTopLabel.setFont(Font.font("Impact", 60));
		opponentBoardTopLabel.setAlignment(Pos.CENTER);

		opponentBoardTopVBox = new VBox(0, opponentBoardTopLabel);
		opponentBoardTopVBox.setAlignment(Pos.CENTER);

		if(myTurn){
			fireButton.setDisable(false);
			opponentBoardTopLabel.setText("Select a target and fire");
		}
		else{
			fireButton.setDisable(true);
			opponentBoardTopLabel.setText("Waiting for opponent");
		}

		opponentBoardBottomLabel = new Label("orange = hit, yellow = miss");
		opponentBoardBottomLabel.setAlignment(Pos.CENTER);
		opponentBoardBottomLabel.setStyle("-fx-text-fill: white");
		opponentBoardBottomLabel.setFont(Font.font("Impact", 40));
		opponentBoardBottomLabel.setAlignment(Pos.CENTER);

		opponentBoardBottomVBox = new VBox(0, opponentBoardBottomLabel);
		opponentBoardBottomVBox.setAlignment(Pos.CENTER);


		opponentBoardPane = new BorderPane();
		opponentBoardPane.setPadding(new Insets(20));
		opponentBoardPane.setLeft(opponentGrid);
		opponentBoardPane.setRight(opponentBoardRightVBox);
		opponentBoardPane.setTop(opponentBoardTopVBox);
		opponentBoardPane.setBottom(opponentBoardBottomVBox);

		Image mainMenuBackground = new Image("boardBackground.jpg");
		BackgroundImage background = new BackgroundImage(mainMenuBackground, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,
				BackgroundSize.DEFAULT);

		opponentBoardPane.setBackground(new Background(background));

		return new Scene(opponentBoardPane, 800, 800);
	}



	public Scene createGameBoardGui(){

		gameBoardGrid = new GridPane();
		gameBoardGrid.setPadding(new Insets(10));
		gameBoardGrid.setHgap(5);
		gameBoardGrid.setVgap(5);

		gridButtons = new Button[10][10];
		shipBoard = new Integer[10][10];

		switchToOpponentBoard = new Button("Opponent");
		switchToOpponentBoard.setDisable(true);
		switchToOpponentBoard.setVisible(false);

		switchToOpponentBoard.setMinWidth(150);
		switchToOpponentBoard.setMinHeight(50);
		switchToOpponentBoard.setFont(Font.font("Impact", FontWeight.SEMI_BOLD , 25));
		switchToOpponentBoard.setStyle("-fx-base: Orange");
		switchToOpponentBoard.setAlignment(Pos.CENTER);

		rightSideGameBoard = new VBox(0, switchToOpponentBoard);
		rightSideGameBoard.setAlignment(Pos.CENTER);

		for(int i = 0; i < 10; i++){

			for(int j = 0; j < 10; j++){

				Button button = new Button();
				button.setPrefSize(50, 50);
				button.setStyle("-fx-background-color: lightblue; -fx-border-color: black;");

				gridButtons[i][j] = button;
				shipBoard[i][j] = 0;
				gameBoardGrid.add(button, j, i);

				button.setOnAction(new GridButtonClick());
				button.setOnMouseEntered(new GridMouseEnter());
				button.setOnMouseExited(new GridMouseLeave());

			}

		}

		gameBoardGrid.setAlignment(Pos.CENTER);


		placeholder = new Label("Please place your ships");
		placeholder.setStyle("-fx-text-fill: white");
		placeholder.setFont(Font.font("Impact", 35));
		placeholder.setAlignment(Pos.CENTER);

		topSideGameBoard = new VBox(50, placeholder);
		topSideGameBoard.setAlignment(Pos.CENTER);

		gameBoardPane = new BorderPane();
		gameBoardPane.setPadding(new Insets(20));
		gameBoardPane.setLeft(gameBoardGrid);
		gameBoardPane.setTop(topSideGameBoard);
		gameBoardPane.setRight(rightSideGameBoard);

		Image mainMenuBackground = new Image("boardBackground.jpg");
		BackgroundImage background = new BackgroundImage(mainMenuBackground, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,
				BackgroundSize.DEFAULT);

		gameBoardPane.setBackground(new Background(background));

		return new Scene(gameBoardPane, 800, 800);

	}



	public Scene createMainMenuGui(){

		mainMenuLabel = new Label("BATTLESHIP");
		mainMenuLabel.setStyle("-fx-text-fill: white");
		mainMenuLabel.setFont(Font.font("Impact", 90));
		mainMenuLabel.setAlignment(Pos.CENTER);

		mainMenuPlayButton = new Button("PLAY");
		mainMenuPlayButton.setMinWidth(150);
		mainMenuPlayButton.setMinHeight(50);
		mainMenuPlayButton.setFont(Font.font("Impact", FontWeight.SEMI_BOLD , 25));
		mainMenuPlayButton.setStyle("-fx-base: Green");
		mainMenuPlayButton.setAlignment(Pos.CENTER);

		mainMenuVBox = new VBox(200, mainMenuLabel, mainMenuPlayButton);
		mainMenuVBox.setAlignment(Pos.CENTER);

		Image mainMenuBackground = new Image("background.png");
		BackgroundImage background = new BackgroundImage(mainMenuBackground, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,
				BackgroundSize.DEFAULT);


		mainMenuPane = new BorderPane();
		mainMenuPane.setCenter(mainMenuVBox);

		mainMenuPane.setBackground(new Background(background));

		return new Scene(mainMenuPane, 600, 600);

	}

	public Scene createOnlineOrSingleScene(){

		onlineButton = new Button("ONLINE");
		onlineButton.setMinWidth(200);
		onlineButton.setMinHeight(50);
		onlineButton.setFont(Font.font("Impact", FontWeight.SEMI_BOLD , 20));
		onlineButton.setStyle("-fx-base: Blue");
		onlineButton.setAlignment(Pos.CENTER);

		singleplayerButton = new Button("SINGLEPLAYER");
		singleplayerButton.setMinWidth(200);
		singleplayerButton.setMinHeight(50);
		singleplayerButton.setFont(Font.font("Impact", FontWeight.SEMI_BOLD , 20));
		singleplayerButton.setStyle("-fx-base: LightBlue");
		singleplayerButton.setAlignment(Pos.CENTER);

		gameTypeVBox = new VBox(50, onlineButton, singleplayerButton);
		gameTypeVBox.setAlignment(Pos.CENTER);

		onlineOrSingleLabel = new Label("Please Select a Gamemode");
		onlineOrSingleLabel.setAlignment(Pos.CENTER);
		onlineOrSingleLabel.setStyle("-fx-text-fill: white");
		onlineOrSingleLabel.setFont(Font.font("Impact", 50));

		onlineOrSingleBox = new HBox(10, onlineOrSingleLabel);
		onlineOrSingleBox.setAlignment(Pos.CENTER);

		onlineOrSinglePane = new BorderPane();
		onlineOrSinglePane.setTop(onlineOrSingleBox);
		onlineOrSinglePane.setCenter(gameTypeVBox);

		Image gameBackground = new Image("background.png");
		BackgroundImage background = new BackgroundImage(gameBackground, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,
				BackgroundSize.DEFAULT);

		onlineOrSinglePane.setBackground(new Background(background));

		return new Scene(onlineOrSinglePane, 600, 600);

	}

	public Scene createWaitingScene(){

		waitingRoomLabel = new Label("Waiting for match...");
		waitingRoomLabel.setStyle("-fx-text-fill: white");
		waitingRoomLabel.setFont(Font.font("Impact", 50));
		waitingRoomLabel.setAlignment(Pos.CENTER);

		waitingScenePane = new BorderPane();
		waitingScenePane.setCenter(waitingRoomLabel);

		Image gameBackground = new Image("background.png");
		BackgroundImage background = new BackgroundImage(gameBackground, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,
				BackgroundSize.DEFAULT);

		waitingScenePane.setBackground(new Background(background));

		return new Scene(waitingScenePane, 600, 600);

	}

}
