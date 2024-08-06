package io.agora.scene.voice.imkit.manager

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import androidx.annotation.Nullable
import io.agora.scene.voice.model.VoiceMemberModel
import io.agora.scene.voice.model.VoiceMicInfoModel
import io.agora.scene.voice.model.VoiceRankUserModel
import io.agora.voice.common.utils.GsonTools
import io.agora.voice.common.utils.LogTools
import io.agora.voice.common.utils.LogTools.logD
import java.io.*

class ChatroomCacheManager {

    private val mSharedPreferences: SharedPreferences by lazy {
        ChatroomConfigManager.getInstance().context.getSharedPreferences(
            "SP_AT_PROFILE",
            Context.MODE_PRIVATE
        )
    }
    private val mEditor: SharedPreferences.Editor
        get() = mSharedPreferences.edit()

    private val mMicInfoMap = mutableMapOf<String, String>()
    private val allInfoMap = mutableMapOf<String, String>()

    private val submitMicList = mutableListOf<VoiceMemberModel>()
    private val submitMicMap = mutableMapOf<String, VoiceMemberModel>()

    private val roomMemberList = mutableListOf<VoiceMemberModel>()
    private val roomMemberMap = mutableMapOf<String, VoiceMemberModel>()

    private val invitationList = mutableListOf<VoiceMemberModel>()
    private val invitationMap = mutableMapOf<String, VoiceMemberModel>()

    private var rankingList = mutableListOf<VoiceRankUserModel>()
    private val rankingMap = mutableMapOf<String, VoiceRankUserModel>()

    private var giftAmount:Int=0

    companion object {
        const val TAG = "ChatroomCacheManager"
        val cacheManager = ChatroomCacheManager()
    }

    fun setGiftAmountCache(amount:Int){
        giftAmount = amount
    }

    fun updateGiftAmountCache(amount:Int){
        giftAmount += amount
        "updateGiftAmountCache(${giftAmount}) ".logD(TAG)
    }

    fun getGiftAmountCache():Int{
        "getGiftAmountCache(${giftAmount}) ".logD(TAG)
        return giftAmount
    }

    var clickCountCache: Int = 0
        set(value) {
            field = value
            LogTools.d(TAG, "updateClickCountCache($field) ")
        }
        get() {
            LogTools.d(TAG, "getClickCache($field) ")
            return field
        }

    fun setKvInfo(kvMap: Map<String,String>){
        for (entry in kvMap.entries) {
            allInfoMap[entry.key] = entry.value
        }
    }

    fun getKvInfo(key: String?):String?{
        return allInfoMap[key]
    }

    fun clearAllCache(){
        allInfoMap.clear()
        clearMemberList()
        clearMicInfo()
        clearSubmitList()
        clearRankList()
        giftAmount = 0
        clickCountCache = 0
    }

    fun setMicInfo(kvMap: Map<String,String>){
        if (mMicInfoMap.isEmpty()){
            mMicInfoMap.putAll(kvMap)
        }else{
            for (mutableEntry in kvMap) {
                if ( mutableEntry.key.contains("mic_")){
                    mMicInfoMap[mutableEntry.key] = mutableEntry.value
                }
            }
        }
    }

    fun clearMicInfo(){
        mMicInfoMap.clear()
    }

    fun getMicInfoMap(): MutableMap<String, String>? {
        return mMicInfoMap
    }

    fun getMicInfoByIndex(micIndex: Int): VoiceMicInfoModel?{
        val indexTag = "mic_$micIndex"
        if (mMicInfoMap.isNotEmpty() && mMicInfoMap.containsKey(indexTag)){
            return GsonTools.toBean(mMicInfoMap[indexTag], VoiceMicInfoModel::class.java)
        }
        return null
    }

    fun getMicInfoByChatUid(chatUid: String):VoiceMicInfoModel?{
        for (entry in mMicInfoMap) {
            val micInfoBean = GsonTools.toBean(entry.value, VoiceMicInfoModel::class.java)
            if (micInfoBean?.member?.chatUid.equals(chatUid)){
                return micInfoBean
            }
        }
        return null
    }

    fun setSubmitMicList(voiceMemberBean: VoiceMemberModel){
        val chatUid = voiceMemberBean.chatUid
        if (chatUid != null){
            submitMicMap[chatUid] = voiceMemberBean
            submitMicList.clear()
            for (entry in submitMicMap.entries) {
                submitMicList.add(entry.value)
            }
        }
    }

    fun getSubmitMicList():MutableList<VoiceMemberModel>{
        return submitMicList
    }

    fun getSubmitMic(chatUid: String):VoiceMemberModel?{
        return if (submitMicMap.containsKey(chatUid)){
            submitMicMap[chatUid]
        }else{
            null
        }
    }

    fun removeSubmitMember(chatUid: String){
        submitMicMap.remove(chatUid)
        submitMicList.clear()
        for (entry in submitMicMap.entries) {
            submitMicList.add(entry.value)
        }
    }

    private fun clearSubmitList(){
        submitMicMap.clear()
        submitMicList.clear()
    }

    fun setMemberList(member: VoiceMemberModel){
        val chatUid = member.chatUid
        if (chatUid != null){
            roomMemberMap[chatUid] = member
            roomMemberList.clear()
            for (entry in roomMemberMap.entries) {
                roomMemberList.add(entry.value)
            }
        }
    }

