/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jbot;

import com.mongodb.BasicDBObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;




/**
 *
 * @author sroemen
 */
class Bot extends PircBot {
    
    Bot() {
        this.setName("HelpBot");
        this.setVersion("HelpBot v0.2");
    }
    
    @Override
    public void onChannelInfo(String chan, int users, String topic) {
        //auto join a channel if it isn't in already
        String[] chans = this.getChannels();
        boolean found = false;
        
        for(int i=0; i<chans.length; i++) {
            if(chans[i].matches(chan)) {
                found = true;
            }
        }
        if(!found) {
            this.joinChannel(chan);
        }
    }
    
    @Override
    protected void onServerPing(String response) {
        this.listChannels();
        super.onServerPing(response);
    }
    
    @Override
    protected void onAction(String sender, String login, String hostname, String target, String action) {
        RecordMessage(target, sender, "*"+sender+" "+action);
        super.onAction(sender, login, hostname, target, action);
    }

    @Override
    protected void onTopic(String channel, String topic, String setBy, long date, boolean changed) {
        RecordMessage(channel, setBy, "*TOPIC CHANGE* "+topic);
        super.onTopic(channel, topic, setBy, date, changed);
    }

    @Override
    protected void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
        RecordMessage(channel, kickerNick, "*USER KICKED* "+recipientNick+" was kicked by "+kickerNick+" reason:"+reason);
        //rejoin if kicked.
        if(recipientNick.matches(this.getName())) {
            this.joinChannel(channel);
        } else {
            super.onKick(channel, kickerNick, kickerLogin, kickerHostname, recipientNick, reason);
        }
    }

    @Override
    protected void onPart(String channel, String sender, String login, String hostname) {
        RecordMessage(channel, sender, "*USER Left* ");
        super.onPart(channel, sender, login, hostname);
    }

    @Override
    protected void onDisconnect() {
        try {
            super.reconnect();
        } catch (IOException ex) {
            Logger.getLogger(Bot.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IrcException ex) {
            Logger.getLogger(Bot.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    
    @Override
    protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
        RecordMessage("&SERVER", sourceNick, "*USER Quit* "+sourceNick+"("+sourceLogin
                +"@"+sourceHostname+") "+reason);
        super.onQuit(sourceNick, sourceLogin, sourceHostname, reason);
    }

    @Override
    protected void onJoin(String channel, String sender, String login, String hostname) {
        RecordMessage(channel, sender, "*USER JOIN* "+sender+"("+login+"@"+hostname+")");
        super.onJoin(channel, sender, login, hostname);
    }
   
    public void onInvite(String tnick, String snick, String slogin, 
            String shostname, String channel) {
        if(tnick.equalsIgnoreCase(this.getName())) {
            int pos = channel.indexOf("#");
            String chan = channel.substring(pos, channel.length());
            
            System.out.println("\t joining channel "+chan);
            this.joinChannel(chan);
        }
    }
    
    public void onMessage(String channel, String sender,
                       String login, String hostname, String message) {
        Boolean responded = false;
        
        RecordMessage(channel, sender, message);
        
        if (message.equalsIgnoreCase("time") || message.toLowerCase().matches("what time is it\\??")) {
            String time = new java.util.Date().toString();
            sendMessage(channel, sender + ": The time is now " + time);
            responded = true;
        }
        
        if(message.toLowerCase().startsWith("helpbot") || message.toLowerCase().startsWith("how") || message.toLowerCase().startsWith("what") && !responded ) {
            FuzzySearch lst = new FuzzySearch(message.toLowerCase(), sender, channel);
            ArrayList<MSG> msgs = lst.getList();
            Iterator it = msgs.iterator();
            while(it.hasNext()) {
                MSG ans = (MSG) it.next();
                // answer to the room
                //sendMessage(ans.channel, ans.sender+": "+ans.message);
                // answer to a private msg
                if(ans.ANSWERED) {
                    //respond if we know the answer. (be less annoying)
                    sendMessage(ans.sender, ans.message);
                }
            }
            
        } 
    }
    
    public void onPrivateMessage(String sender, String login, String hostname,
            String message) {
        
        if(message.toLowerCase().startsWith("#learn")) {
            //teach me!
            int qs = message.indexOf("<")+1;
            int qe = message.indexOf(">");
            int as = message.indexOf("<<")+2;
            int ae = message.indexOf(">>");
            String q = message.substring(qs, qe);
            String a = message.substring(as, ae);
            
            Jbot.TeachBrain(q.toLowerCase(), a.toLowerCase(), sender);
            System.out.println("taught q:"+q.toLowerCase()+" a:"+a.toLowerCase());
            sendMessage(sender, "Thanks "+sender+" for teaching me that!");
        } if(message.toLowerCase().startsWith("who")) {
            //answer who am i questions.
            String AboutMe = "I am a bot that tries to answer your questions, "
                    +"and I also log chat messages into http:// "
                    +"if you don't want your message logged, start the message with OTR (Off The Record)"
                    +"use the pastebin for large pastes to not dirty the room, and possibly get kicked."
                    +"the pastebin is at http:// <pastebin url>";
            sendMessage(sender, AboutMe);
        } else {
            FuzzySearch lst = new FuzzySearch(message.toLowerCase(), sender);
            ArrayList<MSG> msgs = lst.getList();
            Iterator it = msgs.iterator();
            while(it.hasNext()) {
                MSG ans = (MSG) it.next();
            
                sendMessage(ans.sender, ans.message);
            }
        }
    }

    
    /*
    public void TailLog() throws IOException {
        Reader fr;
        String log = "/tmp/beta.log";
        
        try {
            fr = new FileReader(log);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while(true) {
                String[] chan = this.getChannels();
                if( (line = br.readLine()) != null) {
                    for(int i=0; i<chan.length; i++) {
                        sendMessage(chan[i], line);
                    }
                    continue;
                }
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException x) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            br.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Bot.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
     * 
     */

    private void RecordMessage(String channel, String sender, String message) {
        
        //Here we log the chats for display on the web
        // if a message starts with OTR then don't record
        if(message.toLowerCase().startsWith("otr ") || channel.toLowerCase().matches("#offtopic")) {
            System.out.println("That message is OTR.");
        } else {       
            BasicDBObject doc = new BasicDBObject();
                doc.put("Channel", channel);
                doc.put("Nick", sender);
                doc.put("Message", message);
                doc.put("Date", System.currentTimeMillis()/1000);
                doc.put("Tags", FuzzySearch.GetTags(message));
            Jbot.logColl.insert(doc);
        }
    }
}
