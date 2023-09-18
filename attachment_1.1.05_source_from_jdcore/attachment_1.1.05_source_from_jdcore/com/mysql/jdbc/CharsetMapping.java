package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;







































public class CharsetMapping
{
  public static final int MAP_SIZE = 2048;
  public static final String[] COLLATION_INDEX_TO_COLLATION_NAME;
  public static final MysqlCharset[] COLLATION_INDEX_TO_CHARSET;
  public static final Map<String, MysqlCharset> CHARSET_NAME_TO_CHARSET;
  public static final Map<String, Integer> CHARSET_NAME_TO_COLLATION_INDEX;
  private static final Map<String, List<MysqlCharset>> JAVA_ENCODING_UC_TO_MYSQL_CHARSET;
  private static final Set<String> MULTIBYTE_ENCODINGS;
  private static final Map<String, String> ERROR_MESSAGE_FILE_TO_MYSQL_CHARSET;
  private static final Set<String> ESCAPE_ENCODINGS;
  public static final Set<Integer> UTF8MB4_INDEXES;
  private static final String MYSQL_CHARSET_NAME_armscii8 = "armscii8";
  private static final String MYSQL_CHARSET_NAME_ascii = "ascii";
  private static final String MYSQL_CHARSET_NAME_big5 = "big5";
  private static final String MYSQL_CHARSET_NAME_binary = "binary";
  private static final String MYSQL_CHARSET_NAME_cp1250 = "cp1250";
  private static final String MYSQL_CHARSET_NAME_cp1251 = "cp1251";
  private static final String MYSQL_CHARSET_NAME_cp1256 = "cp1256";
  private static final String MYSQL_CHARSET_NAME_cp1257 = "cp1257";
  private static final String MYSQL_CHARSET_NAME_cp850 = "cp850";
  private static final String MYSQL_CHARSET_NAME_cp852 = "cp852";
  private static final String MYSQL_CHARSET_NAME_cp866 = "cp866";
  private static final String MYSQL_CHARSET_NAME_cp932 = "cp932";
  private static final String MYSQL_CHARSET_NAME_dec8 = "dec8";
  private static final String MYSQL_CHARSET_NAME_eucjpms = "eucjpms";
  private static final String MYSQL_CHARSET_NAME_euckr = "euckr";
  private static final String MYSQL_CHARSET_NAME_gb18030 = "gb18030";
  private static final String MYSQL_CHARSET_NAME_gb2312 = "gb2312";
  private static final String MYSQL_CHARSET_NAME_gbk = "gbk";
  private static final String MYSQL_CHARSET_NAME_geostd8 = "geostd8";
  private static final String MYSQL_CHARSET_NAME_greek = "greek";
  private static final String MYSQL_CHARSET_NAME_hebrew = "hebrew";
  private static final String MYSQL_CHARSET_NAME_hp8 = "hp8";
  private static final String MYSQL_CHARSET_NAME_keybcs2 = "keybcs2";
  private static final String MYSQL_CHARSET_NAME_koi8r = "koi8r";
  private static final String MYSQL_CHARSET_NAME_koi8u = "koi8u";
  private static final String MYSQL_CHARSET_NAME_latin1 = "latin1";
  private static final String MYSQL_CHARSET_NAME_latin2 = "latin2";
  private static final String MYSQL_CHARSET_NAME_latin5 = "latin5";
  private static final String MYSQL_CHARSET_NAME_latin7 = "latin7";
  private static final String MYSQL_CHARSET_NAME_macce = "macce";
  private static final String MYSQL_CHARSET_NAME_macroman = "macroman";
  private static final String MYSQL_CHARSET_NAME_sjis = "sjis";
  private static final String MYSQL_CHARSET_NAME_swe7 = "swe7";
  private static final String MYSQL_CHARSET_NAME_tis620 = "tis620";
  private static final String MYSQL_CHARSET_NAME_ucs2 = "ucs2";
  private static final String MYSQL_CHARSET_NAME_ujis = "ujis";
  private static final String MYSQL_CHARSET_NAME_utf16 = "utf16";
  private static final String MYSQL_CHARSET_NAME_utf16le = "utf16le";
  private static final String MYSQL_CHARSET_NAME_utf32 = "utf32";
  private static final String MYSQL_CHARSET_NAME_utf8 = "utf8";
  private static final String MYSQL_CHARSET_NAME_utf8mb4 = "utf8mb4";
  private static final String MYSQL_4_0_CHARSET_NAME_cp1251cias = "cp1251cias";
  private static final String MYSQL_4_0_CHARSET_NAME_cp1251csas = "cp1251csas";
  private static final String MYSQL_4_0_CHARSET_NAME_croat = "croat";
  private static final String MYSQL_4_0_CHARSET_NAME_czech = "czech";
  private static final String MYSQL_4_0_CHARSET_NAME_danish = "danish";
  private static final String MYSQL_4_0_CHARSET_NAME_dos = "dos";
  private static final String MYSQL_4_0_CHARSET_NAME_estonia = "estonia";
  private static final String MYSQL_4_0_CHARSET_NAME_euc_kr = "euc_kr";
  private static final String MYSQL_4_0_CHARSET_NAME_german1 = "german1";
  private static final String MYSQL_4_0_CHARSET_NAME_hungarian = "hungarian";
  private static final String MYSQL_4_0_CHARSET_NAME_koi8_ru = "koi8_ru";
  private static final String MYSQL_4_0_CHARSET_NAME_koi8_ukr = "koi8_ukr";
  private static final String MYSQL_4_0_CHARSET_NAME_latin1_de = "latin1_de";
  private static final String MYSQL_4_0_CHARSET_NAME_latvian = "latvian";
  private static final String MYSQL_4_0_CHARSET_NAME_latvian1 = "latvian1";
  private static final String MYSQL_4_0_CHARSET_NAME_usa7 = "usa7";
  private static final String MYSQL_4_0_CHARSET_NAME_win1250 = "win1250";
  private static final String MYSQL_4_0_CHARSET_NAME_win1251 = "win1251";
  private static final String MYSQL_4_0_CHARSET_NAME_win1251ukr = "win1251ukr";
  public static final String NOT_USED = "latin1";
  public static final String COLLATION_NOT_DEFINED = "none";
  public static final int MYSQL_COLLATION_INDEX_utf8 = 33;
  public static final int MYSQL_COLLATION_INDEX_binary = 63;
  private static int numberOfEncodingsConfigured = 0;
  
