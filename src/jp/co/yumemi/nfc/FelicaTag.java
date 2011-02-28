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
 * * 2011/2/5: k_morishita
 * ** net.kazzz.felica.lib.FeliCaLib.java を複製して修正。
 */

package jp.co.yumemi.nfc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.util.Log;

import jp.co.yumemi.rd.misc.Util;

public class FelicaTag extends NfcTag {
    private static String TAG = "FelicaTag";

    // polling
    public static final byte COMMAND_POLLING = 0x00;
    public static final byte RESPONSE_POLLING = 0x01;

    // request service
    public static final byte COMMAND_REQUEST_SERVICE = 0x02;
    public static final byte RESPONSE_REQUEST_SERVICE = 0x03;

    // request RESPONSE
    public static final byte COMMAND_REQUEST_RESPONSE = 0x04;
    public static final byte RESPONSE_REQUEST_RESPONSE = 0x05;

    // read without encryption
    public static final byte COMMAND_READ_WO_ENCRYPTION = 0x06;
    public static final byte RESPONSE_READ_WO_ENCRYPTION = 0x07;

    // write without encryption
    public static final byte COMMAND_WRITE_WO_ENCRYPTION = 0x08;
    public static final byte RESPONSE_WRITE_WO_ENCRYPTION = 0x09;

    // search service code
    public static final byte COMMAND_SEARCH_SERVICECODE = 0x0a;
    public static final byte RESPONSE_SEARCH_SERVICECODE = 0x0b;

    // request system code
    public static final byte COMMAND_REQUEST_SYSTEMCODE = 0x0c;
    public static final byte RESPONSE_REQUEST_SYSTEMCODE = 0x0d;

    // authentication 1
    public static final byte COMMAND_AUTHENTICATION1 = 0x10;
    public static final byte RESPONSE_AUTHENTICATION1 = 0x11;

    // authentication 2
    public static final byte COMMAND_AUTHENTICATION2 = 0x12;
    public static final byte RESPONSE_AUTHENTICATION2 = 0x13;

    // read
    public static final byte COMMAND_READ = 0x14;
    public static final byte RESPONSE_READ = 0x15;

    // write
    public static final byte COMMAND_WRITE = 0x16;
    public static final byte RESPONSE_WRITE = 0x17;

    protected IDm idm;
    protected final NfcF felicaTag;

    public IDm getIdm() {
        return idm;
    }

    public FelicaTag(Tag tag) {
        super(tag);
        felicaTag = NfcF.get(tag);
        this.idm = new IDm(getId());
    }

    protected CommandResponse execute(CommandPacket commandPacket)
            throws NfcException {
        byte[] result;

        if (this.felicaTag == null) {
            throw new NfcException("felicaTag is null!");
        }

        try {
            if (!felicaTag.isConnected()) {
                felicaTag.connect();
            }
            result = felicaTag.transceive(commandPacket.getBytes());
        } catch (IOException e) {
            throw new NfcException(e);
        }
        return new CommandResponse(result);
    }

    /**
     * Pollingを行います。正常に終了した場合、取得したIDmを自身オブジェクト内に保存します。
     * 
     * @param systemCode
     *            Pollingを行う対象の systemCode
     * @return そのシステム領域の IDm を返します。
     * @throws NfcException
     */
    public IDm polling(int systemCode) throws NfcException {
        CommandPacket polling = new CommandPacket(COMMAND_POLLING, new byte[] {
                (byte) (systemCode >> 8) // システムコード
                , (byte) (systemCode & 0xff), (byte) 0x01 // 　システムコードリクエスト
                , (byte) 0x00 }); // タイムスロット};
        return doPolling(polling);
    }

    /**
     * @see #polling(int)
     */
    public IDm polling(SystemCode systemCode) throws NfcException {
        byte bytes[] = systemCode.getBytes();
        CommandPacket polling = new CommandPacket(COMMAND_POLLING, new byte[] {
                (byte) bytes[0] // システムコード
                , (byte) bytes[1], (byte) 0x01 // 　システムコードリクエスト
                , (byte) 0x00 }); // タイムスロット};
        return doPolling(polling);
    }

