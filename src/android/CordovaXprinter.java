package com.hengan.Xprinter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import java.lang.Exception;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 蓝牙打印机安卓实现
 */
public class CordovaXprinter extends CordovaPlugin {

    PrinterAdapter printerAdapter = new PrinterAdapter();

    /**
     * 打印纸一行最大的字节
     */
    private static final int LINE_BYTE_SIZE = 32;

    /**
     * 打印三列时，中间一列的中心线距离打印纸左侧的距离
     */
    private static final int LEFT_LENGTH = 16;

    /**
     * 打印三列时，中间一列的中心线距离打印纸右侧的距离
     */
    private static final int RIGHT_LENGTH = 16;

    /**
     * 打印三列时，第一列汉字最多显示几个文字
     */
    private static final int LEFT_TEXT_MAX_LENGTH = 5;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("scanDevice")) {
            this.scanDevice(callbackContext);
            return true;
        }

        if (action.equals("connectDevice")) {
            String name = args.getString(0);
            this.connectDevice(name,callbackContext);
            return true;
        }

        if (action.equals("writeDevice")) {
            JSONObject order = args.getJSONObject(0);
            this.writeDevice(order,callbackContext);
            return true;
        }

        if (action.equals("printTwoData")) {
            JSONObject jsonobj = args.getJSONObject(0);
            this.printTwoData(jsonobj.getString("left"),jsonobj.getString("right"),callbackContext);
            return true;
        }

        if (action.equals("printThreeData")) {
            JSONObject jsonobj = args.getJSONObject(0);
            this.printThreeData(jsonobj.getString("left"),jsonobj.getString("middle"),jsonobj.getString("right"),callbackContext);
            return true;
        }

        if (action.equals("selectCommand")) {
            String command = args.getString(0);
            this.selectCommand(command,callbackContext);
            return true;
        }

        if (action.equals("selectCommand")) {
            String command = args.getString(0);
            this.selectCommand(command,callbackContext);
            return true;
        }

        if (action.equals("printImg")) {
            this.printImg(callbackContext);
            return true;
        }

        return false;
    }

    /**
     * 扫描设备
     *
     * @param callbackContext
     */
    private void scanDevice(CallbackContext callbackContext) {
        try {
            printerAdapter.open();
            String[] pair = printerAdapter.makePair();
            JSONArray jsonStrs = new JSONArray();

            for(String p : pair){
                jsonStrs.put(p);
            }

            callbackContext.success(jsonStrs);
        }catch (SecurityException e){
            callbackContext.error("无法打开蓝牙：程序无权限，请手动开启权限！");
        }
        catch (Exception e){
            callbackContext.error(e.toString());
        }
    }

    /**
     * 链接设备
     *
     * @param name
     * @param callbackContext
     */
    private void connectDevice(String name, CallbackContext callbackContext) {
        if (name != null && name != "") {
            try{
                String result = printerAdapter.connect(name);
                if(result.contains("success")){
                    callbackContext.success("连接设备成功:"+result);
                }else{
                    callbackContext.error("连接失败:"+result);
                }
            }catch (Exception e){
                callbackContext.error(e.toString());
            }
        }
    }

    /**
     * 写入数据
     *
     * @param message
     * @param callbackContext
     */
    private void writeDevice(JSONObject order, CallbackContext callbackContext) {
        try {
            printHenganOrder(order);
        }catch (Exception e){
            callbackContext.error(e.toString());
        }

        /*if (message != null && message != "") {
            try {
                printerAdapter.printer(message);
                callbackContext.success("写入消息成功");
            }catch (Exception e){
                callbackContext.success("写入消息失败");
            }

        } else {
            callbackContext.error("消息不能为空");
        }*/
    }

    private void writeDevice(String message, CallbackContext callbackContext) {
        if (message != null && message != "") {
            try {
                printerAdapter.printer(message);
                callbackContext.success("写入消息成功");
            }catch (Exception e){
                callbackContext.success("写入消息失败");
            }

        } else {
            callbackContext.error("消息不能为空");
        }
    }

    /**
     * 设置命令
     *
     * @param command
     * @param callbackContext
     */
    private void selectCommand(String command, CallbackContext callbackContext) {
        if (command != null && command != "") {
            try {
                byte[] b = null;

                //n换行
                if("RESET".equals(command)){//复位打印机
                    b = new byte[]{0x1b, 0x40};
                }
                else if("ALIGN_LEFT".equals(command)){ //左对齐
                    b = new byte[]{0x1b, 0x61, 0x00};
                }
                else if("ALIGN_CENTER".equals(command)){ //中间对齐
                    b = new byte[]{0x1b, 0x61, 0x01};
                }
                else if("ALIGN_RIGHT".equals(command)){ //右对齐
                    b = new byte[]{0x1b, 0x61, 0x02};
                }
                else if("BOLD".equals(command)){ //选择加粗模式
                    b = new byte[]{0x1b, 0x45, 0x01};
                }
                else if("BOLD_CANCEL".equals(command)){ //取消加粗模式
                    b = new byte[]{0x1b, 0x45, 0x00};
                }
                else if("DOUBLE_HEIGHT_WIDTH".equals(command)){ //宽高加倍
                    b = new byte[]{0x1d, 0x21, 0x11};
                }
                else if("DOUBLE_WIDTH".equals(command)){ //宽加倍
                    b = new byte[]{0x1d, 0x21, 0x10};
                }
                else if("DOUBLE_HEIGHT".equals(command)){ //高加倍
                    b = new byte[]{0x1d, 0x21, 0x01};
                }
                else if("NORMAL".equals(command)){ //字体不放大
                    b = new byte[]{0x1d, 0x21, 0x00};
                }
                else if("LINE_SPACING_DEFAULT".equals(command)){ //设置默认行间距
                    b = new byte[]{0x1b, 0x32};
                }else {
                    throw new Exception();
                }

                printerAdapter.selectCommand(b);
                callbackContext.success("写入命令成功");
            }catch (Exception e){
                callbackContext.error("写入命令失败");
            }

        } else {
            callbackContext.error("命令不能为空");
        }
    }

    private void selectCommand(String command) {
        if (command != null && command != "") {
            try {
                byte[] b = null;

                //n换行
                if("RESET".equals(command)){//复位打印机
                    b = new byte[]{0x1b, 0x40};
                }
                else if("ALIGN_LEFT".equals(command)){ //左对齐
                    b = new byte[]{0x1b, 0x61, 0x00};
                }
                else if("ALIGN_CENTER".equals(command)){ //中间对齐
                    b = new byte[]{0x1b, 0x61, 0x01};
                }
                else if("ALIGN_RIGHT".equals(command)){ //右对齐
                    b = new byte[]{0x1b, 0x61, 0x02};
                }
                else if("BOLD".equals(command)){ //选择加粗模式
                    b = new byte[]{0x1b, 0x45, 0x01};
                }
                else if("BOLD_CANCEL".equals(command)){ //取消加粗模式
                    b = new byte[]{0x1b, 0x45, 0x00};
                }
                else if("DOUBLE_HEIGHT_WIDTH".equals(command)){ //宽高加倍
                    b = new byte[]{0x1d, 0x21, 0x11};
                }
                else if("DOUBLE_WIDTH".equals(command)){ //宽加倍
                    b = new byte[]{0x1d, 0x21, 0x10};
                }
                else if("DOUBLE_HEIGHT".equals(command)){ //高加倍
                    b = new byte[]{0x1d, 0x21, 0x01};
                }
                else if("NORMAL".equals(command)){ //字体不放大
                    b = new byte[]{0x1d, 0x21, 0x00};
                }
                else if("LINE_SPACING_DEFAULT".equals(command)){ //设置默认行间距
                    b = new byte[]{0x1b, 0x32};
                }else {
                    throw new Exception();
                }

                printerAdapter.selectCommand(b);
            }catch (Exception e){
            }
        }
    }

    /**
     * 获取字节长度
     *
     * @param msg
     * @return
     */
    private int getBytesLength(String msg) {
        return msg.getBytes(Charset.forName("GBK")).length;
    }

    /**
     * 打印两列
     *
     * @param leftText  左侧文字
     * @param rightText 右侧文字
     * @return
     */
    public void printTwoData(String leftText, String rightText,CallbackContext callbackContext) {
        StringBuilder sb = new StringBuilder();
        int leftTextLength = getBytesLength(leftText);
        int rightTextLength = getBytesLength(rightText);
        sb.append(leftText);

        // 计算两侧文字中间的空格
        int marginBetweenMiddleAndRight = LINE_BYTE_SIZE - leftTextLength - rightTextLength;

        for (int i = 0; i < marginBetweenMiddleAndRight; i++) {
            sb.append(" ");
        }
        sb.append(rightText);

        this.writeDevice(sb.toString(),callbackContext);
    }

    public String printTwoData(String leftText, String rightText) {
        StringBuilder sb = new StringBuilder();
        int leftTextLength = getBytesLength(leftText);
        int rightTextLength = getBytesLength(rightText);
        sb.append(leftText);

        // 计算两侧文字中间的空格
        int marginBetweenMiddleAndRight = LINE_BYTE_SIZE - leftTextLength - rightTextLength;

        for (int i = 0; i < marginBetweenMiddleAndRight; i++) {
            sb.append(" ");
        }
        sb.append(rightText);

        return sb.toString();
    }

    /**
     * 打印三列
     *
     * @param leftText   左侧文字
     * @param middleText 中间文字
     * @param rightText  右侧文字
     * @return
     */
    public void printThreeData(String leftText, String middleText, String rightText,CallbackContext callbackContext) {
        StringBuilder sb = new StringBuilder();
        // 左边最多显示 LEFT_TEXT_MAX_LENGTH 个汉字 + 两个点
        if (leftText.length() > LEFT_TEXT_MAX_LENGTH) {
            leftText = leftText.substring(0, LEFT_TEXT_MAX_LENGTH) + "..";
        }
        int leftTextLength = getBytesLength(leftText);
        int middleTextLength = getBytesLength(middleText);
        int rightTextLength = getBytesLength(rightText);

        sb.append(leftText);
        // 计算左侧文字和中间文字的空格长度
        int marginBetweenLeftAndMiddle = LEFT_LENGTH - leftTextLength - middleTextLength / 2;

        for (int i = 0; i < marginBetweenLeftAndMiddle; i++) {
            sb.append(" ");
        }
        sb.append(middleText);

        // 计算右侧文字和中间文字的空格长度
        int marginBetweenMiddleAndRight = RIGHT_LENGTH - middleTextLength / 2 - rightTextLength;

        for (int i = 0; i < marginBetweenMiddleAndRight; i++) {
            sb.append(" ");
        }

        // 打印的时候发现，最右边的文字总是偏右一个字符，所以需要删除一个空格
        sb.delete(sb.length() - 1, sb.length()).append(rightText);

        this.writeDevice(sb.toString(),callbackContext);
    }

    /**
     * 打印图
     *
     * @param callbackContext
     */
    public void printImg(CallbackContext callbackContext){

        //获取打印位图
        BufferedInputStream bis = null;
        bis = new BufferedInputStream(printerAdapter.getResource("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1510651025&di=390c1b93cc005671643b98c03c2fe65f&imgtype=jpg&er=1&src=http%3A%2F%2Fpic.ijjnews.com%2F003%2F000%2F487%2F00300048778_0e3864a4.jpg"));
        if(bis==null){
            callbackContext.error("图片获取失败");
            return;
        }


        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(bis);
        }catch(Exception e){
            callbackContext.error("图片转换bmp失败");
            return;
        }

        byte[] start = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1B, 0x40, 0x1B, 0x33, 0x00 };
        byte[] data = null;
        byte[] end = { 0x1d, 0x4c, 0x1f, 0x00 };

        try{
            data = PicFromPrintUtils.draw2PxPoint(bitmap);
        }catch(Exception e){
            callbackContext.error("bmp转换二进制数组失败");
            return;
        }

        try {
            printerAdapter.selectCommand(start);
            printerAdapter.selectCommand(data);
            printerAdapter.selectCommand(end);
            callbackContext.success("图片打印成功");
        } catch (IOException e) {
            e.printStackTrace();
            callbackContext.error("图片打印失败");
        }

    }

    public void printImg(){

        //获取打印位图
        BufferedInputStream bis = null;
        bis = new BufferedInputStream(printerAdapter.getResource("https://ss1.bdstatic.com/70cFuXSh_Q1YnxGkpoWK1HF6hhy/it/u=1922736860,4073748895&fm=27&gp=0.jpg"));

        Bitmap bitmap = null;
        bitmap = BitmapFactory.decodeStream(bis);

        byte[] start = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1B, 0x40, 0x1B, 0x33, 0x00 };
        byte[] data = null;
        byte[] end = { 0x1d, 0x4c, 0x1f, 0x00 };

        try{
            data = PicFromPrintUtils.draw2PxPoint(PicFromPrintUtils.compressPic(bitmap));
        }catch(Exception e){
        }

        try {
            printerAdapter.selectCommand(start);
            printerAdapter.selectCommand(data);
            printerAdapter.selectCommand(end);
        } catch (IOException e) {

        }

    }

    private void printHenganOrder(JSONObject order) throws Exception{
		printerAdapter.printer("\n\n");
		selectCommand("ALIGN_CENTER");
		printerAdapter.printer("恒安集团\n");
		printerAdapter.printer(order.getString("storeName")+"\n");
		printerAdapter.printer("\n");
		printerAdapter.printer("收货确认联\n");
		printerAdapter.printer("\n");
		selectCommand("RESET");
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String nowDate = df.format(new Date());
		printerAdapter.printer("打印时间：  "+nowDate+"\n");
		printerAdapter.printer("********************************\n");
		printerAdapter.printer("门店名称：  "+order.getString("buyerStoreName")+"\n");
		printerAdapter.printer("联系方式：  "+order.getString("mobile")+"\n");
		printerAdapter.printer("收件地址：  "+order.getString("provinceName")+order.getString("cityName")+order.getString("districtName")+order.getString("address")+"\n");
		printerAdapter.printer("********************************\n");
		printerAdapter.printer("订单号：    "+order.getString("orderNo")+"\n");
		String orderType = order.getString("orderType");
		String orderTypeStr = "代客下单";
		if("3".equals(orderType)){
			orderTypeStr = "门店订单";
		} else if("4".equals(orderType)){
			orderTypeStr = "车销订单";
		}
		printerAdapter.printer("订单类型：  "+orderTypeStr+"\n");
		String shippingStatus = order.getString("shippingStatus");
		String shippingStatusStr = "";
		if("10".equals(shippingStatus)){
			shippingStatusStr = "待配送";
		} else if("20".equals(shippingStatus)){
			shippingStatusStr = "配送中";
		} else if("30".equals(shippingStatus)){
			shippingStatusStr = "已完成";
		} else if("40".equals(shippingStatus)){
			shippingStatusStr = "已拒收";
		}
		printerAdapter.printer("订单状态：  "+shippingStatusStr+"\n");
		printerAdapter.printer("********************************\n");
		printerAdapter.printer("商品货号：  小计\n");
		JSONArray goods = order.getJSONArray("orderDetailList");
		for(int i = 0;i<goods.length();i++){
			JSONObject good = goods.getJSONObject(i);
			String productCode = good.getString("goodsSn");
			int blankLen = 12 - productCode.length();
			for(int i = 0; i < blankLen; i ++){
				productCode = productCode + " ";
			}
			printerAdapter.printer(productCode+good.getString("goodsNumber")+good.getString("unit")+"*"+good.getString("goodsPrice")+" = "+good.getString("totalFee")+"\n");
		}
		printerAdapter.printer("********************************\n");
		printerAdapter.printer(printTwoData("商品金额："), order.getString("totalFee"));
		printerAdapter.printer("\n");
		printerAdapter.printer(printTwoData("优惠金额："), order.getString("discount"));
		printerAdapter.printer("\n");
		printerAdapter.printer(printTwoData("实际应付："), order.getString("orderAmount"));
		printerAdapter.printer("\n");
		printerAdapter.printer("********************************\n");
		String payCode = order.getString("payCode");
		if("free".equals(payCode)){
			printerAdapter.printer("支付方式：非在线支付\n");
		} else if("wx".equals(payCode)){
			printerAdapter.printer("支付方式：已支付\n");
		} else if("ali".equals(payCode)){
			printerAdapter.printer("支付方式：支付完成\n");
		}
		String payCode = order.getString("payStatus");
		if("10".equals(payStatus)){
			printerAdapter.printer("支付状态：待支付\n");
		} else if("20".equals(payStatus)){
			printerAdapter.printer("支付状态：已支付\n");
		} else if("30".equals(payStatus)){
			printerAdapter.printer("支付状态：支付完成\n");
		} else if("40".equals(payStatus)){
			printerAdapter.printer("支付状态：待线下支付\n");
		}
		printerAdapter.printer("\n");
		printerAdapter.printer("线下支付金额：________\n");
		printerAdapter.printer("********************************\n");
		printerAdapter.printer("收货人签字：\n");
		printerAdapter.printer("\n\n\n");
		printerAdapter.printer("（签字表明您已经收到商品）\n");
		printerAdapter.printer("********************************\n");
		selectCommand("ALIGN_CENTER");
		printerAdapter.printer("感谢你对恒安产品的支持！\n");
		selectCommand("RESET");
		printerAdapter.printer("\n\n\n\n\n\n\n\n\n\n");
    }
}
