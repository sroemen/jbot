/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jbot;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jibble.pircbot.IrcException;

/**
 *
 * @author sroemen
 */
public class Jbot {

    protected static DB db = null;
    //coll is for answered questions
    protected static DBCollection coll = null;
    //coll2 is for un-answered questions
    protected static DBCollection coll2 = null;
    //logCol is to log conversations
    protected static DBCollection logColl = null;
    
    protected static Set DontTag = new HashSet();
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        initDB();
        
        DontTag.add("a");
        DontTag.add("the");
        DontTag.add("this");
        DontTag.add("is");
        DontTag.add("of");
        DontTag.add("i");
        
        
                
        
        Bot bot = new Bot();
        bot.setVerbose(true);
        try {
            bot.connect(" <your IRC server>");
            bot.listChannels();
        } catch (IOException ex) {
            Logger.getLogger(Jbot.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IrcException ex) {
            Logger.getLogger(Jbot.class.getName()).log(Level.SEVERE, null, ex);
        } 
                
        if(bot.isConnected()) {
            System.out.println("connected.");
            
            /*
            try {
                bot.TailLog();
            } catch (IOException ex) {
                Logger.getLogger(Jbot.class.getName()).log(Level.SEVERE, null, ex);
            }
             * 
             */
        } else {
            System.out.println("Not connected.");
        }
    }
    
    private static void initDB() {
        try {
            Mongo DB = new Mongo("127.0.0.1");
            db = DB.getDB("jBot");
            coll = db.getCollection("jBotQA");
            coll2 = db.getCollection("jBotUnAnswered");
            logColl = db.getCollection("IRCLogs");
            
            //index the Tags column, since that is what we search from
            coll.ensureIndex("Tags");
            logColl.ensureIndex("Room");
            logColl.ensureIndex("Nick");
            logColl.ensureIndex("Tags");
            
        } catch (UnknownHostException ex) {
            Logger.getLogger(Jbot.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MongoException ex) {
            Logger.getLogger(Jbot.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public static void TeachBrain(String q, String a, String user) {
        BasicDBObject doc = new BasicDBObject();
        doc.put("question", q);
        doc.put("answer", a);
        doc.put("by", user);
        doc.put("Tags", FuzzySearch.GetTags(q));
        coll.insert(doc);
        
        if(FuzzySearch.IsUnAnswered(q)) {
            System.out.println("removing question "+q+" from the unanswered collection");
            
            BasicDBObject LookFor = new BasicDBObject("Tags", 
                new BasicDBObject("$all", FuzzySearch.GetTagSearchString(q)));
            DBCursor cur = Jbot.coll2.find(LookFor);
            while(cur.hasNext()) {
                DBObject item = cur.next();
                Jbot.coll2.remove(item);
            }
        }
    }
    
    
}
