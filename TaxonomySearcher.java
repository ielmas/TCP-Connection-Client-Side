import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class TaxonomySearcher {

    public static void main(String[] args){
        try{
            String command              = "";
            String response             = "";
            String modifiedSentence     = "";
            String host                 = args[0];
            int port                    = Integer.parseInt(args[1]);
            Response responseObj        = null;
            Socket clientSocket         = null;

            try{
                clientSocket            = new Socket(host, port);
            } catch (Exception e) {
                System.out.println("Could not connect to server!");
                System.exit(-1);
            }
            
            PrintWriter outToServer     = new PrintWriter (clientSocket.getOutputStream(), true);
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            authenticate(clientSocket, outToServer, inFromServer);
            Set<String> imageNames = getImageNameList(clientSocket, outToServer, inFromServer);
            
            
            printList(imageNames);
            
            findImages(clientSocket, outToServer, inFromServer, imageNames, new ArrayList<String>());

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printList(Set<String> list){
        for( String image: list){
            System.out.println("- "+image);
        }
        System.out.println();
    }
    private static void findImages(Socket socket, PrintWriter outToServer, BufferedReader inFromServer, Set<String> imageSet, ArrayList<String> path){
        try{
            String command = "NLST";
            Response response = null;
            System.out.println("Command sent: " + command);
            outToServer.println(command);
            response = parseResponse(inFromServer.readLine());
            
            Set<String> folderAndFiles = parseList(response.statusText);
            System.out.println("Response status code: " + response.statusCode);
            if (folderAndFiles == null){
                System.out.println("this folder is empty\n");
                return;
            }
            System.out.println("Folder and files: ");
            printList(folderAndFiles);

            for(String dir : folderAndFiles){
                int index = dir.indexOf(".jpg");
                if (index != -1){
                    if (imageSet.contains(dir)){
                        imageSet.remove(dir);
                        System.out.println("image found: " + dir + " at " + convertPathToString(path));
                        downloadImage(socket, outToServer, dir);
                    }
                }else{
                    if (dir.indexOf(".") == -1){
                        enterDir(socket, outToServer, inFromServer, dir, path);
                        findImages(socket, outToServer, inFromServer, imageSet, path);
                        goUpDir(socket, outToServer, inFromServer, path);
                    }
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String convertPathToString(ArrayList<String> path){
        String result = "";
        for(String dir : path){
            result += " / " + dir;
        }
        return result;
    }
    public static void goUpDir(Socket socket, PrintWriter outToServer, BufferedReader inFromServer, ArrayList<String> path){
        try {
            String command = "CDUP";
            System.out.println("Command sent: " + command);
            outToServer.println(command);
            Response response = parseResponse(inFromServer.readLine());
            System.out.println("Response: " + response.statusCode + "\n");
            if (response.isStatusOk()){
                path.remove(path.size()-1);
            }else{
                System.exit(-1);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void enterDir(Socket socket, PrintWriter outToServer, BufferedReader inFromServer, String dir,  ArrayList<String> path){
        try {
            String command = "CWDR " + dir;
            System.out.println("Command sent: " + command);
            outToServer.println(command);
            Response response = parseResponse(inFromServer.readLine());
            System.out.println("Response: " + response.statusCode + "\n");
            if (response.isStatusOk()){
                path.add(dir);
            }else{
                System.exit(-1);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void downloadImage(Socket clientSocket, PrintWriter outToServer, String imageName){
        try {
            String command = "GET "+ imageName;
            System.out.println("Command sent: " + command);

            InputStream stream = new BufferedInputStream(clientSocket.getInputStream());

            outToServer.println(command);

            // Getting status code
            byte[] byteStatusCode = new byte[4];
            stream.read(byteStatusCode, 0, 4);
            String statusCode = new String(byteStatusCode);
            System.out.println("Response status code: " + statusCode);
            
            // Getting image size
            int imageSize = (int)(stream.read() * Math.pow(2, 16) + stream.read() * Math.pow(2, 8) + stream.read());
            // Getting image
            byte[] image = new byte[imageSize];
            stream.read(image, 0, imageSize);

            // Converting byte array to image
            ByteArrayInputStream bufInputStream = new ByteArrayInputStream(image);
            BufferedImage imageFile = ImageIO.read(bufInputStream);
            ImageIO.write(imageFile, "jpg", new File(imageName) );
            System.out.println(imageName + " is downloaded!! \n\n");

        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static Set<String> getImageNameList(Socket socket, PrintWriter outToServer, BufferedReader inFromServer){
        String command = "";
        String response = "";
        Response responseObj = null;
        try{
            command = "OBJ";
            System.out.println("OBJ command is sent: ");
            outToServer.println(command);
            response = inFromServer.readLine();
            responseObj = parseResponse(response);
            if (!responseObj.isStatusOk()){
                System.out.println("Invalid request, response: " + responseObj.statusCode);
                System.exit(-1);
            }
            //System.out.println("Image List: "+ responseObj.statusText);
            return parseList(responseObj.statusText);
        }catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    private static Set<String> parseList(String list){
        if (list.equals("")) return null;
        Set<String> listItems = new HashSet<>();
        int previousSpace = -1;
        int spaceIndex = list.indexOf(" ");
        while(spaceIndex != -1){
            listItems.add(list.substring(previousSpace+1, spaceIndex));
            previousSpace = spaceIndex;
            spaceIndex = list.indexOf( " ", spaceIndex+1);
        }
        listItems.add(list.substring(previousSpace+1, list.length()));
        return listItems;
    }
    private static void authenticate(Socket socket, PrintWriter outToServer, BufferedReader inFromServer){
        String command = "";
        String response = "";
        Response responseObj = null;
        try{
            command = "USER bilkentstu";
            System.out.print("USER Command is sent: ");
            outToServer.println(command);
            response = inFromServer.readLine();
            responseObj = parseResponse(response);
            if (!responseObj.isStatusOk()){
                System.out.println("Authenticate failed, response: " + responseObj.statusCode);
                System.exit(-1);
            }
            System.out.println("Username is valid");
            command = "PASS cs421f2020";
            System.out.print("PASS command is sent: ");
            outToServer.println(command);
            response = inFromServer.readLine();
            responseObj = parseResponse(response);
            if (!responseObj.isStatusOk()){
                System.out.println("Authenticate failed, response:  " + responseObj.statusCode);
                System.exit(-1);
            }
            System.out.println("Password is valid");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static Response parseResponse(String response){
        int spaceIndex = response.indexOf(" ");
        String statusCode = "";
        String statusText = "";
        if (spaceIndex == -1){
            statusCode = response;
        }else{
            statusCode = response.substring(0, spaceIndex);
            statusText = response.substring(spaceIndex+1, response.length());
        }
        return new Response(statusCode, statusText);
    }
}
