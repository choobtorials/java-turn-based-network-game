import java.io.*;
import java.net.*;

public class GameServer {

    private ServerSocket ss;
    private int numPlayers;
    private ServerSideConnection player1;
    private ServerSideConnection player2;
    private int turnsMade;
    private final int maxTurns = 6;
    private final int numValues = 4;
    private int[] values;
    private int player1ButtonNum;
    private int player2ButtonNum;
    private boolean endGame;

    public GameServer() {
        System.out.println("===== Game Server =====");
        numPlayers = 0;
        turnsMade = 0;
        endGame = false;
        values = new int[numValues];

        for (int i = 0; i < numValues; i++) {
            values[i] = (int) Math.ceil(Math.random() * 100);
            System.out.println("Value #" + (i + 1) + " is " + values[i]);
        }

        try {
            ss = new ServerSocket(51734);
        } catch (IOException ex) {
            System.out.println("IOException from GameServer Constructor");
        }
    }

    public void acceptConnections() {
        try {
            System.out.println("Waiting for connections...");
            while (numPlayers < 2) {
                Socket s = ss.accept();
                numPlayers++;
                System.out.println("Player #" + numPlayers + " has connected.");
                ServerSideConnection ssc = new ServerSideConnection(s, numPlayers);
                if (numPlayers == 1) {
                    player1 = ssc;
                } else {
                    player2 = ssc;
                }
                Thread t = new Thread(ssc);
                t.start();
            }
            System.out.println("We now have 2 players. No longer accepting connections.");
        } catch (IOException ex) {
            System.out.println("IOException from acceptConnections()");
        }
    }

    private class ServerSideConnection implements Runnable {

        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;
        private int playerID;

        public ServerSideConnection(Socket s, int id) {
            socket = s;
            playerID = id;
            try {
                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());
            } catch (IOException ex) {
                System.out.println("IOException from SSC constructor");
            }
        }

        public void run() {
            try {
                dataOut.writeInt(playerID);
                dataOut.writeInt(maxTurns);
                dataOut.writeInt(numValues);
                for(int v : values) {
                    dataOut.writeInt(v);
                }
                dataOut.flush();

                while (true) {
                    //System.out.println("hi");
                    if (playerID == 1 && turnsMade < maxTurns - 1) {
                        player1ButtonNum = dataIn.readInt();
                        System.out.println("Player 1 clicked button #" + player1ButtonNum);
                        player2.sendButtonNum(player1ButtonNum);
                    } else if (playerID == 2) {
                        player2ButtonNum = dataIn.readInt();
                        System.out.println("Player 2 clicked button #" + player2ButtonNum);
                        player1.sendButtonNum(player2ButtonNum);
                    }
                    if(turnsMade != maxTurns) {
                        turnsMade++;
                    }  else {
                        break;
                    }
                }

                //System.out.println("Player #" + playerID + " has broken out of the loop.");

                if(playerID == 2) {
                    player1.closeConnection();
                    player2.closeConnection();
                }


            } catch (IOException ex) {
                System.out.println("IOException from run() SSC from Player #" + playerID);
            }
        }

        public void sendButtonNum(int n) {
            try {
                dataOut.writeInt(n);
                dataOut.flush();
            } catch (IOException ex) {
                System.out.println("IOException from sendButtonNum() ssc");
            }
        }

        public void closeConnection() {
            try {
                socket.close();
                System.out.println("Player #" + playerID + " connection closed.");
            } catch (IOException ex) {
                System.out.println("IOException on closeConnect() SSC");
            }
        }

    }

    public static void main(String[] args) {
        GameServer gs = new GameServer();
        gs.acceptConnections();
    }

}
