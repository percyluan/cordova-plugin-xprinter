/********* CordovaXprinter.m Cordova Plugin Implementation *******/

#import <Cordova/CDV.h>
#import "HLBLEManager.h"
#import "HLPrinter.h"
typedef void(^PrintBlock)(HLPrinter *printer);
@interface CordovaXprinter : CDVPlugin {
    // Member variables go here.
}
    @property (strong, nonatomic)   CBPeripheral            *perpheral;  /**< 记录发现的设备 */
    @property (strong, nonatomic)   NSMutableArray              *deviceArray;  /**< 蓝牙设备个数 */
    @property (strong, nonatomic)   NSArray            *goodsArray;  /**< 商品数组 */
    @property (copy, nonatomic) PrintBlock            printBlock;    /**< 打印block */
    @property (strong, nonatomic)   NSMutableArray            *infos;  /**< 详情数组 */
    @property (strong, nonatomic)   CBCharacteristic            *character;  /**< 可写入数据的特性 */
    @property (strong, nonatomic)   NSDictionary            *writeParams;  /**< 需要打印的参数 */
    
    @end

@implementation CordovaXprinter
    
- (void)checkStatus:(CDVInvokedUrlCommand*)command
    {
        HLBLEManager *manager = [HLBLEManager sharedInstance];
        __weak HLBLEManager *weakManager = manager;
        manager.stateUpdateBlock = ^(CBCentralManager *central) {
            CDVPluginResult* pluginResult = nil;
            NSString *info = nil;
            NSString *infoCode = nil;
            switch (central.state) {
                case CBCentralManagerStatePoweredOn:
                info = @"蓝牙已打开，并且可用";
                infoCode = @"1";
                //三种种方式
                // 方式1
                [weakManager scanForPeripheralsWithServiceUUIDs:nil options:nil];
                //                // 方式2
                //                [central scanForPeripheralsWithServices:nil options:nil];
                //                // 方式3
                //                [weakManager scanForPeripheralsWithServiceUUIDs:nil options:nil didDiscoverPeripheral:^(CBCentralManager *central, CBPeripheral *peripheral, NSDictionary *advertisementData, NSNumber *RSSI) {
                //
                //                }];
                break;
                case CBCentralManagerStatePoweredOff:
                info = @"蓝牙可用，未打开";
                infoCode = @"0";
                break;
                case CBCentralManagerStateUnsupported:
                info = @"SDK不支持";
                infoCode = @"0";
                break;
                case CBCentralManagerStateUnauthorized:
                info = @"程序未授权";
                infoCode = @"0";
                break;
                case CBCentralManagerStateResetting:
                info = @"CBCentralManagerStateResetting";
                infoCode = @"0";
                break;
                case CBCentralManagerStateUnknown:
                info = @"CBCentralManagerStateUnknown";
                infoCode = @"0";
                break;
            }
            NSDictionary *returnDict = @{@"infoCode":infoCode,@"info":info};
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:returnDict];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        };
    }
    
- (void)scanDevice:(CDVInvokedUrlCommand*)command
    {
        _deviceArray = [[NSMutableArray alloc] init];
        HLBLEManager *manager = [HLBLEManager sharedInstance];
        __weak HLBLEManager *weakManager = manager;
        manager.discoverPeripheralBlcok = ^(CBCentralManager *central, CBPeripheral *peripheral, NSDictionary *advertisementData, NSNumber *RSSI) {
            NSString* uuid = @"0248D56F-BBEC-8286-3CDE-BF7208BFF978";
//            NSString* name = @"Printer_DC2A";//TODO 1
            NSString* name = @"Printer";
            CDVPluginResult* pluginResult = nil;
            if (peripheral.name.length <= 0) {
                return ;
            }
            //        if ([peripheral.identifier.UUIDString isEqualToString:uuid]){
            //            _perpheral = peripheral;
            //        }
            if ([peripheral.name hasPrefix:name]){
                name = peripheral.name;
                _perpheral = peripheral;
            }
            
            if (self.deviceArray.count == 0) {
                NSDictionary *dict = @{@"uuid":peripheral.identifier.UUIDString, @"RSSI":RSSI,@"name":peripheral.name};
                [self.deviceArray addObject:dict];
            } else {
                BOOL isExist = NO;
                for (int i = 0; i < self.deviceArray.count; i++) {
                    NSDictionary *dict = [self.deviceArray objectAtIndex:i];
                    NSString *nameStr = dict[@"name"];
                    if ([nameStr isEqualToString:peripheral.name]) {
                        isExist = YES;
                        NSDictionary *dict = @{@"uuid":peripheral.identifier.UUIDString, @"RSSI":RSSI,@"name":peripheral.name};
                        [_deviceArray replaceObjectAtIndex:i withObject:dict];
                    }
                }
                
                if (!isExist) {
                    NSDictionary *dict = @{@"uuid":peripheral.identifier.UUIDString, @"RSSI":RSSI,@"name":peripheral.name};
                    [self.deviceArray addObject:dict];
                }
            }
            
            //如果已经发现打印机设备，立即回调Cordova
            for(int i = 0; i < self.deviceArray.count; i++){
                NSDictionary *dict = [self.deviceArray objectAtIndex:i];
                NSString *nameStr = dict[@"name"];
                if([name isEqualToString:nameStr]){
                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:self.deviceArray];
                    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                }
            }
            
            
            
        };
        NSLog(@"插件成功调用，等待代理回调");
    }
