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

package jp.co.yumemi.rd.misc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

/**
 * 簡易メッセージダイアログ用クラス。
 * @author morishita_2
 *
 */
public class SimpleAlert implements OnClickListener {
    private final Activity parent;
    
    private AlertDialog.Builder builder;
    public AlertDialog.Builder getBuilder() {
        return builder;
    }

    private boolean finishParent;
    public SimpleAlert(Activity activity) {
        this.parent = activity;
    }
    
    /**
     * ダイアログにメッセージとOKボタンを表示します。
     * @param message 表示するMessage
     * @param finishParent if true ならば、閉じたときに parent.finish() を呼び出します。
     */
    public void show(String message, boolean finishParent) {
        if (builder == null) {
            initBuilder();
        }
        this.finishParent = finishParent;
        builder.setMessage(message);
        builder.create().show();
    }

    public AlertDialog.Builder initBuilder() {
        builder = new AlertDialog.Builder(parent);
        builder.setPositiveButton("OK", this);
        return builder;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (finishParent) {
            this.parent.finish();
        }
    }
}
