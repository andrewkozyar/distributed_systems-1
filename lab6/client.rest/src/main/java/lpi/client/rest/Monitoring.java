package lpi.client.rest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TimerTask;

public class Monitoring extends TimerTask {

    private ArrayList<String> currUsers = new ArrayList<>();
    private ArrayList<String> oldUsers = new ArrayList<>();

    private Client client;  // jersey REST client
    private boolean isLoggedIn = false;
    private final String targetURL;
    private String username;

    Monitoring(Client client, String targetURL, String username) {
        this.targetURL = targetURL;
        this.client = client;
        this.isLoggedIn = true;
        this.username = username;
    }

    @Override
    public void run() {
        try {
            if(isLoggedIn)
                checkUsers();

            if(isLoggedIn)
                receiveMessages();

            if(isLoggedIn)
                receiveFiles();

        } catch (Exception e) {
            System.out.println(e.getMessage() + "\n");
        }
    }





    private void receiveMessages() {
        Response response = client.target(targetURL + "/" + this.username + "/messages")
                .request(MediaType.APPLICATION_JSON_TYPE).get(Response.class);

//        System.out.println("Debug response:");
//        String responseAsString = response.readEntity(String.class);
//        System.out.println(responseAsString);
//        System.out.println();

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
//            System.out.println(response.getStatus());
//            System.out.println("Error\n");
            return;
        }

        String jsonResponse = client.target(targetURL + "/" + this.username + "/messages")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(String.class);

        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray messageIds = new JSONArray();
            try {
                messageIds = (JSONArray) jsonObject.get("items");
            } catch (ClassCastException e) {
                messageIds.put(jsonObject.get("items"));
            }

            String endOfSentence = "message!";
            if (messageIds.length() > 1)
                endOfSentence = "messages!";

            System.out.println("You have " + messageIds.length() + " new " + endOfSentence);

            if (messageIds.length() > 0){
                for (int i = 0; i < messageIds.length(); i++) {
                    receiveMessage(this.username, messageIds.get(i));
                    deleteMessage(this.username, messageIds.get(i));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void receiveMessage(String username, Object messageId) {
        String jsonString =
                client.target(targetURL + "/" + username + "/messages/" + messageId)
                        .request(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        try {
            JSONObject jsonObjectMessage = new JSONObject(jsonString);

            System.out.println("Sender: " + jsonObjectMessage.get("sender"));
            System.out.println("Message: " + jsonObjectMessage.get("message"));
            System.out.println();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void deleteMessage(String username, Object messageId) {
        Response responseOfDelete =
                client.target(targetURL + "/" + username + "/messages/" + messageId)
                        .request().delete();

//        System.out.println("Code of DELETE: " + responseOfDelete.getStatus() + "\n");
    }






    private void receiveFiles() {
        Response response = client.target(targetURL + "/" + this.username + "/files")
                .request(MediaType.APPLICATION_JSON_TYPE).get();

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
//            System.out.println(response.getStatus());
//            System.out.println("Error\n");
            return;
        }

        String jsonResponse = client.target(targetURL + "/" + this.username + "/files")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(String.class);

        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);

            JSONArray fileIds = new JSONArray();
            try {
                fileIds = (JSONArray) jsonObject.get("items");
            } catch (ClassCastException e) {
                fileIds.put(jsonObject.get("items"));
            }


            String endOfSentence = "file!";
            if (fileIds.length() > 1)
                endOfSentence = "files!";

            System.out.println("You have " + fileIds.length() + " new " + endOfSentence);

            if (fileIds.length() > 0){
                for (int i = 0; i < fileIds.length(); i++) {
                    receiveFile(this.username, fileIds.get(i));
                    deleteFile(this.username, fileIds.get(i));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void receiveFile(String username, Object fileId) {
        String folderPath = "./receivedFiles";

        String jsonString =
                client.target(targetURL + "/" + username + "/files/" + fileId)
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get(String.class);

        try {
            JSONObject jsonObjectFile = new JSONObject(jsonString);

            System.out.println("Sender: " + jsonObjectFile.get("sender"));
            System.out.println("Filename: " + jsonObjectFile.get("filename"));

            String encodedFileContent = (String) jsonObjectFile.get("content");
            java.util.Base64.Decoder decoder = java.util.Base64.getDecoder();
            byte[] decodedContent = decoder.decode(encodedFileContent);


            // check if there is a folder to save the files
            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdir();
            }

            // create a file and write bytes to it
            try (FileOutputStream stream =
                         new FileOutputStream(folder.getPath() + "/" + jsonObjectFile.get("filename"))) {
                stream.write(decodedContent);
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void deleteFile(String username, Object fileId) {
        Response responseOfDelete =
                client.target(targetURL + "/" + username + "/files/" + fileId)
                        .request().delete();

//        System.out.println("Code of DELETE: " + responseOfDelete.getStatus() + "\n");
    }




    private void checkUsers() {
        Response response = client.target(targetURL + "/users")
                .request(MediaType.APPLICATION_JSON_TYPE).get();

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            return;
        }

        String jsonResponse = client.target(targetURL + "/users")
                .request(MediaType.APPLICATION_JSON_TYPE).get(String.class);

        ArrayList <String> usersOnServer = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray jsonArray = new JSONArray();
            try {
                jsonArray = (JSONArray) jsonObject.get("items");
            } catch (ClassCastException e) {
                jsonArray.put(jsonObject.get("items"));
            }

            // converting JSONArray to ArrayList
            if (jsonArray != null) {
                for (int i=0;i<jsonArray.length();i++){
                    usersOnServer.add(jsonArray.getString(i));
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (usersOnServer.size() != 0){
            if (oldUsers.size() == 0) {
                for (String user: usersOnServer) {
                    oldUsers.add(user);
                    System.out.println(user + " is logged in.");
                }
                System.out.println();
            } else {
                // Add all users to currentUsers
                currUsers.addAll(usersOnServer);

                // compare with oldUsers and print new users on server
                for (String user: currUsers) {
                    if (!oldUsers.contains(user)) {
                        System.out.println(user + " is logged in.");

                    }
                }

                // compare with oldUsers and then print users which logged out from server
                for (String user: oldUsers) {
                    if (!currUsers.contains(user)) {
                        System.out.println(user + " is logged out.");
                    }
                }

                // move current users to old, and remove current
                oldUsers = (ArrayList<String>) currUsers.clone();
                currUsers.clear();
            }
        }
    }
}
