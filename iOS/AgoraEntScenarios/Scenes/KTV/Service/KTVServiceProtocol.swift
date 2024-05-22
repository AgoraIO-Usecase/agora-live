//
//  KTVServiceProtocol.swift
//  AgoraEntScenarios
//
//  Created by CP on 2023/7/13.
//

import Foundation
import RTMSyncManager

@objc protocol KTVServiceListenerProtocol: NSObjectProtocol {
    
    func onUserCountUpdate(userCount: UInt)
    
    /// 麦位全量更新
    /// - Parameter seat: <#seat description#>
    func onMicSeatSnapshot(seat: [String: VLRoomSeatModel])
    
    /// 有成员上麦（主动上麦/房主抱人上麦）
    /// - Parameters:
    ///   - seat: 麦位信息
    func onAnchorEnterSeat(seat: VLRoomSeatModel)
    
    
    /// 有成员下麦（主动下麦/房主踢人下麦）
    /// - Parameters:
    ///   - seat: 麦位信息
    func onAnchorLeaveSeat(seat: VLRoomSeatModel)
    
    
    /// 房主对麦位进行了静音/解禁
    /// - Parameters:
    ///   - seat: 麦位信息
    func onSeatAudioMute(seat: VLRoomSeatModel)

    
    /// 房主对麦位摄像头进行禁用/启用
    /// - Parameters:
    ///   - seat: 麦位信息
    func onSeatVideoMute(seat: VLRoomSeatModel)
    
    
    
    /// 新增一首歌曲回调
    /// - Parameter song: <#song description#>
    func onAddChooseSong(song: VLRoomSelSongModel)
    
    /// 删除一首歌歌曲回调
    /// - Parameter song: <#song description#>
    func onRemoveChooseSong(song: VLRoomSelSongModel)
    
    /// 更新一首歌曲回调（例如修改play status）
    /// - Parameter song: <#song description#>
    func onUpdateChooseSong(song: VLRoomSelSongModel)
    
    /// 更新所有歌曲回调（例如pin）
    /// - Parameter song: <#song description#>
    func onUpdateAllChooseSongs(songs: [VLRoomSelSongModel])
    
    
    func onRoomDidExpire()
    
    func onRoomDidDestroy()
}

@objc protocol KTVServiceProtocol: NSObjectProtocol {
    
    // room info
    
    /// 获取房间列表
    /// - Parameters:
    ///   - page: 页码
    ///   - completion: 完成回调
    func getRoomList(page: UInt, completion: @escaping (Error?, [AUIRoomInfo]?) -> Void)
    
    /// 创建房间
    /// - Parameters:
    ///   - inputModel: 输入模型
    ///   - completion: 完成回调
    func createRoom(inputModel: KTVCreateRoomInfo, completion: @escaping (Error?, AUIRoomInfo?) -> Void)
    
    
    /// 加入房间
    /// - Parameters:
    ///   - roomId: 房间id
    ///   - password: 密码
    ///   - completion: 完成回调
    func joinRoom(roomId: String, password: String, completion: @escaping (Error?) -> Void)
    
    /// 离开房间
    /// - Parameter completion: 完成回调
    func leaveRoom(completion: @escaping (Error?) -> Void)
    
    // mic seat
    
    /// 上麦
    /// - Parameters:
    ///   - seatIndex: 麦位索引
    ///   - completion: 完成回调
    func enterSeat(seatIndex: NSNumber?, completion: @escaping (Error?) -> Void)
    
    /// 下麦
    /// - Parameters:
    ///   - completion: 完成回调
    func leaveSeat(completion: @escaping (Error?) -> Void)
    
    /// 设置麦位声音
    /// - Parameters:
    ///   - muted: 是否关闭声音，YES: 关闭声音，NO: 开启声音
    ///   - completion: 完成回调
    func updateSeatAudioMuteStatus(muted: Bool, completion: @escaping (Error?) -> Void)
    
    /// 打开麦位摄像头
    /// - Parameters:
    ///   - muted: 是否关闭摄像头，YES: 关闭摄像头，NO: 开启摄像头
    ///   - completion: 完成回调
    func updateSeatVideoMuteStatus(muted: Bool, completion: @escaping (Error?) -> Void)
    
    // choose songs
    
    /// 删除选中歌曲
    /// - Parameters:
    ///   - songCode: 歌曲id
    ///   - completion: 完成回调
    func removeSong(songCode: String, completion: @escaping (Error?) -> Void)
    
    /// 获取选择歌曲列表
    /// - Parameter completion: 完成回调
    func getChoosedSongsList(completion: @escaping (Error?, [VLRoomSelSongModel]?) -> Void)
    
    /// 主唱告诉后台当前播放的歌曲
    /// - Parameters:
    ///   - songCode: 房间id
    ///   - completion: 完成回调
    func markSongDidPlay(songCode: String, completion: @escaping (Error?) -> Void)
    
    /// 点歌
    /// - Parameters:
    ///   - inputModel: 输入模型
    ///   - completion: 完成回调
    func chooseSong(inputModel: KTVChooseSongInputModel, completion: @escaping (Error?) -> Void)
    
    /// 置顶歌曲
    /// - Parameters:
    ///   - songCode: 歌曲id
    ///   - completion: 完成回调
    func pinSong(songCode: String, completion: @escaping (Error?) -> Void)
    
    // lyrics
    
    /// 加入合唱
    /// - Parameters:
    ///   - songCode: 歌曲id
    ///   - completion: 完成回调
    func joinChorus(songCode: String, completion: @escaping (Error?) -> Void)
    
    /// 伴唱取消合唱
    /// - Parameter completion: 完成回调
    func coSingerLeaveChorus(completion: @escaping (Error?) -> Void)
    
    /// 当前歌曲合唱改为独唱
    func enterSoloMode()
    
    func subscribe(listener: KTVServiceListenerProtocol?) 
    
    func getCurrentDuration(channelName: String) -> UInt64
}
