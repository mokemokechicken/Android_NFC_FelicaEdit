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

import jp.co.yumemi.nfc.FelicaTag;
import jp.co.yumemi.nfc.NfcException;
import jp.co.yumemi.nfc.TagFactory;
import jp.co.yumemi.nfc.FelicaTag.SystemCode;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * １つのFeliCaに含まれるシステムコードの一覧を表示する Activity
 * @author morishita_2
 *
 */
public class SystemList extends ListActivity {
    private SystemCode[] systemCodeList;
    private FelicaTag felica;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        felica = (FelicaTag)TagFactory.create(intent);
        systemCodeList = null;
        try {
            systemCodeList = felica.getSystemCodeList();
            ArrayAdapter<SystemCode> adapter = new ArrayAdapter<SystemCode>(this,
                    android.R.layout.simple_list_item_1, systemCodeList); 
            // アダプタを設定
            setListAdapter(adapter);
        } catch (NfcException e) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, new String[]{"読込みに失敗しました"}); 
            // アダプタを設定
            setListAdapter(adapter);
        }
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if (systemCodeList == null) return;
        SystemCode sc = systemCodeList[position];
        Intent intent = new Intent(SystemList.this, ServiceList.class);
        felica.putTagService(intent);
        // intent.putExtra(NfcTag.ANDROID_NFC_EXTRA_TAG, felica.getTagService());
        intent.putExtra(SystemCode.class.getCanonicalName(), sc.getBytes());
        startActivity(intent);
        
    }
}