  static
  {
    MysqlCharset[] charset = { new MysqlCharset("usa7", 1, 0, new String[] { "US-ASCII" }, 4, 0), new MysqlCharset("ascii", 1, 0, new String[] { "US-ASCII", "ASCII" }), new MysqlCharset("big5", 2, 0, new String[] { "Big5" }), new MysqlCharset("gbk", 2, 0, new String[] { "GBK" }), new MysqlCharset("sjis", 2, 0, new String[] { "SHIFT_JIS", "Cp943", "WINDOWS-31J" }), new MysqlCharset("cp932", 2, 1, new String[] { "WINDOWS-31J" }), new MysqlCharset("gb2312", 2, 0, new String[] { "GB2312" }), new MysqlCharset("ujis", 3, 0, new String[] { "EUC_JP" }), new MysqlCharset("eucjpms", 3, 0, new String[] { "EUC_JP_Solaris" }, 5, 0, 3), new MysqlCharset("gb18030", 4, 0, new String[] { "GB18030" }, 5, 7, 4), new MysqlCharset("euc_kr", 2, 0, new String[] { "EUC_KR" }, 4, 0), new MysqlCharset("euckr", 2, 0, new String[] { "EUC-KR" }), new MysqlCharset("latin1", 1, 1, new String[] { "Cp1252", "ISO8859_1" }), new MysqlCharset("swe7", 1, 0, new String[] { "Cp1252" }), new MysqlCharset("hp8", 1, 0, new String[] { "Cp1252" }), new MysqlCharset("dec8", 1, 0, new String[] { "Cp1252" }), new MysqlCharset("armscii8", 1, 0, new String[] { "Cp1252" }), new MysqlCharset("geostd8", 1, 0, new String[] { "Cp1252" }), new MysqlCharset("latin2", 1, 0, new String[] { "ISO8859_2" }), new MysqlCharset("czech", 1, 0, new String[] { "ISO8859_2" }, 4, 0), new MysqlCharset("hungarian", 1, 0, new String[] { "ISO8859_2" }, 4, 0), new MysqlCharset("croat", 1, 0, new String[] { "ISO8859_2" }, 4, 0), new MysqlCharset("greek", 1, 0, new String[] { "ISO8859_7", "greek" }), new MysqlCharset("latin7", 1, 0, new String[] { "ISO-8859-13" }), new MysqlCharset("hebrew", 1, 0, new String[] { "ISO8859_8" }), new MysqlCharset("latin5", 1, 0, new String[] { "ISO8859_9" }), new MysqlCharset("latvian", 1, 0, new String[] { "ISO8859_13" }, 4, 0), new MysqlCharset("latvian1", 1, 0, new String[] { "ISO8859_13" }, 4, 0), new MysqlCharset("estonia", 1, 1, new String[] { "ISO8859_13" }, 4, 0), new MysqlCharset("cp850", 1, 0, new String[] { "Cp850", "Cp437" }), new MysqlCharset("dos", 1, 0, new String[] { "Cp850", "Cp437" }, 4, 0), new MysqlCharset("cp852", 1, 0, new String[] { "Cp852" }), new MysqlCharset("keybcs2", 1, 0, new String[] { "Cp852" }), new MysqlCharset("cp866", 1, 0, new String[] { "Cp866" }), new MysqlCharset("koi8_ru", 1, 0, new String[] { "KOI8_R" }, 4, 0), new MysqlCharset("koi8r", 1, 1, new String[] { "KOI8_R" }), new MysqlCharset("koi8u", 1, 0, new String[] { "KOI8_R" }), new MysqlCharset("koi8_ukr", 1, 0, new String[] { "KOI8_R" }, 4, 0), new MysqlCharset("tis620", 1, 0, new String[] { "TIS620" }), new MysqlCharset("cp1250", 1, 0, new String[] { "Cp1250" }), new MysqlCharset("win1250", 1, 0, new String[] { "Cp1250" }, 4, 0), new MysqlCharset("cp1251", 1, 1, new String[] { "Cp1251" }), new MysqlCharset("win1251", 1, 0, new String[] { "Cp1251" }, 4, 0), new MysqlCharset("cp1251cias", 1, 0, new String[] { "Cp1251" }, 4, 0), new MysqlCharset("cp1251csas", 1, 0, new String[] { "Cp1251" }, 4, 0), new MysqlCharset("win1251ukr", 1, 0, new String[] { "Cp1251" }, 4, 0), new MysqlCharset("cp1256", 1, 0, new String[] { "Cp1256" }), new MysqlCharset("cp1257", 1, 0, new String[] { "Cp1257" }), new MysqlCharset("macroman", 1, 0, new String[] { "MacRoman" }), new MysqlCharset("macce", 1, 0, new String[] { "MacCentralEurope" }), new MysqlCharset("utf8", 3, 1, new String[] { "UTF-8" }), new MysqlCharset("utf8mb4", 4, 0, new String[] { "UTF-8" }), new MysqlCharset("ucs2", 2, 0, new String[] { "UnicodeBig" }), new MysqlCharset("binary", 1, 1, new String[] { "ISO8859_1" }), new MysqlCharset("latin1_de", 1, 0, new String[] { "ISO8859_1" }, 4, 0), new MysqlCharset("german1", 1, 0, new String[] { "ISO8859_1" }, 4, 0), new MysqlCharset("danish", 1, 0, new String[] { "ISO8859_1" }, 4, 0), new MysqlCharset("utf16", 4, 0, new String[] { "UTF-16" }), new MysqlCharset("utf16le", 4, 0, new String[] { "UTF-16LE" }), new MysqlCharset("utf32", 4, 0, new String[] { "UTF-32" }) };
    


















































































    HashMap<String, MysqlCharset> charsetNameToMysqlCharsetMap = new HashMap();
    HashMap<String, List<MysqlCharset>> javaUcToMysqlCharsetMap = new HashMap();
    Set<String> tempMultibyteEncodings = new HashSet();
    Set<String> tempEscapeEncodings = new HashSet();
    for (int i = 0; i < charset.length; i++) {
      String charsetName = charsetName;
      
      charsetNameToMysqlCharsetMap.put(charsetName, charset[i]);
      
      numberOfEncodingsConfigured += javaEncodingsUc.size();
      
      for (String encUC : javaEncodingsUc)
      {

        List<MysqlCharset> charsets = (List)javaUcToMysqlCharsetMap.get(encUC);
        if (charsets == null) {
          charsets = new ArrayList();
          javaUcToMysqlCharsetMap.put(encUC, charsets);
        }
        charsets.add(charset[i]);
        

        if (mblen > 1) {
          tempMultibyteEncodings.add(encUC);
        }
      }
      



      if ((charsetName.equals("big5")) || (charsetName.equals("gbk")) || (charsetName.equals("sjis"))) {
        tempEscapeEncodings.addAll(javaEncodingsUc);
      }
    }
    
    CHARSET_NAME_TO_CHARSET = Collections.unmodifiableMap(charsetNameToMysqlCharsetMap);
    JAVA_ENCODING_UC_TO_MYSQL_CHARSET = Collections.unmodifiableMap(javaUcToMysqlCharsetMap);
    MULTIBYTE_ENCODINGS = Collections.unmodifiableSet(tempMultibyteEncodings);
    ESCAPE_ENCODINGS = Collections.unmodifiableSet(tempEscapeEncodings);
    

    Collation[] collation = new Collation['ࠀ'];
    collation[1] = new Collation(1, "big5_chinese_ci", 1, "big5");
    collation[2] = new Collation(2, "latin2_czech_cs", 0, "latin2");
    collation[3] = new Collation(3, "dec8_swedish_ci", 0, "dec8");
    collation[4] = new Collation(4, "cp850_general_ci", 1, "cp850");
    collation[5] = new Collation(5, "latin1_german1_ci", 1, "latin1");
    collation[6] = new Collation(6, "hp8_english_ci", 0, "hp8");
    collation[7] = new Collation(7, "koi8r_general_ci", 0, "koi8r");
    collation[8] = new Collation(8, "latin1_swedish_ci", 0, "latin1");
    collation[9] = new Collation(9, "latin2_general_ci", 1, "latin2");
    collation[10] = new Collation(10, "swe7_swedish_ci", 0, "swe7");
    collation[11] = new Collation(11, "ascii_general_ci", 0, "ascii");
    collation[12] = new Collation(12, "ujis_japanese_ci", 0, "ujis");
    collation[13] = new Collation(13, "sjis_japanese_ci", 0, "sjis");
    collation[14] = new Collation(14, "cp1251_bulgarian_ci", 0, "cp1251");
    collation[15] = new Collation(15, "latin1_danish_ci", 0, "latin1");
    collation[16] = new Collation(16, "hebrew_general_ci", 0, "hebrew");
    collation[17] = new Collation(17, "latin1_german1_ci", 0, "win1251");
    collation[18] = new Collation(18, "tis620_thai_ci", 0, "tis620");
    collation[19] = new Collation(19, "euckr_korean_ci", 0, "euckr");
    collation[20] = new Collation(20, "latin7_estonian_cs", 0, "latin7");
    collation[21] = new Collation(21, "latin2_hungarian_ci", 0, "latin2");
    collation[22] = new Collation(22, "koi8u_general_ci", 0, "koi8u");
    collation[23] = new Collation(23, "cp1251_ukrainian_ci", 0, "cp1251");
    collation[24] = new Collation(24, "gb2312_chinese_ci", 0, "gb2312");
    collation[25] = new Collation(25, "greek_general_ci", 0, "greek");
    collation[26] = new Collation(26, "cp1250_general_ci", 1, "cp1250");
    collation[27] = new Collation(27, "latin2_croatian_ci", 0, "latin2");
    collation[28] = new Collation(28, "gbk_chinese_ci", 1, "gbk");
    collation[29] = new Collation(29, "cp1257_lithuanian_ci", 0, "cp1257");
    collation[30] = new Collation(30, "latin5_turkish_ci", 1, "latin5");
    collation[31] = new Collation(31, "latin1_german2_ci", 0, "latin1");
    collation[32] = new Collation(32, "armscii8_general_ci", 0, "armscii8");
    collation[33] = new Collation(33, "utf8_general_ci", 1, "utf8");
    collation[34] = new Collation(34, "cp1250_czech_cs", 0, "cp1250");
    collation[35] = new Collation(35, "ucs2_general_ci", 1, "ucs2");
    collation[36] = new Collation(36, "cp866_general_ci", 1, "cp866");
    collation[37] = new Collation(37, "keybcs2_general_ci", 1, "keybcs2");
    collation[38] = new Collation(38, "macce_general_ci", 1, "macce");
    collation[39] = new Collation(39, "macroman_general_ci", 1, "macroman");
    collation[40] = new Collation(40, "cp852_general_ci", 1, "cp852");
    collation[41] = new Collation(41, "latin7_general_ci", 1, "latin7");
    collation[42] = new Collation(42, "latin7_general_cs", 0, "latin7");
    collation[43] = new Collation(43, "macce_bin", 0, "macce");
    collation[44] = new Collation(44, "cp1250_croatian_ci", 0, "cp1250");
    collation[45] = new Collation(45, "utf8mb4_general_ci", 1, "utf8mb4");
    collation[46] = new Collation(46, "utf8mb4_bin", 0, "utf8mb4");
    collation[47] = new Collation(47, "latin1_bin", 0, "latin1");
    collation[48] = new Collation(48, "latin1_general_ci", 0, "latin1");
    collation[49] = new Collation(49, "latin1_general_cs", 0, "latin1");
    collation[50] = new Collation(50, "cp1251_bin", 0, "cp1251");
    collation[51] = new Collation(51, "cp1251_general_ci", 1, "cp1251");
    collation[52] = new Collation(52, "cp1251_general_cs", 0, "cp1251");
    collation[53] = new Collation(53, "macroman_bin", 0, "macroman");
    collation[54] = new Collation(54, "utf16_general_ci", 1, "utf16");
    collation[55] = new Collation(55, "utf16_bin", 0, "utf16");
    collation[56] = new Collation(56, "utf16le_general_ci", 1, "utf16le");
    collation[57] = new Collation(57, "cp1256_general_ci", 1, "cp1256");
    collation[58] = new Collation(58, "cp1257_bin", 0, "cp1257");
    collation[59] = new Collation(59, "cp1257_general_ci", 1, "cp1257");
    collation[60] = new Collation(60, "utf32_general_ci", 1, "utf32");
    collation[61] = new Collation(61, "utf32_bin", 0, "utf32");
    collation[62] = new Collation(62, "utf16le_bin", 0, "utf16le");
    collation[63] = new Collation(63, "binary", 1, "binary");
    collation[64] = new Collation(64, "armscii8_bin", 0, "armscii8");
    collation[65] = new Collation(65, "ascii_bin", 0, "ascii");
    collation[66] = new Collation(66, "cp1250_bin", 0, "cp1250");
    collation[67] = new Collation(67, "cp1256_bin", 0, "cp1256");
    collation[68] = new Collation(68, "cp866_bin", 0, "cp866");
    collation[69] = new Collation(69, "dec8_bin", 0, "dec8");
    collation[70] = new Collation(70, "greek_bin", 0, "greek");
    collation[71] = new Collation(71, "hebrew_bin", 0, "hebrew");
    collation[72] = new Collation(72, "hp8_bin", 0, "hp8");
    collation[73] = new Collation(73, "keybcs2_bin", 0, "keybcs2");
    collation[74] = new Collation(74, "koi8r_bin", 0, "koi8r");
    collation[75] = new Collation(75, "koi8u_bin", 0, "koi8u");
    collation[76] = new Collation(76, "utf8_tolower_ci", 0, "utf8");
    collation[77] = new Collation(77, "latin2_bin", 0, "latin2");
    collation[78] = new Collation(78, "latin5_bin", 0, "latin5");
    collation[79] = new Collation(79, "latin7_bin", 0, "latin7");
    collation[80] = new Collation(80, "cp850_bin", 0, "cp850");
    collation[81] = new Collation(81, "cp852_bin", 0, "cp852");
    collation[82] = new Collation(82, "swe7_bin", 0, "swe7");
    collation[83] = new Collation(83, "utf8_bin", 0, "utf8");
    collation[84] = new Collation(84, "big5_bin", 0, "big5");
    collation[85] = new Collation(85, "euckr_bin", 0, "euckr");
    collation[86] = new Collation(86, "gb2312_bin", 0, "gb2312");
    collation[87] = new Collation(87, "gbk_bin", 0, "gbk");
    collation[88] = new Collation(88, "sjis_bin", 0, "sjis");
    collation[89] = new Collation(89, "tis620_bin", 0, "tis620");
    collation[90] = new Collation(90, "ucs2_bin", 0, "ucs2");
    collation[91] = new Collation(91, "ujis_bin", 0, "ujis");
    collation[92] = new Collation(92, "geostd8_general_ci", 0, "geostd8");
    collation[93] = new Collation(93, "geostd8_bin", 0, "geostd8");
    collation[94] = new Collation(94, "latin1_spanish_ci", 0, "latin1");
    collation[95] = new Collation(95, "cp932_japanese_ci", 1, "cp932");
    collation[96] = new Collation(96, "cp932_bin", 0, "cp932");
    collation[97] = new Collation(97, "eucjpms_japanese_ci", 1, "eucjpms");
    collation[98] = new Collation(98, "eucjpms_bin", 0, "eucjpms");
    collation[99] = new Collation(99, "cp1250_polish_ci", 0, "cp1250");
    
    collation[101] = new Collation(101, "utf16_unicode_ci", 0, "utf16");
    collation[102] = new Collation(102, "utf16_icelandic_ci", 0, "utf16");
    collation[103] = new Collation(103, "utf16_latvian_ci", 0, "utf16");
    collation[104] = new Collation(104, "utf16_romanian_ci", 0, "utf16");
    collation[105] = new Collation(105, "utf16_slovenian_ci", 0, "utf16");
    collation[106] = new Collation(106, "utf16_polish_ci", 0, "utf16");
    collation[107] = new Collation(107, "utf16_estonian_ci", 0, "utf16");
    collation[108] = new Collation(108, "utf16_spanish_ci", 0, "utf16");
    collation[109] = new Collation(109, "utf16_swedish_ci", 0, "utf16");
    collation[110] = new Collation(110, "utf16_turkish_ci", 0, "utf16");
    collation[111] = new Collation(111, "utf16_czech_ci", 0, "utf16");
    collation[112] = new Collation(112, "utf16_danish_ci", 0, "utf16");
    collation[113] = new Collation(113, "utf16_lithuanian_ci", 0, "utf16");
    collation[114] = new Collation(114, "utf16_slovak_ci", 0, "utf16");
    collation[115] = new Collation(115, "utf16_spanish2_ci", 0, "utf16");
    collation[116] = new Collation(116, "utf16_roman_ci", 0, "utf16");
    collation[117] = new Collation(117, "utf16_persian_ci", 0, "utf16");
    collation[118] = new Collation(118, "utf16_esperanto_ci", 0, "utf16");
    collation[119] = new Collation(119, "utf16_hungarian_ci", 0, "utf16");
    collation[120] = new Collation(120, "utf16_sinhala_ci", 0, "utf16");
    collation[121] = new Collation(121, "utf16_german2_ci", 0, "utf16");
    collation[122] = new Collation(122, "utf16_croatian_ci", 0, "utf16");
    collation[123] = new Collation(123, "utf16_unicode_520_ci", 0, "utf16");
    collation[124] = new Collation(124, "utf16_vietnamese_ci", 0, "utf16");
    
    collation[''] = new Collation(128, "ucs2_unicode_ci", 0, "ucs2");
    collation[''] = new Collation(129, "ucs2_icelandic_ci", 0, "ucs2");
    collation[''] = new Collation(130, "ucs2_latvian_ci", 0, "ucs2");
    collation[''] = new Collation(131, "ucs2_romanian_ci", 0, "ucs2");
    collation[''] = new Collation(132, "ucs2_slovenian_ci", 0, "ucs2");
    collation[''] = new Collation(133, "ucs2_polish_ci", 0, "ucs2");
    collation[''] = new Collation(134, "ucs2_estonian_ci", 0, "ucs2");
    collation[''] = new Collation(135, "ucs2_spanish_ci", 0, "ucs2");
    collation[''] = new Collation(136, "ucs2_swedish_ci", 0, "ucs2");
    collation[''] = new Collation(137, "ucs2_turkish_ci", 0, "ucs2");
    collation[''] = new Collation(138, "ucs2_czech_ci", 0, "ucs2");
    collation[''] = new Collation(139, "ucs2_danish_ci", 0, "ucs2");
    collation[''] = new Collation(140, "ucs2_lithuanian_ci", 0, "ucs2");
    collation[''] = new Collation(141, "ucs2_slovak_ci", 0, "ucs2");
    collation[''] = new Collation(142, "ucs2_spanish2_ci", 0, "ucs2");
    collation[''] = new Collation(143, "ucs2_roman_ci", 0, "ucs2");
    collation[''] = new Collation(144, "ucs2_persian_ci", 0, "ucs2");
    collation[''] = new Collation(145, "ucs2_esperanto_ci", 0, "ucs2");
    collation[''] = new Collation(146, "ucs2_hungarian_ci", 0, "ucs2");
    collation[''] = new Collation(147, "ucs2_sinhala_ci", 0, "ucs2");
    collation[''] = new Collation(148, "ucs2_german2_ci", 0, "ucs2");
    collation[''] = new Collation(149, "ucs2_croatian_ci", 0, "ucs2");
    collation[''] = new Collation(150, "ucs2_unicode_520_ci", 0, "ucs2");
    collation[''] = new Collation(151, "ucs2_vietnamese_ci", 0, "ucs2");
    
    collation[''] = new Collation(159, "ucs2_general_mysql500_ci", 0, "ucs2");
    collation[' '] = new Collation(160, "utf32_unicode_ci", 0, "utf32");
    collation['¡'] = new Collation(161, "utf32_icelandic_ci", 0, "utf32");
    collation['¢'] = new Collation(162, "utf32_latvian_ci", 0, "utf32");
    collation['£'] = new Collation(163, "utf32_romanian_ci", 0, "utf32");
    collation['¤'] = new Collation(164, "utf32_slovenian_ci", 0, "utf32");
    collation['¥'] = new Collation(165, "utf32_polish_ci", 0, "utf32");
    collation['¦'] = new Collation(166, "utf32_estonian_ci", 0, "utf32");
    collation['§'] = new Collation(167, "utf32_spanish_ci", 0, "utf32");
    collation['¨'] = new Collation(168, "utf32_swedish_ci", 0, "utf32");
    collation['©'] = new Collation(169, "utf32_turkish_ci", 0, "utf32");
    collation['ª'] = new Collation(170, "utf32_czech_ci", 0, "utf32");
    collation['«'] = new Collation(171, "utf32_danish_ci", 0, "utf32");
    collation['¬'] = new Collation(172, "utf32_lithuanian_ci", 0, "utf32");
    collation['­'] = new Collation(173, "utf32_slovak_ci", 0, "utf32");
    collation['®'] = new Collation(174, "utf32_spanish2_ci", 0, "utf32");
    collation['¯'] = new Collation(175, "utf32_roman_ci", 0, "utf32");
    collation['°'] = new Collation(176, "utf32_persian_ci", 0, "utf32");
    collation['±'] = new Collation(177, "utf32_esperanto_ci", 0, "utf32");
    collation['²'] = new Collation(178, "utf32_hungarian_ci", 0, "utf32");
    collation['³'] = new Collation(179, "utf32_sinhala_ci", 0, "utf32");
    collation['´'] = new Collation(180, "utf32_german2_ci", 0, "utf32");
    collation['µ'] = new Collation(181, "utf32_croatian_ci", 0, "utf32");
    collation['¶'] = new Collation(182, "utf32_unicode_520_ci", 0, "utf32");
    collation['·'] = new Collation(183, "utf32_vietnamese_ci", 0, "utf32");
    
    collation['À'] = new Collation(192, "utf8_unicode_ci", 0, "utf8");
    collation['Á'] = new Collation(193, "utf8_icelandic_ci", 0, "utf8");
    collation['Â'] = new Collation(194, "utf8_latvian_ci", 0, "utf8");
    collation['Ã'] = new Collation(195, "utf8_romanian_ci", 0, "utf8");
    collation['Ä'] = new Collation(196, "utf8_slovenian_ci", 0, "utf8");
    collation['Å'] = new Collation(197, "utf8_polish_ci", 0, "utf8");
    collation['Æ'] = new Collation(198, "utf8_estonian_ci", 0, "utf8");
    collation['Ç'] = new Collation(199, "utf8_spanish_ci", 0, "utf8");
    collation['È'] = new Collation(200, "utf8_swedish_ci", 0, "utf8");
    collation['É'] = new Collation(201, "utf8_turkish_ci", 0, "utf8");
    collation['Ê'] = new Collation(202, "utf8_czech_ci", 0, "utf8");
    collation['Ë'] = new Collation(203, "utf8_danish_ci", 0, "utf8");
    collation['Ì'] = new Collation(204, "utf8_lithuanian_ci", 0, "utf8");
    collation['Í'] = new Collation(205, "utf8_slovak_ci", 0, "utf8");
    collation['Î'] = new Collation(206, "utf8_spanish2_ci", 0, "utf8");
    collation['Ï'] = new Collation(207, "utf8_roman_ci", 0, "utf8");
    collation['Ð'] = new Collation(208, "utf8_persian_ci", 0, "utf8");
    collation['Ñ'] = new Collation(209, "utf8_esperanto_ci", 0, "utf8");
    collation['Ò'] = new Collation(210, "utf8_hungarian_ci", 0, "utf8");
    collation['Ó'] = new Collation(211, "utf8_sinhala_ci", 0, "utf8");
    collation['Ô'] = new Collation(212, "utf8_german2_ci", 0, "utf8");
    collation['Õ'] = new Collation(213, "utf8_croatian_ci", 0, "utf8");
    collation['Ö'] = new Collation(214, "utf8_unicode_520_ci", 0, "utf8");
    collation['×'] = new Collation(215, "utf8_vietnamese_ci", 0, "utf8");
    
    collation['ß'] = new Collation(223, "utf8_general_mysql500_ci", 0, "utf8");
    collation['à'] = new Collation(224, "utf8mb4_unicode_ci", 0, "utf8mb4");
    collation['á'] = new Collation(225, "utf8mb4_icelandic_ci", 0, "utf8mb4");
    collation['â'] = new Collation(226, "utf8mb4_latvian_ci", 0, "utf8mb4");
    collation['ã'] = new Collation(227, "utf8mb4_romanian_ci", 0, "utf8mb4");
    collation['ä'] = new Collation(228, "utf8mb4_slovenian_ci", 0, "utf8mb4");
    collation['å'] = new Collation(229, "utf8mb4_polish_ci", 0, "utf8mb4");
    collation['æ'] = new Collation(230, "utf8mb4_estonian_ci", 0, "utf8mb4");
    collation['ç'] = new Collation(231, "utf8mb4_spanish_ci", 0, "utf8mb4");
    collation['è'] = new Collation(232, "utf8mb4_swedish_ci", 0, "utf8mb4");
    collation['é'] = new Collation(233, "utf8mb4_turkish_ci", 0, "utf8mb4");
    collation['ê'] = new Collation(234, "utf8mb4_czech_ci", 0, "utf8mb4");
    collation['ë'] = new Collation(235, "utf8mb4_danish_ci", 0, "utf8mb4");
    collation['ì'] = new Collation(236, "utf8mb4_lithuanian_ci", 0, "utf8mb4");
    collation['í'] = new Collation(237, "utf8mb4_slovak_ci", 0, "utf8mb4");
    collation['î'] = new Collation(238, "utf8mb4_spanish2_ci", 0, "utf8mb4");
    collation['ï'] = new Collation(239, "utf8mb4_roman_ci", 0, "utf8mb4");
    collation['ð'] = new Collation(240, "utf8mb4_persian_ci", 0, "utf8mb4");
    collation['ñ'] = new Collation(241, "utf8mb4_esperanto_ci", 0, "utf8mb4");
    collation['ò'] = new Collation(242, "utf8mb4_hungarian_ci", 0, "utf8mb4");
    collation['ó'] = new Collation(243, "utf8mb4_sinhala_ci", 0, "utf8mb4");
    collation['ô'] = new Collation(244, "utf8mb4_german2_ci", 0, "utf8mb4");
    collation['õ'] = new Collation(245, "utf8mb4_croatian_ci", 0, "utf8mb4");
    collation['ö'] = new Collation(246, "utf8mb4_unicode_520_ci", 0, "utf8mb4");
    collation['÷'] = new Collation(247, "utf8mb4_vietnamese_ci", 0, "utf8mb4");
    collation['ø'] = new Collation(248, "gb18030_chinese_ci", 1, "gb18030");
    collation['ù'] = new Collation(249, "gb18030_bin", 0, "gb18030");
    collation['ú'] = new Collation(250, "gb18030_unicode_520_ci", 0, "gb18030");
    
    collation['ÿ'] = new Collation(255, "utf8mb4_0900_ai_ci", 0, "utf8mb4");
    collation['Ā'] = new Collation(256, "utf8mb4_de_pb_0900_ai_ci", 0, "utf8mb4");
    collation['ā'] = new Collation(257, "utf8mb4_is_0900_ai_ci", 0, "utf8mb4");
    collation['Ă'] = new Collation(258, "utf8mb4_lv_0900_ai_ci", 0, "utf8mb4");
    collation['ă'] = new Collation(259, "utf8mb4_ro_0900_ai_ci", 0, "utf8mb4");
    collation['Ą'] = new Collation(260, "utf8mb4_sl_0900_ai_ci", 0, "utf8mb4");
    collation['ą'] = new Collation(261, "utf8mb4_pl_0900_ai_ci", 0, "utf8mb4");
    collation['Ć'] = new Collation(262, "utf8mb4_et_0900_ai_ci", 0, "utf8mb4");
    collation['ć'] = new Collation(263, "utf8mb4_es_0900_ai_ci", 0, "utf8mb4");
    collation['Ĉ'] = new Collation(264, "utf8mb4_sv_0900_ai_ci", 0, "utf8mb4");
    collation['ĉ'] = new Collation(265, "utf8mb4_tr_0900_ai_ci", 0, "utf8mb4");
    collation['Ċ'] = new Collation(266, "utf8mb4_cs_0900_ai_ci", 0, "utf8mb4");
    collation['ċ'] = new Collation(267, "utf8mb4_da_0900_ai_ci", 0, "utf8mb4");
    collation['Č'] = new Collation(268, "utf8mb4_lt_0900_ai_ci", 0, "utf8mb4");
    collation['č'] = new Collation(269, "utf8mb4_sk_0900_ai_ci", 0, "utf8mb4");
    collation['Ď'] = new Collation(270, "utf8mb4_es_trad_0900_ai_ci", 0, "utf8mb4");
    collation['ď'] = new Collation(271, "utf8mb4_la_0900_ai_ci", 0, "utf8mb4");
    
    collation['đ'] = new Collation(273, "utf8mb4_eo_0900_ai_ci", 0, "utf8mb4");
    collation['Ē'] = new Collation(274, "utf8mb4_hu_0900_ai_ci", 0, "utf8mb4");
    collation['ē'] = new Collation(275, "utf8mb4_hr_0900_ai_ci", 0, "utf8mb4");
    
    collation['ĕ'] = new Collation(277, "utf8mb4_vi_0900_ai_ci", 0, "utf8mb4");
    collation['Ė'] = new Collation(278, "utf8mb4_0900_as_cs", 0, "utf8mb4");
    collation['ė'] = new Collation(279, "utf8mb4_de_pb_0900_as_cs", 0, "utf8mb4");
    collation['Ę'] = new Collation(280, "utf8mb4_is_0900_as_cs", 0, "utf8mb4");
    collation['ę'] = new Collation(281, "utf8mb4_lv_0900_as_cs", 0, "utf8mb4");
    collation['Ě'] = new Collation(282, "utf8mb4_ro_0900_as_cs", 0, "utf8mb4");
    collation['ě'] = new Collation(283, "utf8mb4_sl_0900_as_cs", 0, "utf8mb4");
    collation['Ĝ'] = new Collation(284, "utf8mb4_pl_0900_as_cs", 0, "utf8mb4");
    collation['ĝ'] = new Collation(285, "utf8mb4_et_0900_as_cs", 0, "utf8mb4");
    collation['Ğ'] = new Collation(286, "utf8mb4_es_0900_as_cs", 0, "utf8mb4");
    collation['ğ'] = new Collation(287, "utf8mb4_sv_0900_as_cs", 0, "utf8mb4");
    collation['Ġ'] = new Collation(288, "utf8mb4_tr_0900_as_cs", 0, "utf8mb4");
    collation['ġ'] = new Collation(289, "utf8mb4_cs_0900_as_cs", 0, "utf8mb4");
    collation['Ģ'] = new Collation(290, "utf8mb4_da_0900_as_cs", 0, "utf8mb4");
    collation['ģ'] = new Collation(291, "utf8mb4_lt_0900_as_cs", 0, "utf8mb4");
    collation['Ĥ'] = new Collation(292, "utf8mb4_sk_0900_as_cs", 0, "utf8mb4");
    collation['ĥ'] = new Collation(293, "utf8mb4_es_trad_0900_as_cs", 0, "utf8mb4");
    collation['Ħ'] = new Collation(294, "utf8mb4_la_0900_as_cs", 0, "utf8mb4");
    
    collation['Ĩ'] = new Collation(296, "utf8mb4_eo_0900_as_cs", 0, "utf8mb4");
    collation['ĩ'] = new Collation(297, "utf8mb4_hu_0900_as_cs", 0, "utf8mb4");
    collation['Ī'] = new Collation(298, "utf8mb4_hr_0900_as_cs", 0, "utf8mb4");
    
    collation['Ĭ'] = new Collation(300, "utf8mb4_vi_0900_as_cs", 0, "utf8mb4");
    
    collation['į'] = new Collation(303, "utf8mb4_ja_0900_as_cs", 0, "utf8mb4");
    collation['İ'] = new Collation(304, "utf8mb4_ja_0900_as_cs_ks", 0, "utf8mb4");
    collation['ı'] = new Collation(305, "utf8mb4_0900_as_ci", 0, "utf8mb4");
    collation['Ĳ'] = new Collation(306, "utf8mb4_ru_0900_ai_ci", 0, "utf8mb4");
    collation['ĳ'] = new Collation(307, "utf8mb4_ru_0900_as_cs", 0, "utf8mb4");
    
    collation['ņ'] = new Collation(326, "utf8mb4_test_ci", 0, "utf8mb4");
    collation['Ň'] = new Collation(327, "utf16_test_ci", 0, "utf16");
    collation['ň'] = new Collation(328, "utf8mb4_test_400_ci", 0, "utf8mb4");
    
    collation['Ő'] = new Collation(336, "utf8_bengali_standard_ci", 0, "utf8");
    collation['ő'] = new Collation(337, "utf8_bengali_traditional_ci", 0, "utf8");
    
    collation['Š'] = new Collation(352, "utf8_phone_ci", 0, "utf8");
    collation['š'] = new Collation(353, "utf8_test_ci", 0, "utf8");
    collation['Ţ'] = new Collation(354, "utf8_5624_1", 0, "utf8");
    collation['ţ'] = new Collation(355, "utf8_5624_2", 0, "utf8");
    collation['Ť'] = new Collation(356, "utf8_5624_3", 0, "utf8");
    collation['ť'] = new Collation(357, "utf8_5624_4", 0, "utf8");
    collation['Ŧ'] = new Collation(358, "ucs2_test_ci", 0, "ucs2");
    collation['ŧ'] = new Collation(359, "ucs2_vn_ci", 0, "ucs2");
    collation['Ũ'] = new Collation(360, "ucs2_5624_1", 0, "ucs2");
    
    collation['Ű'] = new Collation(368, "utf8_5624_5", 0, "utf8");
    collation['Ƈ'] = new Collation(391, "utf32_test_ci", 0, "utf32");
    collation['߿'] = new Collation(2047, "utf8_maxuserid_ci", 0, "utf8");
    
    COLLATION_INDEX_TO_COLLATION_NAME = new String['ࠀ'];
    COLLATION_INDEX_TO_CHARSET = new MysqlCharset['ࠀ'];
    Map<String, Integer> charsetNameToCollationIndexMap = new TreeMap();
    Map<String, Integer> charsetNameToCollationPriorityMap = new TreeMap();
    Set<Integer> tempUTF8MB4Indexes = new HashSet();
    
    Collation notUsedCollation = new Collation(0, "none", 0, "latin1");
    for (int i = 1; i < 2048; i++) {
      Collation coll = collation[i] != null ? collation[i] : notUsedCollation;
      COLLATION_INDEX_TO_COLLATION_NAME[i] = collationName;
      COLLATION_INDEX_TO_CHARSET[i] = mysqlCharset;
      String charsetName = mysqlCharset.charsetName;
      
      if ((!charsetNameToCollationIndexMap.containsKey(charsetName)) || (((Integer)charsetNameToCollationPriorityMap.get(charsetName)).intValue() < priority)) {
        charsetNameToCollationIndexMap.put(charsetName, Integer.valueOf(i));
        charsetNameToCollationPriorityMap.put(charsetName, Integer.valueOf(priority));
      }
      

      if (charsetName.equals("utf8mb4")) {
        tempUTF8MB4Indexes.add(Integer.valueOf(i));
      }
    }
    
    CHARSET_NAME_TO_COLLATION_INDEX = Collections.unmodifiableMap(charsetNameToCollationIndexMap);
    UTF8MB4_INDEXES = Collections.unmodifiableSet(tempUTF8MB4Indexes);
    



    Map<String, String> tempMap = new HashMap();
    tempMap.put("czech", "latin2");
    tempMap.put("danish", "latin1");
    tempMap.put("dutch", "latin1");
    tempMap.put("english", "latin1");
    tempMap.put("estonian", "latin7");
    tempMap.put("french", "latin1");
    tempMap.put("german", "latin1");
    tempMap.put("greek", "greek");
    tempMap.put("hungarian", "latin2");
    tempMap.put("italian", "latin1");
    tempMap.put("japanese", "ujis");
    tempMap.put("japanese-sjis", "sjis");
    tempMap.put("korean", "euckr");
    tempMap.put("norwegian", "latin1");
    tempMap.put("norwegian-ny", "latin1");
    tempMap.put("polish", "latin2");
    tempMap.put("portuguese", "latin1");
    tempMap.put("romanian", "latin2");
    tempMap.put("russian", "koi8r");
    tempMap.put("serbian", "cp1250");
    tempMap.put("slovak", "latin2");
    tempMap.put("spanish", "latin1");
    tempMap.put("swedish", "latin1");
    tempMap.put("ukrainian", "koi8u");
    ERROR_MESSAGE_FILE_TO_MYSQL_CHARSET = Collections.unmodifiableMap(tempMap);
    
    collation = null;
  }
  
