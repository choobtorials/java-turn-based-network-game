import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.awt.event.*;
import java.util.ArrayList;

/**
* The Player class contains the GUI code for this simple turn-based game.
* It also contains an inner class called ClientSideConnection that
* connects to the game server. 
* 
* This is the code from the video:
* Creating a simple turn-based network game in Java (https://youtu.be/HQoWN28H80w).
*
* If you found this helpful, would really appreciate if you could support the channel
* by subscribing and sharing the link with others.
*
* YT: youtube.com/choobtorials
* Twitter: twitter.com/choobtorials
*
* @author choob
*/

public class Player {
    private int width;
    private int height;
    private JFrame frame;
    private Container contentPane;
    private JTextArea message;
    private int playerID;
    private int otherPlayer;
    private int numValues;
    private int[] values;
    private ArrayList<JButton> buttons;
    private int maxTurns;
    private int turnsMade;
    private int myPoints;
    private int enemyPoints;
    private boolean buttonsEnabled;

    private ClientSideConnection csc;

    public Player(int w, int h) {
        width = w;
        height = h;
        frame = new JFrame();
        contentPane = frame.getContentPane();
        message = new JTextArea();
        buttons = new ArrayList<>();
        turnsMade = 0;
        myPoints = 0;
        enemyPoints = 0;
    }

    public void setUpGUI() {
        frame.setSize(width, height);
        frame.setTitle("Player #" + playerID);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        contentPane.setLayout(new GridLayout(1, numValues + 1));
        message.setText("Creating a simple turn-based game in Java.");
        message.setWrapStyleWord(true);
        message.setLineWrap(true);
        message.setEditable(false);
        contentPane.add(message);
        for(int i = 1; i <= numValues; i++) {
            JButton b = new JButton(Integer.toString(i));
            buttons.add(b);
            contentPane.add(b);
        }

        if (playerID == 1) {
            message.setText("You are player #1. You go first.");
            otherPlayer = 2;
            buttonsEnabled = true;
        } else {
            message.setText("You are player #2. Wait for your turn");
            otherPlayer = 1;
            buttonsEnabled = false;
            Thread t = new Thread(new Runnable() {
                public void run() {
                    updateTurn();
                }
            });
            t.start();
        }

        toggleButtons();

        frame.setVisible(true);
    }

    public void initClient() {
        csc = new ClientSideConnection();
        csc.connectToServer();
    }

    public void setUpButtons() {
        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JButton b = (JButton) ae.getSource();
                int bNum = Integer.parseInt(b.getText());

                message.setText("You clicked button #" + bNum + ". Now wait for player #" + otherPlayer);
                turnsMade++;
                System.out.println("Turns made: " + turnsMade);

                buttonsEnabled = false;
                toggleButtons();

                myPoints += values[bNum - 1];
                System.out.println("My points: " + myPoints);
                csc.sendButtonNum(bNum);

                if (playerID == 2 && turnsMade == maxTurns) {
                    checkWinner();
                } else {
                    Thread t = new Thread(new Runnable() {
                        public void run() {
                            updateTurn();
                        }
                    });
                    t.start();
                }
            }
        };

        for(JButton b : buttons) {
            b.addActionListener(al);
        }
    }

    public void toggleButtons() {
        for(JButton b : buttons) {
            b.setEnabled(buttonsEnabled);
        }
    }

    public void updateTurn() {
        int n = csc.receiveButtonNum();
        message.setText("Your enemy clicked button #" + n + ". Your turn.");
        enemyPoints += values[n-1];
        //System.out.println("Your enemy has " + enemyPoints + " points.");
        if (playerID == 1 && turnsMade == maxTurns) {
            checkWinner();
        } else {
            buttonsEnabled = true;
        }
        toggleButtons();
    }

    private void checkWinner() {
        buttonsEnabled = false;
        if (myPoints > enemyPoints) {
            message.setText("YOU: " + myPoints + "\nENEMY: " + enemyPoints + "\nYou WON!\n");
        } else if (myPoints < enemyPoints) {
            message.setText("YOU: " + myPoints + "\nENEMY: " + enemyPoints + "\nYou LOST!\n");
        } else {
            message.setText("It's a tie! You both got " + myPoints + " points.");
        }

        csc.closeConnection();
    }

    // Client Connection Inner Class
    private class ClientSideConnection {

        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;

        public ClientSideConnection() {
            System.out.println("===== Client =====");

        }

        public void connectToServer() {
          try {
              socket = new Socket("localhost", 51734);
              dataIn = new DataInputStream(socket.getInputStream());
              dataOut = new DataOutputStream(socket.getOutputStream());
              playerID = dataIn.readInt();
              System.out.println("Connected to server as Player #" + playerID + ".");
              maxTurns = dataIn.readInt() / 2;
              System.out.println("maxTurns: " + maxTurns);
              numValues = dataIn.readInt();
              System.out.println("numValues: " + numValues);
              values = new int[numValues];
              for(int i = 0; i < numValues; i++) {
                  values[i] = dataIn.readInt();
                  System.out.println("Value #" + (i + 1) + " is " + values[i]);
              }

          } catch (IOException ex) {
              System.out.println("IO Exception from connectToServer method");
          }
        }

        public void sendButtonNum(int n) {
            try {
                dataOut.writeInt(n);
                dataOut.flush();
            } catch (IOException ex) {
                System.out.println("IOException from sendButtonNum() CSC");
            }
        }

        public int receiveButtonNum() {
            int n = -1;
            try {
                n = dataIn.readInt();
                System.out.println("Player #" + otherPlayer + " clicked button #" + n);
            } catch (IOException ex) {
                System.out.println("IOException from receiveButtonNum() CSC");
            }
            return n;
        }

        public void closeConnection() {
            try {
                socket.close();
                System.out.println("---CONNECTION CLOSED---");

            } catch (IOException ex) {
                System.out.println("IOException on closeConnection() CSC");
            }
        }
    }

    public static void main(String[] args) {
        Player p = new Player(500, 100);
        p.initClient();
        p.setUpGUI();
        p.setUpButtons();
    }
}
