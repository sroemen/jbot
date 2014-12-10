/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jbot;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import java.util.ArrayList;

/**
 *
 * @author sroemen
 */
class FuzzySearch {
    private String message = "";
    private String sender = "";
    private String channel = "";
    
    
    
    FuzzySearch(String message, String sender, String channel) {
        this.message=message.toLowerCase();
        this.sender=sender;
        this.channel=channel;
    }

    FuzzySearch(String message, String sender) {
        this.message=message.toLowerCase();
        this.sender=sender;
    }
    
    FuzzySearch() {}
    
    
    
    ArrayList<MSG> getList() {
        ArrayList<MSG> ret = new ArrayList();
        Boolean exact = false;
        
        System.out.println("looking for: "+this.message);
        System.out.println("QA DB size: "+Jbot.coll.count());
        System.out.println("un-answered DB size: "+Jbot.coll2.count());
        
        BasicDBObject LookFor1 = new BasicDBObject();
        LookFor1.put("Tags", GetTagSearchString(this.message));
        
        BasicDBObject LookFor2 = new BasicDBObject();
        LookFor2.put("Tags", new BasicDBObject("$all", GetTagSearchString(this.message)));
                
        System.out.println("query string: (match exact) "+LookFor1.toString());
        System.out.println("query string: (match all) "+LookFor2.toString());
        
        DBCursor cur = Jbot.coll.find(LookFor1).sort(new BasicDBObject("answer", "1"));
        DBCursor cur2 = Jbot.coll.find(LookFor2).sort(new BasicDBObject("answer", "1"));
        
        
        if(cur.hasNext() || cur2.hasNext()) {
            while(cur.hasNext()) {
                DBObject ans = cur.next();
                DBObject ot = (DBObject) JSON.parse(ans.toString());
                String answer = (String) "Q: "+ot.get("question")+"  A: "+ot.get("answer");
                MSG out = new MSG(true, sender+": ANSWER to \""+this.message+"\" IS: "+answer, sender, channel);
                ret.add(out);
                exact = true;
            }
            // run this if there wasn't an exact match
            if(!exact) {
                while(cur2.hasNext()) {
                    DBObject ans = cur2.next();
                    DBObject ot = (DBObject) JSON.parse(ans.toString());
                    String answer = (String) "Q: "+ot.get("question")+"  A: "+ot.get("answer");
                    MSG out = new MSG(true, sender+": ANSWER to \""+this.message+"\" IS: "+answer, sender, channel);
                    ret.add(out);
                }
            }
        } else {
            SaveUnansweredQuestion(this.message, this.sender);
            
            MSG out = new MSG(false, "Sorry, I don't have an answer right now.", sender, channel);
            ret.add(out);
            MSG out2 = new MSG(false, "send me a /msg with #learn <question> <<answer>> to add to my DB.", sender, channel);
            ret.add(out2);
        }
        
        return ret;
    }

    public static String[] GetTagSearchString(String message) {
        String filtered = FilterTags(message);
        String m1 = filtered.replaceAll("\\s", ",");
        return m1.split(",");
    }
    
    public static String[] GetTags(String message) {
        String filtered = FilterTags(message);
        String[] tags;
        tags = filtered.split("\\s");
        return tags;
    }
    
    private static String FilterTags(String in) {
        StringBuilder out = new StringBuilder();
        String[] tags;
        tags = in.replaceAll("\\?\\!\\'\\\"", "").split("\\s");
        
        for(int i=0; i<tags.length; i++) {
            String t = tags[i].replaceAll("\\s", "");
            if(!Jbot.DontTag.contains(t) && (!t.isEmpty())) {
                out.append(t);
                if(i!= (tags.length-1)) {
                    out.append(" ");
                }
            }
        }
        return out.toString();
    }

    private void SaveUnansweredQuestion(String message, String sender) {
        BasicDBObject doc = new BasicDBObject();
        doc.put("question", message);
        doc.put("answer", null);
        doc.put("by", sender);
        doc.put("Tags", FuzzySearch.GetTags(message));
        Jbot.coll2.insert(doc);
    }
    
    public static boolean IsUnAnswered(String question) {
        BasicDBObject LookFor = new BasicDBObject("Tags", 
                new BasicDBObject("$all", GetTagSearchString(question)));
        DBCursor cur = Jbot.coll2.find(LookFor);
        
        if(cur.hasNext()) {
            return true;
        } 
        return false;
    }
}
