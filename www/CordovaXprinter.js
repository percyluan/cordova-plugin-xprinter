var exec = require('cordova/exec');
var pluginName = 'CordovaXprinter';
var CordovaXprinter = {};

/**
 * 检查蓝牙，返回当前状态
 * @param params 不需要传
 * @param success 字符串：蓝牙状态
 * @param error
 */
CordovaXprinter.checkStatus = function (params, success, error) {
    exec(success, error, pluginName, 'checkStatus', [params]);
};
/**
 * 扫描设备，返回列表
 * @param params 不需要传
 * @param success ["设备名1"，“设备名2”]
 * @param error
 */
CordovaXprinter.scanDevice = function (params, success, error) {
    exec(success, error, pluginName, 'scanDevice', [params]);
};

/**
 * 根据设备名称，连接指定的外设
 * @param params 字符串：设备名
 * @param success
 * @param error
 */
CordovaXprinter.connectDevice = function (params, success, error) {
    exec(success, error, pluginName, 'connectDevice', [params]);
};
/**
 * 写入消息
 * @param params 字符串：消息 \n换行
 * @param success
 * @param error
 */
CordovaXprinter.writeDevice = function (params, success, error) {
    exec(success, error, pluginName, 'writeDevice', [params]);
};

/**
 * 设置命令
 * @param params
 * @param success
 * @param error
 */
CordovaXprinter.selectCommand = function (params, success, error) {
    exec(success, error, pluginName, 'selectCommand', [params]);
};

/**
 * 模板:消息分两栏显示
 * @param params {left:"xxx",right:"xxx"}
 * @param success
 * @param error
 */
CordovaXprinter.printTwoData = function (params, success, error) {
    exec(success, error, pluginName, 'printTwoData', [params]);
};

/**
 * 模板：消息分三栏显示
 * @param params{left:"xxx",middle:"xxx",right:"xxx"}
 * @param success
 * @param error
 */
CordovaXprinter.printThreeData = function (params, success, error) {
    exec(success, error, pluginName, 'printThreeData', [params]);
};

/**
 * 打印图片
 * @param params
 * @param success
 * @param error
 */
CordovaXprinter.printImg = function (params, success, error) {
    exec(success, error, pluginName, 'printImg', [params]);
};

module.exports = CordovaXprinter;