-(void)reloadConnectedData
    {
        for(int i = 0;i < _infos.count; i++){
            CBService *service = _infos[i];
            for(int j = 0; j < service.characteristics.count; j++){
                CBCharacteristic *character = [service.characteristics objectAtIndex:j];
                CBCharacteristicProperties properties = character.properties;
                /**
                 CBCharacteristicPropertyWrite和CBCharacteristicPropertyWriteWithoutResponse类型的特性都可以写入数据
                 但是后者写入完成后，不会回调写入完成的代理方法{peripheral:didWriteValueForCharacteristic:error:},
                 因此，你也不会受到block回调。
                 所以首先考虑使用CBCharacteristicPropertyWrite的特性写入数据，如果没有这种特性，再考虑使用后者写入吧。
                 */
                //
                if (properties & CBCharacteristicPropertyWrite) {
                    //        if (self.chatacter == nil) {
                    //            self.chatacter = character;
                    //        }
                    self.character = character;
                }
            }
            
        }
    }
- (void)connectDevice:(CDVInvokedUrlCommand*)command
    {
        _infos = [[NSMutableArray alloc] init];
        HLBLEManager *manager = [HLBLEManager sharedInstance];
        [manager connectPeripheral:_perpheral
                    connectOptions:@{CBConnectPeripheralOptionNotifyOnDisconnectionKey:@(YES)}
            stopScanAfterConnected:YES
                   servicesOptions:nil
            characteristicsOptions:nil
                     completeBlock:^(HLOptionStage stage, CBPeripheral *peripheral, CBService *service, CBCharacteristic *character, NSError *error) {
                         
                         NSString *connectMsg = [[NSString alloc] init];
                         switch (stage) {
                             case HLOptionStageConnection:
                             {
                                 if (error) {
                                     //                                 [SVProgressHUD showErrorWithStatus:@"连接失败"];
                                     connectMsg = @"连接失败";
                                     
                                 } else {
                                     //                                 [SVProgressHUD showSuccessWithStatus:@"连接成功"];
                                     connectMsg = @"连接成功";
                                     
                                 }
                                 break;
                             }
                             case HLOptionStageSeekServices:
                             {
                                 if (error) {
                                     //                                 [SVProgressHUD showSuccessWithStatus:@"查找服务失败"];
                                     connectMsg = @"查找服务失败";
                                 } else {
                                     //                                 [SVProgressHUD showSuccessWithStatus:@"查找服务成功"];
                                     connectMsg = @"查找服务成功";
                                     [_infos addObjectsFromArray:peripheral.services];
                                     //                                 [_tableView reloadData];
                                     [self reloadConnectedData];
                                 }
                                 break;
                             }
                             case HLOptionStageSeekCharacteristics:
                             {
                                 // 该block会返回多次，每一个服务返回一次
                                 if (error) {
                                     //                                 NSLog(@"查找特性失败");
                                 } else {
                                     NSLog(@"查找特性成功");
                                     [self reloadConnectedData];
                                     //                                 [_tableView reloadData];
                                 }
                                 break;
                             }
                             case HLOptionStageSeekdescriptors:
                             {
                                 // 该block会返回多次，每一个特性返回一次
                                 if (error) {
                                     NSLog(@"查找特性的描述失败");
                                 } else {
                                     //                                 NSLog(@"查找特性的描述成功");
                                 }
                                 break;
                             }
                             default:
                             break;
                         }
                         if(self.character){
                             CDVPluginResult* pluginResult = nil;
                             pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:connectMsg];
                             [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                         }
                         
                         
                         
                     }];
        NSLog(@"插件成功调用，等待代理回调");
    }
- (void)writeDevice:(CDVInvokedUrlCommand*)command
    {
        
        self.writeParams = [command.arguments objectAtIndex:0];
        [self initPrintBlock];
        HLPrinter *printInfo = [self getPrinter];
        _printBlock(printInfo);
        NSLog(@"插件成功调用，等待代理回调");
    }