    private IDm doPolling(CommandPacket polling) throws NfcException {
        CommandResponse r = execute(polling);
        PollingResponse pr = new jp.co.yumemi.nfc.PollingResponse(r);
        this.idm = pr.getIDm();
        return idm;
    }

    /**
     * @see #getSystemCodeList(IDm)
     */
    public SystemCode[] getSystemCodeList() throws NfcException {
        return getSystemCodeList(this.getIdm());
    }

    /**
     * SystemCodeの一覧を取得します。
     * 
     * @return 検出された SystemCodeの一覧を返します。
     * @throws NfcException
     */
    public SystemCode[] getSystemCodeList(IDm idm) throws NfcException {
        // request systemCode
        CommandPacket reqSystemCode = new CommandPacket(
                COMMAND_REQUEST_SYSTEMCODE, idm);
        CommandResponse r = execute(reqSystemCode);
        byte[] retBytes = r.getBytes();
        int num = (int) retBytes[10];
        Log.d(TAG, "Num SystemCode: " + num);
        SystemCode retCodeList[] = new SystemCode[num];
        for (int i = 0; i < num; i++) {
            retCodeList[i] = new SystemCode(Arrays.copyOfRange(retBytes,
                    11 + i * 2, 13 + i * 2));
        }
        return retCodeList;
    }

    /**
     * 前回 Polling したシステム領域のサービスの一覧を取得します。
     * 必ずしもそのシステム領域をPollingしなくても取得できるのかもしれません。
     * 
     * このServiceCode取得コマンドの仕様がよくわからなかったので、手探りの結果良さそうな方法を実装してあります。
     * 戻ってきたデータが2バイト長なら ServiceCode と判定していますが、正しい判定法なのかは定かではないです。
     * 
     * @return 検出された ServiceCode の List
     * @throws NfcException
     */
    public List<ServiceCode> getServiceCodeList() throws NfcException {
        int index = 1; // 0番目は root area らしいので、1番目から始めます。
        List<ServiceCode> serviceCodeList = new ArrayList<ServiceCode>();
        while (true) {
            byte[] bytes = doSearchServiceCode(idm, index); // 1件1件 通信して聞き出します。
            if (bytes.length != 2 && bytes.length != 4)
                break; // 2 or 4 バイトじゃない場合は、とりあえず終了しておきます。正しい判定ではないかもしれません。
            if (bytes.length == 2) { // 2バイトは ServiceCode として扱っています。
                if (bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xff)
                    break; // FFFF が終了コードのようです
                serviceCodeList.add(new ServiceCode(bytes));
            }
            index++;
        }
        return serviceCodeList;
    }

    /**
     * COMMAND_SEARCH_SERVICECODE を実行します。 参考:
     * http://wiki.osdev.info/index.php?PaSoRi%2FRC-S320#content_1_25
     * 
     * @param idm
     *            問い合わせるシステム領域のIDm
     * @param index
     *            ？番目か
     * @return Response部分
     * @throws NfcException
     */
    public byte[] doSearchServiceCode(IDm idm, int index) throws NfcException {
        CommandPacket reqServiceCode = new CommandPacket(
                COMMAND_SEARCH_SERVICECODE, idm, new byte[] {
                        (byte) (index & 0xff), (byte) (index >> 8) });
        CommandResponse r = execute(reqServiceCode);
        byte[] bytes = r.getBytes();
        if (bytes[1] != (byte) 0x0b) { // 正常応答かどうか
            throw new NfcException("ResponseCode is not 0x0b");
        }
        return Arrays.copyOfRange(bytes, 10, bytes.length);
    }

    /**
     * @see #readWithoutEncryption(ServiceCode, byte)
     */
    public byte[] readWithoutEncryption(int serviceCode, byte addr)
            throws NfcException {
        return readWithoutEncryption(new ServiceCode(serviceCode), addr);
    }

