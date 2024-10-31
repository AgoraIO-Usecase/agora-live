//
//  FUDynmicResourceConfig.h
//  AgoraEntScenarios
//
//  Created by wushengtao on 2024/3/12.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface FUDynmicResourceConfig : NSObject

+ (instancetype)shareInstance;

/// Xiangxin Beauty Resource Catalogue
@property (nonatomic, copy, nullable) NSString* resourceFolderPath;

/// Phase core beauty lic path
@property (nonatomic, copy, nullable) NSString* licFilePath;

@end

NS_ASSUME_NONNULL_END
