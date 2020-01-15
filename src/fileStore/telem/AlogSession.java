package fileStore.telem;

public class AlogSession {
    String call;
    String ssid;
    int session; // serial_no
    
    AlogSession(String call, String ssid, int session) {
    	this.call = call;
    	this.ssid = ssid;
    	this.session = session;
    }
}
