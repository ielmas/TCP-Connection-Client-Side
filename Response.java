public class Response{
    String statusCode;
    String statusText;

    Response(String statusCode, String statusText){
        this.statusCode = statusCode;
        this.statusText = statusText;
    }
    boolean isStatusOk(){
        return statusCode.equals("OK");
    }
}