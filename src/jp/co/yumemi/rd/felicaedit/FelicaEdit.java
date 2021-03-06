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

package jp.co.yumemi.rd.felicaedit;

import jp.co.yumemi.nfc.NfcTag;
import jp.co.yumemi.nfc.TagFactory;
import jp.co.yumemi.rd.misc.Util;
import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 起動時に呼び出される Activity。
 * 結局あまり仕事していないので不要かもしれない。
 * 
 * 何度か起動しているうちに、ちゃんとTagを読まなくなることがあります(><).
 * その場合は、アプリを一旦終了してから、かざしてみてください。
 * @author k_morishita
 */
public class FelicaEdit extends Activity {
    static private String TAG = "FelicaEdit";
    private NfcTag nfcTag;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Intent intent = this.getIntent();
        String action = intent.getAction();
        
        if (!NfcAdapter.getDefaultAdapter().isEnabled()) {
            setText("NFCが使えません");
        } else {
            if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
                this.nfcTag = TagFactory.create(intent);
                String txt = action + "\n" + nfcTag.getTechListAsString();
                Toast.makeText(this, txt, 15).show();
                scan();
            }
        }
    }
    
    private void setText(String text) {
        TextView tv = (TextView)findViewById(R.id.textView1);
        tv.setText(text);
    }
    
    private void scan() {
        if (nfcTag == null || !nfcTag.getType().equals(NfcTag.TYPE_FELICA)) {
            StringBuffer sb = new StringBuffer();
            sb.append("FeliCaカードではないようです\n");
            sb.append("TagType: " + nfcTag.getType()+"\n");
            sb.append(Util.getHexString(nfcTag.getId())+"\n");
            if (nfcTag != null) {
                sb.append(nfcTag.getTechListAsString()+"\n");
            }
            setText(sb.toString());
            return;
        }
        Intent intent = new Intent(FelicaEdit.this, SystemList.class);
        nfcTag.putTagService(intent);
        startActivity(intent);
        finish();
    }

}