package jp.co.yumemi.nfc;

import android.nfc.Tag;
import android.nfc.tech.TagTechnology;

public class NFCUtil {
    static public boolean hasTech(Tag tag, String klassName) {
        for (String tech : tag.getTechList()) {
            if (tech.equals(klassName)) {
                return true;
            }
        }
        return false;
    }

    static public boolean hasTech(Tag tag, Class<? extends TagTechnology> tech) {
        return hasTech(tag, tech.getCanonicalName());
    }
}
