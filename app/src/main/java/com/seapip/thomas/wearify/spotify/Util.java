package com.seapip.thomas.wearify.spotify;

import android.text.TextUtils;

import com.seapip.thomas.wearify.spotify.objects.Artist;
import com.seapip.thomas.wearify.spotify.objects.Image;

import java.util.ArrayList;

public class Util {
    static public String smallestImageUrl(Image[] images) {
        int size = 0;
        if(images != null) {
            for (Image image : images) {
                if (image.width < size || size == 0) {
                    size = image.width;
                }
            }
            for (Image image : images) {
                if (image.width == size) {
                    return image.url;
                }
            }
        }
        return null;
    }

    static public String largestImageUrl(Image[] images) {
        int size = 0;
        if(images != null) {
            for (Image image : images) {
                if (image.width > size) {
                    size = image.width;
                }
            }
            for (Image image : images) {
                if (image.width == size) {
                    return image.url;
                }
            }
        }
        return null;
    }

    static public String names(Artist[] artists) {
        ArrayList<String> names = new ArrayList<>();
        for (Artist artist : artists) {
            names.add(artist.name);
        }
        return TextUtils.join(", ", names);
    }

    static public String songCount(int count) {
        return count + " song" + (count > 1 ? "s" : "");
    }
}