    /**
     * 認証不要なサービスコードのデータを読み取ります。 本来は、複数のブロックを同時に読めます。 JIS_X_6319_4
     * を見ると複数ブロックにアクセスする方法がわかります。
     * 
     * @param serviceCode
     *            データを読み取るサービスコード
     * @param addr
     *            何ブロック目を読むか。 0～N
     * @return 読み取ったブロックのByte列を返します。読み取りステータスが正常でなければnullを返します。
     * @throws NfcException
     */
    public byte[] readWithoutEncryption(ServiceCode serviceCode, int addr)
            throws NfcException {
        byte[] bytes = serviceCode.getBytes();
        CommandPacket readWoEncrypt = new CommandPacket(
                COMMAND_READ_WO_ENCRYPTION, idm, new byte[] { (byte) 0x01 // サービス数
                        , (byte) bytes[0], (byte) bytes[1], (byte) 0x01 // 同時読み込みブロック数
                        , (byte) 0x80, (byte) addr }); // ブロックリスト

        CommandResponse r = execute(readWoEncrypt);
        ReadResponse rr = new ReadResponse(r);
        if (rr.getStatusFlag1() == 0) {
            return rr.getBlockData();
        } else {
            return null; // error
        }
    }

    /**
     * 本来は複数ブロックやサービスに同時に書き込めますが、この実装は１ブロックだけです。 JIS_X_6319_4
     * を見ると複数ブロックに書き込む方法がわかります。
     * 
     * @param serviceCode
     *            書込むサービスコード
     * @param addr
     *            何ブロック目に書き込むか。 0～N
     * @param buff
     *            書きこむブロックデータ. 16バイトである必要があります。
     * @return 0: 正常終了, -1: 異常終了
     * @throws NfcException
     */
    public int writeWithoutEncryption(ServiceCode serviceCode, int addr,
            byte[] buff) throws NfcException {
        if (buff == null || buff.length != 16) {
            return -1;
        }

        byte[] bytes = serviceCode.getBytes();
        ByteBuffer b = ByteBuffer.allocate(6 + buff.length);
        b.put(new byte[] { (byte) 0x01 // Number of Service
                , (byte) bytes[0] // サービスコード (little endian)
                , (byte) bytes[1], (byte) 1 // 同時書き込みブロック数
                , (byte) 0x80, (byte) addr // ブロックリスト
                });
        b.put(buff); // 書き出すデータ

        CommandPacket writeWoEncrypt = new CommandPacket(
                COMMAND_WRITE_WO_ENCRYPTION, idm, b.array());
        CommandResponse r = execute(writeWoEncrypt);
        byte[] retBytes = r.getBytes();
        if (retBytes != null && retBytes.length > 10
                && retBytes[10] == (byte) 0) {
            return 0; // normal
        } else {
            return -1; // error
        }
    }

    /**
     * @see #writeWithoutEncryption(ServiceCode, byte, byte[])
     */
    public int writeWithoutEncryption(int serviceCode, byte addr, byte[] buff)
            throws NfcException {
        return writeWithoutEncryption(new ServiceCode(serviceCode), addr, buff);
    }

    // ////////////////////////////////////////////////////////////////////
    // 以下、データ構造的Class
    // ////////////////////////////////////////////////////////////////////
    /**
     * FeliCa コマンドパケットクラスを提供します
     * 
     * @author Kazzz
     * @date 2011/01/20
     * @since Android API Level 9
     */
    public static class CommandPacket {
        protected final byte length; // 全体のデータ長
        protected final byte commandCode;// コマンドコード
        protected final IDm idm; // FeliCa IDm
        protected final byte[] data; // コマンドデータ

        /**
         * コンストラクタ
         * 
         * @param response
         *            他のレスポンスをセット
         */
        public CommandPacket(CommandPacket command) {
            this(command.getBytes());
        }

        /**
         * コンストラクタ
         * 
         * @param data
         *            コマンドパケット全体を含むバイト列をセット
         * @throws FeliCaException
         */
        public CommandPacket(final byte[] data) {
            this(data[0], Arrays.copyOfRange(data, 1, data.length));
        }

        /**
         * コンストラクタ
         * 
         * @param commandCode
         *            コマンドコードをセット
         * @param data
         *            コマンドデータをセット (IDmを含みます)
         * @throws FeliCaException
         */
        public CommandPacket(byte commandCode, final byte... data) {
            this.commandCode = commandCode;
            if (data.length >= 8) {
                this.idm = new IDm(Arrays.copyOfRange(data, 0, 8));
                this.data = Arrays.copyOfRange(data, 8, data.length);
            } else {
                this.idm = null;
                this.data = Arrays.copyOfRange(data, 0, data.length);
            }
            this.length = (byte) (data.length + 2);
        }

