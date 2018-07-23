package com.wr.qt.wr2;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by wr-app1 on 2017/8/26.
 */

public class TextFormatter {
    public static String getMusicTime(long duration) {
        return new SimpleDateFormat("mm:ss", Locale.CHINA).format(new Date(
                duration));
    }
}
