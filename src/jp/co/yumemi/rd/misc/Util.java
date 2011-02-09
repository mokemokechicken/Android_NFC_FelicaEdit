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
 * ** net.kazzz.felica.lib.Util から複製して修正。
 */

package jp.co.yumemi.rd.misc;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

public class Util {
    public static String getHexString(byte[] byteArray, int... split) {
        StringBuilder builder = new StringBuilder();
        byte[] target = null;
        if (split.length <= 1) {
            target = byteArray;
        } else if (split.length < 2) {
            target = Arrays.copyOfRange(byteArray, 0, 0 + split[0]);
        } else {
            target = Arrays.copyOfRange(byteArray, split[0], split[0]
                    + split[1]);
        }
        int index = 0;
        for (byte b : target) {
            if (index > 0 && index % 4 == 0) {
                builder.append(" ");
            }
            builder.append(String.format("%02X", b).toUpperCase());
            index++;
        }
        return builder.toString();
    }

    public static String getHexString(byte data) {
        return String.format("%02X", data);
    }

    public static String getHexString(Object[] objList) {
        if (objList == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (Object obj : objList) {
            sb.append(obj.toString()+",");
        }
        return sb.toString();
    }

    /**
     * intent が ACTION_TAG_DISCOVERED なら適当に文字列化して返す。
     * 
     * @param intent
     * @return 文字列化したIntent内の情報
     */
    public static String tagIntent2String(Intent intent) {
        String action = intent.getAction();
        StringBuffer sb = new StringBuffer();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equalsIgnoreCase(action)) {
            Parcelable[] rawMsgs = intent
                    .getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
                sb.append("This is NDEF! ok!"); // TODO: implemet this
            } else {
                // Unknown tag type: Dump 
                Bundle map = intent.getExtras();
                for (String key : map.keySet()) {
                    sb.append("KEY: " + key + "\n");
                    Object obj = map.get(key);
                    String className = obj.getClass().getCanonicalName();
                    if (className.equals("byte[]")) {
                        byte id[] = map.getByteArray(key);
                        sb.append(Util.getHexString(id) + "\n\n");
                    } else {
                        Util.dumpObject(sb, obj, className);
                    }
                }
            }
        } else {
            Log.d("TagIntent2String", "Unknown intent" + intent);
        }
        return sb.toString();
    }
    
    /**
     * オブジェクトのDumpを StringBuffer に出力します。
     * @param sb 出力する StringBuffer
     * @param obj Dumpする対象のObject
     */
    public static void dumpObject(StringBuffer sb, Object obj) {
        dumpObject(sb, obj, obj.getClass().getCanonicalName());
    }
    
    /**
     * @see #dumpObject(StringBuffer, Object)
     */
    public static void dumpObject(StringBuffer sb, Object obj, String className) {
        try {
            Class<?> tag = Class.forName(className);
            // dump fields
            Field fields[] = tag.getDeclaredFields();
            for (Field f : fields) {
                sb.append("Field: " + f.getName() + "\n");
                String cname = f.getType().getCanonicalName();
                sb.append("Type: " + cname + "\n");
                sb.append("Value: ");
                try {
                    if (cname.equals("java.lang.String")) {
                        sb.append(f.get(obj).toString());
                    } else if (cname.equals("byte[]")) {
                        sb.append(Util.getHexString((byte[]) f.get(obj)));
                    } else if (cname.equals("java.lang.String[]")) {
                        sb.append("\n");
                        for (String v : (String[]) f.get(obj)) {
                            sb.append(v + "\n");
                        }
                    } else if (cname.equals("int")) {
                        sb.append(f.getInt(obj));
                    }
                } catch (IllegalAccessException e) {
                    sb.append("Exception:" + e.getMessage());
                }
                sb.append("\n------------\n");
            }
            // dump getter
            for (Method m : tag.getDeclaredMethods()) {
                sb.append("Method: " + m.getName() + "\n");
                String retType = m.getReturnType().getCanonicalName();
                sb.append("ReturnType: " + retType + "\n");
                int numP = m.getParameterTypes().length;
                sb.append("NumParams: " + numP + "\n");
                sb.append("Value: ");
                try {
                    if (numP == 0) {
                        if (retType.equals("int")
                                || retType.equals("java.lang.String")) {
                            sb.append(m.invoke(obj));
                        } else if (retType.equals("byte[]")) {
                            sb.append(Util.getHexString((byte[]) m.invoke(obj)));
                        } else if (retType.equals("java.lang.String[]")) {
                            for (String v : (String[]) m.invoke(obj)) {
                                sb.append(v + "\n");
                            }
                        }
                    }
                } catch (IllegalAccessException e) {
                    sb.append(e.getMessage());
                } catch (InvocationTargetException e) {
                    sb.append(e.getMessage());
                }
                sb.append("\n------------\n");
            }
        } catch (ClassNotFoundException cnfe) {
            sb.append("ClassNotFoundException: " + className);
        }
    }

    /**
     * byte配列を2進数文字列で戻します
     * 
     * @param byteArray byte配列をセット 
     * @return 文字列が戻ります
     */
    public static String getBinString(byte[] byteArray, int... split) {
        StringBuilder builder = new StringBuilder();
        byte[] target = null;
        if ( split.length <= 1 ) {
            target = byteArray;
        } else  if ( split.length < 2 ) {
            target = Arrays.copyOfRange(byteArray, 0, 0 + split[0]);
        } else {
            target = Arrays.copyOfRange(byteArray, split[0], split[0] + split[1]);
        }
        
        for (byte b : target) {
            builder.append(String.format("%8s"
                    , Integer.toBinaryString(b & 0xFF)).replaceAll(" ", "0"));
        }
        return builder.toString();
    }

}