        /**
         * コンストラクタ
         * 
         * @param commandCode
         *            コマンドコードをセット
         * @param idm
         *            システム製造ID(IDm)をセット
         * @param data
         *            コマンドデータをセット
         * @throws FeliCaException
         */
        public CommandPacket(byte commandCode, IDm idm, final byte... data) {
            this.commandCode = commandCode;
            this.idm = idm;
            this.data = data;
            this.length = (byte) (idm.getBytes().length + data.length + 2);
        }

        /**
         * コンストラクタ
         * 
         * @param commandCode
         *            コマンドコードをセット
         * @param idm
         *            システム製造ID(IDm)がセットされたバイト配列をセット
         * @param data
         *            コマンドデータをセット
         * @throws FeliCaException
         */
        public CommandPacket(byte commandCode, byte[] idm, final byte... data) {
            this.commandCode = commandCode;
            this.idm = new IDm(idm);
            this.data = data;
            this.length = (byte) (idm.length + data.length + 2);
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.felica.IFeliCaCommand#getIDm()
         */
        public IDm getIDm() {
            return this.idm;
        }

        /**
         * バイト列表現を戻します
         * 
         * @return byte[] このデータのバイト列表現を戻します
         */
        public byte[] getBytes() {
            ByteBuffer buff = ByteBuffer.allocate(this.length);
            if (this.idm != null) {
                buff.put(this.length).put(this.commandCode).put(
                        this.idm.getBytes()).put(this.data);
            } else {
                buff.put(this.length).put(this.commandCode).put(this.data);
            }
            return buff.array();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("FeliCa コマンドパケット \n");
            sb.append(" コマンド名:" + Util.getHexString(this.commandCode) + "\n");
            sb.append(" データ長: " + Util.getHexString(this.length) + "\n");
            sb.append(" コマンドコード : " + Util.getHexString(this.commandCode)
                    + "\n");
            if (this.idm != null)
                sb.append(" " + this.idm.toString() + "\n");
            sb.append(" データ: " + Util.getHexString(this.data) + "\n");
            return sb.toString();
        }

    }

    /**
     * FeliCa コマンドレスポンスクラスを提供します
     * 
     * @author Kazz
     * @since Android API Level 9
     */
    public static class CommandResponse {
        protected final byte[] rawData;
        protected final byte length; // 全体のデータ長 (FeliCaには無い)
        protected final byte responseCode;// コマンドレスポンスコード)
        protected final IDm idm; // FeliCa IDm
        protected final byte[] data; // コマンドデータ

        /**
         * コンストラクタ
         * 
         * @param response
         *            他のレスポンスをセット
         */
        public CommandResponse(CommandResponse response) {
            this(response.getBytes());
        }

        /**
         * コンストラクタ
         * 
         * @param data
         *            コマンド実行結果で戻ったバイト列をセット
         */
        public CommandResponse(byte[] data) {
            this.rawData = data;
            this.length = data[0];
            this.responseCode = data[1];
            this.idm = new IDm(Arrays.copyOfRange(data, 2, 10));
            this.data = Arrays.copyOfRange(data, 10, data.length);
        }

        /*
         * (non-Javadoc)
         */
        public IDm getIDm() {
            return this.idm;
        }

        /**
         * バイト列表現を戻します
         * 
         * @return byte[] このデータのバイト列表現を戻します
         */
        public byte[] getBytes() {
            return this.rawData;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(" \n\n");
            sb.append("FeliCa レスポンスパケット \n");
            sb.append(" コマンド名:" + Util.getHexString(this.responseCode) + "\n");
            sb.append(" データ長: " + Util.getHexString(this.length) + "\n");
            sb.append(" レスポンスコード: " + Util.getHexString(this.responseCode)
                    + "\n");
            sb.append(" " + this.idm.toString() + "\n");
            sb.append(" データ: " + Util.getHexString(this.data) + "\n");
            return sb.toString();
        }
    }

