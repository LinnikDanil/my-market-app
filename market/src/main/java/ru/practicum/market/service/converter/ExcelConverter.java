package ru.practicum.market.service.converter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.practicum.market.domain.exception.ItemUploadException;
import ru.practicum.market.domain.model.Item;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ExcelConverter {
    private static final String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String SHEET = "Items";

    public void checkExcelFormat(FilePart file) {
        var contentType = file.headers().getContentType();
        if (contentType == null || !TYPE.equals(contentType.toString())) {
            throw new ItemUploadException("Please upload an excel file!");
        }
    }

    public Mono<List<Item>> excelToItemList(FilePart file) {
        return DataBufferUtils.join(file.content())
                .flatMap(dataBuffer -> Mono.fromCallable(() -> {
                            try (InputStream is = dataBuffer.asInputStream()) {
                                return parseWorkbook(is);
                            } finally {
                                DataBufferUtils.release(dataBuffer);
                            }
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                );
    }

    private List<Item> parseWorkbook(InputStream is) {
        try (Workbook workbook = new XSSFWorkbook(is)) {
            var sheet = workbook.getSheet(SHEET);
            if (sheet == null) {
                throw new ItemUploadException("Sheet '%s' not found in Excel file".formatted(SHEET));
            }

            var rows = sheet.iterator();
            if (!rows.hasNext()) {
                return List.of(); // пустой файл
            }

            // 1. читаем header
            var headerRow = rows.next();
            var columnIndexes = mapColumns(headerRow);
            validateRequiredColumns(columnIndexes);

            // 2. читаем данные
            List<Item> itemList = new ArrayList<>();
            while (rows.hasNext()) {
                var currentRow = rows.next();
                if (isRowEmpty(currentRow)) {
                    continue;
                }

                var item = new Item();
                item.setTitle(getStringCell(currentRow, columnIndexes.get(ExcelItemColumn.TITLE)));
                item.setDescription(getStringCell(currentRow, columnIndexes.get(ExcelItemColumn.DESCRIPTION)));
                item.setPrice((long) getNumericCell(currentRow, columnIndexes.get(ExcelItemColumn.PRICE)));

                itemList.add(item);
            }

            return itemList;
        } catch (IOException e) {
            throw new ItemUploadException("Fail to parse Excel file: " + e.getMessage(), e);
        }
    }

    private Map<ExcelItemColumn, Integer> mapColumns(Row headerRow) {
        Map<ExcelItemColumn, Integer> map = new HashMap<>();
        for (Cell cell : headerRow) {
            var header = cell.getStringCellValue().trim();
            for (ExcelItemColumn c : ExcelItemColumn.values()) {
                if (c.getHeader().equalsIgnoreCase(header)) {
                    map.put(c, cell.getColumnIndex());
                }
            }
        }
        return map;
    }

    private void validateRequiredColumns(Map<ExcelItemColumn, Integer> columns) {
        for (ExcelItemColumn col : ExcelItemColumn.values()) {
            if (!columns.containsKey(col)) {
                throw new ItemUploadException(
                        "Required column '%s' is missing in Excel header".formatted(col.name()));
            }
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            var cell = row.getCell(i);
            if (cell != null && cell.getCellType() != org.apache.poi.ss.usermodel.CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private String getStringCell(Row row, int columnIndex) {
        var cell = row.getCell(columnIndex);
        return cell == null ? null : cell.getStringCellValue();
    }

    private double getNumericCell(Row row, int columnIndex) {
        var cell = row.getCell(columnIndex);
        if (cell == null) {
            throw new ItemUploadException("Numeric cell at column %d is null".formatted(columnIndex));
        }
        return cell.getNumericCellValue();
    }
}
