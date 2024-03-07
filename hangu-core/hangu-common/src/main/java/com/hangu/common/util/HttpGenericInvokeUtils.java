package com.hangu.common.util;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import java.util.Objects;

/**
 * http 泛化调用工具类
 *
 * @author wuzhenhong
 * @date 2024/2/9 9:10
 */
public class HttpGenericInvokeUtils {


    public static boolean isApplicationJson(FullHttpRequest request) {

        if (request.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
            String[] headerArr = splitHeaderContentType(request.headers().get(HttpHeaderNames.CONTENT_TYPE));
            return Objects.nonNull(headerArr) && headerArr.length > 0 && headerArr[0].equals(
                HttpHeaderValues.APPLICATION_JSON.toString());
        } else {
            return false;
        }
    }

    public static String[] splitHeaderContentType(String sb) {
        int aStart;
        int aEnd;
        int bStart;
        int bEnd;
        int cStart;
        int cEnd;
        aStart = HttpGenericInvokeUtils.findNonWhitespace(sb, 0);
        aEnd = sb.indexOf(';');
        if (aEnd == -1) {
            return new String[]{sb, "", ""};
        }
        bStart = HttpGenericInvokeUtils.findNonWhitespace(sb, aEnd + 1);
        if (sb.charAt(aEnd - 1) == ' ') {
            aEnd--;
        }
        bEnd = sb.indexOf(';', bStart);
        if (bEnd == -1) {
            bEnd = HttpGenericInvokeUtils.findEndOfString(sb);
            return new String[]{sb.substring(aStart, aEnd), sb.substring(bStart, bEnd), ""};
        }
        cStart = HttpGenericInvokeUtils.findNonWhitespace(sb, bEnd + 1);
        if (sb.charAt(bEnd - 1) == ' ') {
            bEnd--;
        }
        cEnd = HttpGenericInvokeUtils.findEndOfString(sb);
        return new String[]{sb.substring(aStart, aEnd), sb.substring(bStart, bEnd), sb.substring(cStart, cEnd)};
    }

    private static int findNonWhitespace(String sb, int offset) {
        int result;
        for (result = offset; result < sb.length(); result++) {
            if (!Character.isWhitespace(sb.charAt(result))) {
                break;
            }
        }
        return result;
    }

    private static int findEndOfString(String sb) {
        int result;
        for (result = sb.length(); result > 0; result--) {
            if (!Character.isWhitespace(sb.charAt(result - 1))) {
                break;
            }
        }
        return result;
    }
}
