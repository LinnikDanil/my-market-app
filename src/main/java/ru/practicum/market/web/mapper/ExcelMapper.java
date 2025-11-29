package ru.practicum.market.web.mapper;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import ru.practicum.market.domain.exception.ItemUploadException;
import ru.practicum.market.domain.model.Item;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelMapper {
    private static final String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String SHEET = "Items";

    public static void checkExcelFormat(MultipartFile file) {
        if (!TYPE.equals(file.getContentType())) {
            throw new ItemUploadException("Please upload an excel file!");
        }
    }

    public static List<Item> excelToItemList(InputStream is) {
        try {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheet(SHEET);
            Iterator<Row> rows = sheet.iterator();
            List<Item> itemList = new ArrayList<>();
            int rowNumber = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();
                // skip header
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }
                Iterator<Cell> cellsInRow = currentRow.iterator();
                Item item = new Item();
                int cellIdx = 0;
                while (cellsInRow.hasNext()) {
                    Cell currentCell = cellsInRow.next();
                    switch (cellIdx) {
                        case 0 -> item.setTitle(currentCell.getStringCellValue());
                        case 1 -> item.setDescription(currentCell.getStringCellValue());
                        case 2 -> item.setPrice((long) currentCell.getNumericCellValue());
                    }
                    cellIdx++;
                }
                itemList.add(item);
            }
            workbook.close();
            return itemList;
        } catch (IOException e) {
            throw new RuntimeException("fail to parse Excel file: " + e.getMessage());
        }
    }
}
