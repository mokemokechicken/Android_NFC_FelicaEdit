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

import java.lang.reflect.InvocationTargetException;

import jp.co.yumemi.nfc.FelicaTag.CommandPacket;
import jp.co.yumemi.nfc.FelicaTag.CommandResponse;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Parcelable;

public class NfcTag {
    private static String TAG = "NfcTag";
    static public String ANDROID_NFC_EXTRA_TAG = "android.nfc.extra.TAG";
    
    protected final Parcelable tagService;
    public Parcelable getTagService() {
        return tagService;
    }

    private byte[] idbytes;
    
    public static final String TYPE_NULL = "NULL";
    public static final String TYPE_OTHER = "OTHER";
    public static final String TYPE_FELICA = "FeliCa";
    
    public NfcTag(Parcelable tagService, byte[] id) {
        this.tagService = tagService;
        this.idbytes = id;
    }

    /**
     * コマンドを実行します.
     * 
     *
     * <pre>Android 2.3の隠しクラス(@hide)に依存しています。今後の仕様変更で使えなくなるリスクを考慮してください</pre>
     * 
     * @param commandPacket 実行するコマンドパケットをセットします
     * @return CommandResponse コマンドの実行結果が戻ります 
     * @throws NfcException コマンドの発行に失敗した場合にスローされます
     */
     public CommandResponse execute(CommandPacket commandPacket) throws NfcException {
         if ( this.tagService == null ) {
             throw new NfcException("tagService is null. no read execution");
         }
         
         byte[] result = RawCommand.executeRaw(this.tagService, commandPacket.getBytes());
         return new CommandResponse(result);
     }

     public static NfcTag create(Intent intent) {
         String action = intent.getAction();
         if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
             return create(intent.getParcelableExtra(ANDROID_NFC_EXTRA_TAG));
         } else {
             return new NullNfcTag(null);
         }
     }
     
     public void putTagService(Intent intent) {
         intent.putExtra(ANDROID_NFC_EXTRA_TAG, getTagService());
     }
     
     public static NfcTag create(Parcelable tag) {
        try {
            if (tag != null) {
                byte[] bytes = (byte[])tag.getClass().getMethod("getId").invoke(tag);
                if (bytes.length == 8) {
                    return new FelicaTag(tag, bytes);
                } else {
                    return new NfcTag(tag, bytes);
                }
            }
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        return new NullNfcTag(tag); 
    }

    public byte[] getId() {
        return idbytes;
    }
    
    public String getType() {
        return TYPE_OTHER;
    }
}

class NullNfcTag extends NfcTag {
    public NullNfcTag(Parcelable tagService) {
        super(tagService, null);
    }

    public String getType() {
        return TYPE_NULL;
    }
}
