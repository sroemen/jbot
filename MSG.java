/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jbot;

/**
 *
 * @author sroemen
 */
class MSG {
    public boolean ANSWERED = false;
    public String message;
    public String sender;
    public String channel;
    
    MSG(boolean ans, String message, String sender, String channel) {
        this.ANSWERED=ans;
        this.channel=channel;
        this.message=message;
        this.sender=sender;
    }
}