  public static final String getMysqlCharsetForJavaEncoding(String javaEncoding, Connection conn) throws SQLException
  {
    try {
      List<MysqlCharset> mysqlCharsets = (List)JAVA_ENCODING_UC_TO_MYSQL_CHARSET.get(javaEncoding.toUpperCase(Locale.ENGLISH));
      
      if (mysqlCharsets != null) {
        Iterator<MysqlCharset> iter = mysqlCharsets.iterator();
        
        MysqlCharset versionedProp = null;
        
        while (iter.hasNext()) {
          MysqlCharset charset = (MysqlCharset)iter.next();
          
          if (conn == null)
          {

            return charsetName;
          }
          
          if ((versionedProp == null) || (major < major) || (minor < minor) || (subminor < subminor) || ((priority < priority) && (major == major) && (minor == minor) && (subminor == subminor)))
          {

            if (charset.isOkayForVersion(conn)) {
              versionedProp = charset;
            }
          }
        }
        
        if (versionedProp != null) {
          return charsetName;
        }
      }
      
      return null;
    } catch (SQLException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1009", null);
      sqlEx.initCause(ex);
      throw sqlEx;
    }
  }
  
  public static int getCollationIndexForJavaEncoding(String javaEncoding, java.sql.Connection conn) throws SQLException
  {
    String charsetName = getMysqlCharsetForJavaEncoding(javaEncoding, (Connection)conn);
    if (charsetName != null) {
      Integer ci = (Integer)CHARSET_NAME_TO_COLLATION_INDEX.get(charsetName);
      if (ci != null) {
        return ci.intValue();
      }
    }
    return 0;
  }
  
