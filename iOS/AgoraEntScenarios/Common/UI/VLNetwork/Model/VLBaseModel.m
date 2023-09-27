//
//  VLBaseModel.m
//  VoiceOnLine
//

#import "VLBaseModel.h"

@implementation VLBaseModel

+ (instancetype)vj_modelWithDictionary:(NSDictionary *)dict {
    return [self yy_modelWithDictionary:dict];
}

- (void)encodeWithCoder:(NSCoder *)aCoder {
    [self yy_modelEncodeWithCoder:aCoder];
}
- (id)initWithCoder:(NSCoder *)aDecoder {
    self = [super init];
    return [self yy_modelInitWithCoder:aDecoder];
}

- (id)copyWithZone:(NSZone *)zone {
    return [self yy_modelCopy];
}

- (NSUInteger)hash {
    return [self yy_modelHash];
}
- (BOOL)isEqual:(id)object {
    return [self yy_modelIsEqual:object];
}

- (NSString *)description {
    return  [self yy_modelDescription];
}

+ (NSDictionary *)vj_modelDictionaryWithJson:(id)json {
    return [NSDictionary yy_modelDictionaryWithClass:[self class] json:json];
}

+ (NSArray *)vj_modelArrayWithJson:(id)json {
    return [NSArray yy_modelArrayWithClass:[self class] json:json];
}

- (void)setValue:(id)value forUndefinedKey:(nonnull NSString *)key {
    NSString *error = [NSString stringWithFormat:@"%@The value is set not to exist key = %@",[self class],key];
    NSAssert(nil, error);
}

- (id)valueForUndefinedKey:(NSString *)key {
    NSString *error = [NSString stringWithFormat:@"%@The acquisition does not exist key = %@",[self class],key];
    NSAssert(nil, error);
    return @"";
}
/**
 *  Filter null values, if the dictionary is "null","<null>",""," ",""," "," and object empty This field will be removed from the dictionary, if the argument is not a dictionary then it cannot be converted to the model, return nil to prevent crash
 *
 *  @param dic Dictionary to convert
 *
 *  @return Filtered dictionary
 */
- (NSDictionary *)modelCustomWillTransformFromDictionary:(NSDictionary *)dic {
    if ([dic isKindOfClass:[NSDictionary class]]) {
        NSMutableDictionary *filterDic = [[NSMutableDictionary alloc] init];
        [[dic allKeys] enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
            id value = [dic objectForKey:obj];
            if ([value isKindOfClass:[NSNull class]] && (value  == [NSNull null])) {
                ;
            } else if (([value isKindOfClass:[NSString class]]) &&
                     ([[dic objectForKey:obj] isEqualToString:@"<null>"] ||
                      [value isEqualToString:@"null"] ||
                      [value isEqualToString:@""] ||
                      [value isEqualToString:@" "] ||
                      [value isEqualToString:@"  "] ||
                      [value isEqualToString:@"   "])) {
                ;
            } else if (!value) {
                ;
            } else {
                [filterDic setObject:value forKey:obj];
            }
        }];
        return filterDic;
    }
    return nil;
}
@end
