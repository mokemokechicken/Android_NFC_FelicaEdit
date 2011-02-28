package jp.co.yumemi.nfc;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcF;
import android.os.Parcelable;

public class TagFactory {
    
    public static NfcTag create(Intent intent) {
        return create(intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
    }

    public static NfcTag create(Parcelable tag) {
       if (tag != null && tag instanceof Tag) {
           Tag t = (Tag)tag;
           if (NFCUtil.hasTech(t, NfcF.class)) {
               return new FelicaTag(t);
           } else {
               return new NfcTag(t);
           }
       }
       return new NullNfcTag(null); 
   }
    
}