  public static String getMysqlCharsetNameForCollationIndex(Integer collationIndex) {
    if ((collationIndex != null) && (collationIndex.intValue() > 0) && (collationIndex.intValue() < 2048)) {
      return COLLATION_INDEX_TO_CHARSETintValuecharsetName;
    }
    return null;
  }
  













  public static String getJavaEncodingForMysqlCharset(String mysqlCharsetName, String javaEncoding)
  {
    String res = javaEncoding;
    MysqlCharset cs = (MysqlCharset)CHARSET_NAME_TO_CHARSET.get(mysqlCharsetName);
    if (cs != null) {
      res = cs.getMatchingJavaEncoding(javaEncoding);
    }
    return res;
  }
  
  public static String getJavaEncodingForMysqlCharset(String mysqlCharsetName) {
    return getJavaEncodingForMysqlCharset(mysqlCharsetName, null);
  }
  
  public static String getJavaEncodingForCollationIndex(Integer collationIndex, String javaEncoding) {
    if ((collationIndex != null) && (collationIndex.intValue() > 0) && (collationIndex.intValue() < 2048)) {
      MysqlCharset cs = COLLATION_INDEX_TO_CHARSET[collationIndex.intValue()];
      return cs.getMatchingJavaEncoding(javaEncoding);
    }
    return null;
  }
  
