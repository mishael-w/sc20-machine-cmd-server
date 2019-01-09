package com.qucetel.andrew.serialjnidemo;


import org.json.JSONException;
import org.json.JSONObject;

public class Post {
    public final static int POST_TYPE_HEARTBEAT = 0;
    public final static int POST_TYPE_REPORT = 1;
    public final static int POST_TYPE_PERSONAL = 2;
    public final static int POST_TYPE_GLOBAL = 3;
    public final static int POST_WAS_DELIVERED = 4;
    public final static String POST_TOPIC_AVAILABILITY = "ava";

    public int type;
    public int destination; // 1=hsl0, 2=hsl1
    private String sender;
    public String data;

    public Post(int type, int destination, String sender, String data) {
        this.type = type;
        this.destination = destination;
        this.sender = sender;
        this.data = data;
    }

    public Post(int type, String sender, String data) {
        this.type = type;
        this.sender = sender;
        this.data = data;
    }

    public Post(int type, String sender) {
        this.type = type;
        this.sender = sender;
    }

    @Override
    public String toString() {
        return sender + ":\n" + data + "\n";
    }



    public static Post postFromJson(String jsonString){
        try {
            JSONObject cmd = new JSONObject(jsonString);
            int type = cmd.getInt("type");
            String senderName = cmd.getString("sender");
            int dest = -1;
            if (cmd.has("dest")) {
                dest = cmd.getInt("dest");
            }
            if (cmd.has("data"))
                return new Post(type, dest , senderName, cmd.getString("data"));
            return new Post(type , senderName);
        } catch (JSONException e) {
            e.printStackTrace();
            return new Post(-1 , "unknown", jsonString);
        }
    }

    public String toJSon(){
        JSONObject root = new JSONObject();
        try {
            root.put("type", type);
            root.put("sender", sender);
            root.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
            return "{" +
                    "\"type\": " + type + ",\n" +
                    "\"sender\": \"" + sender + "\",\n" +
                    "\"data\": \"" + data + "\"\n" +
                    "}" ;
        }
        return root.toString();

    }
}
