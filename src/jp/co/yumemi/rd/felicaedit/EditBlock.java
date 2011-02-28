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
import java.util.List;

import jp.co.yumemi.nfc.FelicaTag;
import jp.co.yumemi.nfc.NfcException;
import jp.co.yumemi.nfc.TagFactory;
import jp.co.yumemi.nfc.FelicaTag.ServiceCode;
import jp.co.yumemi.nfc.FelicaTag.SystemCode;
import jp.co.yumemi.rd.misc.SimpleAlert;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemClickListener;

/**
 * 特定のサービスコード・ブロックを編集するための Activity
 * @author k_morishita
 */
public class EditBlock extends Activity implements DialogInterface.OnClickListener, View.OnClickListener, OnItemClickListener {
    private static final String TAG = "EditBlock";
    private static final String BYTE_DATA = "data";
    public static final String BLOCK_INDEX = "BLOCK_INDEX";
    private static final int BYTE_DIALOG = 1; 
    
    private FelicaTag felica; // FeliCa オブジェクト
    private SystemCode systemCode; // システムコード
    private ServiceCode serviceCode; // サービスコード
    private int blockIndex; // 編集対象BlockのIndex
    private byte[] blockData; // 対象BlockのData

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        this.felica = (FelicaTag)TagFactory.create(intent);
        this.systemCode = new SystemCode(intent.getByteArrayExtra(SystemCode.class.getCanonicalName()));
        this.serviceCode = new ServiceCode(intent.getByteArrayExtra(ServiceCode.class.getCanonicalName()));
        this.blockIndex = intent.getIntExtra(BLOCK_INDEX, -1);
        setContentView(R.layout.edit_block);
        findViewById(R.id.btn_save).setOnClickListener(this);
        ((ListView)findViewById(R.id.data_list_view)).setOnItemClickListener(this);
        try {
            felica.polling(systemCode);
            blockData = felica.readWithoutEncryption(serviceCode, blockIndex);
            updateList();
        } catch (NfcException e) {
            new SimpleAlert(this).show("データを読み込めませんでした", true);
        }
    }
    
    /**
     * 画面を更新する
     */
    private void updateList() {
        if (blockData == null) return;
        List<String> byteList = new ArrayList<String>();
        for (int i=0; i<blockData.length; i++) {
            byteList.add(String.format("%02X: %02X", i, blockData[i]));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, byteList); 
        // アダプタを設定
        ListView lv = (ListView)findViewById(R.id.data_list_view);
        lv.setAdapter(adapter);
    }
    
    /**
     * 編集用に選択された位置を保持
     */
    private int bytePosition;
    
    /**
     * BlockDataの中のどのByteを編集するか、という画面で、１つの項目がTapされた。
     */
    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        if (blockData == null) return;
        byte data = blockData[position];
        Bundle bundle = new Bundle();
        bundle.putByte(BYTE_DATA, data);
        bytePosition = position;
        showDialog(BYTE_DIALOG, bundle);
    }

    /**
     * Save ボタンが押された。書込みを行い、呼び出し元Activityに戻ります。
     */
    @Override
    public void onClick(View v) {
        SimpleAlert sa = new SimpleAlert(this);
        if (blockData != null) {
            try {
                if (felica.writeWithoutEncryption(serviceCode, blockIndex, blockData) == 0) {
                    
                    sa.show("保存しました", true);
                } else {
                    sa.show("保存失敗しました", true);
                }
            } catch (NfcException e) {
                sa.show("保存に失敗しました\n"+e.getMessage(), true);
            }
        }
    }

    //
    // 個々以降は、 ByteEdit Dialog 用の部分です。まとまりが悪い。。
    //
    /**
     * 最初に編集用ダイアログ（Selectが２つ並んでいるダイアログ）が呼び出されたときに、実行されます。
     */
    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater factory = LayoutInflater.from(this);
        final View inputView = factory.inflate(R.layout.edit_block_byte, null);
        builder.setView(inputView)
                .setTitle("入力してください")
                .setPositiveButton("OK", this)
                .setNegativeButton("CANCEL", this);
        return builder.create();
    }

    private Spinner sp1;
    private Spinner sp2;
    /**
     * 編集用ダイアログが呼び出される度に実行されます。
     */
    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        super.onPrepareDialog(id, dialog, args);
        sp1 = (Spinner)dialog.findViewById(R.id.sp1);
        sp2 = (Spinner)dialog.findViewById(R.id.sp2);
        byte data = args.getByte(BYTE_DATA);
        sp1.setSelection((int)((data >>> 4) & 0x0F)); // 元データをデフォルト値として設定
        sp2.setSelection((int)(data & 0x0F)); // 元データをデフォルト値として設定
    }

    /**
     * ByteEdit Dialog で OK か NG が押された。
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch(which) {
        case DialogInterface.BUTTON_POSITIVE:
            byte data = (byte)((sp1.getSelectedItemPosition() << 4) + sp2.getSelectedItemPosition());
            blockData[bytePosition] = data;
            updateList();
            break;
           
        case DialogInterface.BUTTON_NEGATIVE:
            break;
        }
        sp1 = sp2 = null;
    }
}