  public static String getJavaEncodingForCollationIndex(Integer collationIndex) {
    return getJavaEncodingForCollationIndex(collationIndex, null);
  }
  
  static final int getNumberOfCharsetsConfigured() {
    return numberOfEncodingsConfigured;
  }
  














  static final String getCharacterEncodingForErrorMessages(ConnectionImpl conn)
    throws SQLException
  {
    if (conn.versionMeetsMinimum(5, 5, 0)) {
      String errorMessageCharsetName = conn.getServerVariable("jdbc.local.character_set_results");
      if (errorMessageCharsetName != null) {
        String javaEncoding = getJavaEncodingForMysqlCharset(errorMessageCharsetName);
        if (javaEncoding != null) {
          return javaEncoding;
        }
      }
      
      return "UTF-8";
    }
    
    String errorMessageFile = conn.getServerVariable("language");
    
    if ((errorMessageFile == null) || (errorMessageFile.length() == 0))
    {
      return "Cp1252";
    }
    
    int endWithoutSlash = errorMessageFile.length();
    
    if ((errorMessageFile.endsWith("/")) || (errorMessageFile.endsWith("\\"))) {
      endWithoutSlash--;
    }
    
    int lastSlashIndex = errorMessageFile.lastIndexOf('/', endWithoutSlash - 1);
    
    if (lastSlashIndex == -1) {
      lastSlashIndex = errorMessageFile.lastIndexOf('\\', endWithoutSlash - 1);
    }
    
    if (lastSlashIndex == -1) {
      lastSlashIndex = 0;
    }
    
    if ((lastSlashIndex == endWithoutSlash) || (endWithoutSlash < lastSlashIndex))
    {
      return "Cp1252";
    }
    
    errorMessageFile = errorMessageFile.substring(lastSlashIndex + 1, endWithoutSlash);
    
    String errorMessageEncodingMysql = (String)ERROR_MESSAGE_FILE_TO_MYSQL_CHARSET.get(errorMessageFile);
    
    if (errorMessageEncodingMysql == null)
    {
      return "Cp1252";
    }
    
    String javaEncoding = getJavaEncodingForMysqlCharset(errorMessageEncodingMysql);
    
    if (javaEncoding == null)
    {
      return "Cp1252";
    }
    
    return javaEncoding;
  }
  
  static final boolean requiresEscapeEasternUnicode(String javaEncodingName) {
    return ESCAPE_ENCODINGS.contains(javaEncodingName.toUpperCase(Locale.ENGLISH));
  }
  




  public static final boolean isMultibyteCharset(String javaEncodingName)
  {
    return MULTIBYTE_ENCODINGS.contains(javaEncodingName.toUpperCase(Locale.ENGLISH));
  }
  
  public static int getMblen(String charsetName) {
    if (charsetName != null) {
      MysqlCharset cs = (MysqlCharset)CHARSET_NAME_TO_CHARSET.get(charsetName);
      if (cs != null) {
        return mblen;
      }
    }
    return 0;
  }
  
  public CharsetMapping() {}
}
