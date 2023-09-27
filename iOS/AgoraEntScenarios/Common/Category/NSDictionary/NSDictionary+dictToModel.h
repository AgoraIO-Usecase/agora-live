//
//  NSDictionary+dictToModel.h
//  FT_iPhone
//
//  Created by 吴文海 on 2019/4/17.
//  Copyright © 2019 ChangDao. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface NSDictionary (dictToModel)

/**
 It is used for data parsing when converting the Model, extracting all the JSON data returned by the background to the outermost layer

 @param startDic Raw JSON data returned by the background
 @param lastDic processed data
 @return The processed data
 */
+ (NSDictionary *)addObjToDictStartDic:(NSDictionary *)startDic lastDict:(NSMutableDictionary *)lastDic;
@end

NS_ASSUME_NONNULL_END
