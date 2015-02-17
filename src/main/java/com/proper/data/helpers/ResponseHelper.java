package com.proper.data.helpers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;

/**
 * Created by Lebel on 17/02/2015.
 */
public class ResponseHelper {

    public String refineOutgoingMessage(String input){
        String refined = input;
        if (input.contains("Full_Artist_Value1")) {
            refined = refined.replace("Full_Artist_Value1", "FullArtist");
        }
        if (input.contains("Full_Title_Value1")){
            refined = refined.replace("Full_Title_Value1", "FullTitle");
        }
        if (input.contains("Packshot_URL_Value1")) {
            refined = refined.replace("Packshot_URL_Value1", "PackshotURL");
        }
        if (input.contains("PRO:")) {
            refined = refined.replace("PRO:", "");
        }
        return refined;
    }

    public String refineResponse(String input) {
        String refined = input;

        if (input.contains("Full_Artist_Value1")) {
            refined = refined.replace("Full_Artist_Value1", "FullArtist");
        }
        if (input.contains("Full_Title_Value1")){
            refined = refined.replace("Full_Title_Value1", "FullTitle");
        }
        if (input.contains("Packshot_URL_Value1")) {
            refined = refined.replace("Packshot_URL_Value1", "PackshotURL");
        }
        if (input.contains("PRO:")) {
            refined = refined.replace("PRO:", "");
        }
        try {
            JSONObject jsonResp = new JSONObject(refined);
            JSONArray products = jsonResp.getJSONArray("Products");

            for (int i = 0; i < products.length(); i++) {
                JSONObject prod = products.getJSONObject(i);
                if (prod.has("FullArtist")) {
                    prod.remove("FullArtist");
                }
                if (prod.has("FullTitle")) {
                    prod.remove("FullTitle");
                }
                if (!prod.has("PackshotURL")) {
                    prod.put("PackshotURL", "");
                }
            }
            jsonResp.remove("Products");
            jsonResp.put("Products", products);
            refined = jsonResp.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return refined;
    }

    public String refineProductResponse(String input) {
        String refined = input;

        if (input.contains("Full_Artist_Value1")) {
            refined = refined.replace("Full_Artist_Value1", "FullArtist");
        }
        if (input.contains("Full_Title_Value1")){
            refined = refined.replace("Full_Title_Value1", "FullTitle");
        }
        if (input.contains("Packshot_URL_Value1")) {
            refined = refined.replace("Packshot_URL_Value1", "PackshotURL");
        }
        if (input.contains("PRO:")) {
            refined = refined.replace("PRO:", "");
        }
        try {
            JSONObject jsonResp = new JSONObject(refined);
            JSONArray products = jsonResp.getJSONArray("Products");

            for (int i = 0; i < products.length(); i++) {
                JSONObject prod = products.getJSONObject(i);
                if (!prod.has("PackshotURL")) {
                    prod.put("PackshotURL", "");
                }
            }
            jsonResp.remove("Products");
            jsonResp.put("Products", products);
            refined = jsonResp.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return refined;
    }

    public static int nthOccurrence(String str, String c, int n) {
        int pos = str.indexOf(c, 0);
        while (n-- > 0 && pos != -1)
            pos = str.indexOf(c, pos + 1);
        return pos;
    }

}

