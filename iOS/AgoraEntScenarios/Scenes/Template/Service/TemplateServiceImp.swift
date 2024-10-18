//
//  TemplateServiceImp.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/10/18.
//

import Foundation

class TemplateServiceImp: NSObject {
    var channelName: String?
}

extension TemplateServiceImp: TemplateServiceProtocol {
    func join(roomName: String, completion: @escaping (Error?, TemplateScene.JoinResponse?) -> Void) {
    }

    func leave() {
    }

    func removeRoom() {
    }

    func addUser(user: TemplateScene.UsersModel, completion: @escaping (Error?, TemplateScene.UsersModel?) -> Void) {
    }

    func removeUser(user: TemplateScene.UsersModel, completion: @escaping (Error?, [TemplateScene.UsersModel]?) -> Void) {
    }

    func updateUser(user: TemplateScene.UsersModel, completion: @escaping (Error?, TemplateScene.UsersModel?) -> Void) {
    }

    func getUserStatus(completion: @escaping (Error?, [TemplateScene.UsersModel]?) -> Void) {
    }

    func subscribeRoom(subscribeClosure: @escaping (TemplateScene.SubscribeStatus, TemplateScene.LiveRoomInfo?) -> Void,
                       onSubscribed: (() -> Void)?,
                       fail: ((Error) -> Void)?) {
    }

    func subscribeUser(subscribeClosure: @escaping (TemplateScene.SubscribeStatus, TemplateScene.UsersModel?) -> Void,
                       onSubscribed: (() -> Void)?,
                       fail: ((Error) -> Void)?) {
        
    }

    func unsubscribe() {
    }
}