- (void)initPrintBlock
    {
        _printBlock = ^(HLPrinter *printInfo) {
            
            NSData *mainData = [printInfo getFinalData];
            HLBLEManager *bleManager = [HLBLEManager sharedInstance];
            if (self.character.properties & CBCharacteristicPropertyWrite) {
                [bleManager writeValue:mainData forCharacteristic:self.character type:CBCharacteristicWriteWithResponse completionBlock:^(CBCharacteristic *characteristic, NSError *error) {
                    if (!error) {
                        NSLog(@"写入成功");
                    }
                }];
            } else if (self.character.properties & CBCharacteristicPropertyWriteWithoutResponse) {
                [bleManager writeValue:mainData forCharacteristic:self.character type:CBCharacteristicWriteWithoutResponse];
            }
        };
    }
- (HLPrinter *)getPrinter
    {
        
        
        NSDictionary *dict1 = @{@"name":@"铅笔",@"amount":@"5",@"price":@"2.0"};
        NSDictionary *dict2 = @{@"name":@"橡皮",@"amount":@"1",@"price":@"1.0"};
        NSDictionary *dict3 = @{@"name":@"笔记本",@"amount":@"3",@"price":@"3.0"};
        self.goodsArray = @[dict1, dict2, dict3];
        
        
        HLPrinter *printer = [[HLPrinter alloc] init];
        
        [printer appendText:@"" alignment:HLTextAlignmentCenter];
        [printer appendText:@"" alignment:HLTextAlignmentCenter];
        [printer appendText:@"" alignment:HLTextAlignmentCenter];
        [printer appendText:@"" alignment:HLTextAlignmentCenter];
        
        NSString *title = @"恒安集团";
        NSString *seller = [self.writeParams objectForKey:@"storeName"];
        NSString *str1 = @"收货确认联";
        
        [printer appendText:title alignment:HLTextAlignmentCenter];
        
        [printer appendText:seller alignment:HLTextAlignmentCenter fontSize:HLFontSizeTitleSmalle];
        
        [printer appendText:@"" alignment:HLTextAlignmentCenter];
        
        [printer appendText:str1 alignment:HLTextAlignmentCenter];
        
        [printer appendText:@"" alignment:HLTextAlignmentCenter];
        // 条形码
        //    [printer appendBarCodeWithInfo:@"123456789012"];
        NSDateFormatter *nsdf2=[[NSDateFormatter alloc] init];
        [nsdf2 setDateStyle:NSDateFormatterShortStyle];
        [nsdf2 setDateFormat:@"yyyy-MM-dd hh:mm"];
        NSString *date=[nsdf2 stringFromDate:[NSDate date]];
        [printer appendTitle:@"打印时间:" value:date valueOffset:150];
        [printer appendSeperatorLine];
        
        NSString* buyerStoreName = [self.writeParams objectForKey:@"buyerStoreName"];
        [printer appendTitle:@"门店名称:" value:buyerStoreName valueOffset:150];
        NSString* mobile = [self.writeParams objectForKey:@"mobile"];
        [printer appendTitle:@"联系方式:" value:mobile valueOffset:150];
        NSString* provinceName = [self.writeParams objectForKey:@"provinceName"];
        NSString* cityName = [self.writeParams objectForKey:@"cityName"];
        NSString* districtName = [self.writeParams objectForKey:@"districtName"];
        NSString* address = [self.writeParams objectForKey:@"address"];
        NSString* reveiveAddress = [[[provinceName stringByAppendingString:cityName] stringByAppendingString:districtName] stringByAppendingString:address];
        [printer appendTitle:@"收件地址:" value:reveiveAddress valueOffset:150];
        [printer appendSeperatorLine];

        NSString* orderNo = [self.writeParams objectForKey:@"orderNo"];
//        [printer appendTitle:@"订单号:" value:orderNo];
        [printer appendTitle:@"订单号：" value:orderNo valueOffset:150];
        //    NSString* source = [self.writeParams objectForKey:@"source"];
        NSString *source = @"恒安集团微商城";
        NSString *orderTypeStr = @"";
        if([@"3" isEqualToString:[self.writeParams objectForKey:@"orderType"]]){
            orderTypeStr =@"门店订单";
        }
        else if([@"4" isEqualToString:[self.writeParams objectForKey:@"orderType"]]){
            orderTypeStr =@"车销订单";
        }else{
            orderTypeStr =@"代客下单";
        }
        [printer appendTitle:@"订单类型：" value:orderTypeStr valueOffset:150];
        NSString *shippingStatusStr = @"";
        if([@"10" isEqualToString:[self.writeParams objectForKey:@"shippingStatus"]]){
            shippingStatusStr = @"待配送";
        }else if([@"20" isEqualToString:[self.writeParams objectForKey:@"shippingStatus"]]){
            shippingStatusStr = @"配送中";
        }else if([@"30" isEqualToString:[self.writeParams objectForKey:@"shippingStatus"]]){
            shippingStatusStr = @"已完成";
        }else if([@"40" isEqualToString:[self.writeParams objectForKey:@"shippingStatus"]]){
            shippingStatusStr = @"已拒收";
        }
        [printer appendTitle:@"订单状态：" value:shippingStatusStr valueOffset:150];
        [printer appendSeperatorLine];

        self.goodsArray = [self.writeParams objectForKey:@"orderDetailList"];
        [printer appendTitle:@"商品货号" value:@"小计" valueOffset:150 fontSize:HLFontSizeTitleSmalle];
        for(int i = 0; i < self.goodsArray.count; i++){
            NSString *goodsSn = [self.goodsArray[i] objectForKey:@"goodsSn"];
            NSNumber* goodsNumber = [self.goodsArray[i] objectForKey:@"goodsNumber"];
            NSNumber* goodsPrice = [self.goodsArray[i] objectForKey:@"transactionPrice"];
            NSNumber* totalFee = [self.goodsArray[i] objectForKey:@"totalFee"];
            NSString* unit = [self.goodsArray[i] objectForKey:@"unit"];
            NSString* goodsNumberStr = [goodsNumber stringValue];
            NSString* goodsPriceStr =[goodsPrice stringValue];
            NSString* totalFeeStr =[totalFee stringValue];
            NSString* totalSum = [[[[[goodsNumberStr stringByAppendingString:unit] stringByAppendingString:@"*"] stringByAppendingString:goodsPriceStr] stringByAppendingString:@" = "]stringByAppendingString: totalFeeStr];
            [printer appendTitle:goodsSn value:totalSum valueOffset:150 fontSize:HLFontSizeTitleSmalle];
//            [printer appendTitle:goodsSn value:totalSum];
        }
        [printer appendSeperatorLine];
        
        NSNumber* totalFee = [self.writeParams objectForKey:@"totalFee" ];
        NSNumber* discount = [self.writeParams objectForKey:@"discount"];
        NSNumber* orderAmount = [self.writeParams objectForKey:@"orderAmount"];
        [printer appendTitle:@"商品金额:" value:[totalFee stringValue]];
        [printer appendTitle:@"优惠金额:" value:[discount stringValue]];
        [printer appendTitle:@"实际应付:" value:[orderAmount stringValue]];
        
        [printer appendSeperatorLine];
        if([@"free" isEqualToString:[self.writeParams objectForKey:@"payCode"]]){
            [printer appendTitle:@"支付方式：非在线支付" value:@"" valueOffset:150];
        }else if([@"wx" isEqualToString:[self.writeParams objectForKey:@"payCode"]]){
            [printer appendTitle:@"支付方式：已支付" value:@"" valueOffset:150];
        }else if([@"ali" isEqualToString:[self.writeParams objectForKey:@"payCode"]]){
            [printer appendTitle:@"支付方式：支付完成" value:@"" valueOffset:150];
        }
        if([@"10" isEqualToString:[self.writeParams objectForKey:@"payStatus"]]){
            [printer appendTitle:@"支付状态：待支付" value:@"" valueOffset:150];
        }else if([@"20" isEqualToString:[self.writeParams objectForKey:@"payStatus"]]){
            [printer appendTitle:@"支付状态：已支付" value:@"" valueOffset:150];
        }else if([@"30" isEqualToString:[self.writeParams objectForKey:@"payStatus"]]){
            [printer appendTitle:@"支付状态：支付完成" value:@"" valueOffset:150];
        }else if([@"40" isEqualToString:[self.writeParams objectForKey:@"payStatus"]]){
            [printer appendTitle:@"支付状态：待线下支付" value:@"" valueOffset:150];
        }
        [printer appendText:@"" alignment:HLTextAlignmentCenter];
        [printer appendTitle:@"线下支付金额:" value:@"________" valueOffset:150];//TODO 直接改，改好了后保存文件传到git服务器
        [printer appendSeperatorLine];
        [printer appendTitle:@"收货人签字:" value:@""];
        [printer appendText:@"" alignment:HLTextAlignmentCenter];
        [printer appendText:@"" alignment:HLTextAlignmentCenter];
        [printer appendText:@"" alignment:HLTextAlignmentCenter];
        [printer appendFooter:nil];
        [printer appendText:@"" alignment:HLTextAlignmentCenter];
        [printer appendText:@"" alignment:HLTextAlignmentCenter];
        [printer appendText:@"" alignment:HLTextAlignmentCenter];
        [printer appendText:@"" alignment:HLTextAlignmentCenter];
        [printer appendText:@"" alignment:HLTextAlignmentCenter];
        [printer appendText:@"" alignment:HLTextAlignmentCenter];
        [printer appendText:@"" alignment:HLTextAlignmentCenter];
        return printer;
    }
    @end

