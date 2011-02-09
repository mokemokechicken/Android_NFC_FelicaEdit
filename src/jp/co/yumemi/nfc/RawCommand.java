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
 * Changes
 * * 2010/2/5: k_morishita
 * ** net.kazzz.felica.lib.FeliCaLib.executeRaw() より複製して修正。
 */

package jp.co.yumemi.nfc;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jp.co.yumemi.rd.misc.Util;

import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.util.Log;

public class RawCommand {
    private static final String TAG = "RawCommand";

    /**
     * Rawデータを使ってコマンドを実行します
     * 
     * <pre>Android 2.3の隠しクラス(@hide)に依存しています。今後の仕様変更で使えなくなるリスクを考慮してください</pre>
     * 
     * @param Tag 隠しクラスである android.nfc.Tag クラスの参照をセットします
     * @param commandPacket 実行するコマンドパケットをセットします
     * @return byte[] コマンドの実行結果バイト列で戻ります 
     * @throws FeliCaException コマンドの発行に失敗した場合にスローされます
     * @author Kazzz
     */
    public static final byte[] executeRaw(Parcelable tag, byte[] commandPacket) throws NfcException {
        try {
            NfcAdapter adapter = NfcAdapter.getDefaultAdapter();
            // android.nfc.RawTagConnectionを生成
            Class<?> tagClass = Class.forName("android.nfc.Tag");
            Method createRawTagConnection = 
                adapter.getClass().getMethod("createRawTagConnection", tagClass);
            Object rawTagConnection = createRawTagConnection.invoke(adapter, tag);

            // android.nfc.RawTagConnection#mTagServiceフィールドを取得 (NfcService.INfcTagへの参照が入っている)
            Field f = rawTagConnection.getClass().getDeclaredField("mTagService");
            f.setAccessible(true);
            Object tagService = f.get(rawTagConnection);

            //ServiceHandleを取得
            f = tagClass.getDeclaredField("mServiceHandle");
            f.setAccessible(true);
            //int serviceHandle = (Integer) f.get(tagService); 
            int serviceHandle = (Integer) f.get(tag); 
            
            //INfcTag#transceive
            Method transeive = tagService.getClass().getMethod("transceive", Integer.TYPE, byte[].class);

            Log.d(TAG, "invoking transceive commandPacket :" +  Util.getHexString(commandPacket) + "\n");
            byte[] response = (byte[])transeive.invoke(tagService, serviceHandle, commandPacket);
            if ( response != null ) {
                Log.d(TAG, "transceive successful. commandResponse = " + Util.getHexString(response) + "\n");
            } else {
                Log.d(TAG, "transceive fail. result null");
                throw new NfcException("execute transceive fail" + "\n");
            }
            return response;
        } catch (ClassNotFoundException e){
            throw new NfcException(e);
        } catch (NoSuchMethodException e){
            throw new NfcException(e);
        } catch (SecurityException e){
            throw new NfcException(e);
        } catch (NoSuchFieldException e){
            throw new NfcException(e);
        } catch (IllegalAccessException e){
            throw new NfcException(e);
        } catch (IllegalArgumentException e){
            throw new NfcException(e);
        } catch (InvocationTargetException e){
            throw new NfcException(e);
        }
    }
}
