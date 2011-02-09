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
 * ** net.kazzz.felica.command.ReadResponse から複製して修正。
 */

package jp.co.yumemi.nfc;

import java.util.Arrays;

import jp.co.yumemi.nfc.FelicaTag.CommandResponse;
import jp.co.yumemi.rd.misc.Util;

/**
 * Read コマンドのレスポンスを抽象化したクラスを提供します
 * 
 * @author Kazzz
 * @date 2011/01/22
 * @since Android API Level 9
 *
 */

public class ReadResponse extends CommandResponse {
    final int statusFlag1; 
    final int statusFlag2;
    final int blockCount;
    final byte[] blockData; 
    /**
     * コンストラクタ
     * 
     * @param data コマンド実行結果で戻ったバイト列をセット
     */
    public ReadResponse(CommandResponse response) {
        super(response);
        this.statusFlag1 = this.data[0];
        this.statusFlag2 = this.data[1];
        if ( this.getStatusFlag1() == 0 ) {
            this.blockCount  = this.data[2];
            this.blockData = Arrays.copyOfRange(this.data, 3, data.length);
        } else {
            this.blockCount  = 0;
            this.blockData = null;
        }
    }
    
    /**
     * statusFlag1を取得します
     * @return int statusFlag1が戻ります
     */
    public int getStatusFlag1() {
        return this.statusFlag1;
    }

    /**
     * statusFlag2を取得します
     * @return int statusFlag2が戻ります
     */
    public int getStatusFlag2() {
        return this.statusFlag2;
    }

    /**
     * blockDataを取得します
     * @return byte[] blockDataが戻ります
     */
    public byte[] getBlockData() {
        return this.blockData;
    }

    /**
     * blockCountを取得します
     * @return int blockCountが戻ります
     */
    public int getBlockCount() {
        return this.blockCount;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FeliCa レスポンス　パケット \n");
        sb.append(" コマンドコード : " + Util.getHexString(this.responseCode) +  "\n");
        sb.append(" データ長 : " + Util.getHexString(this.length) + "\n");
        if ( this.idm != null )
            sb.append(" " + this.idm.toString() + "\n");
        sb.append(" ステータスフラグ1 : " + Util.getHexString((byte)(this.statusFlag1 & 0xff)) +  "\n");
        sb.append(" ステータスフラグ2 : " + Util.getHexString((byte)(this.statusFlag2 & 0xff)) +  "\n");
        if ( this.blockData != null )
            sb.append(" ブロックデータ:  " + Util.getHexString(this.blockData) + "\n");
        return sb.toString();
    }
}