    fun getMember(chatUid: String): VoiceMemberModel?{
        "roomMemberMap(${roomMemberMap}) getMember: $chatUid ".logD(TAG)
        return roomMemberMap[chatUid]
    }

    fun getMemberList():MutableList<VoiceMemberModel>{
        return roomMemberList
    }

    fun getInvitationList():MutableList<VoiceMemberModel>{
        invitationMap.clear()
        invitationList.clear()
        invitationMap.putAll(roomMemberMap)
        for (entry in getMicInfoMap()?.entries!!) {
            val micInfo = GsonTools.toBean(entry.value, VoiceMicInfoModel::class.java)
            micInfo?.member?.chatUid.let {
                "invitationMap remove(${it})".logD(TAG)
                invitationMap.remove(it)
            }
        }
        for (entry in invitationMap.entries) {
            invitationList.add(entry.value)
        }
        "invitationList(${invitationList})".logD(TAG)
        return invitationList
    }

    fun checkInvitationByChatUid(chatUid:String): Boolean{
        for (entry in getMicInfoMap()?.entries!!) {
            val micInfo = GsonTools.toBean(entry.value, VoiceMicInfoModel::class.java)
            micInfo?.member?.chatUid.let {
                if (it.equals(chatUid)) return true
            }
        }
        return false
    }

    fun removeMember(chatUid: String){
        roomMemberMap.remove(chatUid)
        roomMemberList.clear()
        for (entry in roomMemberMap.entries) {
            roomMemberList.add(entry.value)
        }
    }

    private fun clearMemberList(){
        roomMemberList.clear()
        roomMemberMap.clear()
    }

    fun setRankList(rankBean: VoiceRankUserModel){
        val chatUid = rankBean.chatUid
        if (chatUid != null){
            rankingMap[chatUid] = rankBean
            rankingList.clear()
            for (entry in rankingMap.entries) {
                rankingList.add(entry.value)
            }
        }
    }

    fun getRankList():MutableList<VoiceRankUserModel>{
        val comparator:Comparator<VoiceRankUserModel> = Comparator{ o1, o2 ->
            o2.amount.compareTo(o1.amount)
        }
        rankingList.sortWith(comparator)
        "getRankList (${rankingList})".logD(TAG)
        return rankingList
    }

    fun getRankMap():MutableMap<String, VoiceRankUserModel>{
        return rankingMap
    }

    private fun clearRankList(){
        rankingList.clear()
        rankingMap.clear()
    }

    @SuppressLint("ApplySharedPref")
    fun putString(key: String?, value: String?) {
        mEditor?.putString(key, value)
        mEditor?.commit()
    }

    fun getString(key: String?): String? {
        return getString(key, "")
    }

    fun getString(key: String?, defValue: String?): String? {
        return mSharedPreferences?.getString(key, defValue)
    }

    @SuppressLint("ApplySharedPref")
    fun putBoolean(key: String?, value: Boolean) {
        mEditor?.putBoolean(key, value)
        mEditor?.commit()
    }

    fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return mSharedPreferences?.getBoolean(key, defValue) ?: defValue
    }

    @SuppressLint("ApplySharedPref")
    fun putInt(key: String?, value: Int) {
        mEditor?.putInt(key, value)
        mEditor?.commit()
    }

    fun putList(key: String?, list: List<Serializable?>?) {
        putString(key, obj2Base64(list))
    }

    @Nullable
    fun <E : Serializable?> getList(key: String?): List<E>? {
        return base64ToObj(getString(key)!!) as List<E>?
    }

    fun getInt(key: String?, defValue: Int): Int {
        return mSharedPreferences?.getInt(key, defValue) ?: defValue
    }

    private fun <K : Serializable?, V> putMap(key: String?, map: MutableMap<K, V>?) {
        putString(key, obj2Base64(map))
    }

    @Nullable
    fun <K : Serializable?, V> getMap(key: String?): MutableMap<K, V>? {
        return base64ToObj(getString(key)!!) as MutableMap<K, V>?
    }

    private fun obj2Base64(obj: Any?): String? {
        if (obj == null) {
            return null
        }
        var baos: ByteArrayOutputStream? = null
        var oos: ObjectOutputStream? = null
        var objectStr: String? = null
        try {
            baos = ByteArrayOutputStream()
            oos = ObjectOutputStream(baos)
            oos.writeObject(obj)
            objectStr = String(Base64.encode(baos.toByteArray(), Base64.DEFAULT))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (baos != null) {
                try {
                    baos.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (oos != null) {
                try {
                    oos.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return objectStr
    }

    private fun <T> base64ToObj(base64: String): T? {
        if (TextUtils.isEmpty(base64)) {
            return null
        }
        val objBytes = Base64.decode(base64.toByteArray(), Base64.DEFAULT)
        var bais: ByteArrayInputStream? = null
        var ois: ObjectInputStream? = null
        var t: T? = null
        try {
            bais = ByteArrayInputStream(objBytes)
            ois = ObjectInputStream(bais)
            t = ois.readObject() as T
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            if (bais != null) {
                try {
                    bais.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (ois != null) {
                try {
                    ois.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return t
    }

}