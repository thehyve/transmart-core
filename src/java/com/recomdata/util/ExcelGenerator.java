package com.recomdata.util;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Florian on 05/01/14.
 */
public class ExcelGenerator {

    public static byte[] generateExcel(List headers, List values) {

        ExcelSheet sheet = new ExcelSheet("Sheet 1", headers, values);

        List<ExcelSheet> sheets = new ArrayList<ExcelSheet>();
        sheets.add(sheet);

        return generateExcel(sheets);

    }

    public static byte[] generateExcel(List sheets) {

        HSSFWorkbook wb = new HSSFWorkbook();

        for (ExcelSheet sheet : (List<ExcelSheet>) sheets) {
            HSSFSheet s = wb.createSheet(sheet.getName());
            HSSFFont f = wb.createFont();
            HSSFCellStyle cs = wb.createCellStyle();
            HSSFFont f2 = wb.createFont();

            //set font 1 to 12 point type
            f.setFontHeightInPoints((short) 10);
            //make it blue
            f.setColor(HSSFColor.BLACK.index);
            //arial is the default font
            f.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
            cs.setFont(f);
            cs.setFillForegroundColor((HSSFColor.LIGHT_BLUE.index));
            cs.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

            HSSFRow headerRow = s.createRow(0);
            short hCount = 0;
            for (String title : (List<String>) sheet.getHeaders()) {
                HSSFCell cell = headerRow.createCell((short) hCount);
                cell.setCellValue(title);
                cell.setCellStyle(cs);
                hCount++;
            }

            HSSFCellStyle csWrapText = wb.createCellStyle();
            csWrapText.setWrapText(true);

            short rowCount = 1;
            short cellMax = 0;
            for (Object value : sheet.getValues()) {
                HSSFRow row = s.createRow((short) rowCount);
                short cellCount = 0;
                for (Object v : (List) value) {
                    HSSFCell dcell = row.createCell((short) cellCount);
                    if (v == null || (v instanceof String && v.toString().trim().length() == 0)) {
                        // println("empty cell");
                    } else {
                        try {
                            Double d = Double.parseDouble(v.toString());
                            dcell.setCellValue(d);
                        } catch (NumberFormatException nfe) {
                            dcell.setCellValue(v.toString());
                            if (v.toString().length() > 100) {
                                dcell.setCellStyle(csWrapText);
                            }
                        }
                    }
                    cellCount++;
                }
                cellMax = cellMax < cellCount ? cellCount : cellMax;
                rowCount++;
            }

            for (short cnt = 0; cnt < cellMax; cnt++) {
                s.autoSizeColumn(cnt);
            }
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            wb.write(output);
        } catch (IOException ex) {
            // TODO: log error
        }
        return output.toByteArray();

    }

}

