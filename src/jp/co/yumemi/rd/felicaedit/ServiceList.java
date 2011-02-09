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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.co.yumemi.nfc.FelicaTag;
import jp.co.yumemi.nfc.NfcException;
import jp.co.yumemi.nfc.NfcTag;
import jp.co.yumemi.nfc.FelicaTag.ServiceCode;
import jp.co.yumemi.nfc.FelicaTag.SystemCode;
import jp.co.yumemi.rd.misc.SimpleAlert;
import jp.co.yumemi.rd.misc.Util;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

/**
 * １つのシステムコードに含まれるサービスの一覧を表示するActivity.
 * @author morishita_2
 */
public class ServiceList extends Activity implements ExpandableListView.OnChildClickListener{
    private List<ServiceCode> serviceCodeList;
    private FelicaTag felica;
    private SystemCode systemCode;
    
    List<List<Map<String, String>>> childData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.service);
        setTitle(R.string.service_list_title);
        Intent intent = getIntent();
        this.felica = (FelicaTag)NfcTag.create(intent);
        this.systemCode = new SystemCode(intent.getByteArrayExtra(SystemCode.class.getCanonicalName()));
        // ExpandableListView はなかなかややっこしい: see http://d.hatena.ne.jp/rudi/20100720/1279632918
        ExpandableListView listView = (ExpandableListView)findViewById(R.id.service_list);
        TextView tv = new TextView(this); // HeaderView用
        listView.addHeaderView(tv);
        try {
            felica.polling(systemCode);
            tv.setText(String.format("%s\nIDm: %s", systemCode.toString(), felica.getIdm().simpleToString()));
            this.serviceCodeList = felica.getServiceCodeList();
            List<Map<String, String>> groupData = new ArrayList<Map<String,String>>(); // 親ノードリスト
            childData = new  ArrayList<List<Map<String,String>>>(); // 子ノードリスト
            if (serviceCodeList.isEmpty()) {
                new SimpleAlert(this).show("このシステム領域からサービスコードが検出できませんでした", true);
                return;
            }
            for (ServiceCode sc : serviceCodeList) {
                Map<String, String>groupMap = new HashMap<String, String>();
                List<Map<String, String>> children = new ArrayList<Map<String, String>>(); // 対応する子要素を作り、追加
                String groupLabel = sc.toString();
                if (!sc.encryptNeeded()) {
                    for (int addr=0; ;addr++) {
                        Map<String, String> m = createBlockDataObject(sc, addr);
                        if (m == null) {
                            break;
                        }
                        children.add(m);
                    }
                    groupLabel = String.format("%s(%d)", groupLabel, children.size());
                } 
                groupMap.put("service_code", groupLabel);
                groupData.add(groupMap); // 親リストに追加
                childData.add(children); // 子リストに追加
            }
            ExpandableListAdapter adapter = new SimpleExpandableListAdapter(
                    getApplicationContext(),
                    groupData,
                    android.R.layout.simple_expandable_list_item_1,
                    new String[]{"service_code"},
                    new int[]{android.R.id.text1},
                    childData,
                    android.R.layout.simple_expandable_list_item_2,
                    new String[] {"addr", "data"},
                    new int[]{android.R.id.text1, android.R.id.text2}
                    );
            listView.setAdapter(adapter);
            listView.setOnChildClickListener(this);
        } catch (NfcException e) {
            new SimpleAlert(this).show("読込みに失敗しました", true);
        }
    }
    
    private Map<String, String> createBlockDataObject(ServiceCode sc, int addr) throws NfcException {
        byte[] blockdata = felica.readWithoutEncryption(sc, addr); // read FeliCa Block Data
        if (blockdata == null) {
            return null;
        }
        // データを作成・追加
        Map<String, String> curChildMap = new HashMap<String, String>();
        curChildMap.put("addr", String.format("Block %02d", addr));
        curChildMap.put("data", String.format("%s", Util.getHexString(blockdata)));
        return curChildMap;
    }
    
    private int editGroupPosition; // 親(ServiceCode) の位置
    private int editChildPosition; // 親の中の子供の位置(Block No)
    /**
     * 書込み可能なブロックがTapされた場合、編集用 Activityを呼びます。
     */
    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
            int groupPosition, int childPosition, long id) {
        ServiceCode serviceCode = serviceCodeList.get(groupPosition);
        if (serviceCode.isWritable() && !serviceCode.encryptNeeded()) {
            Intent intent = new Intent(ServiceList.this, EditBlock.class);
            felica.putTagService(intent);
            intent.putExtra(SystemCode.class.getCanonicalName(), systemCode.getBytes());
            intent.putExtra(ServiceCode.class.getCanonicalName(), serviceCode.getBytes());
            intent.putExtra(EditBlock.BLOCK_INDEX, childPosition);
            this.editGroupPosition = groupPosition;
            this.editChildPosition = childPosition;
            startActivityForResult(intent, 0);
        } else {
            (new SimpleAlert(this)).show("このブロックはReadOnlyなので書き込めません", false);
        }
        return false;
    }
    
    /**
     * startActivityForResult で呼び出した Activityが終了したらここに戻ります。
     * 再度カードの内容を読み取り、表示内容を更新します。
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        List<Map<String, String>> children = childData.get(editGroupPosition);
        try {
            Map<String, String> b = createBlockDataObject(serviceCodeList.get(editGroupPosition), editChildPosition);
            children.set(editChildPosition, b);
            ((ExpandableListView)findViewById(R.id.service_list)).invalidateViews();
        } catch (NfcException e) {
        }
    }
}
