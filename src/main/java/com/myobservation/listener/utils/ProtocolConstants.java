package com.myobservation.listener.utils;

import java.time.format.DateTimeFormatter;

/**
 * Definición de constantes
 */
public class ProtocolConstants {

    public static final char START_BLOCK = 0x0B;
    public static final char END_BLOCK = 0x1C;
    public static final char CARRIAGE_RETURN = 0x0D;
    public static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

}
