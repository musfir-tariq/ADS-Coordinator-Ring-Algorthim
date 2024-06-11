
import java.net.*;
import java.util.Objects;
import java.util.Scanner;
import java.io.*;

public class ProcessNode {

    // args[0]: node id
    // args[1]: port
    // args[2]: neighbour server
    // args[3]: neighbour port

    public static final String ELECTION_MSG = "Election";
    public static final String ELECTED_MSG = "Elected";

    public static void main(String args[]) {

        Node node = new Node(
                Integer.parseInt(args[0]),
                Integer.parseInt(args[1]),
                args[2],
                Integer.parseInt(args[3]));

        node.start();

        UserInputThread userInput = new UserInputThread(node);
        userInput.start();

    }// main

    static class Node extends Thread {
        private int nodeId;
        private int serverPort;
        private String neighbourServer;
        private int neighbourPort;
        private Boolean isParticipant;
        ObjectInputStream in;
        ObjectOutputStream out;
        Socket nextSocket;
        Socket prevSocket;
        ServerSocket listenSocket;

        public Node(int nodeId, int serverPort, String neighbourServer, int neighbourPort) {
            this.nodeId = nodeId;
            this.serverPort = serverPort;
            this.neighbourServer = neighbourServer;
            this.neighbourPort = neighbourPort;
            this.isParticipant = false;
            this.nextSocket = null;
            this.in = null;
            this.out = null;
            listenSocket = null;

        }

        public int getNeighbourPort() {
            return this.neighbourPort;
        }

        public String getNeighbourServer() {
            return this.neighbourServer;
        }

        public int getNodeId() {
            return this.nodeId;
        }

        public void setNodeId(int newNodeId) {
            this.nodeId = newNodeId;
        }

        public Boolean getIsParticipant() {
            return this.isParticipant;
        }

        public void setIsParticipant(Boolean val) {
            this.isParticipant = val;
        }

        public ServerSocket getServerSocket() {
            if (listenSocket == null) {
                try {
                    listenSocket = new ServerSocket(serverPort);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return listenSocket;
        }

        public ObjectInputStream getInStream() {
            if (prevSocket == null) {
                try {
                    prevSocket = getServerSocket().accept();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (in == null) {
                try {
                    in = new ObjectInputStream(prevSocket.getInputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return in;
        }

        public ObjectOutputStream getOutStream() {
            if (nextSocket == null) {
                try {
                    nextSocket = new Socket(getNeighbourServer(), getNeighbourPort());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (out == null) {
                try {
                    out = new ObjectOutputStream(nextSocket.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return out;
        }

        @Override
        public void run() {
            System.out.println("Node " + nodeId + " is up");

            while (true) {
                try {
                    ObjectInputStream inputStream = getInStream();
                    ObjectOutputStream outputStream = getOutStream();

                    TokenMessage receivedMessage = (TokenMessage) inputStream.readObject();
                    System.out.println("Received:" + receivedMessage.getText() + " " + receivedMessage.getValue());

                    if (Objects.equals(receivedMessage.getText(), ProcessNode.ELECTION_MSG)) {

                        String messageText = "";
                        int messageVal = -1;

                        if (receivedMessage.getValue() > getNodeId()) {
                            messageText = ProcessNode.ELECTION_MSG;
                            messageVal = receivedMessage.getValue();
                            setIsParticipant(true);
                        }

                        if (receivedMessage.getValue() < getNodeId()) {
                            if (!getIsParticipant()) {
                                messageText = ProcessNode.ELECTION_MSG;
                                messageVal = getNodeId();
                                setIsParticipant(true);
                            }
                        }

                        if (receivedMessage.getValue() == getNodeId()) {
                            setIsParticipant(false);
                            messageText = ProcessNode.ELECTED_MSG;
                            messageVal = getNodeId();
                        }

                        TokenMessage forwardMessage = new TokenMessage(messageText, messageVal);

                        System.out.println("Sent:" + forwardMessage.getText() + " " + forwardMessage.getValue());

                        outputStream.writeObject(forwardMessage);
                    }

                    if (receivedMessage.getValue() != getNodeId()
                            && Objects.equals(receivedMessage.getText(), ProcessNode.ELECTED_MSG)) {

                        setIsParticipant(false);

                        TokenMessage forwardMessage = new TokenMessage(ProcessNode.ELECTED_MSG,
                                receivedMessage.getValue());

                        outputStream.writeObject(forwardMessage);

                        System.out
                                .println("The Elected Node: " + forwardMessage.getText() + " "
                                        + forwardMessage.getValue());
                    }

                } catch (EOFException e) {
                    System.out.println(" EOF:" + e.getMessage());
                } catch (IOException e) {
                    System.out.println(" Node IO:" + e.getMessage());
                    e.printStackTrace();
                    break;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class UserInputThread extends Thread {
        private Node node;

        public UserInputThread(Node aNode) {
            this.node = aNode;
        }

        @Override
        public void run() {
            @SuppressWarnings("resource")
            Scanner scanner = new Scanner(System.in);
            while (true) {
                try {
                    String userInput = scanner.nextLine();

                    if (Objects.equals(userInput, ELECTION_MSG)) {

                        if (Objects.equals(this.node.nextSocket, null)) {
                            this.node.nextSocket = new Socket(this.node.getNeighbourServer(),
                                    this.node.getNeighbourPort());
                        }

                        ObjectOutputStream outputStream = this.node.getOutStream();

                        this.node.setIsParticipant(true);

                        TokenMessage message = new TokenMessage(ELECTION_MSG,
                                this.node.nodeId);

                        outputStream.writeObject(message);
                    } else {
                        System.out.println("New Id: " + userInput);
                        this.node.setNodeId(Integer.parseInt(userInput));
                    }

                } catch (NumberFormatException e) {
                    System.out.println("Wrong id type. " + e.getMessage());
                } catch (UnknownHostException e) {
                    System.out.println(" Sock:" + e.getMessage());
                } catch (EOFException e) {
                    System.out.println(" EOF:" + e.getMessage());
                } catch (IOException e) {
                    System.out.println(" UserInputThread IO:" + e.getMessage());
                }
            }
        }
    }
}

class TokenMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String text;
    private int value;

    public TokenMessage(String text, int value) {
        this.text = text;
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public int getValue() {
        return value;
    }
}