    /**
     * 
     * FeliCa IDmクラスを提供します
     * 
     * @author Kazzz
     * @date 2011/01/20
     * @since Android API Level 9
     */
    public static class IDm {
        final byte[] manufactureCode;
        final byte[] cardIdentification;

        /**
         * コンストラクタ
         * 
         * @param bytes
         *            IDmの格納されているバイト列をセットします
         */
        public IDm(byte[] bytes) {
            this.manufactureCode = new byte[] { bytes[0], bytes[1] };
            this.cardIdentification = new byte[] { bytes[2], bytes[3],
                    bytes[4], bytes[5], bytes[6], bytes[7] };
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.felica.IFeliCaByteData#getBytes()
         */
        public byte[] getBytes() {
            ByteBuffer buff = ByteBuffer.allocate(this.manufactureCode.length
                    + this.cardIdentification.length);
            buff.put(this.manufactureCode).put(this.cardIdentification);
            return buff.array();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("IDm (8byte) : " + Util.getHexString(this.getBytes())
                    + "\n");
            sb.append(" 製造者コード: " + Util.getHexString(this.manufactureCode)
                    + "\n");
            sb.append(" カード識別番号:\n");
            sb.append("   製造器:"
                    + Util.getHexString(this.cardIdentification, 0, 2) + "\n");
            sb.append("   日付:"
                    + Util.getHexString(this.cardIdentification, 2, 2) + "\n");
            sb.append("   シリアル:"
                    + Util.getHexString(this.cardIdentification, 4, 2) + "\n");
            return sb.toString();
        }

        public String simpleToString() {
            return Util.getHexString(getBytes());
        }

    }

    /**
     * 
     * FeliCa PMmクラスを提供します
     * 
     * @author Kazzz
     * @date 2011/01/20
     * @since Android API Level 9
     */
    public static class PMm {
        final byte[] icCode; // ROM種別, IC種別
        final byte[] maximumResponseTime; // 最大応答時間

        /**
         * コンストラクタ
         * 
         * @param bytes
         *            バイト列をセット
         */
        public PMm(byte[] bytes) {
            this.icCode = new byte[] { bytes[0], bytes[1] };
            this.maximumResponseTime = new byte[] { bytes[2], bytes[3],
                    bytes[4], bytes[5], bytes[6], bytes[7] };
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.felica.IFeliCaByteData#getBytes()
         */
        public byte[] getBytes() {
            ByteBuffer buff = ByteBuffer.allocate(this.icCode.length
                    + this.maximumResponseTime.length);
            buff.put(this.icCode).put(this.maximumResponseTime);
            return buff.array();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("PMm(製造パラメータ)\n");
            sb
                    .append(" ICコード(2byte): " + Util.getHexString(this.icCode)
                            + "\n");
            sb.append("   ROM種別: " + Util.getHexString(this.icCode, 0, 1)
                    + "\n");
            sb.append("   IC 種別: " + Util.getHexString(this.icCode, 1, 1)
                    + "\n");
            sb.append("\n");
            sb.append(" 最大応答時間パラメタ(6byte)\n");
            sb.append("  B3(request service):"
                    + Util.getBinString(this.maximumResponseTime, 0, 1) + "\n");
            sb.append("  B4(request response):"
                    + Util.getBinString(this.maximumResponseTime, 1, 1) + "\n");
            sb.append("  B5(authenticate):"
                    + Util.getBinString(this.maximumResponseTime, 2, 1) + "\n");
            sb.append("  B6(read):"
                    + Util.getBinString(this.maximumResponseTime, 3, 1) + "\n");
            sb.append("  B7(write):"
                    + Util.getBinString(this.maximumResponseTime, 4, 1) + "\n");
            sb.append("  B8():"
                    + Util.getBinString(this.maximumResponseTime, 5, 1) + "\n");
            return sb.toString();
        }
    }

    /**
     * FeliCa SystemCodeクラスを提供します
     * 
     * @author Kazzz
     * @date 2011/01/20
     * @since Android API Level 9
     */
    public static class SystemCode {
        final byte[] systemCode;

