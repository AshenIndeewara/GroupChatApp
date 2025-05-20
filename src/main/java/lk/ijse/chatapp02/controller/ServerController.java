package lk.ijse.chatapp02.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerController {

    public TextField clientName;
    @FXML
    private ImageView displayImageView;

    @FXML
    private Button btnAddAnotherUser;

    @FXML
    private ImageView imageView;

    @FXML
    private TextArea txtArea;



    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();
    private File file;
    private ByteArrayOutputStream baos;


    public void initialize() {

        txtArea.setEditable(false);
        txtArea.setFocusTraversable(false);

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(5000);
                Platform.runLater(() -> txtArea.appendText("Server started on port 5000\n"));

                while (true) {
                    Socket socket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(socket);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();
                    Platform.runLater(() -> txtArea.appendText("New client connected\n"));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void enterBtn(KeyEvent keyEvent) throws IOException {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            openClientWindow();
        }
    }

    class ClientHandler implements Runnable {
        private Socket socket;
        private DataInputStream dis;
        private DataOutputStream dos;
        private String clientId;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                clientId = clientName.getText();
                dos.writeUTF(clientId);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendText(String sender, String text) {
            try {
                System.out.println("send text is working");
                dos.writeUTF("text");
                dos.writeUTF(sender + " : " + text);
                dos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendImage(String sender,byte[] imageBytes) throws IOException {
            dos.writeUTF("image");
            dos.writeUTF(sender);
            dos.writeInt(imageBytes.length);
            dos.write(imageBytes);
            dos.flush();
        }

        public void sendFile(String sender,String fileName, byte[] fileBytes) throws IOException {
            dos.writeUTF("file");
            dos.writeUTF(sender);
            dos.writeUTF(fileName);
            dos.writeInt(fileBytes.length);
            dos.write(fileBytes);
            dos.flush();
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String type = dis.readUTF();

                    switch (type) {
                        case "text" -> {
                            String text = dis.readUTF();
                            String fullMessage = clientId + " : " + text;

                            Platform.runLater(() -> txtArea.appendText(fullMessage + "\n"));
                            broadcastMessage(text, this);
                        }
                        case "image" -> {
                            int length = dis.readInt();
                            byte[] bytes = new byte[length];
                            dis.readFully(bytes);
                            Image image = new Image(new ByteArrayInputStream(bytes));

                            Platform.runLater(() -> {
                                displayImageView.setImage(image);
                                txtArea.appendText(clientId + " : [Image received]\n");
                            });
                            broadcastImage(bytes,this);
                        }
                        case "file" -> {
                            String fileName = dis.readUTF();
                            int length = dis.readInt();
                            byte[] bytes = new byte[length];
                            dis.readFully(bytes);

                            File received = new File("received_" + fileName);
                            try (FileOutputStream fos = new FileOutputStream(received)) {
                                fos.write(bytes);
                                Platform.runLater(() -> txtArea.appendText(clientId + " : [File received: " + fileName + "]\n"));

                                // Broadcast to other clients
                                broadcastFile(fileName, bytes, this);

                            }
                        }
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() -> txtArea.appendText(clientId + " disconnected\n"));
                clients.remove(this);
            }
        }
    }

    private void broadcastMessage(String message, ClientHandler sender) {
        System.out.println("broadcast is working");
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendText(sender.clientId, message);
            }
        }
    }

    private void broadcastImage(byte[] imageBytes, ClientHandler sender) {
        System.out.println("broadcast image is working");
        try{
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendImage(sender.clientId,imageBytes);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void broadcastFile(String fileName, byte[] fileBytes, ClientHandler sender) {
        System.out.println("broadcast file is working");
        try {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendFile(sender.clientId,fileName, fileBytes);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void ClickOnBtnAddAnotherUser(ActionEvent event) throws IOException {
        openClientWindow();
    }

    private void openClientWindow() throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/Client.fxml"));
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
    }
}
