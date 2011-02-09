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
 * ** net.kazzz.felica.command.PollingResponse から複製して修正。
 */

package jp.co.yumemi.nfc;

import java.util.Arrays;

import jp.co.yumemi.nfc.FelicaTag.CommandResponse;
import jp.co.yumemi.nfc.FelicaTag.PMm;
import jp.co.yumemi.rd.misc.Util;

/**
 * Pollingコマンドのレスポンスを抽象化したクラスを提供します
 * 
 * @author Kazzz
 * @date 2011/01/22
 * @since Android API Level 9
 *
 */

public class PollingResponse extends CommandResponse {
    final PMm pmm; 
    final byte[] requestData; 
    /**
     * コンストラクタ
     * 
     * @param data コマンド実行結果で戻ったバイト列をセット
     */
    public PollingResponse(CommandResponse response) {
        super(response);
        this.pmm = new PMm(Arrays.copyOfRange(this.data, 0, 8));
        this.requestData = Arrays.copyOfRange(this.data, 8, data.length);
    }
    /**
     * PMmを取得します
     * 
     * @return PMm pmmが戻ります
     */
    public PMm getPMm() {
        return this.pmm;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FeliCa レスポンス　パケット \n");
        sb.append(" コマンド名 :" + Util.getHexString(this.responseCode)  +  "\n");
        sb.append(" データ長 : " + Util.getHexString(this.length) + "\n");
        sb.append(" コマンドコード : " + Util.getHexString(this.responseCode) +  "\n");
        if ( this.idm != null )
            sb.append(" " + this.idm.toString() + "\n");
        if ( this.pmm != null )
            sb.append(" " + this.pmm.toString() + "\n");
        sb.append(" データ: " + Util.getHexString(this.data) + "\n");
        return sb.toString();
    }
}
