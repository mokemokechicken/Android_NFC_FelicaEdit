/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * 本packageは、
 * kazzz さんの nfc-felica(http://d.hatena.ne.jp/Kazzz/20110130/p1) からソースを色々複製して利用しています。
 * 整理しようと思ったのですが、現状あまり整理できてないです。。
 * 最終的には、MIFARE系,FeliCa系,NFC(NDEF)系を、上手く処理できるように整理できると良いと思っています。
 */


/*
 * Changes
 * * 2011/2/5: k_morishita
 * ** net.kazzz.felica.lib.FeliCaLib.executeRaw() を複製して修正。
 */


package jp.co.yumemi.nfc;

import jp.co.yumemi.rd.misc.Util;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Parcelable;

public class NfcTag {
    static private final String TAG = "NfcTag";
    
    protected final Tag tag;
    public Tag getTag() {
        return tag;
    }

    public static final String TYPE_NULL = "NULL";
    public static final String TYPE_OTHER = "OTHER";
    public static final String TYPE_FELICA = "FeliCa";
    
    public NfcTag(Tag tag) {
        this.tag = tag;
    }

    public byte[] getId() {
        return tag.getId();
    }
    
    public void putTagService(Intent intent) {
        intent.putExtra(NfcAdapter.EXTRA_TAG, tag);
    }

    public String getType() {
        return TYPE_OTHER;
    }
    
    public String getTechListAsString() {
        return Util.getHexString(tag.getTechList());
    }
}

class NullNfcTag extends NfcTag {
    public NullNfcTag(Tag tag) {
        super(tag);
    }

    public String getType() {
        return TYPE_NULL;
    }
}