        /**
         * コンストラクタ
         * 
         * @param bytes
         *            バイト列をセット
         */
        public SystemCode(byte[] bytes) {
            this.systemCode = bytes;
        }

        /*
         * (non-Javadoc)
         */
        public byte[] getBytes() {
            return this.systemCode;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("システムコード : " + Util.getHexString(this.systemCode));
            return sb.toString();
        }

        public String simpleToString() {
            return Util.getHexString(systemCode);
        }
    }

    /**
     * FeliCa ServiceCodeクラスを提供します
     * 
     * @author Kazzz
     * @date 2011/01/20
     * @since Android API Level 9
     */
    public static class ServiceCode {
        final byte[] serviceCode;
        final byte[] serviceCodeLE; // little endian

        /**
         * コンストラクタ
         * 
         * @param bytes
         *            バイト列をセット
         */
        public ServiceCode(byte[] bytes) {
            this.serviceCode = bytes;
            if (bytes.length == 2) {
                this.serviceCodeLE = new byte[] { bytes[1], bytes[0] };
            } else {
                this.serviceCodeLE = null;
            }
        }

        public ServiceCode(int serviceCode) {
            this(new byte[] { (byte) (serviceCode & 0xff),
                    (byte) (serviceCode >> 8) });
        }

        /*
         * サービスコードをバイト列として返します。
         * 
         * @return サービスコードのバイト列表現
         */
        public byte[] getBytes() {
            return this.serviceCode;
        }

        /**
         * このサービスコードは、認証が必要か？
         * 
         * @return 必要ならTrue
         * @author morishita_2
         */
        public boolean encryptNeeded() {
            boolean ret = false;
            if (serviceCodeLE != null) {
                ret = (serviceCodeLE[1] & 0x1) == 0;
            }
            return ret;
        }

        /**
         * このサービスコードは書込み可能か？
         * 
         * @return 書込み可能ならTrue
         * @author morishita_2
         */
        public boolean isWritable() {
            boolean ret = false;
            if (serviceCodeLE != null) {
                int accessInfo = serviceCodeLE[1] & 0x3F; // 下位6bitがアクセス情報
                ret = (accessInfo & 0x2) == 0 || accessInfo == 0x13
                        || accessInfo == 0x12;
            }
            return ret;
        }

        /**
         * サービスコードのアクセス権の意味は、JIS_X_6319_4 を参照しました。
         * 
         * @author morishita_2
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(Util.getHexString(serviceCodeLE));
            if (serviceCodeLE != null) {
                int accessInfo = serviceCodeLE[1] & 0x3F; // 下位6bitがアクセス情報
                switch (accessInfo) {
                case 0x09:
                    sb.append(" 固定長RW");
                    break; // RW: ReadWrite
                case 0x0b:
                    sb.append(" 固定長RO");
                    break; // RO: ReadOnly
                case 0x0d:
                    sb.append(" 循環RW");
                    break;
                case 0x0f:
                    sb.append(" 循環RO");
                    break;
                case 0x11:
                    sb.append(" 加減算直接");
                    break;
                case 0x13:
                    sb.append(" 加減算戻入");
                    break;
                case 0x15:
                    sb.append(" 加減算減算");
                    break;
                case 0x17:
                    sb.append(" 加減算RO");
                    break;
                //
                case 0x08:
                    sb.append(" 固定長RW(Locked)");
                    break; // RW: ReadWrite
                case 0x0a:
                    sb.append(" 固定長RO(Locked)");
                    break; // RO: ReadOnly
                case 0x0c:
                    sb.append(" 循環RW(Locked)");
                    break;
                case 0x0e:
                    sb.append(" 循環RO(Locked)");
                    break;
                case 0x10:
                    sb.append(" 加減算直接(Locked)");
                    break;
                case 0x12:
                    sb.append(" 加減算戻入(Locked)");
                    break;
                case 0x14:
                    sb.append(" 加減算減算(Locked)");
                    break;
                case 0x16:
                    sb.append(" 加減算RO(Locked)");
                    break;
                }

            }
            // sb.append("\n");
            return sb.toString();
        }
    }

    /**
     * カードの種別を返します。
     */
    @Override
    public String getType() {
        return TYPE_FELICA;
    }
}
